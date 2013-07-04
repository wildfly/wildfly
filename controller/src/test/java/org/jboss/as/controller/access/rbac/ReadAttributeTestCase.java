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

import org.jboss.as.controller.*;
import org.jboss.as.controller.access.constraint.SensitivityClassification;
import org.jboss.as.controller.access.constraint.management.AccessConstraintDefinition;
import org.jboss.as.controller.access.constraint.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.descriptions.NonResolvingResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.global.GlobalOperationHandlers;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.jboss.as.controller.PathAddress.pathAddress;
import static org.jboss.as.controller.PathElement.pathElement;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;
import static org.junit.Assert.assertEquals;

/**
 * @author Ladislav Thon <lthon@redhat.com>
 */
public class ReadAttributeTestCase extends AbstractRbacTestBase {
    // ..._RESOURCE_1 -> default read attribute handler
    // ..._RESOURCE_2 -> own implementation of read attribute handler
    public static final String UNCONSTRAINED_RESOURCE_1 = "unconstrained-resource-1";
    public static final String SENSITIVE_CONSTRAINED_RESOURCE_1 = "sensitive-constrained-resource-1";
    public static final String UNCONSTRAINED_RESOURCE_2 = "unconstrained-resource-2";
    public static final String SENSITIVE_CONSTRAINED_RESOURCE_2 = "sensitive-constrained-resource-2";

    public static final String UNCONSTRAINED_READONLY_ATTRIBUTE = "unconstrained-readonly-attribute";
    public static final String VALUE_OF_UNCONSTRAINED_READONLY_ATTRIBUTE = "value of unconstrained-readonly-attribute";

    public static final String SENSITIVE_CONSTRAINED_READONLY_ATTRIBUTE = "sensitive-constrained-readonly-attribute";
    public static final String VALUE_OF_SENSITIVE_CONSTRAINED_READONLY_ATTRIBUTE = "value of sensitive-constrained-readonly-attribute";

    public static final String FOO = "foo";

    @Before
    public void setUp() {
        ModelNode operation = Util.createOperation(ADD, pathAddress(UNCONSTRAINED_RESOURCE_1, FOO));
        executeWithRoles(operation, StandardRole.SUPERUSER);

        operation = Util.createOperation(ADD, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE_1, FOO));
        executeWithRoles(operation, StandardRole.SUPERUSER);

        operation = Util.createOperation(ADD, pathAddress(UNCONSTRAINED_RESOURCE_2, FOO));
        executeWithRoles(operation, StandardRole.SUPERUSER);

