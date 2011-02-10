package org.jboss.as.domain.controller.legacy;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

import org.jboss.as.domain.client.api.DomainUpdateApplier;
import org.jboss.as.domain.client.api.DomainUpdateResult;
import org.jboss.as.domain.client.api.HostUpdateResult;
import org.jboss.as.domain.client.api.ServerIdentity;
import org.jboss.as.domain.client.api.ServerStatus;
import org.jboss.as.domain.client.api.deployment.DeploymentPlan;
import org.jboss.as.domain.client.impl.DomainUpdateApplierResponse;
import org.jboss.as.model.AbstractDomainModelUpdate;
import org.jboss.as.model.AbstractHostModelUpdate;
import org.jboss.as.model.AbstractServerModelUpdate;
import org.jboss.as.model.DeploymentUnitElement;
import org.jboss.as.model.DomainModel;
import org.jboss.as.model.HostModel;
import org.jboss.as.model.ServerModel;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.as.model.UpdateResultHandlerResponse;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;

/**
 * Service that acts as the primary management control point for a domain.
 *
 * @author John Bailey
 * @author Brian Stansberry
 */
public interface DomainController {

    /**
     * {@link ServiceName} under which a DomainController instance should be registered
     * with the service container of a Host Controller that is acting as the domain controller.
     */
    ServiceName SERVICE_NAME = ServiceName.JBOSS.append("domain", "controller");

    void execute(final ModelNode request, final Queue<ModelNode> responseQueue);

    /**
     * Registers a Host Controller with this domain controller.
     *
     * @param hostControllerClient client the domain controller can use to communicate with the Host Controller.
     */
    void addClient(final HostControllerClient hostControllerClient);

    /**
     * Deregisters a previously registered Host Controller.
     *
     * @param id the {@link HostControllerClient#getId() id} of the previously
     *           registered Host Controller
     */
    void removeClient(final String id);

    /**
     * Gets the current domain configuration.
     *
     * @return the configuration. Will not return <code>null</code>
     */
    // FIXME remove: this is just another operation to execute
    DomainModel getDomainModel();

    /**
     * Gets the {@link HostModel#getName() names} of the currently registered
     * Host Controllers.
     *
     * @return the names, or an empty set if no host controllers are registered.
     */
    // FIXME remove: this is just another operation to execute
    Set<String> getHostControllerNames();

    /**
     * Gets the current configuration for the given Host Controller.
     *
     * @param hostControllerName the {@link HostModel#getName() name} of the Host Controller
     *
     * @return the host controller configuration, or <code>null</code> if no
     *         Host Controller with the given name is currently registered
     */
    // FIXME remove: this is just another operation to execute
    HostModel getHostModel(final String hostControllerName);

    /**
     * Gets the status of all servers known to the currently registered
     * Host Controllers.
     *
     * @return map keyed by the {@link ServerIdentity fully-specified identity} of the servers
     *        with the server's current {@link ServerStatus} status as the value.
     */
    // FIXME remove: this is just another operation to execute
    Map<ServerIdentity, ServerStatus> getServerStatuses();

    /**
     * Gets the current running configuration for a server.
     *
     * @param hostControllerName the {@link HostModel#getName() name} of the Host Controller responsible for the server
     * @param serverName the  name of the server
     *
     * @return the current server configuration, or <code>null</code> if the Host Controller isn't currently
     *          registered or the server isn't started
     */
    // FIXME remove: this is just another operation to execute
    ServerModel getServerModel(final String hostControllerName, final String serverName);

    /**
     * Attempts to start a server.
     *
     * @param hostControllerName the {@link HostModel#getName() name} of the Host Controller responsible for the server
     * @param serverName the name of the server
     *
     * @return the status of the server after the attempt to start it
     */
    // FIXME remove: this is just another operation to execute
    ServerStatus startServer(final String hostControllerName, final String serverName);

    /**
     * Attempts to stop a currently running server.
     *
     * @param hostControllerName the {@link HostModel#getName() name} of the Host Controller responsible for the server
     * @param serverName the name of the server
     * @param maximum period, in milliseconds, the server should wait for any long-running work to gracefully complete
     *           before proceeding with the shutdown. A value of {@code -1} means an attempt at graceful shutdown
     *           is not required
     *
     * @return the status of the server after the attempt to stop it
     */
    // FIXME remove: this is just another operation to execute
    ServerStatus stopServer(final String hostControllerName, final String serverName, final long gracefulTimeout);

    /**
     * Attempts to restart a currently running server.
     *
     * @param hostControllerName the {@link HostModel#getName() name} of the Host Controller responsible for the server
     * @param serverName the name of the server
     * @param maximum period, in milliseconds, the server should wait for any long-running work to gracefully complete
     *           before proceeding with the restart. A value of {@code -1} means an attempt at graceful shutdown
     *           is not required
     *
     * @return the status of the server after the attempt to restart it
     */
    // FIXME remove: this is just another operation to execute
    ServerStatus restartServer(final String hostControllerName, final String serverName, final long gracefulTimeout);

