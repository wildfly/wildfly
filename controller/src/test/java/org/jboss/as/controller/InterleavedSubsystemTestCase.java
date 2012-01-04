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

package org.jboss.as.controller;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODULE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_RESOURCES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_TYPES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.jboss.as.controller.descriptions.common.CommonProviders;
import org.jboss.as.controller.extension.ExtensionAddHandler;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.global.GlobalOperationHandlers;
import org.jboss.as.controller.persistence.AbstractConfigurationPersister;
import org.jboss.as.controller.persistence.ConfigurationPersister;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests that a mix of extension add ops and subsystem ops works. This is the way the app client container was coded
 * (although that may change).
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class InterleavedSubsystemTestCase {

    private ServiceContainer container;

    @Before
    public void setupController() throws InterruptedException {
    }

    @After
    public void shutdownServiceContainer() {
        if (container != null) {
            container.shutdown();
            try {
                container.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            finally {
                container = null;
            }
        }
    }

    @Test
    public void testInterleavedOps() throws Exception {
        container = ServiceContainer.Factory.create("test");
        ServiceTarget target = container.subTarget();
        ControlledProcessState processState = new ControlledProcessState(true);
        ModelControllerImplUnitTestCase.ModelControllerService svc =
                new InterleavedSubsystemModelControllerService(processState);
        ServiceBuilder<ModelController> builder = target.addService(ServiceName.of("ModelController"), svc);
        builder.install();
        svc.latch.await();
        ModelController controller = svc.getValue();
        processState.setRunning();

        final ModelNode op = Util.getEmptyOperation(READ_RESOURCE_OPERATION, new ModelNode());
        op.get(RECURSIVE).set(true);

        ModelNode result = controller.execute(op, null, null, null);
        assertTrue(result.hasDefined(OUTCOME));
        assertEquals(SUCCESS, result.get(OUTCOME).asString());
        assertTrue(result.hasDefined(RESULT));
        assertTrue(result.get(RESULT).hasDefined(EXTENSION));
        assertTrue(result.get(RESULT, EXTENSION).hasDefined("a"));
        assertTrue(result.get(RESULT, EXTENSION, "a").hasDefined(MODULE));
        assertEquals("a", result.get(RESULT, EXTENSION, "a", MODULE).asString());
        assertTrue(result.get(RESULT, EXTENSION).hasDefined("b"));
        assertTrue(result.get(RESULT, EXTENSION, "b").hasDefined(MODULE));
        assertEquals("b", result.get(RESULT, EXTENSION, "b", MODULE).asString());
        assertTrue(result.get(RESULT, EXTENSION).hasDefined("c"));
        assertTrue(result.get(RESULT, EXTENSION, "c").hasDefined(MODULE));
        assertEquals("c", result.get(RESULT, EXTENSION, "c", MODULE).asString());
        assertTrue(result.get(RESULT).hasDefined(SUBSYSTEM));
        assertTrue(result.get(RESULT, SUBSYSTEM).hasDefined("a"));
        assertTrue(result.get(RESULT, SUBSYSTEM, "a").hasDefined("attribute"));
        assertTrue(result.get(RESULT, SUBSYSTEM, "a", "attribute").asBoolean());
        assertTrue(result.get(RESULT, SUBSYSTEM).hasDefined("b"));
        assertTrue(result.get(RESULT, SUBSYSTEM, "b").hasDefined("attribute"));
        assertTrue(result.get(RESULT, SUBSYSTEM, "b", "attribute").asBoolean());
        assertTrue(result.get(RESULT, SUBSYSTEM).hasDefined("c"));
        assertTrue(result.get(RESULT, SUBSYSTEM, "c").hasDefined("attribute"));
        assertTrue(result.get(RESULT, SUBSYSTEM, "c", "attribute").asBoolean());

    }

    public static class InterleavedSubsystemModelControllerService extends ModelControllerImplUnitTestCase.ModelControllerService {

        InterleavedSubsystemModelControllerService(final ControlledProcessState processState) {
            super(processState, InterleavedConfigurationPersister.INSTANCE);
        }

        @Override
        protected void initModel(Resource rootResource, ManagementResourceRegistration rootRegistration) {

            // The standards (many aren't needed but I'm too lazy to decide which)
            rootRegistration.registerOperationHandler(READ_RESOURCE_OPERATION, GlobalOperationHandlers.READ_RESOURCE, CommonProviders.READ_RESOURCE_PROVIDER, true);
            rootRegistration.registerOperationHandler(READ_ATTRIBUTE_OPERATION, GlobalOperationHandlers.READ_ATTRIBUTE, CommonProviders.READ_ATTRIBUTE_PROVIDER, true);
            rootRegistration.registerOperationHandler(READ_RESOURCE_DESCRIPTION_OPERATION, GlobalOperationHandlers.READ_RESOURCE_DESCRIPTION, CommonProviders.READ_RESOURCE_DESCRIPTION_PROVIDER, true);
            rootRegistration.registerOperationHandler(READ_CHILDREN_NAMES_OPERATION, GlobalOperationHandlers.READ_CHILDREN_NAMES, CommonProviders.READ_CHILDREN_NAMES_PROVIDER, true);
            rootRegistration.registerOperationHandler(READ_CHILDREN_TYPES_OPERATION, GlobalOperationHandlers.READ_CHILDREN_TYPES, CommonProviders.READ_CHILDREN_TYPES_PROVIDER, true);
            rootRegistration.registerOperationHandler(READ_CHILDREN_RESOURCES_OPERATION, GlobalOperationHandlers.READ_CHILDREN_RESOURCES, CommonProviders.READ_CHILDREN_RESOURCES_PROVIDER, true);
            rootRegistration.registerOperationHandler(READ_OPERATION_NAMES_OPERATION, GlobalOperationHandlers.READ_OPERATION_NAMES, CommonProviders.READ_OPERATION_NAMES_PROVIDER, true);
            rootRegistration.registerOperationHandler(READ_OPERATION_DESCRIPTION_OPERATION, GlobalOperationHandlers.READ_OPERATION_DESCRIPTION, CommonProviders.READ_OPERATION_PROVIDER, true);
            rootRegistration.registerOperationHandler(WRITE_ATTRIBUTE_OPERATION, GlobalOperationHandlers.WRITE_ATTRIBUTE, CommonProviders.WRITE_ATTRIBUTE_PROVIDER, true);

            ManagementResourceRegistration extensions = rootRegistration.registerSubModel(PathElement.pathElement(EXTENSION), ModelControllerImplUnitTestCase.DESC_PROVIDER);
            extensions.registerOperationHandler("add", new FakeExtensionAddHandler(rootRegistration), ModelControllerImplUnitTestCase.DESC_PROVIDER);

        }

    }

    private static class FakeExtensionAddHandler extends ExtensionAddHandler {

        private final ManagementResourceRegistration rootRegistration;

        private FakeExtensionAddHandler(ManagementResourceRegistration rootRegistration) {
            super(new ExtensionRegistry(ProcessType.EMBEDDED_SERVER, new RunningModeControl(RunningMode.NORMAL)), false);
            this.rootRegistration = rootRegistration;
        }

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            Resource resource = context.createResource(PathAddress.EMPTY_ADDRESS);

            final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
            String module = address.getLastElement().getValue();
            resource.getModel().get(MODULE).set(module);

            ManagementResourceRegistration subsystem = rootRegistration.registerSubModel(PathElement.pathElement(SUBSYSTEM, module), ModelControllerImplUnitTestCase.DESC_PROVIDER);
            subsystem.registerOperationHandler("add", new FakeSubsystemAddHandler(), ModelControllerImplUnitTestCase.DESC_PROVIDER);

            context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
        }
    }

    private static class FakeSubsystemAddHandler extends AbstractAddStepHandler {

        @Override
        protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
            model.get("attribute").set(true);
        }
    }

    private static class InterleavedConfigurationPersister extends AbstractConfigurationPersister {

        private static final InterleavedConfigurationPersister INSTANCE = new InterleavedConfigurationPersister();

        private InterleavedConfigurationPersister() {
            super(null);
        }

        /** {@inheritDoc} */
        @Override
        public PersistenceResource store(final ModelNode model, Set<PathAddress> affectedAddresses) {
            return NullPersistenceResource.INSTANCE;
        }

        /** {@inheritDoc} */
        @Override
        public List<ModelNode> load() {
            final List<ModelNode> bootOps = new ArrayList<ModelNode>();
            final ModelNode addrAE = new ModelNode().setEmptyList().add(EXTENSION, "a");
            final ModelNode addrAS = new ModelNode().setEmptyList().add(SUBSYSTEM, "a");
            final ModelNode addrBE = new ModelNode().setEmptyList().add(EXTENSION, "b");
            final ModelNode addrBS = new ModelNode().setEmptyList().add(SUBSYSTEM, "b");
            final ModelNode addrCE = new ModelNode().setEmptyList().add(EXTENSION, "c");
            final ModelNode addrCS = new ModelNode().setEmptyList().add(SUBSYSTEM, "c");
            bootOps.add(Util.getEmptyOperation(ADD, addrAE));
            bootOps.add(Util.getEmptyOperation(ADD, addrAS));
            bootOps.add(Util.getEmptyOperation(ADD, addrBE));
            bootOps.add(Util.getEmptyOperation(ADD, addrBS));
            bootOps.add(Util.getEmptyOperation(ADD, addrCE));
            bootOps.add(Util.getEmptyOperation(ADD, addrCS));
            return bootOps;
        }

        private static class NullPersistenceResource implements ConfigurationPersister.PersistenceResource {

            private static final NullPersistenceResource INSTANCE = new NullPersistenceResource();

            @Override
            public void commit() {
            }

            @Override
            public void rollback() {
            }
        }
    }
}
