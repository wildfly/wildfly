/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.domain.controller.legacy;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.xml.stream.XMLInputFactory;

import org.jboss.as.domain.client.api.DomainUpdateResult;
import org.jboss.as.domain.client.api.HostUpdateResult;
import org.jboss.as.domain.client.api.ServerIdentity;
import org.jboss.as.domain.client.api.ServerStatus;
import org.jboss.as.domain.client.api.deployment.DeploymentPlan;
import org.jboss.as.domain.client.impl.DomainUpdateApplierResponse;
import org.jboss.as.domain.controller.DomainDeploymentRepository;
import org.jboss.as.model.AbstractDomainModelUpdate;
import org.jboss.as.model.AbstractHostModelUpdate;
import org.jboss.as.model.AbstractServerModelUpdate;
import org.jboss.as.model.DomainModel;
import org.jboss.as.model.HostModel;
import org.jboss.as.model.ServerModel;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.model.UpdateResultHandlerResponse;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.staxmapper.XMLMapper;

/**
 * Default implementation of {@link DomainController}.
 *
 * @author John Bailey
 * @author Brian Stansberry
 */
public class DomainControllerImpl implements Service<DomainController>, DomainController {
    private static final Logger log = Logger.getLogger("org.jboss.as.domain.controller");
    private DomainModel domainModel;
    private final ConcurrentMap<String, HostControllerClient> clients = new ConcurrentHashMap<String, HostControllerClient>();
    private final InjectedValue<XMLMapper> xmlMapper = new InjectedValue<XMLMapper>();
    private final InjectedValue<File> domainConfigDir = new InjectedValue<File>();
    private final InjectedValue<File> domainDeploymentsDir = new InjectedValue<File>();
    private final InjectedValue<ScheduledExecutorService> scheduledExecutorService = new InjectedValue<ScheduledExecutorService>();
    private volatile DomainConfigurationPersister configPersister;
    private ScheduledFuture<?> pollingFuture;
    private DomainDeploymentHandler deploymentPlanHandler;
    private DomainDeploymentRepository deploymentRepository;

    public DomainControllerImpl() {
    }

    /**
     * For use in testing.
     * @param configPersister
     */
    public DomainControllerImpl(final DomainConfigurationPersister configPersister, final DomainDeploymentRepository deploymentRepository) {
        assert configPersister != null : "configPersister is null";
        assert deploymentRepository != null : "deploymentRepository is null";
        this.configPersister = configPersister;
        this.deploymentRepository = deploymentRepository;
    }

    // ---------------------------------------------------------------  Service

