/*
 * Copyright (C) 2014 Red Hat, inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package org.jboss.as.test.shared;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLBACK_ON_RUNTIME_FAILURE;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.xml.namespace.QName;

import org.jboss.as.controller.AbstractControllerService;
import org.jboss.as.controller.BootErrorCollector;
import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.ResourceBuilder;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.RunningModeControl;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.access.management.DelegatingConfigurableAuthorizer;
import org.jboss.as.controller.audit.AuditLogger;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.NonResolvingResourceDescriptionResolver;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.operations.common.NamespaceAddHandler;
import org.jboss.as.controller.operations.common.SchemaLocationAddHandler;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.common.XmlMarshallingHandler;
import org.jboss.as.controller.parsing.Namespace;
import org.jboss.as.controller.persistence.NullConfigurationPersister;
import org.jboss.as.controller.persistence.XmlConfigurationPersister;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.resource.InterfaceDefinition;
import org.jboss.as.controller.services.path.PathManagerService;
import org.jboss.as.controller.services.path.PathResourceDefinition;
import org.jboss.as.domain.controller.LocalHostControllerInfo;
import org.jboss.as.domain.controller.resources.DomainRootDefinition;
import org.jboss.as.domain.management.CoreManagementResourceDefinition;
import org.jboss.as.domain.management.audit.EnvironmentNameReader;
import org.jboss.as.host.controller.discovery.DiscoveryOption;
import org.jboss.as.host.controller.model.host.HostResourceDefinition;
import org.jboss.as.host.controller.model.jvm.JvmResourceDefinition;
import org.jboss.as.host.controller.operations.HostSpecifiedInterfaceAddHandler;
import org.jboss.as.host.controller.operations.HostSpecifiedInterfaceRemoveHandler;
import org.jboss.as.host.controller.operations.IsMasterHandler;
import org.jboss.as.host.controller.operations.LocalDomainControllerAddHandler;
import org.jboss.as.host.controller.operations.LocalHostControllerInfoImpl;
import org.jboss.as.host.controller.operations.RemoteDomainControllerAddHandler;
import org.jboss.as.host.controller.parsing.DomainXml;
import org.jboss.as.host.controller.parsing.HostXml;
import org.jboss.as.host.controller.resources.NativeManagementResourceDefinition;
import org.jboss.as.host.controller.resources.ServerConfigResourceDefinition;
import org.jboss.as.repository.ContentReference;
import org.jboss.as.repository.ContentRepository;
import org.jboss.as.repository.HostFileRepository;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.controller.resources.ServerRootResourceDefinition;
import org.jboss.as.server.controller.resources.SystemPropertyResourceDefinition;
import org.jboss.as.server.parsing.StandaloneXml;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.jboss.modules.Module;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.vfs.VirtualFile;
import org.junit.Assert;

/**
 *
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a> (c) 2014 Red Hat, inc.
 */
public class ModelParserUtils {

    public static ModelNode standaloneXmlTest(File original, File target) throws Exception {
        ServiceContainer serviceContainer = setupServiceContainer();
        try {
            FileUtils.copyFile(original, target);
            // Only instantiate one per container as service names of the paths will be registered on the container
            final TestPathManagerService pathManager = new TestPathManagerService(serviceContainer);
            ModelNode originalModel = loadServerModel(serviceContainer, target, pathManager);
            ModelNode reparsedModel = loadServerModel(serviceContainer, target, pathManager);
            fixupOSGiStandalone(originalModel);
            fixupOSGiStandalone(reparsedModel);
            compare(originalModel, reparsedModel);
            return reparsedModel;
        } finally {
            cleanup(serviceContainer);
        }
    }

    public static void hostXmlTest(final File original, File target) throws Exception {
        ServiceContainer serviceContainer = setupServiceContainer();
        try {
            FileUtils.copyFile(original, target);
            ModelNode originalModel = loadHostModel(serviceContainer, target);
            ModelNode reparsedModel = loadHostModel(serviceContainer, target);
            compare(originalModel, reparsedModel);
        } finally {
            cleanup(serviceContainer);
        }
    }

    public static ModelNode domainXmlTest(File original, File target) throws Exception {
        ServiceContainer serviceContainer = setupServiceContainer();
        try {
            FileUtils.copyFile(original, target);
            ModelNode originalModel = loadDomainModel(serviceContainer, target);
            ModelNode reparsedModel = loadDomainModel(serviceContainer, target);
            fixupOSGiDomain(originalModel);
            fixupOSGiDomain(reparsedModel);
            compare(originalModel, reparsedModel);
            return reparsedModel;
        } finally {
            cleanup(serviceContainer);
        }
    }

