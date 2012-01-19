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

package org.jboss.as.server.test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import junit.framework.Assert;

import org.jboss.as.controller.AbstractControllerService;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.RunningModeControl;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.common.InterfaceDescription;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.persistence.AbstractConfigurationPersister;
import org.jboss.as.controller.persistence.ConfigurationPersistenceException;
import org.jboss.as.controller.persistence.ModelMarshallingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.ServerControllerModelUtil;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.Services;
import org.jboss.as.server.controller.descriptions.ServerDescriptionProviders;
import org.jboss.as.server.deployment.repository.api.ContentRepository;
import org.jboss.as.server.file.repository.api.DeploymentFileRepository;
import org.jboss.as.server.parsing.StandaloneXml;
import org.jboss.as.version.ProductConfig;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.vfs.VirtualFile;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Basic server controller unit test.
 *
 * @author Emanuel Muckenhuber
 */
public class ServerControllerUnitTestCase {

    private final ServiceContainer container = ServiceContainer.Factory.create();
    private ModelController controller;

    @Before
    public void beforeClass() throws Exception {
        final ServiceTarget target = container.subTarget();
        final ExtensionRegistry extensionRegistry = new ExtensionRegistry(ProcessType.STANDALONE_SERVER, new RunningModeControl(RunningMode.NORMAL));
        final StringConfigurationPersister persister = new StringConfigurationPersister(Collections.<ModelNode>emptyList(), new StandaloneXml(null, null, extensionRegistry));
        extensionRegistry.setWriterRegistry(persister);
        final ControlledProcessState processState = new ControlledProcessState(true);
        final ModelControllerService svc = new ModelControllerService(processState, persister);
        final ServiceBuilder<ModelController> builder = target.addService(Services.JBOSS_SERVER_CONTROLLER, svc);
        builder.install();

        svc.latch.await();
        this.controller = svc.getValue();

    }

    @After
    public void afterClass() {
        container.shutdown();
    }

    @Test
    public void testInterfacesAlternatives() throws IOException {
        final ModelControllerClient client = controller.createClient(Executors.newCachedThreadPool());
        final ModelNode base = new ModelNode();
        base.get(ModelDescriptionConstants.OP).set("add");
        base.get(ModelDescriptionConstants.OP_ADDR).add("interface", "alternative");
        {
            final ModelNode operation = base.clone();
            operation.get("any-address").set(true);
            populateCritieria(operation, false);
            executeForFailure(client, operation);
        }
        {
            final ModelNode operation = base.clone();
            operation.get("any-ipv4-address").set(true);
            populateCritieria(operation, false);
            executeForFailure(client, operation);
        }
        {
            final ModelNode operation = base.clone();
            operation.get("any-ipv6-address").set(true);
            populateCritieria(operation, false);
            executeForFailure(client, operation);
        }
        {
            final ModelNode operation = base.clone();
            operation.get("any-ipv6-address").set(true);
            operation.get("any-ipv4-address").set(true);
            executeForFailure(client, operation);
        }
        {
            final ModelNode operation = base.clone();
            operation.get("any-address").set(true);
            operation.get("any-ipv4-address").set(true);
            executeForFailure(client, operation);
        }
        {
            final ModelNode operation = base.clone();
            // operation.get("any-address").set(true);
            populateCritieria(operation, false);
            executeForResult(client, operation);
        }
    }

