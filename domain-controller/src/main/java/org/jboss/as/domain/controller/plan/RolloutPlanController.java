/**
 *
 */
package org.jboss.as.domain.controller.plan;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONCURRENT_GROUPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.GRACEFUL_SHUTDOWN_TIMEOUT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.IN_SERIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX_FAILED_SERVERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX_FAILURE_PERCENTAGE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESPONSE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLBACK_ACROSS_GROUPS;
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
import java.util.concurrent.ExecutorService;

import org.jboss.as.controller.ResultHandler;
import org.jboss.as.domain.controller.HostControllerClient;
import org.jboss.as.domain.controller.ServerIdentity;
import org.jboss.as.domain.controller.plan.AbstractServerUpdateTask.ServerUpdateResultHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * TODO add class javadoc for RolloutPlanController
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class RolloutPlanController implements ServerUpdateResultHandler {

    private final Map<String, Map<ServerIdentity, ModelNode>> opsByGroup;
    private final ModelNode rolloutPlan;
    private final ResultHandler resultHandler;
    private final boolean rollbackAcrossGroups;
    private final ExecutorService executor;
    private final Runnable rootTask;
    private final Map<String, ServerUpdatePolicy> updatePolicies = new HashMap<String, ServerUpdatePolicy>();
    private final boolean shutdown;
    private final long gracefulShutdownPeriod;
    private final Map<String, HostControllerClient> hostControllerClients;

    public RolloutPlanController(final Map<String, Map<ServerIdentity, ModelNode>> opsByGroup,
            final ModelNode rolloutPlan, final ResultHandler resultHandler,
            final Map<String, HostControllerClient> hostControllerClients, final ExecutorService executor) {

        this.executor = executor;
        this.opsByGroup = opsByGroup;
        this.rolloutPlan = rolloutPlan;
        this.resultHandler = resultHandler;
        this.hostControllerClients = hostControllerClients;
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

    public void execute() {
        this.rootTask.run();
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

    @Override
    public void handleServerUpdateResult(ServerIdentity serverId, ModelNode response) {
        String[] location = { SERVER_GROUPS, serverId.getServerGroupName(), serverId.getServerName(), HOST };
        resultHandler.handleResultFragment(location, new ModelNode().set(serverId.getHostName()));
        location[3] = RESPONSE;
        resultHandler.handleResultFragment(location, response);
    }
}
