/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.core.model.test;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;

import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.RunningModeControl;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.persistence.ExtensibleConfigurationPersister;
import org.jboss.as.controller.persistence.NullConfigurationPersister;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.services.path.PathManagerService;
import org.jboss.as.controller.transform.Transformers;
import org.jboss.as.domain.controller.DomainController;
import org.jboss.as.domain.controller.LocalHostControllerInfo;
import org.jboss.as.domain.controller.SlaveRegistrationException;
import org.jboss.as.domain.controller.resources.DomainRootDefinition;
import org.jboss.as.host.controller.HostControllerConfigurationPersister;
import org.jboss.as.host.controller.HostControllerEnvironment;
import org.jboss.as.host.controller.HostModelUtil;
import org.jboss.as.host.controller.HostModelUtil.HostModelRegistrar;
import org.jboss.as.host.controller.HostPathManagerService;
import org.jboss.as.host.controller.HostRunningModeControl;
import org.jboss.as.host.controller.ignored.IgnoredDomainResourceRegistry;
import org.jboss.as.host.controller.model.host.HostResourceDefinition;
import org.jboss.as.host.controller.operations.LocalHostControllerInfoImpl;
import org.jboss.as.model.test.ModelTestModelControllerService;
import org.jboss.as.model.test.OperationValidation;
import org.jboss.as.model.test.StringConfigurationPersister;
import org.jboss.as.protocol.mgmt.ManagementChannelHandler;
import org.jboss.as.repository.ContentRepository;
import org.jboss.as.repository.HostFileRepository;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironment.LaunchType;
import org.jboss.as.server.ServerEnvironmentResourceDescription;
import org.jboss.as.server.ServerPathManagerService;
import org.jboss.as.server.controller.resources.ServerRootResourceDefinition;
import org.jboss.as.server.services.security.AbstractVaultReader;
import org.jboss.as.version.ProductConfig;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class TestModelControllerService extends ModelTestModelControllerService {

    private final InjectedValue<ContentRepository> injectedContentRepository = new InjectedValue<ContentRepository>();
    private final TestModelType type;
    private final RunningModeControl runningModeControl;
    private final PathManagerService pathManagerService;
    private final ModelInitializer modelInitializer;
    private final DelegatingResourceDefinition rootResourceDefinition;
    private final ControlledProcessState processState;
    private volatile Initializer initializer;

    TestModelControllerService(ProcessType processType, RunningModeControl runningModeControl, StringConfigurationPersister persister, OperationValidation validateOps,
            TestModelType type, ModelInitializer modelInitializer, DelegatingResourceDefinition rootResourceDefinition, ControlledProcessState processState) {
        super(processType, runningModeControl, null, persister, validateOps, rootResourceDefinition, processState);
        this.type = type;
        this.runningModeControl = runningModeControl;
        this.pathManagerService = type == TestModelType.STANDALONE ? new ServerPathManagerService() : new HostPathManagerService();
        this.modelInitializer = modelInitializer;
        this.rootResourceDefinition = rootResourceDefinition;
        this.processState = processState;

        if (type == TestModelType.STANDALONE) {
            initializer = new ServerInitializer();
        } else if (type == TestModelType.HOST) {
            //Remove the write-local-domain-controller operation since we already simulate that here
            for (Iterator<ModelNode> it = persister.getBootOperations().iterator() ; it.hasNext() ; ) {
                ModelNode op = it.next();
                if (op.get(OP).asString().equals("write-local-domain-controller")) {
                    System.out.println("WARNING: Test framework is removing the 'write-local-domain-controller' operation. If you are comparing xml results use a " +
                             "ModelWriteSanitizer to add the \"domain-controller\" => {\"local\" => {}} part (See ShippedConfigurationsModelTestCase.testHostXml() for an example)");
                    it.remove();
                    break;
                }
            }
            initializer = new HostInitializer();
        } else if (type == TestModelType.DOMAIN) {
            initializer = new DomainInitializer();
        }
    }

    @Deprecated
    //TODO remove this once host and domain are ported to resource definition
    TestModelControllerService(ProcessType processType, RunningModeControl runningModeControl, StringConfigurationPersister persister, OperationValidation validateOps,
            TestModelType type, ModelInitializer modelInitializer, DescriptionProvider rootDescriptionProvider, ControlledProcessState processState) {
        super(processType, runningModeControl, null, persister, validateOps, rootDescriptionProvider, processState);
        if (type == TestModelType.STANDALONE || type == TestModelType.HOST) {
            throw new IllegalStateException("Should not be called for standalone or host");
        }
        this.type = type;
        this.runningModeControl = runningModeControl;
        this.pathManagerService = type == TestModelType.STANDALONE ? new ServerPathManagerService() : new HostPathManagerService();
        this.modelInitializer = modelInitializer;
        this.processState = processState;
        this.rootResourceDefinition = null;
    }

    static TestModelControllerService create(ProcessType processType, RunningModeControl runningModeControl, StringConfigurationPersister persister, OperationValidation validateOps, TestModelType type, ModelInitializer modelInitializer) {
        return new TestModelControllerService(processType, runningModeControl, persister, validateOps, type, modelInitializer, new DelegatingResourceDefinition(type), new ControlledProcessState(true));
    }

    InjectedValue<ContentRepository> getContentRepositoryInjector(){
        return injectedContentRepository;
    }


    @Override
    public void start(StartContext context) throws StartException {
        if (initializer != null) {
            initializer.setRootResourceDefinitionDelegate();
        }
        super.start(context);
    }

    @Override
    protected void initCoreModel(Resource rootResource, ManagementResourceRegistration rootRegistration) {
        //See server HttpManagementAddHandler
        System.setProperty("jboss.as.test.disable.runtime", "1");
        if (type == TestModelType.STANDALONE) {
            initializer.initCoreModel(rootResource, rootRegistration);

        } else if (type == TestModelType.HOST){
            initializer.initCoreModel(rootResource, rootRegistration);

        } else if (type == TestModelType.DOMAIN){
            initializer.initCoreModel(rootResource, rootRegistration);
        }
        if (modelInitializer != null) {
            modelInitializer.populateModel(rootResource);
        }
    }

    @Override
    public void stop(StopContext context) {
        super.stop(context);
        System.clearProperty("jboss.as.test.disable.runtime");
    }

    private ServerEnvironment createStandaloneServerEnvironment() {
        Properties props = new Properties();
        File home = new File("target/jbossas");
        delete(home);
        home.mkdir();
        props.put(ServerEnvironment.HOME_DIR, home.getAbsolutePath());

        File standalone = new File(home, "standalone");
        standalone.mkdir();
        props.put(ServerEnvironment.SERVER_BASE_DIR, standalone.getAbsolutePath());

        File configuration = new File(standalone, "configuration");
        configuration.mkdir();
        props.put(ServerEnvironment.SERVER_CONFIG_DIR, configuration.getAbsolutePath());

        File xml = new File(configuration, "standalone.xml");
        try {
            xml.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        props.put(ServerEnvironment.JBOSS_SERVER_DEFAULT_CONFIG, "standalone.xml");

        return new ServerEnvironment(null, props, new HashMap<String, String>(), "standalone.xml", null, LaunchType.STANDALONE, runningModeControl.getRunningMode(), null);
    }

    private HostControllerEnvironment createHostControllerEnvironment() {
        try {
            Map<String, String> props = new HashMap<String, String>();
            File home = new File("target/jbossas");
            delete(home);
            home.mkdir();
            props.put(HostControllerEnvironment.HOME_DIR, home.getAbsolutePath());

            File domain = new File(home, "domain");
            domain.mkdir();
            props.put(HostControllerEnvironment.DOMAIN_BASE_DIR, domain.getAbsolutePath());

            File configuration = new File(domain, "configuration");
            configuration.mkdir();
            props.put(HostControllerEnvironment.DOMAIN_CONFIG_DIR, configuration.getAbsolutePath());


            boolean isRestart = false;
            String modulePath = "";
            InetAddress processControllerAddress = InetAddress.getLocalHost();
            Integer processControllerPort = 9999;
            InetAddress hostControllerAddress = InetAddress.getLocalHost();
            Integer hostControllerPort = 1234;
            String defaultJVM = null;
            String domainConfig = null;
            String initialDomainConfig = null;
            String hostConfig = null;
            String initialHostConfig = null;
            RunningMode initialRunningMode = runningModeControl.getRunningMode();
            boolean backupDomainFiles = false;
            boolean useCachedDc = false;
            ProductConfig productConfig = new ProductConfig(null, "");
            return new HostControllerEnvironment(props, isRestart, modulePath, processControllerAddress, processControllerPort,
                    hostControllerAddress, hostControllerPort, defaultJVM, domainConfig, initialDomainConfig, hostConfig, initialHostConfig,
                    initialRunningMode, backupDomainFiles, useCachedDc, productConfig);
        } catch (UnknownHostException e) {
            // AutoGenerated
            throw new RuntimeException(e);
        }
    }

    private LocalHostControllerInfoImpl createLocalHostControllerInfo(HostControllerEnvironment env) {
        return new LocalHostControllerInfoImpl(null, env);
    }

    private HostFileRepository createHostFileRepository() {
        return new HostFileRepository() {

            @Override
            public File getDeploymentRoot(byte[] deploymentHash) {
                return null;
            }

            @Override
            public File[] getDeploymentFiles(byte[] deploymentHash) {
                return null;
            }

            @Override
            public void deleteDeployment(byte[] deploymentHash) {
            }

            @Override
            public File getFile(String relativePath) {
                return null;
            }

            @Override
            public File getConfigurationFile(String relativePath) {
                return null;
            }
        };
    }

    private DomainController createDomainController(final HostControllerEnvironment env, final LocalHostControllerInfoImpl info) {
        return new DomainController() {

            @Override
            public void unregisterRunningServer(String serverName) {
            }

            @Override
            public void unregisterRemoteHost(String id, Long remoteConnectionId) {
            }

            @Override
            public void stopLocalHost(int exitCode) {
            }

            @Override
            public void stopLocalHost() {
            }

            @Override
            public void registerRunningServer(ProxyController serverControllerClient) {
            }

            @Override
            public void registerRemoteHost(String hostName, ManagementChannelHandler handler, Transformers transformers,
                    Long remoteConnectionId) throws SlaveRegistrationException {
            }

            @Override
            public void pingRemoteHost(String hostName) {
            }

            @Override
            public boolean isHostRegistered(String id) {
                return false;
            }

            @Override
            public HostFileRepository getRemoteFileRepository() {
                return null;
            }

            @Override
            public ModelNode getProfileOperations(String profileName) {
                return null;
            }

            @Override
            public LocalHostControllerInfo getLocalHostInfo() {
                return info;
            }

            @Override
            public HostFileRepository getLocalFileRepository() {
                return null;
            }

            @Override
            public ExtensionRegistry getExtensionRegistry() {
                return null;
            }

            @Override
            public RunningMode getCurrentRunningMode() {
                return null;
            }

            @Override
            public ExpressionResolver getExpressionResolver() {
                return null;
            }

            @Override
            public void initializeMasterDomainRegistry(ManagementResourceRegistration root,
                    ExtensibleConfigurationPersister configurationPersister, ContentRepository contentRepository,
                    HostFileRepository fileRepository, ExtensionRegistry extensionRegistry, PathManagerService pathManager) {
            }

            @Override
            public void initializeSlaveDomainRegistry(ManagementResourceRegistration root,
                    ExtensibleConfigurationPersister configurationPersister, ContentRepository contentRepository,
                    HostFileRepository fileRepository, LocalHostControllerInfo hostControllerInfo,
                    ExtensionRegistry extensionRegistry, IgnoredDomainResourceRegistry ignoredDomainResourceRegistry,
                    PathManagerService pathManager) {
            }
        };
    }

    private void delete(File file) {
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                delete(child);
            }
        }
        file.delete();
    }

    private interface Initializer {
        void setRootResourceDefinitionDelegate();
        void initCoreModel(Resource rootResource, ManagementResourceRegistration rootRegistration);
    }

    private class ServerInitializer implements Initializer {
        final ExtensibleConfigurationPersister persister = new NullConfigurationPersister();
        final ServerEnvironment environment = createStandaloneServerEnvironment();
        final ExtensionRegistry extensionRegistry = new ExtensionRegistry(ProcessType.STANDALONE_SERVER, runningModeControl);
        final boolean parallelBoot = false;
        final AbstractVaultReader vaultReader = null;

        public void setRootResourceDefinitionDelegate() {
            rootResourceDefinition.setDelegate(new ServerRootResourceDefinition(
                    injectedContentRepository.getValue(),
                    persister,
                    environment,
                    processState,
                    runningModeControl,
                    vaultReader,
                    extensionRegistry,
                    parallelBoot,
                    pathManagerService));
        }

        @Override
        public void initCoreModel(Resource rootResource, ManagementResourceRegistration rootRegistration) {
            //Add the same stuff as is added in ServerService.initModel()
            rootResource.registerChild(PathElement.pathElement(ModelDescriptionConstants.CORE_SERVICE, ModelDescriptionConstants.MANAGEMENT), Resource.Factory.create());
            rootResource.registerChild(PathElement.pathElement(ModelDescriptionConstants.CORE_SERVICE, ModelDescriptionConstants.SERVICE_CONTAINER), Resource.Factory.create());
            rootResource.registerChild(ServerEnvironmentResourceDescription.RESOURCE_PATH, Resource.Factory.create());
            pathManagerService.addPathManagerResources(rootResource);
        }
    }

    private class HostInitializer implements Initializer {
        final String hostName = "master";
        final ExtensionRegistry extensionRegistry = new ExtensionRegistry(ProcessType.HOST_CONTROLLER, runningModeControl);
        final HostControllerEnvironment env = createHostControllerEnvironment();
        final LocalHostControllerInfoImpl info = createLocalHostControllerInfo(env);
        final IgnoredDomainResourceRegistry ignoredRegistry = new IgnoredDomainResourceRegistry(info);
        final HostControllerConfigurationPersister persister = new HostControllerConfigurationPersister(env, info, Executors.newCachedThreadPool(), extensionRegistry);
        final HostFileRepository hostFileRepository = createHostFileRepository();
        final DomainController domainController = createDomainController(env, info);

        @Override
        public void setRootResourceDefinitionDelegate() {
            rootResourceDefinition.setDelegate(
                    new HostResourceDefinition(
                            hostName,
                            persister,
                            env,
                            (HostRunningModeControl)runningModeControl,
                            hostFileRepository,
                            info,
                            null /*serverInventory*/,
                            null /*remoteFileRepository*/,
                            injectedContentRepository.getValue(),
                            domainController,
                            extensionRegistry,
                            null /*vaultReader*/,
                            ignoredRegistry,
                            processState,
                            pathManagerService));
        }

        @Override
        public void initCoreModel(Resource rootResource, ManagementResourceRegistration rootRegistration) {
            HostModelUtil.createRootRegistry(
                    rootRegistration,
                    env,
                    ignoredRegistry,
                    new HostModelRegistrar() {
                        @Override
                        public void registerHostModel(String hostName, ManagementResourceRegistration rootRegistration) {
                        }
                    },ProcessType.HOST_CONTROLLER);

            HostModelUtil.createHostRegistry(
                    hostName,
                    rootRegistration,
                    persister,
                    env,
                    (HostRunningModeControl)runningModeControl,
                    hostFileRepository,
                    info,
                    null /*serverInventory*/,
                    null /*remoteFileRepository*/,
                    injectedContentRepository.getValue(),
                    domainController,
                    extensionRegistry,
                    null /*vaultReader*/,
                    ignoredRegistry,
                    processState,
                    pathManagerService);
        }
    }

    private class DomainInitializer implements Initializer {

        @Override
        public void setRootResourceDefinitionDelegate() {
        }

        @Override
        public void initCoreModel(Resource rootResource, ManagementResourceRegistration rootRegistration) {
            final HostControllerEnvironment env = createHostControllerEnvironment();
            final LocalHostControllerInfoImpl info = createLocalHostControllerInfo(env);
            final IgnoredDomainResourceRegistry ignoredRegistry = new IgnoredDomainResourceRegistry(info);
            final ExtensibleConfigurationPersister persister = new NullConfigurationPersister();
            final HostFileRepository hostFIleRepository = createHostFileRepository();
            final ExtensionRegistry extensionRegistry = new ExtensionRegistry(ProcessType.HOST_CONTROLLER, runningModeControl);

            DomainRootDefinition domainDefinition = new DomainRootDefinition(env, persister, injectedContentRepository.getValue(),
                    hostFIleRepository, true, info, extensionRegistry, null, pathManagerService);
            domainDefinition.initialize(rootRegistration);
            rootResourceDefinition.setDelegate(domainDefinition);

            HostModelUtil.createRootRegistry(
                    rootRegistration,
                    env, ignoredRegistry,
                    new HostModelRegistrar() {

                        @Override
                        public void registerHostModel(String hostName, ManagementResourceRegistration root) {
                        }
                    },processType);
        }

    }

    private static class DelegatingResourceDefinition extends ModelTestModelControllerService.DelegatingResourceDefinition {
        private final TestModelType type;

        public DelegatingResourceDefinition(TestModelType type) {
            this.type = type;
        }

        @Override
        public void registerOperations(ManagementResourceRegistration resourceRegistration) {
            if (type == TestModelType.DOMAIN) {
                return;
            }
            super.registerOperations(resourceRegistration);
        }

        @Override
        public void registerChildren(ManagementResourceRegistration resourceRegistration) {
            if (type == TestModelType.DOMAIN) {
                return;
            }
            super.registerChildren(resourceRegistration);
        }

        @Override
        public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
            if (type == TestModelType.DOMAIN) {
                return;
            }
            super.registerAttributes(resourceRegistration);
        }
    }
}
