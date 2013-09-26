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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelOnlyWriteAttributeHandler;
import org.jboss.as.controller.OperationDefinition;
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
import org.jboss.as.controller.operations.global.GlobalOperationHandlers;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.test.AbstractControllerTestBase;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.junit.Test;

/**
 * Tests access control of resource remove.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class RemoveResourceTestCase extends AbstractControllerTestBase {

    private static final PathElement ONE = PathElement.pathElement("one");
    private static final PathElement ONE_A = PathElement.pathElement("one", "a");
    private static final PathElement ONE_B = PathElement.pathElement("one", "b");
    private static final PathAddress ONE_B_ADDR = PathAddress.pathAddress(ONE_B);

    private static final SensitiveTargetAccessConstraintDefinition WRITE_CONSTRAINT = new SensitiveTargetAccessConstraintDefinition(
            new SensitivityClassification("test", "test", false, false, true));

    private volatile ManagementResourceRegistration rootRegistration;
    private volatile Resource rootResource;

    @Test
    public void testMonitorRemoveNoSensitivity() throws Exception {
        testRemoveNoSensitivity(StandardRole.MONITOR, false);
    }

    @Test
    public void testMaintainerRemoveNoSensitivity() throws Exception {
        testRemoveNoSensitivity(StandardRole.MAINTAINER, true);
    }

    @Test
    public void testAdministratorRemoveNoSensitivity() throws Exception {
        testRemoveNoSensitivity(StandardRole.ADMINISTRATOR, true);
    }

    private void testRemoveNoSensitivity(StandardRole role, boolean success) throws Exception {
        ChildResourceDefinition def = new ChildResourceDefinition(ONE);
        def.addAttribute("test");
        rootRegistration.registerSubModel(def);

        Resource resourceA = Resource.Factory.create();
        resourceA.getModel().get("test").set("a");
        rootResource.registerChild(ONE_A, resourceA);

        Resource resourceB = Resource.Factory.create();
        resourceB.getModel().get("test").set("b");
        rootResource.registerChild(ONE_B, resourceB);

        ModelNode op = Util.createRemoveOperation(ONE_B_ADDR);
        op.get(OPERATION_HEADERS, "roles").set(role.toString());
        if (success) {
            executeForResult(op);
        } else {
            executeForFailure(op);
        }
    }

    @Test
    public void testMonitorRemoveWithWriteAttributeSensitivity() throws Exception {
        testRemoveWithWriteAttributeSensitivity(StandardRole.MONITOR, false);
    }

    @Test
    public void testMaintainerRemoveWithWriteAttributeSensitivity() throws Exception {
        testRemoveWithWriteAttributeSensitivity(StandardRole.MAINTAINER, true);
    }

    @Test
    public void testAdministratorRemoveWithWriteAttributeSensitivity() throws Exception {
        testRemoveWithWriteAttributeSensitivity(StandardRole.ADMINISTRATOR, true);
    }

    private void testRemoveWithWriteAttributeSensitivity(StandardRole role, boolean success) throws Exception {
        ChildResourceDefinition def = new ChildResourceDefinition(ONE);
        def.addAttribute("test", WRITE_CONSTRAINT);
        rootRegistration.registerSubModel(def);

        Resource resourceA = Resource.Factory.create();
        resourceA.getModel().get("test").set("a");
        rootResource.registerChild(ONE_A, resourceA);

        Resource resourceB = Resource.Factory.create();
        resourceB.getModel().get("test").set("b");
        rootResource.registerChild(ONE_B, resourceB);

        ModelNode op = Util.createRemoveOperation(ONE_B_ADDR);
        op.get(OPERATION_HEADERS, "roles").set(role.toString());
        if (success) {
            executeForResult(op);
        } else {
            executeForFailure(op);
        }
    }


    @Test
    public void testMonitorRemoveWithVaultWriteSensitivity() throws Exception {
        testRemoveWithVaultWriteSensitivity(StandardRole.MONITOR, false);
    }

    @Test
    public void testMaintainerRemoveWithVaultWriteSensitivity() throws Exception {
        testRemoveWithVaultWriteSensitivity(StandardRole.MAINTAINER, true);
    }

    @Test
    public void testAdministratorRemoveWithVaultWriteSensitivity() throws Exception {
        testRemoveWithVaultWriteSensitivity(StandardRole.ADMINISTRATOR, true);
    }

    private void testRemoveWithVaultWriteSensitivity(StandardRole role, boolean success) throws Exception {
        try {
            VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresWritePermission(true);

            ChildResourceDefinition def = new ChildResourceDefinition(ONE);
            def.addAttribute("test");
            rootRegistration.registerSubModel(def);

            Resource resourceA = Resource.Factory.create();
            resourceA.getModel().get("test").set("a");
            rootResource.registerChild(ONE_A, resourceA);

            Resource resourceB = Resource.Factory.create();
            resourceB.getModel().get("test").set("${VAULT::AA::bb::cc}");
            rootResource.registerChild(ONE_B, resourceB);

            ModelNode op = Util.createRemoveOperation(ONE_B_ADDR);
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


    @Override
    protected AbstractControllerTestBase.ModelControllerService createModelControllerService(ProcessType processType) {
        return new AbstractControllerTestBase.ModelControllerService(processType, new RootResourceDefinition());
    }

    @Override
    protected void initModel(Resource rootResource, ManagementResourceRegistration registration) {
        this.rootResource = rootResource;
        this.rootRegistration = registration;
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
            SimpleAttributeDefinitionBuilder builder = new SimpleAttributeDefinitionBuilder(name, ModelType.STRING);
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
        public List<AccessConstraintDefinition> getAccessConstraints() {
            return constraints;
        }
    }

}
