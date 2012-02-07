/**
 *
 */
package org.jboss.as.domain.controller.plan;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONCURRENT_GROUPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.GRACEFUL_SHUTDOWN_TIMEOUT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.IN_SERIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX_FAILED_SERVERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX_FAILURE_PERCENTAGE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLBACK_ACROSS_GROUPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLING_TO_SERVERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SHUTDOWN;
import static org.jboss.as.domain.controller.DomainControllerLogger.HOST_CONTROLLER_LOGGER;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;

import org.jboss.as.domain.controller.ServerIdentity;
import org.jboss.as.domain.controller.operations.coordination.DomainOperationContext;
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

    private final boolean rollbackAcrossGroups;
    private final Runnable rootTask;
    private final Map<String, ServerUpdatePolicy> updatePolicies = new HashMap<String, ServerUpdatePolicy>();
    private final boolean shutdown;
    private final long gracefulShutdownPeriod;
    private final ServerOperationExecutor serverOperationExecutor;
    private final DomainOperationContext domainOperationContext;
    private final ConcurrentMap<String, Map<ServerIdentity, ModelNode>> serverResults = new ConcurrentHashMap<String, Map<ServerIdentity, ModelNode>>();

    public RolloutPlanController(final Map<String, Map<ServerIdentity, ModelNode>> opsByGroup,
                                 final ModelNode rolloutPlan,
                                 final DomainOperationContext domainOperationContext,
                                 final ServerOperationExecutor serverOperationExecutor,
                                 final ExecutorService executor) {
        this.domainOperationContext = domainOperationContext;
        this.serverOperationExecutor = serverOperationExecutor;

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

                    final String serverGroupName = prop.getName();
                    final Map<ServerIdentity, ModelNode> groupEntry = opsByGroup.get(serverGroupName);
                    if (groupEntry == null) {
                        continue;
                    }

                    final List<Runnable> groupTasks = new ArrayList<Runnable>();
                    final ModelNode policyNode = prop.getValue();
                    final boolean rollingGroup = policyNode.hasDefined(ROLLING_TO_SERVERS) && policyNode.get(ROLLING_TO_SERVERS).asBoolean();
                    seriesTasks.add(rollingGroup ? new RollingUpdateTask(groupTasks) : new ConcurrentUpdateTask(groupTasks, executor));

                    final Set<ServerIdentity> servers = groupEntry.keySet();
                    int maxFailures = 0;
                    if (policyNode.hasDefined(MAX_FAILURE_PERCENTAGE)) {
                        int pct = policyNode.get(MAX_FAILURE_PERCENTAGE).asInt();
                        maxFailures = ((servers.size() * pct) / 100);
                    }
                    else if (policyNode.hasDefined(MAX_FAILED_SERVERS)) {
                        maxFailures = policyNode.get(MAX_FAILED_SERVERS).asInt();
                    }
                    ServerUpdatePolicy policy = new ServerUpdatePolicy(parent, serverGroupName, servers, maxFailures);

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
                domainOperationContext.setServerGroupRollback(policy.getServerGroupName(), true);
                if (rollbackAcrossGroups) {
                    domainOperationContext.setCompleteRollback(true);
                }
                result = (result == null || result == Result.FAILED) ? Result.FAILED : Result.PARTIAL;
            }
            else {
                domainOperationContext.setServerGroupRollback(policy.getServerGroupName(), false);
                result = (result == null || result == Result.SUCCESS) ? Result.SUCCESS : Result.PARTIAL;
            }
        }

        return result;
    }

    @Override
    public void handleServerUpdateResult(ServerIdentity serverId, ModelNode response) {
        if (HOST_CONTROLLER_LOGGER.isTraceEnabled()) {
            HOST_CONTROLLER_LOGGER.tracef("From %s received %s", serverId, response);
        }
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
        if (shutdown) {
            result = new ServerRestartTask(serverOperationExecutor, serverIdentity, policy, this, gracefulShutdownPeriod);
        }
        else {
            result = new RunningServerUpdateTask(serverOperationExecutor, serverIdentity, serverOp, policy, this);
        }
        return result;
    }
}
