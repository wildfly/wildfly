/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelOnlyWriteAttributeHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.access.constraint.SensitivityClassification;
import org.jboss.as.controller.access.constraint.VaultExpressionSensitivityConfig;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.access.management.ConstrainedResourceDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.descriptions.NonResolvingResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.global.GlobalNotifications;
import org.jboss.as.controller.operations.global.GlobalOperationHandlers;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.test.AbstractControllerTestBase;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.junit.Test;

/**
 * Tests access control of resource addition.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public class AddResourceTestCase extends AbstractControllerTestBase {

    private static final PathElement ONE = PathElement.pathElement("one");
    private static final PathElement ONE_A = PathElement.pathElement("one", "a");
    private static final PathElement ONE_B = PathElement.pathElement("one", "b");
    private static final PathAddress ONE_B_ADDR = PathAddress.pathAddress(ONE_B);

    private static final SensitiveTargetAccessConstraintDefinition WRITE_CONSTRAINT = new SensitiveTargetAccessConstraintDefinition(
            new SensitivityClassification("test", "test", false, false, true));

    private volatile ManagementResourceRegistration rootRegistration;
    private volatile Resource rootResource;

    @Test
    public void testMonitorAddNoSensitivity() throws Exception {
        testAddNoSensitivity(StandardRole.MONITOR, false);
    }

    @Test
    public void testMaintainerAddNoSensitivity() throws Exception {
        testAddNoSensitivity(StandardRole.MAINTAINER, true);
    }

    @Test
    public void testAdministratorAddNoSensitivity() throws Exception {
        testAddNoSensitivity(StandardRole.ADMINISTRATOR, true);
    }

    private void testAddNoSensitivity(StandardRole role, boolean success) throws Exception {
        ChildResourceDefinition def = new ChildResourceDefinition(ONE);
        def.addAttribute("test");
        rootRegistration.registerSubModel(def);

        Resource resourceA = Resource.Factory.create();
        resourceA.getModel().get("test").set("a");
        rootResource.registerChild(ONE_A, resourceA);

        ModelNode op = Util.createAddOperation(ONE_B_ADDR);
        op.get("test").set("b");
        op.get(OPERATION_HEADERS, "roles").set(role.toString());
        if (success) {
            executeForResult(op);
        } else {
            executeForFailure(op);
        }
    }

    @Test
    public void testMonitorAddWithWriteAttributeSensitivityDefined() throws Exception {
        testAddWithWriteAttributeSensitivity(StandardRole.MONITOR, false, true);
    }

    @Test
    public void testMaintainerAddWithWriteAttributeSensitivityDefined() throws Exception {
        testAddWithWriteAttributeSensitivity(StandardRole.MAINTAINER, false, true);
    }

    @Test
    public void testAdministratorAddWithWriteAttributeSensitivityDefined() throws Exception {
        testAddWithWriteAttributeSensitivity(StandardRole.ADMINISTRATOR, true, true);
    }

    @Test
    public void testMonitorAddWithWriteAttributeSensitivityUndefined() throws Exception {
        testAddWithWriteAttributeSensitivity(StandardRole.MONITOR, false, false);
    }

    @Test
    public void testMaintainerAddWithWriteAttributeSensitivityUndefined() throws Exception {
        testAddWithWriteAttributeSensitivity(StandardRole.MAINTAINER, false, false);
    }

    @Test
    public void testAdministratorAddWithWriteAttributeSensitivityUndefined() throws Exception {
        // This should fail not due to authz but because the attribute isn't defined
        testAddWithWriteAttributeSensitivity(StandardRole.ADMINISTRATOR, false, false);
    }

    private void testAddWithWriteAttributeSensitivity(StandardRole role, boolean success, boolean defineAttribute) throws Exception {
        ChildResourceDefinition def = new ChildResourceDefinition(ONE);
        def.addAttribute("test", WRITE_CONSTRAINT);
        rootRegistration.registerSubModel(def);

        Resource resourceA = Resource.Factory.create();
        resourceA.getModel().get("test").set("a");
        rootResource.registerChild(ONE_A, resourceA);

        ModelNode op = Util.createAddOperation(ONE_B_ADDR);
        if (defineAttribute) {
            op.get("test").set("b");
        }
        op.get(OPERATION_HEADERS, "roles").set(role.toString());
        if (success) {
            executeForResult(op);
        } else {
            executeForFailure(op);
        }
    }


    @Test
    public void testMonitorAddWithVaultWriteSensitivity() throws Exception {
        testAddWithVaultWriteSensitivity(StandardRole.MONITOR, false);
    }

    @Test
    public void testMaintainerAddWithVaultWriteSensitivity() throws Exception {
        testAddWithVaultWriteSensitivity(StandardRole.MAINTAINER, false);
    }

    @Test
    public void testAdministratorAddWithVaultWriteSensitivity() throws Exception {
        testAddWithVaultWriteSensitivity(StandardRole.ADMINISTRATOR, true);
    }

    private void testAddWithVaultWriteSensitivity(StandardRole role, boolean success) throws Exception {
        try {
            VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresWritePermission(true);

            ChildResourceDefinition def = new ChildResourceDefinition(ONE);
            def.addAttribute("test");
            rootRegistration.registerSubModel(def);

            Resource resourceA = Resource.Factory.create();
            resourceA.getModel().get("test").set("a");
            rootResource.registerChild(ONE_A, resourceA);

            ModelNode op = Util.createAddOperation(ONE_B_ADDR);
            op.get("test").set("${VAULT::AA::bb::cc}");
            op.get(OPERATION_HEADERS, "roles").set(role.toString());
            if (success) {
                executeForResult(op);
            } else {
                executeForFailure(op);
            }
        } finally {
            VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresAccessPermission(null);
            VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresReadPermission(null);
            VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresWritePermission(null);
        }
    }

    @Test
    public void testMonitorAddWithAllowNullWriteAttributeSensitivityUndefined() throws Exception {
        testAddWithAllowNullWriteAttributeSensitivity(StandardRole.MONITOR, false, false);
    }

    @Test
    public void testMaintainerAddWithAllowNullWriteAttributeSensitivityUndefined() throws Exception {
        testAddWithAllowNullWriteAttributeSensitivity(StandardRole.MAINTAINER, true, false);
    }

    @Test
    public void testAdministratorAddWithAllowNullWriteAttributeSensitivityUndefined() throws Exception {
        testAddWithAllowNullWriteAttributeSensitivity(StandardRole.ADMINISTRATOR, true, false);
    }

    @Test
    public void testMonitorAddWithAllowNullWriteAttributeSensitivityDefined() throws Exception {
        testAddWithAllowNullWriteAttributeSensitivity(StandardRole.MONITOR, false, true);
    }

    @Test
    public void testMaintainerAddWithAllowNullWriteAttributeSensitivityDefined() throws Exception {
        testAddWithAllowNullWriteAttributeSensitivity(StandardRole.MAINTAINER, false, true);
    }

    @Test
    public void testAdministratorAddWithAllowNullWriteAttributeSensitivityDefined() throws Exception {
        testAddWithAllowNullWriteAttributeSensitivity(StandardRole.ADMINISTRATOR, true, true);
    }

    private void testAddWithAllowNullWriteAttributeSensitivity(StandardRole role, boolean success, boolean defineAttribute) throws Exception {
        ChildResourceDefinition def = new ChildResourceDefinition(ONE);
        def.addAttribute("test", true, null, null, WRITE_CONSTRAINT);
        rootRegistration.registerSubModel(def);

        Resource resourceA = Resource.Factory.create();
        resourceA.getModel().get("test").set("a");
        rootResource.registerChild(ONE_A, resourceA);

        ModelNode op = Util.createAddOperation(ONE_B_ADDR);
        if (defineAttribute) {
            op.get("test").set("b");
        }
        op.get(OPERATION_HEADERS, "roles").set(role.toString());
        if (success) {
            executeForResult(op);
        } else {
            executeForFailure(op);
        }
    }

    @Test
    public void testResourceConstraintTrumpsAttribute() throws OperationFailedException {
        ChildResourceDefinition def = new ChildResourceDefinition(ONE, WRITE_CONSTRAINT);
        def.addAttribute("test", true, null, null, WRITE_CONSTRAINT);
        rootRegistration.registerSubModel(def);

        Resource resourceA = Resource.Factory.create();
        resourceA.getModel().get("test").set("a");
        rootResource.registerChild(ONE_A, resourceA);

        ModelNode op = Util.createAddOperation(ONE_B_ADDR);
        op.get(OPERATION_HEADERS, "roles").set(StandardRole.MAINTAINER.toString());
        executeForFailure(op);
    }

    @Test
    public void testMonitorAddWithDefaultValueWriteAttributeSensitivityUndefined() throws Exception {
        testAddWithDefaultValueWriteAttributeSensitivity(StandardRole.MONITOR, false, false);
    }

    @Test
    public void testMaintainerAddWithDefaultValueWriteAttributeSensitivityUndefined() throws Exception {
        testAddWithDefaultValueWriteAttributeSensitivity(StandardRole.MAINTAINER, false, false);
    }

    @Test
    public void testAdministratorAddWithDefaultValueWriteAttributeSensitivityUndefined() throws Exception {
        testAddWithDefaultValueWriteAttributeSensitivity(StandardRole.ADMINISTRATOR, true, false);
    }

    @Test
    public void testMonitorAddWithDefaultValueWriteAttributeSensitivityDefined() throws Exception {
        testAddWithDefaultValueWriteAttributeSensitivity(StandardRole.MONITOR, false, false);
    }

    @Test
    public void testMaintainerAddWithDefaultValueWriteAttributeSensitivityDefined() throws Exception {
        testAddWithDefaultValueWriteAttributeSensitivity(StandardRole.MAINTAINER, false, true);
    }

    @Test
    public void testAdministratorAddWithDefaultValueWriteAttributeSensitivityDefined() throws Exception {
        testAddWithDefaultValueWriteAttributeSensitivity(StandardRole.ADMINISTRATOR, true, true);
    }

    private void testAddWithDefaultValueWriteAttributeSensitivity(StandardRole role, boolean success, boolean defineAttribute) throws Exception {
        ChildResourceDefinition def = new ChildResourceDefinition(ONE);
        def.addAttribute("test", true, null, new ModelNode("b"), WRITE_CONSTRAINT);
        rootRegistration.registerSubModel(def);

        Resource resourceA = Resource.Factory.create();
        resourceA.getModel().get("test").set("a");
        rootResource.registerChild(ONE_A, resourceA);

        ModelNode op = Util.createAddOperation(ONE_B_ADDR);
        if (defineAttribute) {
            op.get("test").set("b");
        }
        op.get(OPERATION_HEADERS, "roles").set(role.toString());
        if (success) {
            executeForResult(op);
        } else {
            executeForFailure(op);
        }
    }

    @Test
    public void testMonitorAddWithSignificantNullWriteAttributeSensitivityUndefined() throws Exception {
        testAddWithSignificantNullWriteAttributeSensitivity(StandardRole.MONITOR, false, false);
    }

    @Test
    public void testMaintainerAddWithSignificantNullWriteAttributeSensitivityUndefined() throws Exception {
        testAddWithSignificantNullWriteAttributeSensitivity(StandardRole.MAINTAINER, false, false);
    }

    @Test
    public void testAdministratorAddWithSignificantNullWriteAttributeSensitivityUndefined() throws Exception {
        testAddWithSignificantNullWriteAttributeSensitivity(StandardRole.ADMINISTRATOR, true, false);
    }

    @Test
    public void testMonitorAddWithSignificantNullWriteAttributeSensitivityDefined() throws Exception {
        testAddWithSignificantNullWriteAttributeSensitivity(StandardRole.MONITOR, false, true);
    }

    @Test
    public void testMaintainerAddWithSignificantNullWriteAttributeSensitivityDefined() throws Exception {
        testAddWithSignificantNullWriteAttributeSensitivity(StandardRole.MAINTAINER, false, true);
    }

    @Test
    public void testAdministratorAddWithSignificantNullWriteAttributeSensitivityDefined() throws Exception {
        testAddWithSignificantNullWriteAttributeSensitivity(StandardRole.ADMINISTRATOR, true, true);
    }

    private void testAddWithSignificantNullWriteAttributeSensitivity(StandardRole role, boolean success, boolean defineAttribute) throws Exception {
        ChildResourceDefinition def = new ChildResourceDefinition(ONE);
        def.addAttribute("test", true, Boolean.TRUE, null, WRITE_CONSTRAINT);
        rootRegistration.registerSubModel(def);

        Resource resourceA = Resource.Factory.create();
        resourceA.getModel().get("test").set("a");
        rootResource.registerChild(ONE_A, resourceA);

        ModelNode op = Util.createAddOperation(ONE_B_ADDR);
        if (defineAttribute) {
            op.get("test").set("b");
        }
        op.get(OPERATION_HEADERS, "roles").set(role.toString());
        if (success) {
            executeForResult(op);
        } else {
            executeForFailure(op);
        }
    }


    @Override
    protected AbstractControllerTestBase.ModelControllerService createModelControllerService(ProcessType processType) {
        return new AbstractControllerTestBase.ModelControllerService(processType, new RootResourceDefinition());
    }

    @Override
    protected void initModel(Resource rootResource, ManagementResourceRegistration registration) {
        this.rootResource = rootResource;
        this.rootRegistration = registration;

        GlobalNotifications.registerGlobalNotifications(registration, ProcessType.EMBEDDED_SERVER);
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

        ChildResourceDefinition(PathElement element, AccessConstraintDefinition...constraints){
            super(element);
            this.constraints = Collections.unmodifiableList(Arrays.asList(constraints));
        }

        void addAttribute(String name, AccessConstraintDefinition...constraints) {
            addAttribute(name, false, null, null, constraints);
        }

        void addAttribute(String name, boolean allowNull, Boolean nullSignificant, ModelNode defaultValue, AccessConstraintDefinition...constraints) {
            SimpleAttributeDefinitionBuilder builder = new SimpleAttributeDefinitionBuilder(name, ModelType.STRING, allowNull);
            if (nullSignificant != null) {
                builder.setNullSignficant(nullSignificant);
            }
            if (defaultValue != null) {
                builder.setDefaultValue(defaultValue);
            }
            if (constraints != null) {
                builder.setAccessConstraints(constraints);
            }
            builder.setAllowExpression(true);
            attributes.add(builder.build());
        }

        @Override
        public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
            for (AttributeDefinition attribute : attributes) {
                resourceRegistration.registerReadWriteAttribute(attribute, null, new ModelOnlyWriteAttributeHandler(attribute));
            }
        }

        @Override
        protected void registerAddOperation(ManagementResourceRegistration registration, OperationStepHandler handler, OperationEntry.Flag... flags) {
            // Use an add handler that knows about our attributes
            OperationStepHandler addHandler = new AbstractAddStepHandler(attributes){};
            super.registerAddOperation(registration, addHandler, flags);
        }

        @Override
        public List<AccessConstraintDefinition> getAccessConstraints() {
            return constraints;
        }
    }

}