    private static void fixupOSGiStandalone(ModelNode node) {
        //These multiline properties get extra indentation when marshalled. Put them on one line to compare properly
        node.get("subsystem", "osgi", "property", "org.jboss.osgi.system.modules").set(convertToSingleLine(node.get("subsystem", "osgi", "framework-property", "org.jboss.osgi.system.modules").asString()));
        node.get("subsystem", "osgi", "property", "org.osgi.framework.system.packages.extra").set(convertToSingleLine(node.get("subsystem", "osgi", "framework-property", "org.osgi.framework.system.packages.extra").asString()));
    }

    private static void fixupOSGiDomain(ModelNode node) {
        //These multiline properties get extra indentation when marshalled. Put them on one line to compare properly
        node.get("profile", "default", "subsystem", "osgi", "property", "org.jboss.osgi.system.modules").set(convertToSingleLine(node.get("profile", "default", "subsystem", "osgi", "framework-property", "org.jboss.osgi.system.modules").asString()));
        node.get("profile", "default", "subsystem", "osgi", "property", "org.osgi.framework.system.packages.extra").set(convertToSingleLine(node.get("profile", "default", "subsystem", "osgi", "framework-property", "org.osgi.framework.system.packages.extra").asString()));

        node.get("profile", "ha", "subsystem", "osgi", "property", "org.jboss.osgi.system.modules").set(convertToSingleLine(node.get("profile", "ha", "subsystem", "osgi", "framework-property", "org.jboss.osgi.system.modules").asString()));
        node.get("profile", "ha", "subsystem", "osgi", "property", "org.osgi.framework.system.packages.extra").set(convertToSingleLine(node.get("profile", "ha", "subsystem", "osgi", "framework-property", "org.osgi.framework.system.packages.extra").asString()));
    }

    private static String convertToSingleLine(String value) {
        //Reformat the string so it works better in ParseAndMarshalModelsTestCase
        String[] values = value.split(",");
        StringBuilder formattedValue = new StringBuilder();
        boolean first = true;
        for (String val : values) {
            val = val.trim();
            if (!first) {
                formattedValue.append(", ");
            } else {
                first = false;
            }
            formattedValue.append(val);
        }
        return formattedValue.toString();
    }

    private static ServiceContainer setupServiceContainer() {
        return ServiceContainer.Factory.create("test");
    }

