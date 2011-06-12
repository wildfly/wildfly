/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.host.controller;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RUNNING_SERVER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import org.jboss.as.controller.AbstractControllerService;
import org.jboss.as.controller.BootContext;
import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.NewModelController;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.NewProxyController;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.persistence.ConfigurationPersistenceException;
import org.jboss.as.controller.persistence.ConfigurationPersister;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.as.domain.controller.FileRepository;
import org.jboss.as.domain.controller.LocalHostControllerInfo;
import org.jboss.as.domain.controller.NewDomainController;
import org.jboss.as.domain.controller.NewDomainModelUtil;
import org.jboss.as.domain.controller.PrepareStepHandler;
import org.jboss.as.domain.controller.descriptions.DomainDescriptionProviders;
import org.jboss.as.host.controller.mgmt.ServerToHostOperationHandler;
import org.jboss.as.host.controller.operations.LocalHostControllerInfoImpl;
import org.jboss.as.host.controller.operations.NewStartServersHandler;
import org.jboss.as.network.NetworkInterfaceBinding;
import org.jboss.as.remoting.RemotingServices;
import org.jboss.as.server.services.net.NetworkInterfaceService;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Creates the service that acts as the {@link org.jboss.as.controller.NewModelController} for a host.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DomainModelControllerService extends AbstractControllerService implements NewDomainController {

    public static final ServiceName SERVICE_NAME = NewHostControllerBootstrap.SERVICE_NAME_BASE.append("model", "controller");

    private final HostControllerConfigurationPersister configurationPersister;
    private final HostControllerEnvironment environment;
    private final LocalHostControllerInfoImpl hostControllerInfo;
    private final FileRepository localFileRepository;
    private final InjectedValue<NewServerInventory> injectedServerInventory = new InjectedValue<NewServerInventory>();
    private final InjectedValue<ExecutorService> injectedExecutorService = new InjectedValue<ExecutorService>();
    private final Map<String, NewProxyController> hostProxies;
    private final PrepareStepHandler prepareStepHandler;
    private ModelNodeRegistration modelNodeRegistration;

    public static ServiceController<NewModelController> addService(final ServiceTarget serviceTarget,
                                                            final HostControllerEnvironment environment,
                                                            final ControlledProcessState processState) {
        final Map<String, NewProxyController> hostProxies = new ConcurrentHashMap<String, NewProxyController>();
        final LocalHostControllerInfoImpl hostControllerInfo = new LocalHostControllerInfoImpl(processState);
        final PrepareStepHandler prepareStepHandler = new PrepareStepHandler(hostControllerInfo, hostProxies);
        DomainModelControllerService service = new DomainModelControllerService(environment, processState,
                hostControllerInfo, new HostControllerConfigurationPersister(environment),
                hostProxies, prepareStepHandler);
        return serviceTarget.addService(SERVICE_NAME, service)
                .addDependency(ServerInventoryService.SERVICE_NAME, NewServerInventory.class, service.injectedServerInventory)
                .addDependency(NewHostControllerBootstrap.SERVICE_NAME_BASE.append("executor"), ExecutorService.class, service.injectedExecutorService)
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install();
    }

    private DomainModelControllerService(final HostControllerEnvironment environment,
                                         final ControlledProcessState processState,
                                         final LocalHostControllerInfoImpl hostControllerInfo,
                                         final HostControllerConfigurationPersister configurationPersister,
                                         final Map<String, NewProxyController> hostProxies,
                                         final PrepareStepHandler prepareStepHandler) {
        super(NewOperationContext.Type.HOST, configurationPersister, processState, DomainDescriptionProviders.ROOT_PROVIDER,
                prepareStepHandler);
        this.configurationPersister = configurationPersister;
        this.environment = environment;
        this.hostControllerInfo = hostControllerInfo;
        this.localFileRepository = new LocalFileRepository(environment);
        this.hostProxies = hostProxies;
        this.prepareStepHandler = prepareStepHandler;
    }

    @Override
    public LocalHostControllerInfo getLocalHostInfo() {
        return hostControllerInfo;
    }

    @Override
    public void registerRemoteHost(NewProxyController hostControllerClient) {
        if (!hostControllerInfo.isMasterDomainController()) {
            throw new UnsupportedOperationException("Registration of remote hosts is not supported on slave host controllers");
        }
        PathAddress pa = hostControllerClient.getProxyNodeAddress();
        PathElement pe = pa.getElement(0);
        Logger.getLogger("org.jboss.domain").info("Registering host " + pe.getValue());
        if (modelNodeRegistration.getProxyController(pa) != null || hostControllerInfo.getLocalHostName().equals(pe.getValue())){
            throw new IllegalArgumentException("There is already a registered host named '" + pe.getValue() + "'");
        }
        modelNodeRegistration.registerProxyController(pe, hostControllerClient);
        hostProxies.put(pe.getValue(), hostControllerClient);
    }

    @Override
    public void unregisterRemoteHost(String id) {
        Logger.getLogger("org.jboss.domain").info("Unregistering host " + id);
        hostProxies.remove(id);
        modelNodeRegistration.unregisterProxyController(PathElement.pathElement(HOST, id));
    }

    @Override
    public void registerRunningServer(NewProxyController serverControllerClient) {
        PathAddress pa = serverControllerClient.getProxyNodeAddress();
        PathElement pe = pa.getElement(1);
        if (modelNodeRegistration.getProxyController(pa) != null) {
            throw new IllegalArgumentException("There is already a registered server named '" + pe.getValue() + "'");
        }
        Logger.getLogger("org.jboss.host.controller").info("Registering server " + pe.getValue());
        ModelNodeRegistration hostRegistration = modelNodeRegistration.getSubModel(PathAddress.pathAddress(PathElement.pathElement(HOST)));
        hostRegistration.registerProxyController(pe, serverControllerClient);
    }

    @Override
    public void unregisterRunningServer(String serverName) {
        PathAddress pa = PathAddress.pathAddress(PathElement.pathElement(HOST, hostControllerInfo.getLocalHostName()));
        PathElement pe = PathElement.pathElement(RUNNING_SERVER, serverName);
        Logger.getLogger("org.jboss.host.controller").info("Registering server " + serverName);
        ModelNodeRegistration hostRegistration = modelNodeRegistration.getSubModel(pa);
        hostRegistration.unregisterProxyController(pe);
    }

    @Override
    public ModelNode getProfileOperations(String profileName) {
        ModelNode operation = new ModelNode();

        operation.get(OP).set(DESCRIBE);
        operation.get(OP_ADDR).set(PathAddress.pathAddress(PathElement.pathElement(PROFILE, profileName)).toModelNode());

        ModelNode rsp = getValue().execute(operation, null, null, null);
        if (!rsp.hasDefined(OUTCOME) || !SUCCESS.equals(rsp.get(OUTCOME).asString())) {
            ModelNode msgNode = rsp.get(FAILURE_DESCRIPTION);
            String msg = msgNode.isDefined() ? msgNode.toString() : "Failed to retrieve profile operations from domain controller";
            throw new RuntimeException(msg);
        }
        return rsp.require(RESULT);
    }

    @Override
    public FileRepository getFileRepository() {
        return localFileRepository;
    }

    @Override
    protected ModelNode createCoreModel() {
        final ModelNode coreModel = new ModelNode();
        NewDomainModelUtil.updateCoreModel(coreModel);
        return coreModel;
    }

    @Override
    protected void initModel(ModelNodeRegistration rootRegistration) {
        NewHostModelUtil.createHostRegistry(rootRegistration, configurationPersister, environment, localFileRepository,
                hostControllerInfo, injectedServerInventory.getValue());
        this.modelNodeRegistration = rootRegistration;
    }

    @Override
    public void start(StartContext context) throws StartException {
        prepareStepHandler.setExecutorService(injectedExecutorService.getValue());
        super.start(context);
    }

    // See superclass start. This method is invoked from a separate non-MSC thread after start. So we can do a fair
    // bit of stuff
    @Override
    protected void boot(final BootContext context) throws ConfigurationPersistenceException {

        // TODO add superclass hooks to allow us to completely take over this
        super.boot(context); // This parses the host.xml and invokes all ops
        // See if we need to register with the master.
        if (!hostControllerInfo.isMasterDomainController()) {
            // TODO
            // 1) register with the master
            // 2) get back the domain model, somehow store it (perhaps using an op invoked by the DC?)
            // 3) if 1) fails, check env.isUseCachedDC, if true fall through to that
        } else {
            // parse the domain.xml and load the steps
            ConfigurationPersister domainPersister = configurationPersister.getDomainPersister();
            List<ModelNode> domainBootOps = domainPersister.load();
            // TODO
            // execute ops to populate the domain part of the model
            // -- see super.boot(context)
        }

        ServiceTarget serviceTarget = context.getServiceTarget();

        final ServerInventoryService inventory = new ServerInventoryService(environment, hostControllerInfo.getNativeManagementPort());
        serviceTarget.addService(ServerInventoryService.SERVICE_NAME, inventory)
                .addDependency(ProcessControllerConnectionService.SERVICE_NAME, ProcessControllerConnectionService.class, inventory.getClient())
                .addDependency(NetworkInterfaceService.JBOSS_NETWORK_INTERFACE.append(hostControllerInfo.getNativeManagementInterface()),
                        NetworkInterfaceBinding.class, inventory.getInterface())
                .install();

        // Add the server to host operation handler
        final ServerToHostOperationHandler serverToHost = new ServerToHostOperationHandler();
        serviceTarget.addService(ServerToHostOperationHandler.SERVICE_NAME, serverToHost)
            .addDependency(ServerInventoryService.SERVICE_NAME, ManagedServerLifecycleCallback.class, serverToHost.getCallbackInjector())
            .install();

        RemotingServices.installChannelOpenListenerService(serviceTarget, "server", ServerToHostOperationHandler.SERVICE_NAME, null, null);
        // TODO  other services we should start?


        // Start the servers
        injectedServerInventory.getValue().setDomainController(this);
        startServers();

        // TODO when to call configurationPersister.successful boot? Look into this for standalone as well; may be broken now
    }

    private void startServers() {
        ModelNode addr = new ModelNode();
        addr.add(HOST, hostControllerInfo.getLocalHostName());
        ModelNode op = Util.getEmptyOperation(NewStartServersHandler.OPERATION_NAME, addr);

        getValue().execute(op, null, null, null);
    }


    @Override
    public void stop(StopContext context) {
        // TODO async
        injectedServerInventory.getValue().stopServers(-1); // TODO graceful timeout
        super.stop(context);
    }
}
