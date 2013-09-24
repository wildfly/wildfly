/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.controller.access.rbac;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_RESOURCES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_TYPES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelOnlyWriteAttributeHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.access.constraint.SensitivityClassification;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.access.management.ConstrainedResourceDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.NonResolvingResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.global.GlobalOperationHandlers;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.test.AbstractControllerTestBase;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ReadOperationNamesRbacTestCase extends AbstractControllerTestBase {

    private static final PathElement ONE = PathElement.pathElement("one");
    private static final PathElement ONE_A = PathElement.pathElement("one", "a");
    private static final PathAddress ONE_ADDR = PathAddress.pathAddress(ONE);
    private static final PathAddress ONE_A_ADDR = PathAddress.pathAddress(ONE_A);

    private static final SensitiveTargetAccessConstraintDefinition ADDRESS_READ_WRITE_SENSITIVITY = createSensitivityConstraint("sensitive-access-read-write", true, true, true);
    private static final SensitiveTargetAccessConstraintDefinition READ_WRITE_SENSITIVITY = createSensitivityConstraint("sensitive-read-write", false, true, true);
    private static final SensitiveTargetAccessConstraintDefinition WRITE_SENSITIVITY = createSensitivityConstraint("sensitive-write", false, false, true);
    private static final SensitiveTargetAccessConstraintDefinition READ_SENSITIVITY = createSensitivityConstraint("sensitive-read", false, true, false);


    private static final String OP_CONFIG_RW_ACCESS_READ_WRITE = "config-rw-access-read-write";
    private static final String OP_CONFIG_RW_READ_WRITE = "config-rw-read-write";
    private static final String OP_CONFIG_RW_WRITE = "config-rw-write";
    private static final String OP_CONFIG_RW_READ = "config-rw-read";
    private static final String OP_CONFIG_RW_NONE = "config-rw-none";
    private static final String OP_RUNTIME_RW_ACCESS_READ_WRITE = "runtime-rw-access-read-write";
    private static final String OP_RUNTIME_RW_READ_WRITE = "runtime-rw-read-write";
    private static final String OP_RUNTIME_RW_WRITE = "runtime-rw-write";
    private static final String OP_RUNTIME_RW_READ = "runtime-rw-read";
    private static final String OP_RUNTIME_RW_NONE = "runtime-rw-none";
    private static final String OP_CONFIG_RO_ACCESS_READ_WRITE = "config-ro-access-read-write";
    private static final String OP_CONFIG_RO_READ_WRITE = "config-ro-read-write";
    private static final String OP_CONFIG_RO_WRITE = "config-ro-write";
    private static final String OP_CONFIG_RO_READ = "config-ro-read";
    private static final String OP_CONFIG_RO_NONE = "config-ro-none";
    private static final String OP_RUNTIME_RO_ACCESS_READ_WRITE = "runtime-ro-access-read-write";
    private static final String OP_RUNTIME_RO_READ_WRITE = "runtime-ro-read-write";
    private static final String OP_RUNTIME_RO_WRITE = "runtime-ro-write";
    private static final String OP_RUNTIME_RO_READ = "runtime-ro-read";
    private static final String OP_RUNTIME_RO_NONE = "runtime-ro-none";


    private static final Set<String> ALL_OPERATION_NAMES;
    private static final String[] ALL_OPERATION_NAMES_ARRAY;
    static {
        ALL_OPERATION_NAMES_ARRAY = new String[] {ADD, REMOVE,
                READ_ATTRIBUTE_OPERATION, READ_CHILDREN_NAMES_OPERATION, READ_CHILDREN_RESOURCES_OPERATION, READ_CHILDREN_TYPES_OPERATION, READ_OPERATION_DESCRIPTION_OPERATION,
                READ_OPERATION_NAMES_OPERATION, READ_RESOURCE_OPERATION, READ_RESOURCE_DESCRIPTION_OPERATION, UNDEFINE_ATTRIBUTE_OPERATION, WRITE_ATTRIBUTE_OPERATION,
                OP_CONFIG_RW_ACCESS_READ_WRITE, OP_CONFIG_RW_READ_WRITE, OP_CONFIG_RW_WRITE, OP_CONFIG_RW_READ, OP_CONFIG_RW_NONE,
                OP_RUNTIME_RW_ACCESS_READ_WRITE, OP_RUNTIME_RW_READ_WRITE, OP_RUNTIME_RW_WRITE, OP_RUNTIME_RW_READ, OP_RUNTIME_RW_NONE,
                OP_CONFIG_RO_ACCESS_READ_WRITE, OP_CONFIG_RO_READ_WRITE, OP_CONFIG_RO_WRITE, OP_CONFIG_RO_READ, OP_CONFIG_RO_NONE,
                OP_RUNTIME_RO_ACCESS_READ_WRITE, OP_RUNTIME_RO_READ_WRITE, OP_RUNTIME_RO_WRITE, OP_RUNTIME_RO_READ, OP_RUNTIME_RO_NONE};
        HashSet<String> allNames = new HashSet<String>(
                Arrays.asList(ALL_OPERATION_NAMES_ARRAY));
        ALL_OPERATION_NAMES = Collections.unmodifiableSet(allNames);
    }

    private volatile ManagementResourceRegistration rootRegistration;
    private volatile Resource rootResource;

    @Test
    public void testReadOperationNamesAsMonitorSensitiveAddressableResource() throws Exception {
        registerOperationResource(ADDRESS_READ_WRITE_SENSITIVITY);
        executeForFailure(createReadOperationNamesOperation(ONE_ADDR, StandardRole.MONITOR, false));
    }

    @Test
    public void testReadOperationNamesAsMonitorNonSensitiveResource() throws Exception {
        registerOperationResource(null);
        testReadOperationNames(StandardRole.MONITOR, true, new String[] {
                READ_ATTRIBUTE_OPERATION, READ_CHILDREN_NAMES_OPERATION, READ_CHILDREN_RESOURCES_OPERATION, READ_CHILDREN_TYPES_OPERATION, READ_OPERATION_DESCRIPTION_OPERATION,
                READ_OPERATION_NAMES_OPERATION, READ_RESOURCE_OPERATION, READ_RESOURCE_DESCRIPTION_OPERATION,
                OP_CONFIG_RO_WRITE /*Although this has a write sensitivity, the operation is read-only so the sensitivity should not be relevant*/,
                OP_CONFIG_RO_NONE,
                OP_RUNTIME_RO_WRITE /*Although this has a write sensitivity, the operation is read-only so the sensitivity should not be relevant*/,
                OP_RUNTIME_RO_NONE});
        testReadOperationNames(StandardRole.MONITOR, false, ALL_OPERATION_NAMES_ARRAY);
    }

    @Test
    public void testReadOperationNamesAsMaintainerSensitiveAddressableResource() throws Exception {
        registerOperationResource(ADDRESS_READ_WRITE_SENSITIVITY);
        executeForFailure(createReadOperationNamesOperation(ONE_ADDR, StandardRole.MONITOR, false));
    }

    @Test
    public void testReadOperationNamesAsMaintainerNonSensitiveResource() throws Exception {
        registerOperationResource(null);
        testReadOperationNames(StandardRole.MAINTAINER, true, new String[] {ADD, REMOVE,
                READ_ATTRIBUTE_OPERATION, READ_CHILDREN_NAMES_OPERATION, READ_CHILDREN_RESOURCES_OPERATION, READ_CHILDREN_TYPES_OPERATION, READ_OPERATION_DESCRIPTION_OPERATION,
                READ_OPERATION_NAMES_OPERATION, READ_RESOURCE_OPERATION, READ_RESOURCE_DESCRIPTION_OPERATION, UNDEFINE_ATTRIBUTE_OPERATION, WRITE_ATTRIBUTE_OPERATION,
                OP_CONFIG_RW_NONE, OP_RUNTIME_RW_NONE,
                OP_CONFIG_RO_WRITE /*Although this has a write sensitivity, the operation is read-only so the sensitivity should not be relevant*/,
                OP_CONFIG_RO_NONE,
                OP_RUNTIME_RO_WRITE /*Although this has a write sensitivity, the operation is read-only so the sensitivity should not be relevant*/,
                OP_RUNTIME_RO_NONE});
        testReadOperationNames(StandardRole.MAINTAINER, false, ALL_OPERATION_NAMES_ARRAY);
    }

    @Test
    public void testReadOperationNamesAsAdministratorSensitiveAddressableResource() throws Exception {
        testReadOperationNamesAsAdministrator(ADDRESS_READ_WRITE_SENSITIVITY);
    }

    @Test
    public void testReadOperationNamesAsAdminstratorNonSensitiveResource() throws Exception {
        testReadOperationNamesAsAdministrator(null);
    }


    private void testReadOperationNamesAsAdministrator(SensitiveTargetAccessConstraintDefinition resourceSensitivity) throws Exception {
        registerOperationResource(resourceSensitivity);
        testReadOperationNames(StandardRole.ADMINISTRATOR, true, ALL_OPERATION_NAMES_ARRAY);
        testReadOperationNames(StandardRole.ADMINISTRATOR, false, ALL_OPERATION_NAMES_ARRAY);
    }

    private void testReadOperationNames(StandardRole role, boolean accessControl, String[] expectedOperationNames) throws Exception {
        executeAndCompareOperations(role, accessControl, ONE_ADDR, expectedOperationNames);
        executeAndCompareOperations(role, accessControl, ONE_A_ADDR, expectedOperationNames);
    }

    private void executeAndCompareOperations(StandardRole role, boolean accessControl, PathAddress address, String[] expectedOperationNames) throws Exception {
        ModelNode op = createReadOperationNamesOperation(address, role, accessControl);
        ModelNode result = executeCheckNoFailure(op);
        compareOperations(result.get(ModelDescriptionConstants.RESULT), expectedOperationNames);
        List<String> filteredOps = new ArrayList<String>();
        if (accessControl) {
             filteredOps.addAll(ALL_OPERATION_NAMES);
             filteredOps.removeAll(Arrays.asList(expectedOperationNames));
        }
        ModelNode filtered = result.get(ModelDescriptionConstants.RESPONSE_HEADERS, ModelDescriptionConstants.ACCESS_CONTROL, ModelDescriptionConstants.FILTERED_OPERATIONS);
        if (filteredOps.size() > 0) {
            compareOperations(filtered, filteredOps.toArray(new String[filteredOps.size()]));
        } else {
            Assert.assertFalse(filtered.isDefined());
        }
    }

    private void registerOperationResource(SensitiveTargetAccessConstraintDefinition resourceSensitivity) {
        ChildResourceDefinition oneChild = resourceSensitivity != null ?
                new ChildResourceDefinition(ONE, resourceSensitivity) : new ChildResourceDefinition(ONE);
        oneChild.addOperation(OP_CONFIG_RW_ACCESS_READ_WRITE, false, false, ADDRESS_READ_WRITE_SENSITIVITY);
        oneChild.addOperation(OP_CONFIG_RW_READ_WRITE, false, false, READ_WRITE_SENSITIVITY);
        oneChild.addOperation(OP_CONFIG_RW_WRITE, false, false, WRITE_SENSITIVITY);
        oneChild.addOperation(OP_CONFIG_RW_READ, false, false, READ_SENSITIVITY);
        oneChild.addOperation(OP_CONFIG_RW_NONE, false, false);
        oneChild.addOperation(OP_RUNTIME_RW_ACCESS_READ_WRITE, false, true, ADDRESS_READ_WRITE_SENSITIVITY);
        oneChild.addOperation(OP_RUNTIME_RW_READ_WRITE, false, true, READ_WRITE_SENSITIVITY);
        oneChild.addOperation(OP_RUNTIME_RW_WRITE, false, true, WRITE_SENSITIVITY);
        oneChild.addOperation(OP_RUNTIME_RW_READ, false, true, READ_SENSITIVITY);
        oneChild.addOperation(OP_RUNTIME_RW_NONE, false, true);
        oneChild.addOperation(OP_CONFIG_RO_ACCESS_READ_WRITE, true, false, ADDRESS_READ_WRITE_SENSITIVITY);
        oneChild.addOperation(OP_CONFIG_RO_READ_WRITE, true, false, READ_WRITE_SENSITIVITY);
        oneChild.addOperation(OP_CONFIG_RO_WRITE, true, false, WRITE_SENSITIVITY);
        oneChild.addOperation(OP_CONFIG_RO_READ, true, false, READ_SENSITIVITY);
        oneChild.addOperation(OP_CONFIG_RO_NONE, true, false);
        oneChild.addOperation(OP_RUNTIME_RO_ACCESS_READ_WRITE, true, true, ADDRESS_READ_WRITE_SENSITIVITY);
        oneChild.addOperation(OP_RUNTIME_RO_READ_WRITE, true, true, READ_WRITE_SENSITIVITY);
        oneChild.addOperation(OP_RUNTIME_RO_WRITE, true, true, WRITE_SENSITIVITY);
        oneChild.addOperation(OP_RUNTIME_RO_READ, true, true, READ_SENSITIVITY);
        oneChild.addOperation(OP_RUNTIME_RO_NONE, true, true);

        rootRegistration.registerSubModel(oneChild);
        rootResource.registerChild(ONE_A, Resource.Factory.create());
    }

    private void compareOperations(ModelNode actual, String[] expected) {
        Set<String> expectedStrings = new HashSet<String>(Arrays.asList(expected));
        Set<String> actualStrings = new HashSet<String>();
        for (ModelNode node : actual.asList()) {
            actualStrings.add(node.asString());
        }

        HashSet<String> expectedButNotFound = new HashSet<String>(expectedStrings);
        expectedButNotFound.removeAll(actualStrings);

        HashSet<String> foundButNotExpected = new HashSet<String>(actualStrings);
        foundButNotExpected.removeAll(expectedStrings);

        Assert.assertTrue(foundButNotExpected.toString(), expectedStrings.containsAll(actualStrings));
        Assert.assertTrue(expectedButNotFound.toString(), actualStrings.containsAll(expectedStrings));
    }

    private static SensitiveTargetAccessConstraintDefinition createSensitivityConstraint(String name, boolean access, boolean read, boolean write) {
        SensitivityClassification classification = new SensitivityClassification("test", name, access, read, write);
        return new SensitiveTargetAccessConstraintDefinition(classification);
    }


    @Override
    protected AbstractControllerTestBase.ModelControllerService createModelControllerService(ProcessType processType) {
        return new AbstractControllerTestBase.ModelControllerService(processType, new RootResourceDefinition());
    }

    @Override
    protected void initModel(Resource rootResource, ManagementResourceRegistration registration) {
        this.rootResource = rootResource;
        this.rootRegistration = registration;
    }

    private ModelNode createReadOperationNamesOperation(PathAddress address, StandardRole role, boolean accessControl) {
        ModelNode op = Util.createOperation(READ_OPERATION_NAMES_OPERATION, address);
        if (accessControl) {
            op.get(ModelDescriptionConstants.ACCESS_CONTROL).set(accessControl);
        }
        op.get(OPERATION_HEADERS).get("roles").set(role.toString());
        return op;
    }

    private static class TestResourceDefinition extends SimpleResourceDefinition {
        TestResourceDefinition(PathElement pathElement) {
            super(pathElement,
                    new NonResolvingResourceDescriptionResolver(),
                    new AbstractAddStepHandler() {},
                    new AbstractRemoveStepHandler() {});
        }
    }

    private static class RootResourceDefinition extends TestResourceDefinition {
        RootResourceDefinition() {
            super(PathElement.pathElement("root"));
        }

        @Override
        public void registerOperations(ManagementResourceRegistration resourceRegistration) {
            super.registerOperations(resourceRegistration);
            GlobalOperationHandlers.registerGlobalOperations(resourceRegistration, ProcessType.EMBEDDED_SERVER);
        }
    }

    private static class ChildResourceDefinition extends TestResourceDefinition implements ConstrainedResourceDefinition {
        private final List<AccessConstraintDefinition> constraints;
        private final List<AttributeDefinition> attributes = Collections.synchronizedList(new ArrayList<AttributeDefinition>());
        private final List<AttributeDefinition> readOnlyAttributes = Collections.synchronizedList(new ArrayList<AttributeDefinition>());
        private final List<OperationDefinition> operations = Collections.synchronizedList(new ArrayList<OperationDefinition>());

        ChildResourceDefinition(PathElement element, AccessConstraintDefinition...constraints){
            super(element);
            this.constraints = Collections.unmodifiableList(Arrays.asList(constraints));
        }

        void addOperation(String name, boolean readOnly, boolean runtimeOnly, AccessConstraintDefinition...constraints) {
            SimpleOperationDefinitionBuilder builder = new SimpleOperationDefinitionBuilder(name, new NonResolvingResourceDescriptionResolver());
            if (constraints != null) {
                builder.setAccessConstraints(constraints);
            }
            if (readOnly) {
                builder.setReadOnly();
            }
            if (runtimeOnly) {
                builder.setRuntimeOnly();
            }
            operations.add(builder.build());
        }

        @Override
        public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
            for (AttributeDefinition attribute : attributes) {
                resourceRegistration.registerReadWriteAttribute(attribute, null, new ModelOnlyWriteAttributeHandler(attribute));
            }
            for (AttributeDefinition attribute : readOnlyAttributes) {
                resourceRegistration.registerReadOnlyAttribute(attribute, null);
            }
        }

        @Override
        public void registerOperations(ManagementResourceRegistration resourceRegistration) {
            super.registerOperations(resourceRegistration);
            for (OperationDefinition op : operations) {
                resourceRegistration.registerOperationHandler(op, TestOperationStepHandler.INSTANCE);
            }
        }

        @Override
        public List<AccessConstraintDefinition> getAccessConstraints() {
            return constraints;
        }
    }

    private static class TestOperationStepHandler implements OperationStepHandler {
        static final TestOperationStepHandler INSTANCE = new TestOperationStepHandler();

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            context.stepCompleted();
        }
    }
}