    @Test
    public void testUpdateInterface() throws IOException {
        final ModelControllerClient client = controller.createClient(Executors.newCachedThreadPool());
        final ModelNode address = new ModelNode();
        address.add("interface", "other");
        {
            final ModelNode operation = new ModelNode();
            operation.get(ModelDescriptionConstants.OP).set("add");
            operation.get(ModelDescriptionConstants.OP_ADDR).set(address);
            operation.get("any-address").set(true);

            executeForResult(client, operation);
            final ModelNode resource = readResource(client, operation.get(ModelDescriptionConstants.OP_ADDR));
            Assert.assertTrue(resource.get("any-address").asBoolean());
        }
        {
            final ModelNode composite = new ModelNode();
            composite.get(ModelDescriptionConstants.OP).set("composite");
            composite.get(ModelDescriptionConstants.OP_ADDR).setEmptyList();
            final ModelNode one = composite.get(ModelDescriptionConstants.STEPS).add();
            one.get(ModelDescriptionConstants.OP).set("write-attribute");
            one.get(ModelDescriptionConstants.OP_ADDR).set(address);
            one.get(ModelDescriptionConstants.NAME).set("any-address");
            one.get(ModelDescriptionConstants.VALUE);

            final ModelNode two = composite.get(ModelDescriptionConstants.STEPS).add();
            two.get(ModelDescriptionConstants.OP).set("write-attribute");
            two.get(ModelDescriptionConstants.OP_ADDR).set(address);
            two.get(ModelDescriptionConstants.NAME).set("inet-address");
            two.get(ModelDescriptionConstants.VALUE).set("127.0.0.1");

            executeForResult(client, composite);
            final ModelNode resource = readResource(client, address);
            Assert.assertFalse(resource.hasDefined("any-address"));
            Assert.assertEquals("127.0.0.1", resource.get("inet-address").asString());
        }
    }

    @Test
    public void testComplexInterface() throws IOException {

        final ModelNode operation = new ModelNode();
        operation.get(ModelDescriptionConstants.OP).set("add");
        operation.get(ModelDescriptionConstants.OP_ADDR).add("interface", "complex");
        // This won't be resolvable with the runtime layer enabled
        populateCritieria(operation, false);
        populateCritieria(operation.get("not"), true);
        populateCritieria(operation.get("any"), true);

        final ModelControllerClient client = controller.createClient(Executors.newCachedThreadPool());

        executeForResult(client, operation);
    }

    protected void populateCritieria(final ModelNode model, final boolean nested) {
        for(final AttributeDefinition def : InterfaceDescription.NESTED_ATTRIBUTES) {
            final ModelNode node = model.get(def.getName());
            if(def.getType() == ModelType.BOOLEAN) {
                node.set(true);
            } else if (def == InterfaceDescription.INET_ADDRESS || def == InterfaceDescription.LOOPBACK_ADDRESS) {
                if (nested && def == InterfaceDescription.INET_ADDRESS) {
                    node.add("127.0.0.1");
                } else {
                    node.set("127.0.0.1");
                }
            } else if (def == InterfaceDescription.NIC || def == InterfaceDescription.NIC_MATCH) {
                if (nested) {
                    node.add("lo");
                } else {
                    node.set("lo");
                }
            } else if (def == InterfaceDescription.SUBNET_MATCH) {
                if (nested) {
                    node.add("127.0.0.1/24");
                } else {
                    node.set("127.0.0.0/24");
                }
            }
        }
    }

    private static class ModelControllerService extends AbstractControllerService {

        final CountDownLatch latch = new CountDownLatch(1);
        final StringConfigurationPersister persister;
        final ControlledProcessState processState;
        volatile ManagementResourceRegistration rootRegistration;
        volatile Exception error;

        ModelControllerService(final ControlledProcessState processState, final StringConfigurationPersister persister) {
            super(ProcessType.EMBEDDED_SERVER, new RunningModeControl(RunningMode.ADMIN_ONLY), persister, processState, ServerDescriptionProviders.ROOT_PROVIDER, null, ExpressionResolver.DEFAULT);
            this.persister = persister;
            this.processState = processState;
        }

        @Override
        protected void initModel(Resource rootResource, ManagementResourceRegistration rootRegistration) {
            this.rootRegistration = rootRegistration;
            Properties properties = new Properties();
            properties.put("jboss.home.dir", ".");

            final String hostControllerName = "hostControllerName"; // Host Controller name may not be null when in a managed domain
            final ServerEnvironment environment = new ServerEnvironment(hostControllerName, properties, new HashMap<String, String>(), null, ServerEnvironment.LaunchType.DOMAIN, null, new ProductConfig(Module.getBootModuleLoader(), "."));
            final ExtensionRegistry extensionRegistry = new ExtensionRegistry(ProcessType.STANDALONE_SERVER, new RunningModeControl(RunningMode.NORMAL));
            ServerControllerModelUtil.initOperations(rootRegistration, MockRepository.INSTANCE, persister, environment, processState, null, null, extensionRegistry, false, MockRepository.INSTANCE);
        }