        operation = Util.createOperation(ADD, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE_2, FOO));
        executeWithRoles(operation, StandardRole.SUPERUSER);
    }

    @Test
    public void testMonitor() {
        test(false, StandardRole.MONITOR);
    }

    @Test
    public void testOperator() {
        test(false, StandardRole.OPERATOR);
    }

    @Test
    public void testMaintainer() {
        test(false, StandardRole.MAINTAINER);
    }

    @Test
    public void testAdministrator() {
        test(true, StandardRole.ADMINISTRATOR);
    }

    @Test
    public void testAuditor() {
        test(true, StandardRole.AUDITOR);
    }

    @Test
    public void testSuperuser() {
        test(true, StandardRole.SUPERUSER);
    }

    @Test
    @Ignore("ManagementPermissionCollection.implies doesn't work in case of users with multiple roles")
    public void testMonitorOperator() {
        test(false, StandardRole.MONITOR, StandardRole.OPERATOR);
    }

    @Test
    @Ignore("ManagementPermissionCollection.implies doesn't work in case of users with multiple roles")
    public void testMonitorAdministrator() {
        test(true, StandardRole.MONITOR, StandardRole.ADMINISTRATOR);
    }

    @Test
    @Ignore("ManagementPermissionCollection.implies doesn't work in case of users with multiple roles")
    public void testAdministratorAuditor() {
        test(true, StandardRole.ADMINISTRATOR, StandardRole.AUDITOR);
    }

    private void testOperation(ResultExpectation resultExpectation, String resourceName, String attributeName, String attributeValue,
                               StandardRole... roles) {
        ModelNode operation = Util.createOperation(READ_ATTRIBUTE_OPERATION, pathAddress(resourceName, FOO));
        operation.get(NAME).set(attributeName);
        ModelNode result = executeWithRoles(operation, roles);
        assertOperationResult(result, resultExpectation);
        if (resultExpectation == ResultExpectation.PERMITTED) {
            assertEquals(attributeValue, result.get(RESULT).asString());
        }
    }

    private void test(boolean canAccessSensitive, StandardRole... roles) {
        testOperation(ResultExpectation.PERMITTED, UNCONSTRAINED_RESOURCE_1,
                UNCONSTRAINED_READONLY_ATTRIBUTE, VALUE_OF_UNCONSTRAINED_READONLY_ATTRIBUTE, roles);
        testOperation(ResultExpectation.PERMITTED, UNCONSTRAINED_RESOURCE_2,
                UNCONSTRAINED_READONLY_ATTRIBUTE, VALUE_OF_UNCONSTRAINED_READONLY_ATTRIBUTE, roles);
        testOperation(canAccessSensitive ? ResultExpectation.PERMITTED : ResultExpectation.DENIED, UNCONSTRAINED_RESOURCE_1,
                SENSITIVE_CONSTRAINED_READONLY_ATTRIBUTE, VALUE_OF_SENSITIVE_CONSTRAINED_READONLY_ATTRIBUTE, roles);
        testOperation(canAccessSensitive ? ResultExpectation.PERMITTED : ResultExpectation.DENIED, UNCONSTRAINED_RESOURCE_2,
                SENSITIVE_CONSTRAINED_READONLY_ATTRIBUTE, VALUE_OF_SENSITIVE_CONSTRAINED_READONLY_ATTRIBUTE, roles);

        testOperation(canAccessSensitive ? ResultExpectation.PERMITTED : ResultExpectation.NO_ACCESS, SENSITIVE_CONSTRAINED_RESOURCE_1,
                UNCONSTRAINED_READONLY_ATTRIBUTE, VALUE_OF_UNCONSTRAINED_READONLY_ATTRIBUTE, roles);
        testOperation(canAccessSensitive ? ResultExpectation.PERMITTED : ResultExpectation.NO_ACCESS, SENSITIVE_CONSTRAINED_RESOURCE_2,
                UNCONSTRAINED_READONLY_ATTRIBUTE, VALUE_OF_UNCONSTRAINED_READONLY_ATTRIBUTE, roles);
        testOperation(canAccessSensitive ? ResultExpectation.PERMITTED : ResultExpectation.NO_ACCESS, SENSITIVE_CONSTRAINED_RESOURCE_1,
                SENSITIVE_CONSTRAINED_READONLY_ATTRIBUTE, VALUE_OF_SENSITIVE_CONSTRAINED_READONLY_ATTRIBUTE, roles);
        testOperation(canAccessSensitive ? ResultExpectation.PERMITTED : ResultExpectation.NO_ACCESS, SENSITIVE_CONSTRAINED_RESOURCE_2,
                SENSITIVE_CONSTRAINED_READONLY_ATTRIBUTE, VALUE_OF_SENSITIVE_CONSTRAINED_READONLY_ATTRIBUTE, roles);
    }

    // model definition

    private static final SensitivityClassification MY_SENSITIVITY
            = new SensitivityClassification("test", "my-sensitivity", true, true, true);
    private static final AccessConstraintDefinition MY_SENSITIVE_CONSTRAINT
            = new SensitiveTargetAccessConstraintDefinition(MY_SENSITIVITY);

    @Override
    protected void initModel(Resource rootResource, ManagementResourceRegistration registration) {
        GlobalOperationHandlers.registerGlobalOperations(registration, ProcessType.EMBEDDED_SERVER);

        registration.registerSubModel(new TestResourceDefinition(UNCONSTRAINED_RESOURCE_1, true));
        registration.registerSubModel(new TestResourceDefinition(SENSITIVE_CONSTRAINED_RESOURCE_1, true,
                MY_SENSITIVE_CONSTRAINT));
        registration.registerSubModel(new TestResourceDefinition(UNCONSTRAINED_RESOURCE_2, false));
        registration.registerSubModel(new TestResourceDefinition(SENSITIVE_CONSTRAINED_RESOURCE_2, false,
                MY_SENSITIVE_CONSTRAINT));
    }

    private static final class TestResourceDefinition extends SimpleResourceDefinition {
        private final List<AccessConstraintDefinition> constraintDefinitions;
        private final boolean useDefaultReadAttributeHandler;

        TestResourceDefinition(String path, boolean useDefaultReadAttributeHandler, AccessConstraintDefinition... constraintDefinitions) {
            super(pathElement(path),
                    new NonResolvingResourceDescriptionResolver(),
                    new AbstractAddStepHandler() {},
                    new AbstractRemoveStepHandler() {}
            );

            this.useDefaultReadAttributeHandler = useDefaultReadAttributeHandler;
            this.constraintDefinitions = Collections.unmodifiableList(Arrays.asList(constraintDefinitions));
        }

        @Override
        public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
            super.registerAttributes(resourceRegistration);

            OperationStepHandler readAttributeHandler = useDefaultReadAttributeHandler
                    ? null : new TestReadAttributeHandler(new ModelNode(VALUE_OF_UNCONSTRAINED_READONLY_ATTRIBUTE));
            ModelNode defaultValue = useDefaultReadAttributeHandler
                    ? new ModelNode(VALUE_OF_UNCONSTRAINED_READONLY_ATTRIBUTE) : null;

            AttributeDefinition attributeDefinition = SimpleAttributeDefinitionBuilder
                    .create(UNCONSTRAINED_READONLY_ATTRIBUTE, ModelType.STRING)
                    .setDefaultValue(defaultValue)
                    .build();
            resourceRegistration.registerReadOnlyAttribute(attributeDefinition, readAttributeHandler);

            readAttributeHandler = useDefaultReadAttributeHandler
                    ? null : new TestReadAttributeHandler(new ModelNode(VALUE_OF_SENSITIVE_CONSTRAINED_READONLY_ATTRIBUTE));
            defaultValue = useDefaultReadAttributeHandler
                    ? new ModelNode(VALUE_OF_SENSITIVE_CONSTRAINED_READONLY_ATTRIBUTE) : null;

            attributeDefinition = SimpleAttributeDefinitionBuilder
                    .create(SENSITIVE_CONSTRAINED_READONLY_ATTRIBUTE, ModelType.STRING)
                    .setDefaultValue(defaultValue)
                    .setAccessConstraints(MY_SENSITIVE_CONSTRAINT)
                    .build();
            resourceRegistration.registerReadOnlyAttribute(attributeDefinition, readAttributeHandler);
        }

        @Override
        public List<AccessConstraintDefinition> getAccessConstraints() {
            return constraintDefinitions;
        }
    }

    private static final class TestReadAttributeHandler implements OperationStepHandler {
        private final ModelNode value;

        private TestReadAttributeHandler(ModelNode value) {
            this.value = value;
        }

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            context.getResult().set(value);
            context.stepCompleted();
        }
    }
}