    /**
     * Start the domain controller with configuration.  This will launch required service for the domain controller.
     */
    @Override
    public synchronized void start(final StartContext context) throws StartException {

        try {
            log.info("Starting Domain Controller");

            if (configPersister == null) {
                configPersister = new DomainConfigurationPersisterImpl(getDomainConfigDir());
            }
            if (deploymentRepository == null) {
                deploymentRepository = new DomainDeploymentRepository(getDomainDeploymentsDir());
            }

            log.info("Parsing Domain Configuration");
            domainModel = parseDomain(xmlMapper.getValue());

            deploymentPlanHandler = new DomainDeploymentHandler(this, scheduledExecutorService.getValue());
            pollingFuture = scheduledExecutorService.getValue().scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    for(final HostControllerClient client : clients.values()) {
                        if(!client.isActive()) {
                            log.warnf("Registered host controller [%s] is no longer active", client.getId());
                        }
                    }
                }
            }, 30L, 30L, TimeUnit.SECONDS);
        } catch (final IllegalStateException e) {
            throw new StartException("Failed to start " + getClass().getSimpleName(), e);
        }
    }

    /**
     * Stop the domain controller
     */
    @Override
    public synchronized void stop(final StopContext stopContext) {
        log.info("Stopping Domain Controller");
        domainModel = null;
        if(pollingFuture != null) {
            pollingFuture.cancel(true);
        }
    }

    @Override
    public DomainControllerImpl getValue() throws IllegalStateException {
        return this;
    }

    // ----------------------------------  Operations invoked by Host Controller

    /* (non-Javadoc)
     * @see org.jboss.as.domain.controller.DomainController#addClient(org.jboss.as.domain.controller.HostControllerClient)
     */
    @Override
    public void addClient(final HostControllerClient domainControllerClient) {
        if(clients.putIfAbsent(domainControllerClient.getId(), domainControllerClient) != null) {
            // TODO: Handle duplicate client
        }
    }

    /* (non-Javadoc)
     * @see org.jboss.as.domain.controller.DomainController#removeClient(java.lang.String)
     */
    @Override
    public void removeClient(final String id) {
        if(clients.remove(id) == null) {
            // TODO: Handle non-existent client
        }
    }

    // -----------------------------------  Operations invoked by DomainClient

    @Override
    public void execute(final ModelNode request, final Queue<ModelNode> responseQueue) {
        // FIXME implemenet execute(ModelNode, Queue<ModelNode)
        throw new UnsupportedOperationException("implement me");
    }

    /* (non-Javadoc)
     * @see org.jboss.as.domain.controller.DomainController#getDomainModel()
     */
    @Override
    public synchronized DomainModel getDomainModel() {
        return domainModel;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.domain.controller.DomainController#getHostControllerNames()
     */
    @Override
    public Set<String> getHostControllerNames() {
        return Collections.unmodifiableSet(clients.keySet());
    }

    /* (non-Javadoc)
     * @see org.jboss.as.domain.controller.DomainController#getHostModel(java.lang.String)
     */
    @Override
    public HostModel getHostModel(final String hostControllerName) {

        final HostControllerClient client = clients.get(hostControllerName);
        if (client == null) {
            return null;
        }
        else {
            return client.getHostModel();
        }
    }

    /* (non-Javadoc)
     * @see org.jboss.as.domain.controller.DomainController#getServerStatuses()
     */
    @Override
    public Map<ServerIdentity, ServerStatus> getServerStatuses() {
        final Map<ServerIdentity, ServerStatus> result = new HashMap<ServerIdentity, ServerStatus>();
        final Map<String, Future<Map<ServerIdentity, ServerStatus>>> futures = new HashMap<String, Future<Map<ServerIdentity, ServerStatus>>>();
        for (final Map.Entry<String, HostControllerClient> entry : clients.entrySet()) {
            final HostControllerClient client = entry.getValue();
            final Callable<Map<ServerIdentity, ServerStatus>> callable = new Callable<Map<ServerIdentity, ServerStatus>>() {

                @Override
                public Map<ServerIdentity, ServerStatus> call() {
                    return client.getServerStatuses();
                }

            };
            futures.put(entry.getKey(), scheduledExecutorService.getValue().submit(callable));
        }

        for (final Map.Entry<String, Future<Map<ServerIdentity, ServerStatus>>> entry : futures.entrySet()) {
            try {
                final Map<ServerIdentity, ServerStatus> map = entry.getValue().get();
                if (map != null) {
                    result.putAll(map);
                }
            } catch (final InterruptedException e) {
                log.errorf("Interrupted while reading server statuses from host controller %s -- aborting", entry.getKey());
                Thread.currentThread().interrupt();
                break;
            } catch (final ExecutionException e) {
                log.errorf(e, "Caught exception while reading server statuses from host controller %s -- ignoring that host controller", entry.getKey());
            }
        }
        return result;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.domain.controller.DomainController#getServerModel(java.lang.String, java.lang.String)
     */
    @Override
    public ServerModel getServerModel(final String hostControllerName, final String serverName) {

        final HostControllerClient client = clients.get(hostControllerName);
        if (client == null) {
            log.debugf("Received getServerModel request for unknown host controller %s", hostControllerName);
            return null;
        }
        else {
            return client.getServerModel(serverName);
        }
    }

    /* (non-Javadoc)
     * @see org.jboss.as.domain.controller.DomainController#startServer(java.lang.String, java.lang.String)
     */
    @Override
    public ServerStatus startServer(final String hostControllerName, final String serverName) {

        final HostControllerClient client = clients.get(hostControllerName);
        if (client == null) {
            log.debugf("Received startServer request for unknown host controller %s", hostControllerName);
            return ServerStatus.UNKNOWN;
        }
        else {
            return client.startServer(serverName);
        }
    }

    /* (non-Javadoc)
     * @see org.jboss.as.domain.controller.DomainController#stopServer(java.lang.String, java.lang.String, long)
     */
    @Override
    public ServerStatus stopServer(final String hostControllerName, final String serverName, final long gracefulTimeout) {

        final HostControllerClient client = clients.get(hostControllerName);
        if (client == null) {
            log.debugf("Received stopServer request for unknown host controller %s", hostControllerName);
            return ServerStatus.UNKNOWN;
        }
        else {
            return client.stopServer(serverName, gracefulTimeout);
        }
    }

    /* (non-Javadoc)
     * @see org.jboss.as.domain.controller.DomainController#restartServer(java.lang.String, java.lang.String, long)
     */
    @Override
    public ServerStatus restartServer(final String hostControllerName, final String serverName, final long gracefulTimeout) {

        final HostControllerClient client = clients.get(hostControllerName);
        if (client == null) {
            log.debugf("Received restartServer request for unknown host controller %s", hostControllerName);
            return ServerStatus.UNKNOWN;
        }
        else {
            return client.restartServer(serverName, gracefulTimeout);
        }
    }

    private DomainModel parseDomain(final XMLMapper mapper) {
        try {
            final InputStream reader = configPersister.getConfigurationInputStream();
            final List<AbstractDomainModelUpdate<?>> domainUpdates = new ArrayList<AbstractDomainModelUpdate<?>>();
            mapper.parseDocument(domainUpdates, XMLInputFactory.newInstance().createXMLStreamReader(new BufferedInputStream(reader)));
            final DomainModel domainModel = new DomainModel();
            for(final AbstractDomainModelUpdate<?> update : domainUpdates) {
                domainModel.update(update);
            }
            return domainModel;
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new RuntimeException("Caught exception during processing of domain.xml", e);
        }
    }

    public Injector<XMLMapper> getXmlMapperInjector() {
        return xmlMapper;
    }

    File getDomainConfigDir() {
        return domainConfigDir.getValue();
    }

    File getDomainDeploymentsDir() {
        return domainDeploymentsDir.getValue();
    }

    public Injector<File> getDomainConfigDirInjector() {
        return domainConfigDir;
    }

    public Injector<File> getDomainDeploymentsDirInjector() {
        return domainDeploymentsDir;
    }

    public Injector<ScheduledExecutorService> getScheduledExecutorServiceInjector() {
        return scheduledExecutorService;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.domain.controller.DomainController#applyUpdates(java.util.List)
     */
    @Override
    public List<DomainUpdateResult<?>> applyUpdates(final List<AbstractDomainModelUpdate<?>> updates) {
        if (updates == null || updates.size() == 0) {
            throw new IllegalArgumentException("updates is " + (updates == null ? "null" : "empty"));
        }
        List<DomainUpdateResult<?>> result;

        final List<DomainUpdateApplierResponse> domainResults = applyUpdatesToModel(updates);

        // Check the last update to verify overall success
        final DomainUpdateApplierResponse last = domainResults.get(domainResults.size() - 1);
        if (last.isCancelled() || last.isRolledBack() || last.getDomainFailure() != null || last.getHostFailures().size() > 0) {
            // Something failed; don't push to servers
            result = new ArrayList<DomainUpdateResult<?>>();
            for (final DomainUpdateApplierResponse duar : domainResults) {
                if (duar.isCancelled()) {
                    result.add(new DomainUpdateResult<Object>(true));
                }
                else if (duar.isRolledBack()) {
                    result.add(new DomainUpdateResult<Object>(false));
                }
                else if (duar.getDomainFailure() != null) {
                    result.add(new DomainUpdateResult<Object>(duar.getDomainFailure()));
                }
                else {
                    result.add(new DomainUpdateResult<Object>(duar.getHostFailures()));
                }
            }
        }
        else {
            // Push to servers
            result = applyUpdatesToServers(updates, domainResults, false);
        }

        return result;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.domain.controller.DomainController#applyUpdate(org.jboss.as.model.AbstractDomainModelUpdate)
     */
    @Override
    public <T> DomainUpdateResult<T> applyUpdate(final AbstractDomainModelUpdate<T> update) {

        @SuppressWarnings("unchecked")
        final
        DomainUpdateResult<T> result = (DomainUpdateResult<T>) applyUpdates(Collections.<AbstractDomainModelUpdate<?>>singletonList(update)).get(0);
        return result;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.domain.controller.DomainController#applyUpdateToModel(org.jboss.as.model.AbstractDomainModelUpdate)
     */
    @Override
    public DomainUpdateApplierResponse applyUpdateToModel(final AbstractDomainModelUpdate<?> update) {
        final List<DomainUpdateApplierResponse> responses = applyUpdatesToModel(Collections.<AbstractDomainModelUpdate<?>>singletonList(update));
        return responses.get(0);
    }

    /* (non-Javadoc)
     * @see org.jboss.as.domain.controller.DomainController#applyUpdatesToModel(java.util.List)
     */
    @Override
    public List<DomainUpdateApplierResponse> applyUpdatesToModel(final List<AbstractDomainModelUpdate<?>> updates) {

        final int updateCount = updates.size();
        log.debugf("Applying %s domain updates", updateCount);

        List<DomainUpdateApplierResponse> result = new ArrayList<DomainUpdateApplierResponse>(updateCount);

        // First we apply updates to our local model copy
        boolean ok = true;
        final List<AbstractDomainModelUpdate<?>> rollbacks = new ArrayList<AbstractDomainModelUpdate<?>>();
        for (final AbstractDomainModelUpdate<?> update : updates) {
            if (ok) {
                try {
                    final AbstractDomainModelUpdate<?> rollback = update.getCompensatingUpdate(domainModel);
                    domainModel.update(update);
                    // Add the rollback after success so we don't rollback
                    // the failed update -- which should not have changed anything
                    rollbacks.add(0, rollback);
                    // Stick in a placeholder result that will survive if
                    // a domain update faiure triggers a rollback or will get replaced with
                    // the final result if we apply to servers
                    result.add(new DomainUpdateApplierResponse(false));
                }
                catch (final UpdateFailedException e) {
                    log.debugf(e, "Failed applying %s", update);
                    ok = false;
                    result.add(new DomainUpdateApplierResponse(e));
                }
            } else {
                // Add a cancellation response
                result.add(new DomainUpdateApplierResponse(true));
            }
        }

        if (!ok) {
            // Apply compensating updates to fix our local model
            for (int i = 0; i < rollbacks.size(); i++) {
                final AbstractDomainModelUpdate<?> rollback = rollbacks.get(i);
                try {
                    domainModel.update(rollback);
                }
                catch (final UpdateFailedException e) {
                    // TODO uh oh. Reload from the file?
                }
            }
        }
        else {
            log.debug("Domain updates applied successfully locally; pushing to host controllers");
            // Persist model
            configPersister.persistConfiguration(domainModel);
            // Move on to host controllers.
            result = applyUpdatesToHostControllers(updates, rollbacks);
        }

        return result;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.domain.controller.DomainController#applyUpdatesToServer(org.jboss.as.domain.client.api.ServerIdentity, java.util.List, boolean)
     */
    @Override
    public List<UpdateResultHandlerResponse<?>> applyUpdatesToServer(final ServerIdentity server, final List<AbstractServerModelUpdate<?>> updates, final boolean allowOverallRollback) {

        final HostControllerClient client = clients.get(server.getHostName());
        List<UpdateResultHandlerResponse<?>> responses;

        if (client == null) {
            log.debugf("Unknown host controller %s", server.getHostName());
            // TODO better handle disappearance of host
            responses = new ArrayList<UpdateResultHandlerResponse<?>>();
            final UpdateResultHandlerResponse<?> failure = UpdateResultHandlerResponse.createFailureResponse(new IllegalStateException("unknown host " + server.getHostName()));
            for (int i = 0; i < updates.size(); i++) {
                responses.add(failure);
            }
        }
        else {
            responses = client.updateServerModel(server.getServerName(), updates, allowOverallRollback);
        }
        return responses;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.domain.controller.DomainController#executeDeploymentPlan(org.jboss.as.domain.client.api.deployment.DeploymentPlan, java.util.concurrent.BlockingQueue)
     */
    @Override
    public void executeDeploymentPlan(final DeploymentPlan plan, final BlockingQueue<List<StreamedResponse>> responseQueue) {
        deploymentPlanHandler.executeDeploymentPlan(plan, responseQueue);
    }

    @Override
    public byte[] addDeploymentContent(final String name, final String runtimeName, final InputStream stream)
        throws IOException {
        if (deploymentRepository == null) {
            throw new IllegalStateException("Must call start before requesting " + DomainDeploymentRepository.class.getSimpleName());
        }
        return deploymentRepository.addDeploymentContent(name, runtimeName, stream);
    }

    /* (non-Javadoc)
     * @see org.jboss.as.domain.controller.DomainController#isDeploymentNameUnique(java.lang.String)
     */
    @Override
    public boolean isDeploymentNameUnique(final String deploymentName) {
        return (domainModel.getDeployment(deploymentName) == null);
    }

    private List<DomainUpdateApplierResponse> applyUpdatesToHostControllers(final List<AbstractDomainModelUpdate<?>> updates,
            final List<AbstractDomainModelUpdate<?>> rollbacks) {

        final List<DomainUpdateApplierResponse> result = new ArrayList<DomainUpdateApplierResponse>(updates.size());

        // We update host controllers concurrently
        final Map<String, Future<List<ModelUpdateResponse<List<ServerIdentity>>>>> futures = new HashMap<String, Future<List<ModelUpdateResponse<List<ServerIdentity>>>>>();
        for (final Map.Entry<String, HostControllerClient> entry : clients.entrySet()) {
            final HostControllerClient client = entry.getValue();
            final Callable<List<ModelUpdateResponse<List<ServerIdentity>>>> callable = new Callable<List<ModelUpdateResponse<List<ServerIdentity>>>>() {

                @Override
                public List<ModelUpdateResponse<List<ServerIdentity>>> call() throws Exception {
                    return client.updateDomainModel(updates);
                }

            };

            futures.put(entry.getKey(), scheduledExecutorService.getValue().submit(callable));
        }

        log.debugf("Domain updates pushed to %s host controller(s)", futures.size());

        // Collate the results for each update
        boolean ok = true;
        for (int i = 0; i < updates.size(); i++) {

            final Map<String, UpdateFailedException> hostFailures = new HashMap<String, UpdateFailedException>();
            final List<ServerIdentity> servers = new ArrayList<ServerIdentity>();

            for (final Map.Entry<String, Future<List<ModelUpdateResponse<List<ServerIdentity>>>>> entry : futures.entrySet()) {
                try {
                    final List<ModelUpdateResponse<List<ServerIdentity>>> list = entry.getValue().get();
                    if (list.size() > i) {
                        final ModelUpdateResponse<List<ServerIdentity>> hostResponse = list.get(i);
                        if (hostResponse.isSuccess()) {
                            servers.addAll(hostResponse.getResult());
                        }
                        else {
                            hostFailures.put(entry.getKey(), hostResponse.getUpdateException());
                        }
                    }
                    // else this host didn't get this far
                } catch (final InterruptedException e) {
                    log.debug("Interrupted reading host controller response");
                    Thread.currentThread().interrupt();
                    hostFailures.put(entry.getKey(), new UpdateFailedException(e));
                } catch (final ExecutionException e) {
                    log.debug("Execution exception reading host controller response", e);
                    hostFailures.put(entry.getKey(), new UpdateFailedException(e));
                }
            }
            if (hostFailures.size() == 0) {
                log.debugf("%s servers affected by update %s", servers.size(), i);
                result.add(new DomainUpdateApplierResponse(servers));
            }
            else {
                log.debugf("%s host controllers failed on update %s", hostFailures.size(), i);
                result.add(new DomainUpdateApplierResponse(hostFailures));
                ok = false;
                // No point processing other updates, as we are going to roll them back.
                // Act as if we did the whole thing one update at a time and this
                // failure stopped us doing the rest
                break;
            }
        }

        if (!ok) {

            // Some host controller failed, so we gotta roll 'em all back

            log.warn("One or more updates failed on some host controllers; rolling back");

            // Apply compensating updates to fix our local model
            for (int i = 0; i < rollbacks.size(); i++) {
                final AbstractDomainModelUpdate<?> rollback = rollbacks.get(i);
                try {
                    domainModel.update(rollback);
                }
                catch (final UpdateFailedException e) {
                    // TODO uh oh. Reload from the file?
                }
            }

            // List of servers we fail to successfully roll back
            final Set<String> outOfSync = new HashSet<String>();

            final Map<String, Future<Boolean>> rollbackFutures = new HashMap<String, Future<Boolean>>(futures.size());
            for (final Map.Entry<String, Future<List<ModelUpdateResponse<List<ServerIdentity>>>>> entry : futures.entrySet()) {
                try {
                    // For this host figure out how many updates need to be rolled back
                    final List<ModelUpdateResponse<List<ServerIdentity>>> rspList = entry.getValue().get();
                    int idx = rspList.size() - 1;
                    if (idx >= 0 && !rspList.get(idx).isSuccess()) {
                        idx--; // !isSuccess one shouldn't have affected model state so no rollback of it
                    }
                    if (idx < 0) {
                        // This host didn't apply anything
                        continue;
                    }

                    // Set up the rollback list
                    final List<AbstractDomainModelUpdate<?>> hostControllerRollbacks =
                        (idx == rollbacks.size() -1) ? rollbacks : new ArrayList<AbstractDomainModelUpdate<?>>(idx + 1);
                    if (hostControllerRollbacks != rollbacks) {
                        // Rollbacks are in reverse order from updates. We take
                        // the last X=idx items from the rollback list since
                        // those correspond to the updates that didn't fail and need rollback
                        for (int j = rollbacks.size() - 1 - idx; j < rollbacks.size(); j++) {
                            hostControllerRollbacks.add(rollbacks.get(j));
                        }
                    }
                    // Tell the host to roll back
                    final HostControllerClient client = clients.get(entry.getKey());
                    final Callable<Boolean> callable = new Callable<Boolean>() {
                        @Override
                        public Boolean call() throws Exception {
                            final List<ModelUpdateResponse<List<ServerIdentity>>> rsp = client.updateDomainModel(hostControllerRollbacks);
                            return Boolean.valueOf(rsp.size() == hostControllerRollbacks.size() && rsp.get(rsp.size() - 1).isSuccess());
                        }
                    };
                    rollbackFutures.put(entry.getKey(), scheduledExecutorService.getValue().submit(callable));

                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    outOfSync.add(entry.getKey());
                } catch (final ExecutionException e) {
                    outOfSync.add(entry.getKey());
                }
            }

            // Wait until rollbacks complete
            for (final Map.Entry<String, Future<Boolean>> entry : rollbackFutures.entrySet()) {
                try {
                    if (!entry.getValue().get()) {
                        outOfSync.add(entry.getKey());
                    }
                }  catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    outOfSync.add(entry.getKey());
                } catch (final ExecutionException e) {
                    outOfSync.add(entry.getKey());
                }
            }

            for (final String host : outOfSync) {
                // Rollback failed; need to push the whole model
                final HostControllerClient client = clients.get(host);
                client.updateDomainModel(domainModel);
            }

            // Update the result list to record the rollbacks
            for (int i = 0; i < result.size(); i++) {
                final DomainUpdateApplierResponse rsp = result.get(i);
                if (rsp.getHostFailures().size() < 0) {
                    result.set(i, new DomainUpdateApplierResponse(false));
                }
            }
        }

        return result;
    }

    private List<DomainUpdateResult<?>> applyUpdatesToServers(final List<AbstractDomainModelUpdate<?>> updates,
                                                              final List<DomainUpdateApplierResponse> domainResults,
                                                              final boolean allowOverallRollback) {
        List<DomainUpdateResult<?>> result;
        final Map<AbstractDomainModelUpdate<?>, AbstractServerModelUpdate<?>> serverByDomain =
            new HashMap<AbstractDomainModelUpdate<?>, AbstractServerModelUpdate<?>>();
        final Map<AbstractServerModelUpdate<?>, DomainUpdateResult<Object>> resultsByUpdate = new HashMap<AbstractServerModelUpdate<?>, DomainUpdateResult<Object>>();
        for (int i = 0; i < updates.size(); i++) {
            final AbstractDomainModelUpdate<?> domainUpdate = updates.get(i);
            final AbstractServerModelUpdate<?> serverUpdate = domainUpdate.getServerModelUpdate();
            if (serverUpdate != null) {
                serverByDomain.put(domainUpdate, serverUpdate);
                resultsByUpdate.put(serverUpdate, new DomainUpdateResult<Object>());
            }
        }
        final Map<ServerIdentity, List<AbstractServerModelUpdate<?>>> updatesByServer = getUpdatesByServer(updates, domainResults, serverByDomain);

        log.debugf("updates affect %s", updatesByServer.keySet());

        // TODO Add param to configure pushing out concurrently
        for (final Map.Entry<ServerIdentity, List<AbstractServerModelUpdate<?>>> entry : updatesByServer.entrySet()) {
            final ServerIdentity server = entry.getKey();
            final List<AbstractServerModelUpdate<?>> serverUpdates = entry.getValue();
            // Push them out
            final List<UpdateResultHandlerResponse<?>> rsps = applyUpdatesToServer(server, serverUpdates, allowOverallRollback);
            for (int i = 0; i < serverUpdates.size(); i++) {
                final UpdateResultHandlerResponse<?> rsp = rsps.get(i);
                final AbstractServerModelUpdate<?> serverUpdate = entry.getValue().get(i);
                DomainUpdateResult<Object> dur = resultsByUpdate.get(serverUpdate);

                if (rsp.isCancelled()) {
                    dur = dur.newWithAddedCancellation(server);
                }
                else if (rsp.isTimedOut()) {
                    dur = dur.newWithAddedTimeout(server);
                }
                else if (rsp.isRolledBack()) {
                    dur = dur.newWithAddedRollback(server);
                }
                else if (rsp.getFailureResult() != null) {
                    dur = dur.newWithAddedFailure(server, rsp.getFailureResult());
                }
                else {
                    dur = dur.newWithAddedResult(server, rsp.getSuccessResult());
                }
                resultsByUpdate.put(serverUpdate, dur);
            }
        }

        result = new ArrayList<DomainUpdateResult<?>>();
        for (final AbstractDomainModelUpdate<?> domainUpdate : updates) {
            final AbstractServerModelUpdate<?> serverUpdate = serverByDomain.get(domainUpdate);
            DomainUpdateResult<?> dur = resultsByUpdate.get(serverUpdate);
            if (dur == null) {
                // Update did not impact servers
                dur = new DomainUpdateResult<Object>();
            }
            result.add(dur);
        }
        return result;
    }

    private Map<ServerIdentity, List<AbstractServerModelUpdate<?>>> getUpdatesByServer(
            final List<AbstractDomainModelUpdate<?>> domainUpdates,
            final List<DomainUpdateApplierResponse> domainResults,
            final Map<AbstractDomainModelUpdate<?>, AbstractServerModelUpdate<?>> serverByDomain) {

        final Map<ServerIdentity, List<AbstractServerModelUpdate<?>>> result = new HashMap<ServerIdentity, List<AbstractServerModelUpdate<?>>>();

        for (int i = 0; i < domainResults.size(); i++) {
            final DomainUpdateApplierResponse domainResult = domainResults.get(i);
            final AbstractDomainModelUpdate<?> domainUpdate = domainUpdates.get(i);
            final AbstractServerModelUpdate<?> serverUpdate = serverByDomain.get(domainUpdate);
            for (final ServerIdentity server : domainResult.getServers()) {
                List<AbstractServerModelUpdate<?>> serverList = result.get(server);
                if (serverList == null) {
                    serverList = new ArrayList<AbstractServerModelUpdate<?>>();
                    result.put(server, serverList);
                }
                serverList.add(serverUpdate);
            }
        }
        return result;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.domain.controller.DomainController#applyHostUpdates(java.lang.String, java.util.List)
     */
    @Override
    public List<HostUpdateResult<?>> applyHostUpdates(final String hostControllerName, final List<AbstractHostModelUpdate<?>> updates) {

        List<HostUpdateResult<?>> result;
        final HostControllerClient client = clients.get(hostControllerName);
        if (client == null) {
            result = new ArrayList<HostUpdateResult<?>>(updates.size());
            final HostUpdateResult<Object> hur = new HostUpdateResult<Object>(new UpdateFailedException("Host " + hostControllerName + " is unknown"));
            for (int i = 0; i < updates.size(); i++) {
                result.add(hur);
            }
        }
        else {
            result = client.updateHostModel(updates);
        }
        return result;
    }
}
