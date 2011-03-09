/**
 *
 */
package org.jboss.as.domain.controller.plan;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CANCELLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPENSATING_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONCURRENT_GROUPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.GRACEFUL_SHUTDOWN_TIMEOUT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.IN_SERIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX_FAILED_SERVERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX_FAILURE_PERCENTAGE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESPONSE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLBACK_ACROSS_GROUPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLED_BACK;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLING_TO_SERVERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SHUTDOWN;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;

import org.jboss.as.controller.ResultHandler;
import org.jboss.as.domain.controller.HostControllerClient;
import org.jboss.as.domain.controller.ServerIdentity;
import org.jboss.as.domain.controller.plan.AbstractServerUpdateTask.ServerUpdateResultHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * Coordinates rolling out a series of operations to the servers specified in a rollout plan.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class RolloutPlanController implements ServerUpdateResultHandler {

    public static enum Result {
        SUCCESS,
        PARTIAL,
        FAILED
    }

    private final ModelNode rolloutPlan;
    private final ResultHandler resultHandler;
    private final boolean rollbackAcrossGroups;
    private final ExecutorService executor;
    private final Runnable rootTask;
    private final Map<String, ServerUpdatePolicy> updatePolicies = new HashMap<String, ServerUpdatePolicy>();
    private final boolean shutdown;
    private final long gracefulShutdownPeriod;
    private final Map<String, HostControllerClient> hostControllerClients;
    private final ConcurrentMap<String, Map<ServerIdentity, ModelNode>> serverResults = new ConcurrentHashMap<String, Map<ServerIdentity, ModelNode>>();
    private final boolean forRollback;

    public RolloutPlanController(final Map<String, Map<ServerIdentity, ModelNode>> opsByGroup,
            final ModelNode rolloutPlan, final ResultHandler resultHandler,
            final Map<String, HostControllerClient> hostControllerClients, final ExecutorService executor, boolean forRollback) {

        this.executor = executor;
        this.rolloutPlan = rolloutPlan;
        this.resultHandler = resultHandler;
        this.hostControllerClients = hostControllerClients;
        this.forRollback = forRollback;

        this.rollbackAcrossGroups = !rolloutPlan.hasDefined(ROLLBACK_ACROSS_GROUPS) || rolloutPlan.get(ROLLBACK_ACROSS_GROUPS).asBoolean();
        this.shutdown = rolloutPlan.hasDefined(SHUTDOWN) && rolloutPlan.get(SHUTDOWN).asBoolean();
        this.gracefulShutdownPeriod = rolloutPlan.hasDefined(GRACEFUL_SHUTDOWN_TIMEOUT) ? rolloutPlan.get(GRACEFUL_SHUTDOWN_TIMEOUT).asInt() : -1;

        final List<Runnable> rollingTasks = new ArrayList<Runnable>();
        this.rootTask = new RollingUpdateTask(rollingTasks);

        if (rolloutPlan.hasDefined(IN_SERIES)) {
            ConcurrentGroupServerUpdatePolicy predecessor = null;
            for (ModelNode series : rolloutPlan.get(IN_SERIES).asList()) {

                final List<Runnable> seriesTasks = new ArrayList<Runnable>();
                rollingTasks.add(new ConcurrentUpdateTask(seriesTasks, executor));

                Set<String> groupNames = new HashSet<String>();
                List<Property> groupPolicies = new ArrayList<Property>();
                if (series.hasDefined(CONCURRENT_GROUPS)) {
                    for (Property pol : series.get(CONCURRENT_GROUPS).asPropertyList()) {
                        groupNames.add(pol.getName());
                        groupPolicies.add(pol);
                    }
                }
                else {
                    Property pol = series.require(SERVER_GROUP).asProperty();
                    groupNames.add(pol.getName());
                    groupPolicies.add(pol);
                }

                ConcurrentGroupServerUpdatePolicy parent = new ConcurrentGroupServerUpdatePolicy(predecessor, groupNames);
                for (Property prop : groupPolicies) {

                    final List<Runnable> groupTasks = new ArrayList<Runnable>();
                    final ModelNode policyNode = prop.getValue();
                    final boolean rollingGroup = policyNode.hasDefined(ROLLING_TO_SERVERS) && policyNode.get(ROLLING_TO_SERVERS).asBoolean();
                    seriesTasks.add(rollingGroup ? new RollingUpdateTask(groupTasks) : new ConcurrentUpdateTask(groupTasks, executor));

                    final String serverGroupName = prop.getName();
                    final Map<ServerIdentity, ModelNode> groupEntry = opsByGroup.get(serverGroupName);
                    final Set<ServerIdentity> servers = groupEntry.keySet();
                    ServerUpdatePolicy policy;
                    if (forRollback) {
                        policy = new ServerUpdatePolicy(parent, serverGroupName, servers);
                    }
                    else {
                        int maxFailures = 0;
                        if (policyNode.hasDefined(MAX_FAILURE_PERCENTAGE)) {
                            int pct = policyNode.get(MAX_FAILURE_PERCENTAGE).asInt();
                            maxFailures = ((servers.size() * pct) / 100);
                        }
                        else if (policyNode.hasDefined(MAX_FAILED_SERVERS)) {
                            maxFailures = policyNode.get(MAX_FAILED_SERVERS).asInt();
                        }
                        policy = new ServerUpdatePolicy(parent, serverGroupName, servers, maxFailures);
                    }
                    updatePolicies.put(serverGroupName, policy);

                    for (Map.Entry<ServerIdentity, ModelNode> entry : groupEntry.entrySet()) {
                        groupTasks.add(createServerTask(entry.getKey(), entry.getValue(), policy));
                    }
                }
            }
        }
    }

    public Result execute() {
        this.rootTask.run();

        Result result = null;
        for (ServerUpdatePolicy policy : updatePolicies.values()) {
            if (policy.isFailed()) {
                result = (result == null || result == Result.FAILED) ? Result.FAILED : Result.PARTIAL;
            }
            else {
                result = (result == null || result == Result.SUCCESS) ? Result.SUCCESS : Result.PARTIAL;
            }
        }

        return result;
    }

    public Result rollback() {
        if (forRollback) {
            throw new IllegalStateException("Cannot call rollback() on a controller that itself is managing a rollback");
        }
        RolloutPlanController rollbackController = createRollbackController();
        return rollbackController.execute();
    }

    @Override
    public void handleServerUpdateResult(ServerIdentity serverId, ModelNode response) {
        String[] location = { SERVER_GROUPS, serverId.getServerGroupName(), serverId.getServerName(), HOST };
        resultHandler.handleResultFragment(location, new ModelNode().set(serverId.getHostName()));
        location[3] = RESPONSE;
        resultHandler.handleResultFragment(location, response);

        Map<ServerIdentity, ModelNode> groupResults = serverResults.get(serverId.getServerGroupName());
        if (groupResults == null) {
            groupResults = new ConcurrentHashMap<ServerIdentity, ModelNode>();
        }
        Map<ServerIdentity, ModelNode> existing = serverResults.putIfAbsent(serverId.getServerGroupName(), groupResults);
        if (existing != null) {
            groupResults = existing;
        }
        groupResults.put(serverId, response);
    }

    private Runnable createServerTask(final ServerIdentity serverIdentity, final ModelNode serverOp, final ServerUpdatePolicy policy) {
        Runnable result;
        HostControllerClient client = hostControllerClients.get(serverIdentity.getHostName());
        if (client == null) {
            // TODO host disappeared
            result = new Runnable() {
                @Override
                public void run() {}
            };
        }
        else if (shutdown) {
            result = new ServerRestartTask(client, serverIdentity, policy, this, gracefulShutdownPeriod);
        }
        else {
            result = new RunningServerUpdateTask(client, serverIdentity, serverOp, policy, this);
        }
        return result;
    }

    private RolloutPlanController createRollbackController() {
        Map<String, Map<ServerIdentity, ModelNode>> rollbackOpsByGroup = new HashMap<String, Map<ServerIdentity, ModelNode>>();

        for (Map.Entry<String, ServerUpdatePolicy> entry : updatePolicies.entrySet()) {
            if (rollbackAcrossGroups || entry.getValue().isFailed()) {
                Map<ServerIdentity, ModelNode> groupResults = serverResults.get(entry.getKey());
                for (Map.Entry<ServerIdentity, ModelNode> serverEntry : groupResults.entrySet()) {
                    ModelNode serverResult = serverEntry.getValue();
                    if (needsRollback(serverResult) && serverResult.hasDefined(COMPENSATING_OPERATION)) {
                        String groupName = serverEntry.getKey().getServerGroupName();
                        Map<ServerIdentity, ModelNode> groupRollbacks = rollbackOpsByGroup.get(groupName);
                        if (groupRollbacks == null) {
                            groupRollbacks = new HashMap<ServerIdentity, ModelNode>();
                            rollbackOpsByGroup.put(groupName, groupRollbacks);
                        }
                        groupRollbacks.put(serverEntry.getKey(), serverResult.get(COMPENSATING_OPERATION));
                    }
                }
            }
        }

        ModelNode rollbackRolloutPlan = new ModelNode();
        rollbackRolloutPlan.get(ROLLBACK_ACROSS_GROUPS).set(false);
        for (ModelNode series : rolloutPlan.get(IN_SERIES).asList()) {
            if (series.hasDefined(CONCURRENT_GROUPS)) {
                ModelNode item = null;
                for (Property prop : series.get(SERVER_GROUP).asPropertyList()) {
                    if (rollbackOpsByGroup.containsKey(prop.getName())) {
                        ModelNode rollbackPolicy = getRollbackPolicy(prop.getValue());
                        if (item == null) {
                            item = new ModelNode();
                        }
                        item.get(prop.getName()).set(rollbackPolicy);
                    }
                }
                rollbackRolloutPlan.get(IN_SERIES).add().get(CONCURRENT_GROUPS).set(item);
            }
            else {
                Property prop = series.get(SERVER_GROUP).asProperty();
                if (rollbackOpsByGroup.containsKey(prop.getName())) {
                    ModelNode rollbackPolicy = getRollbackPolicy(prop.getValue());
                    rollbackRolloutPlan.get(IN_SERIES).add().get(SERVER_GROUP, prop.getName()).set(rollbackPolicy);
                }
            }
        }

        return new RolloutPlanController(rollbackOpsByGroup, rollbackRolloutPlan, this.resultHandler, this.hostControllerClients, this.executor, true);
    }

    private boolean needsRollback(ModelNode serverResult) {
        String outcome = serverResult.require(OUTCOME).asString();
        if (CANCELLED.equals(outcome)) {
            return false;
        }
        if (serverResult.hasDefined(ROLLED_BACK) && serverResult.get(ROLLED_BACK).asBoolean()) {
            return false;
        }
        // TODO what about rollback-failure-description? For now we'll just try again
        return true;
    }

    private ModelNode getRollbackPolicy(ModelNode preRollback) {
        ModelNode result = new ModelNode();
        if (preRollback.hasDefined(ROLLING_TO_SERVERS)) {
            result.get(ROLLING_TO_SERVERS).set(preRollback.get(ROLLING_TO_SERVERS));
        }
        result.get(MAX_FAILURE_PERCENTAGE).set(100);
        return result;
    }
}