    /**
     * Apply a list of updates to the domain model, pushing the change out to
     * all registered Host Controllers and to all currently running servers. Updates
     * will be rolled to servers one at a time. Failure of an update on a server
     * will not trigger rollback of previously executed updates on that server,
     * although it will prevent application of subsequent updates. Failure to
     * apply updates on one server will not trigger rollback of the updates on
     * other servers, nor will it prevent application of the updates to subsequent
     * servers.
     *
     * @param updates the updates. Cannot be <code>null</code>
     *
     * @return the results of the updates
     */
    // FIXME remove: this is just another operation to execute
    List<DomainUpdateResult<?>> applyUpdates(List<AbstractDomainModelUpdate<?>> updates);

    /**
     * Apply a single update to the domain model, pushing the change out to
     * all registered Host Controllers and to all currently running servers. The update
     * will be rolled to servers one at a time. Failure to apply the update on
     * one server will not trigger rollback of the updates on other servers,
     * nor will it prevent application of the updates to subsequent servers.
     *
     * @param T the type of the result object generated by the server update
     * @param update the update. Cannot be <code>null</code>
     *
     * @return the result of the update
     */
    // FIXME remove: this is just another operation to execute
    <T> DomainUpdateResult<T> applyUpdate(AbstractDomainModelUpdate<T> update);

    /**
     * Apply a list of updates to the domain model, pushing the change out to
     * all registered Host Controllers but <strong>not</strong> to the currently
     * running servers.
     *
     * @param updates the updates. Cannot be <code>null</code>
     *
     * @return the results of the updates in a form appropriate for providing
     *          input to a {@link DomainUpdateApplier}'s callbacks
     */
    // FIXME remove as the only usage is DomainDeploymentHandler, which should disappear
    List<DomainUpdateApplierResponse> applyUpdatesToModel(final List<AbstractDomainModelUpdate<?>> updates);

    /**
     * Apply an update to the domain model, pushing the change out to
     * all registered Host Controllers but <strong>not</strong> to the currently
     * running servers.
     *
     * @param update the update. Cannot be <code>null</code>
     *
     * @return the results of the update in a form appropriate for providing
     *          input to a {@link DomainUpdateApplier}'s callbacks
     */
    // FIXME -- remove -- clients don't get to control the overall process of applying updates
    // across the domain
    DomainUpdateApplierResponse applyUpdateToModel(AbstractDomainModelUpdate<?> update);

    /**
     * Apply a list of updates to a Host Controller's {@link HostModel}, pushing
     * the change out to all currently running servers. Updates
     * will be rolled to servers one at a time. Failure of an update on a server
     * will not trigger rollback of previously executed updates on that server,
     * although it will prevent application of subsequent updates. Failure to
     * apply updates on one server will not trigger rollback of the updates on
     * other servers, nor will it prevent application of the updates to subsequent
     * servers.
     *
     * @param hostControllerName the host controller name
     * @param updates the updates. Cannot be <code>null</code>
     *
     * @return the results of the updates
     */
    // FIXME remove: this is just another operation to execute
    List<HostUpdateResult<?>> applyHostUpdates(String hostControllerName, List<AbstractHostModelUpdate<?>> updates);

    /**
     * Push a list of updates out to a running server.
     *
     * @param server the server. Cannot be <code>null</code>
     * @param updates the updates. Cannot be <code>null</code>
     * @param allowOverallRollback <code>true</code> if successfully applied updates should be
     *            rolled back if an update later in the list cannot be applied successfully
     *
     * @return the result of the update in a form appropriate for providing input
     *          to an {@link UpdateResultHandler}'s callbacks
     */
    // FIXME -- remove -- clients don't get to control the overall process of applying updates
    // across the domain
    List<UpdateResultHandlerResponse<?>> applyUpdatesToServer(final ServerIdentity server,
            final List<AbstractServerModelUpdate<?>> updates, final boolean allowOverallRollback);

    /**
     * Gets whether a given deployment name would be unique in the domain.
     *
     * @param deploymentName the {@link DeploymentUnitElement#getUniqueName() unique name}
     *                       of the deployment. Cannot be <code>null</code>
     *
     * @return <code>true</code> if there is no deployment with the given name
     *         currently registered with the domain; <code>false</code> otherwise
     */
    // FIXME remove: this is just another operation to execute
    boolean isDeploymentNameUnique(String deploymentName);

    /**
     * Add deployment content to the domain's content repository and determine
     * the hash of the content
     *
     * @param uniqueName the uniqueName of the content
     * @param runtimeName the name by which this content should be known to
     *                     runtime services
     * @param stream InputStream from which the content can be read
     *
     * @return the hash of the content
     *
     * @throws IOException if there is a problem reading or storing the content
     */
    // TODO figure out how to handle this
    byte[] addDeploymentContent(final String uniqueName, final String runtimeName, final InputStream stream)
    throws IOException;

    /**
     * Executes a deployment plan, pushing progress updates to a queue as the
     * execution proceeds.
     *
     * @param plan the plan. Cannot be <code>null</code>
     * @param responseQueue the queue to which progress updates should be written
     */
    // FIXME remove: this is just another operation to execute
    void executeDeploymentPlan(DeploymentPlan plan, BlockingQueue<List<StreamedResponse>> responseQueue);

}