    private static void cleanup(final ServiceContainer serviceContainer) throws Exception {
        if (serviceContainer != null) {
            serviceContainer.shutdown();
            try {
                serviceContainer.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static void compare(ModelNode node1, ModelNode node2) {
        Assert.assertEquals(node1.getType(), node2.getType());
        if (node1.getType() == ModelType.OBJECT) {
            final Set<String> keys1 = node1.keys();
            final Set<String> keys2 = node2.keys();
            Assert.assertEquals(node1 + "\n" + node2, keys1.size(), keys2.size());

            for (String key : keys1) {
                final ModelNode child1 = node1.get(key);
                Assert.assertTrue("Missing: " + key + "\n" + node1 + "\n" + node2, node2.has(key));
                final ModelNode child2 = node2.get(key);
                if (child1.isDefined()) {
                    Assert.assertTrue(child1.toString(), child2.isDefined());
                    compare(child1, child2);
                } else {
                    Assert.assertFalse(child2.asString(), child2.isDefined());
                }
            }
        } else if (node1.getType() == ModelType.LIST) {
            List<ModelNode> list1 = node1.asList();
            List<ModelNode> list2 = node2.asList();
            Assert.assertEquals(list1 + "\n" + list2, list1.size(), list2.size());

            for (int i = 0; i < list1.size(); i++) {
                compare(list1.get(i), list2.get(i));
            }

        } else if (node1.getType() == ModelType.PROPERTY) {
            Property prop1 = node1.asProperty();
            Property prop2 = node2.asProperty();
            Assert.assertEquals(prop1 + "\n" + prop2, prop1.getName(), prop2.getName());
            compare(prop1.getValue(), prop2.getValue());

        } else {
            Assert.assertEquals("\n\"" + node1.asString() + "\"\n\"" + node2.asString() + "\"\n-----", node1.asString().trim(), node2.asString().trim());
        }
    }

    private static ModelController createController(final ServiceContainer serviceContainer, final ProcessType processType, final ModelNode model, final Setup registration) throws InterruptedException {
        final ServiceController<?> existingController = serviceContainer.getService(ServiceName.of("ModelController"));
        if (existingController != null) {
            final CountDownLatch latch = new CountDownLatch(1);
            existingController.addListener(new AbstractServiceListener<Object>() {
                public void listenerAdded(ServiceController<?> serviceController) {
                    serviceController.setMode(ServiceController.Mode.REMOVE);
                }

                public void transition(ServiceController<?> serviceController, ServiceController.Transition transition) {
                    if (transition.equals(ServiceController.Transition.REMOVING_to_REMOVED)) {
                        latch.countDown();
                    }
                }
            });
            latch.await();
        }

        ServiceTarget target = serviceContainer.subTarget();
        ModelControllerService svc = new ModelControllerService(processType, registration, model);
        ServiceBuilder<ModelController> builder = target.addService(ServiceName.of("ModelController"), svc);
        builder.install();
        svc.latch.await(30, TimeUnit.SECONDS);
        ModelController controller = svc.getValue();
        ModelNode setup = Util.getEmptyOperation("setup", new ModelNode());
        controller.execute(setup, null, null, null);

        return controller;
    }

    private static void executeOperations(ModelController controller, List<ModelNode> ops) {
        for (final ModelNode op : ops) {
            op.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);
            controller.execute(op, null, null, null);
        }
    }

    private static ModelNode loadServerModel(final ServiceContainer serviceContainer, final File file, final PathManagerService pathManagerService) throws Exception {
        final ExtensionRegistry extensionRegistry = new ExtensionRegistry(ProcessType.STANDALONE_SERVER, new RunningModeControl(RunningMode.ADMIN_ONLY), null, null);
        final QName rootElement = new QName(Namespace.CURRENT.getUriString(), "server");
        final StandaloneXml parser = new StandaloneXml(Module.getBootModuleLoader(), null, extensionRegistry);
        final XmlConfigurationPersister persister = new XmlConfigurationPersister(file, rootElement, parser, parser);
        for (Namespace namespace : Namespace.domainValues()) {
            if (namespace != Namespace.CURRENT) {
                persister.registerAdditionalRootElement(new QName(namespace.getUriString(), "server"), parser);
            }
        }
        extensionRegistry.setWriterRegistry(persister);
        final List<ModelNode> ops = persister.load();

        final ModelNode model = new ModelNode();
        final ModelController controller = createController(serviceContainer, ProcessType.STANDALONE_SERVER, model, new Setup() {
            @Override
            public void setup(ModelControllerService modelControllerService, Resource resource,
                    ManagementResourceRegistration rootRegistration, DelegatingConfigurableAuthorizer authorizer) {
                ServerRootResourceDefinition def = new ServerRootResourceDefinition(new MockContentRepository(),
                        persister, null, null, null, null, extensionRegistry, false, pathManagerService, authorizer,
                        AuditLogger.NO_OP_LOGGER, modelControllerService.getBootErrorCollector());
                def.registerAttributes(rootRegistration);
                def.registerOperations(rootRegistration);
                def.registerChildren(rootRegistration);
            }
        });

        final ModelNode caputreModelOp = new ModelNode();
        caputreModelOp.get(OP_ADDR).set(PathAddress.EMPTY_ADDRESS.toModelNode());
        caputreModelOp.get(OP).set("capture-model");

        final List<ModelNode> toRun = new ArrayList<ModelNode>(ops);
        toRun.add(caputreModelOp);
        executeOperations(controller, toRun);
        persister.store(model, null).commit();
        return model;
    }

    //TODO use HostInitializer & TestModelControllerService
    private static ModelNode loadHostModel(final ServiceContainer serviceContainer, final File file) throws Exception {
        final QName rootElement = new QName(Namespace.CURRENT.getUriString(), "host");
        final HostXml parser = new HostXml("host-controller", RunningMode.NORMAL, false);
        final XmlConfigurationPersister persister = new XmlConfigurationPersister(file, rootElement, parser, parser);
        for (Namespace namespace : Namespace.domainValues()) {
            if (namespace != Namespace.CURRENT) {
                persister.registerAdditionalRootElement(new QName(namespace.getUriString(), "host"), parser);
            }
        }
        final List<ModelNode> ops = persister.load();

        final ModelNode model = new ModelNode();

        final ModelController controller = createController(serviceContainer, ProcessType.HOST_CONTROLLER, model, new Setup() {
            public void setup(ModelControllerService modelControllerService, Resource resource, ManagementResourceRegistration root, DelegatingConfigurableAuthorizer authorizer) {

                final Resource host = Resource.Factory.create();
                resource.registerChild(PathElement.pathElement(HOST, "master"), host);

                // TODO maybe make creating of empty nodes part of the MNR description
                host.registerChild(PathElement.pathElement(ModelDescriptionConstants.CORE_SERVICE, ModelDescriptionConstants.MANAGEMENT), Resource.Factory.create());
                host.registerChild(PathElement.pathElement(ModelDescriptionConstants.CORE_SERVICE, ModelDescriptionConstants.SERVICE_CONTAINER), Resource.Factory.create());

                // Add of the host itself
                ManagementResourceRegistration hostRegistration = root.registerSubModel(
                        ResourceBuilder.Factory.create(PathElement.pathElement(HOST), new NonResolvingResourceDescriptionResolver()).build());

                // Other root resource operations
                XmlMarshallingHandler xmh = new XmlMarshallingHandler(persister);
                hostRegistration.registerOperationHandler(XmlMarshallingHandler.DEFINITION, xmh);
                hostRegistration.registerOperationHandler(NamespaceAddHandler.DEFINITION, NamespaceAddHandler.INSTANCE);
                hostRegistration.registerOperationHandler(SchemaLocationAddHandler.DEFINITION, SchemaLocationAddHandler.INSTANCE);
                hostRegistration.registerReadOnlyAttribute(HostResourceDefinition.MASTER, IsMasterHandler.INSTANCE);

                // System Properties
                ManagementResourceRegistration sysProps = hostRegistration.registerSubModel(SystemPropertyResourceDefinition.createForDomainOrHost(SystemPropertyResourceDefinition.Location.HOST));

                // Central Management
                final LocalHostControllerInfoImpl hostControllerInfo = new LocalHostControllerInfoImpl(new ControlledProcessState(false), "master");
                ResourceDefinition nativeDef = new NativeManagementResourceDefinition(hostControllerInfo);

                ResourceDefinition core = CoreManagementResourceDefinition.forHost(authorizer,
                        AuditLogger.NO_OP_LOGGER, MOCK_PATH_MANAGER, new EnvironmentNameReader() {

                            @Override
                            public boolean isServer() {
                                return false;
                            }

                            @Override
                            public String getServerName() {
                                return null;
                            }

                            @Override
                            public String getProductName() {
                                return null;
                            }

                            @Override
                            public String getHostName() {
                                return null;
                            }
                        }, new BootErrorCollector(), nativeDef);
                hostRegistration.registerSubModel(core);

                // Domain controller
                LocalDomainControllerAddHandler localDcAddHandler = new MockLocalDomainControllerAddHandler();
                hostRegistration.registerOperationHandler(LocalDomainControllerAddHandler.DEFINITION, localDcAddHandler, false);
                RemoteDomainControllerAddHandler remoteDcAddHandler = new MockRemoteDomainControllerAddHandler();
                hostRegistration.registerOperationHandler(RemoteDomainControllerAddHandler.DEFINITION, remoteDcAddHandler, false);

                // Jvms
                final ManagementResourceRegistration jvms = hostRegistration.registerSubModel(JvmResourceDefinition.GLOBAL);

                //Paths
                ManagementResourceRegistration paths = hostRegistration.registerSubModel(PathResourceDefinition.createSpecified(MOCK_PATH_MANAGER));

                //interface
                ManagementResourceRegistration interfaces = hostRegistration.registerSubModel(new InterfaceDefinition(
                        HostSpecifiedInterfaceAddHandler.INSTANCE,
                        HostSpecifiedInterfaceRemoveHandler.INSTANCE,
                        true
                ));

                //server configurations
                hostRegistration.registerSubModel(new ServerConfigResourceDefinition(null, MOCK_PATH_MANAGER));
            }
        });

        final ModelNode caputreModelOp = new ModelNode();
        caputreModelOp.get(OP_ADDR).set(PathAddress.EMPTY_ADDRESS.toModelNode());
        caputreModelOp.get(OP).set("capture-model");

        final List<ModelNode> toRun = new ArrayList<ModelNode>(ops);
        toRun.add(caputreModelOp);
        executeOperations(controller, toRun);

        model.get(HOST, "master", NAME).set("master");
        persister.store(model.get(HOST, "master"), null).commit();
        return model;
    }

    private static ModelNode loadDomainModel(final ServiceContainer serviceContainer, File file) throws Exception {
        final ExtensionRegistry extensionRegistry = new ExtensionRegistry(ProcessType.HOST_CONTROLLER, new RunningModeControl(RunningMode.NORMAL), null, null);
        final QName rootElement = new QName(Namespace.CURRENT.getUriString(), "domain");
        final DomainXml parser = new DomainXml(Module.getBootModuleLoader(), null, extensionRegistry);
        final XmlConfigurationPersister persister = new XmlConfigurationPersister(file, rootElement, parser, parser);
        for (Namespace namespace : Namespace.domainValues()) {
            if (namespace != Namespace.CURRENT) {
                persister.registerAdditionalRootElement(new QName(namespace.getUriString(), "domain"), parser);
            }
        }
        extensionRegistry.setWriterRegistry(persister);
        final List<ModelNode> ops = persister.load();
        final ModelNode model = new ModelNode();
        final ModelController controller = createController(serviceContainer, ProcessType.HOST_CONTROLLER, model, new Setup() {
            @Override
            public void setup(ModelControllerService modelControllerService, Resource resource,
                    ManagementResourceRegistration rootRegistration, DelegatingConfigurableAuthorizer authorizer) {
                DomainRootDefinition def = new DomainRootDefinition(null, persister, new MockContentRepository(),
                        new MockFileRepository(), true, null, extensionRegistry, null, MOCK_PATH_MANAGER, authorizer);
                def.initialize(rootRegistration);
            }
        });

        final ModelNode caputreModelOp = new ModelNode();
        caputreModelOp.get(OP_ADDR).set(PathAddress.EMPTY_ADDRESS.toModelNode());
        caputreModelOp.get(OP).set("capture-model");

        final List<ModelNode> toRun = new ArrayList<ModelNode>(ops);
        toRun.add(caputreModelOp);

        executeOperations(controller, toRun);

        persister.store(model, null).commit();
        return model;
    }

    interface Setup {

        void setup(ModelControllerService modelControllerService, Resource resource, ManagementResourceRegistration rootRegistration, DelegatingConfigurableAuthorizer authorizer);
    }

    static class ModelControllerService extends AbstractControllerService {

        private final CountDownLatch latch = new CountDownLatch(2);
        private final ModelNode model;
        private final Setup registration;

        ModelControllerService(final ProcessType processType, final Setup registration, final ModelNode model) {
            super(processType, new RunningModeControl(RunningMode.ADMIN_ONLY), new NullConfigurationPersister(), new ControlledProcessState(true),
                    ResourceBuilder.Factory.create(PathElement.pathElement("root"), new NonResolvingResourceDescriptionResolver()).build(), null, ExpressionResolver.TEST_RESOLVER, AuditLogger.NO_OP_LOGGER, new DelegatingConfigurableAuthorizer());
            this.model = model;
            this.registration = registration;
        }

        @Override
        protected BootErrorCollector getBootErrorCollector() {
            return super.getBootErrorCollector();
        }

        @Override
        public void start(StartContext context) throws StartException {
            super.start(context);
            latch.countDown();
        }

        @Override
        protected void bootThreadDone() {
            super.bootThreadDone();
            latch.countDown();
        }

        @Override
        protected void initModel(Resource rootResource, ManagementResourceRegistration rootRegistration, Resource modelControllerResource) {
            registration.setup(this, rootResource, rootRegistration, authorizer);

            rootRegistration.registerOperationHandler(new SimpleOperationDefinitionBuilder("capture-model", new NonResolvingResourceDescriptionResolver()).build(), new OperationStepHandler() {
                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    model.set(Resource.Tools.readModel(context.readResource(PathAddress.EMPTY_ADDRESS)));
                }
            });
            // TODO maybe make creating of empty nodes part of the MNR description
            rootResource.registerChild(PathElement.pathElement(ModelDescriptionConstants.CORE_SERVICE, ModelDescriptionConstants.MANAGEMENT), Resource.Factory.create());
            rootResource.registerChild(PathElement.pathElement(ModelDescriptionConstants.CORE_SERVICE, ModelDescriptionConstants.SERVICE_CONTAINER), Resource.Factory.create());
        }

    }

    private static class MockContentRepository implements ContentRepository {

        @Override
        public byte[] addContent(InputStream stream) throws IOException {
            return null;
        }

        @Override
        public VirtualFile getContent(byte[] hash) {
            return null;
        }

        @Override
        public boolean syncContent(ContentReference reference) {
            return hasContent(reference.getHash());
        }

        @Override
        public boolean hasContent(byte[] hash) {
            return false;
        }

        @Override
        public void removeContent(ContentReference reference) {
        }

        @Override
        public void addContentReference(ContentReference reference) {
        }

        @Override
        public Map<String, Set<String>> cleanObsoleteContent() {
            return null;
        }
    }

    private static class MockFileRepository implements HostFileRepository {

        @Override
        public File getFile(String relativePath) {
            return null;
        }

        @Override
        public File getConfigurationFile(String relativePath) {
            return null;
        }

        @Override
        public File[] getDeploymentFiles(ContentReference reference) {
            return null;
        }

        @Override
        public File getDeploymentRoot(ContentReference reference) {
            return null;
        }

        @Override
        public void deleteDeployment(ContentReference reference) {
        }
    }

    private static class MockLocalDomainControllerAddHandler extends LocalDomainControllerAddHandler {

        /**
         * Create the ServerAddHandler
         */
        protected MockLocalDomainControllerAddHandler() {
            super(null, null, null, null, null, null, null, MOCK_PATH_MANAGER);
        }

        @Override
        protected void initializeDomain() {
            // no-op
        }
    }

    private static class MockRemoteDomainControllerAddHandler extends RemoteDomainControllerAddHandler {

        /**
         * Create the ServerAddHandler
         */
        protected MockRemoteDomainControllerAddHandler() {
            super(null, null, null, null, null, null, null, null, MOCK_PATH_MANAGER);
        }

        @Override
        protected void initializeDomain(OperationContext context, ModelNode remoteDC) throws OperationFailedException {
            // no-op
        }
    }

    private static class TestPathManagerService extends PathManagerService {
        private TestPathManagerService(final ServiceTarget target) {
            final Properties props;
            if (System.getSecurityManager() == null) {
                props = System.getProperties();
            } else {
                props = AccessController.doPrivileged(new PrivilegedAction<Properties>() {
                    @Override
                    public Properties run() {
                        return System.getProperties();
                    }
                });
            }
            addHardcodedAbsolutePath(target, ServerEnvironment.SERVER_BASE_DIR, props.getProperty(ServerEnvironment.SERVER_BASE_DIR));
            addHardcodedAbsolutePath(target, ServerEnvironment.SERVER_CONFIG_DIR, props.getProperty(ServerEnvironment.SERVER_CONFIG_DIR));
            addHardcodedAbsolutePath(target, ServerEnvironment.SERVER_DATA_DIR, props.getProperty(ServerEnvironment.SERVER_DATA_DIR));
            addHardcodedAbsolutePath(target, ServerEnvironment.SERVER_LOG_DIR, props.getProperty(ServerEnvironment.SERVER_LOG_DIR));
            addHardcodedAbsolutePath(target, ServerEnvironment.SERVER_TEMP_DIR, props.getProperty(ServerEnvironment.SERVER_TEMP_DIR));
        }
    }

    private static PathManagerService MOCK_PATH_MANAGER = new PathManagerService() {
    };

    static final LocalHostControllerInfo MOCK_HOST_CONTROLLER_INFO = new LocalHostControllerInfo() {

        @Override
        public boolean isMasterDomainController() {
            return true;
        }

        @Override
        public String getRemoteDomainControllerUsername() {
            return null;
        }

        @Override
        public List<DiscoveryOption> getRemoteDomainControllerDiscoveryOptions() {
            return null;
        }

        @Override
        public ControlledProcessState.State getProcessState() {
            return null;
        }

        @Override
        public String getNativeManagementSecurityRealm() {
            return null;
        }

        @Override
        public int getNativeManagementPort() {
            return 0;
        }

        @Override
        public String getNativeManagementInterface() {
            return null;
        }

        @Override
        public String getLocalHostName() {
            return null;
        }

        @Override
        public String getHttpManagementSecurityRealm() {
            return null;
        }

        @Override
        public int getHttpManagementSecurePort() {
            return 0;
        }

        @Override
        public int getHttpManagementPort() {
            return 0;
        }

        @Override
        public String getHttpManagementInterface() {
            return null;
        }

        @Override
        public String getHttpManagementSecureInterface() {
            return null;
        }
    };
}