        @Override
        protected void boot(List<ModelNode> bootOperations) throws ConfigurationPersistenceException {
            try {
                super.boot(persister.bootOperations);
            } catch (Exception e) {
                error = e;
            } catch (Throwable t) {
                error = new Exception(t);
            } finally {
                latch.countDown();
            }
        }

        @Override
        public void start(StartContext context) throws StartException {
            super.start(context);
        }
    }

    static class StringConfigurationPersister extends AbstractConfigurationPersister {

        private final List<ModelNode> bootOperations;
        volatile String marshalled;

        public StringConfigurationPersister(List<ModelNode> bootOperations, XMLElementWriter<ModelMarshallingContext> rootDeparser) {
            super(rootDeparser);
            this.bootOperations = bootOperations;
        }

        @Override
        public PersistenceResource store(ModelNode model, Set<PathAddress> affectedAddresses)
                throws ConfigurationPersistenceException {
            return new StringPersistenceResource(model, this);
        }

        @Override
        public List<ModelNode> load() throws ConfigurationPersistenceException {
            return bootOperations;
        }

        private class StringPersistenceResource implements PersistenceResource {

            private byte[] bytes;
            private final AbstractConfigurationPersister persister;

            StringPersistenceResource(final ModelNode model, final AbstractConfigurationPersister persister) throws ConfigurationPersistenceException {
                this.persister = persister;
                ByteArrayOutputStream output = new ByteArrayOutputStream(1024 * 8);
                try {
                    try {
                        persister.marshallAsXml(model, output);
                    } finally {
                        try {
                            output.close();
                        } catch (Exception ignore) {
                        }
                        bytes = output.toByteArray();
                    }
                } catch (Exception e) {
                    throw new ConfigurationPersistenceException("Failed to marshal configuration", e);
                }
            }

            @Override
            public void commit() {
                StringConfigurationPersister.this.marshalled = new String(bytes);
            }

            @Override
            public void rollback() {
                marshalled = null;
            }
        }
    }

    static ModelNode readResource(final ModelControllerClient client, final ModelNode address) {
        final ModelNode operation = new ModelNode();
        operation.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.READ_RESOURCE_OPERATION);
        operation.get(ModelDescriptionConstants.OP_ADDR).set(address);
        return executeForResult(client, operation);
    }

    static void executeForFailure(final ModelControllerClient client, final ModelNode operation) {
        try {
            final ModelNode result = client.execute(operation);
            if (! result.hasDefined("outcome") && ! ModelDescriptionConstants.FAILED.equals(result.get("outcome").asString())) {
                Assert.fail("Operation outcome is " + result.get("outcome").asString());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static ModelNode executeForResult(final ModelControllerClient client, final ModelNode operation) {
        try {
            final ModelNode result = client.execute(operation);
            if (result.hasDefined("outcome") && "success".equals(result.get("outcome").asString())) {
                return result.get("result");
            } else {
                Assert.fail("Operation outcome is " + result.get("outcome").asString());
                throw new RuntimeException(); // not reached
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static final class MockRepository implements ContentRepository, DeploymentFileRepository {

        static MockRepository INSTANCE = new MockRepository();

        @Override
        public File[] getDeploymentFiles(byte[] deploymentHash) {
            return null;
        }

        @Override
        public File getDeploymentRoot(byte[] deploymentHash) {
            return null;
        }

        @Override
        public void deleteDeployment(byte[] deploymentHash) {
        }

        @Override
        public byte[] addContent(InputStream stream) throws IOException {
            return null;
        }

        @Override
        public VirtualFile getContent(byte[] hash) {
            return null;
        }

        @Override
        public boolean hasContent(byte[] hash) {
            return false;
        }

        @Override
        public void removeContent(byte[] hash) {
        }

    }

}
