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
public class WriteAttributeTestCase extends AbstractRbacTestBase {
    public static final String UNCONSTRAINED_RESOURCE = "unconstrained-resource";
    public static final String SENSITIVE_CONSTRAINED_RESOURCE = "sensitive-constrained-resource";

    public static final String UNCONSTRAINED_READWRITE_ATTRIBUTE = "unconstrained-readwrite-attribute";
    public static final String OLD_VALUE_OF_UNCONSTRAINED_READWRITE_ATTRIBUTE = "old value of unconstrained-readwrite-attribute";
    public static final String NEW_VALUE_OF_UNCONSTRAINED_READWRITE_ATTRIBUTE = "new value of unconstrained-readwrite-attribute";

    public static final String SENSITIVE_CONSTRAINED_READWRITE_ATTRIBUTE = "sensitive-constrained-readwrite-attribute";
    public static final String OLD_VALUE_OF_SENSITIVE_CONSTRAINED_READWRITE_ATTRIBUTE = "old value of sensitive-constrained-readwrite-attribute";
    public static final String NEW_VALUE_OF_SENSITIVE_CONSTRAINED_READWRITE_ATTRIBUTE = "new value of sensitive-constrained-readwrite-attribute";

    public static final String FOO = "foo";

    @Before
    public void setUp() {
        ModelNode operation = Util.createOperation(ADD, pathAddress(UNCONSTRAINED_RESOURCE, FOO));
        executeWithRoles(operation, StandardRole.SUPERUSER);

        operation = Util.createOperation(ADD, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO));
        executeWithRoles(operation, StandardRole.SUPERUSER);
    }

    @Test
    public void testMonitor() {
        test(false, false, StandardRole.MONITOR);
    }

    @Test
    public void testOperator() {
        test(true, false, StandardRole.OPERATOR);
    }

    @Test
    public void testMaintainer() {
        test(true, false, StandardRole.MAINTAINER);
    }

    @Test
    public void testAdministrator() {
        test(true, true, StandardRole.ADMINISTRATOR);
    }

    @Test
    public void testAuditor() {
        test(false, true, StandardRole.AUDITOR);
    }

    @Test
    public void testSuperuser() {
        test(true, true, StandardRole.SUPERUSER);
    }

    @Test
    @Ignore("ManagementPermissionCollection.implies doesn't work in case of users with multiple roles")
    public void testMonitorOperator() {
        test(true, false, StandardRole.MONITOR, StandardRole.OPERATOR);
    }

    @Test
    @Ignore("ManagementPermissionCollection.implies doesn't work in case of users with multiple roles")
    public void testMonitorAdministrator() {
        test(true, true, StandardRole.MONITOR, StandardRole.ADMINISTRATOR);
    }

    @Test
    @Ignore("ManagementPermissionCollection.implies doesn't work in case of users with multiple roles")
    public void testAdministratorAuditor() {
        test(true, true, StandardRole.ADMINISTRATOR, StandardRole.AUDITOR);
    }

    @Test
    @Ignore("ManagementPermissionCollection.implies doesn't work in case of users with multiple roles")
    public void testMopnitorAuditor() {
        test(false, true, StandardRole.MONITOR, StandardRole.AUDITOR);
    }

    private void testOperation(ResultExpectation readResultExpectation, ResultExpectation writeResultExpectation,
                               String resourceName, String attributeName, String oldAttributeValue,
                               String newAttributeValue, StandardRole... roles) {
        ModelNode operation = Util.createOperation(READ_ATTRIBUTE_OPERATION, pathAddress(resourceName, FOO));
        operation.get(NAME).set(attributeName);
        ModelNode result = executeWithRoles(operation, roles);
        assertOperationResult(result, readResultExpectation);
        if (readResultExpectation == ResultExpectation.PERMITTED) {
            assertEquals(oldAttributeValue, result.get(RESULT).asString());
        }

        operation = Util.createOperation(WRITE_ATTRIBUTE_OPERATION, pathAddress(resourceName, FOO));
        operation.get(NAME).set(attributeName);
        operation.get(VALUE).set(newAttributeValue);
        result = executeWithRoles(operation, roles);
        assertOperationResult(result, writeResultExpectation);

        if (writeResultExpectation != ResultExpectation.PERMITTED) {
            return;
        }

        operation = Util.createOperation(READ_ATTRIBUTE_OPERATION, pathAddress(resourceName, FOO));
        operation.get(NAME).set(attributeName);
        result = executeWithRoles(operation, roles);
        assertOperationResult(result, readResultExpectation);
        if (readResultExpectation == ResultExpectation.PERMITTED) {
            assertEquals(newAttributeValue, result.get(RESULT).asString());
        }
    }

    private void test(boolean canWrite, boolean canAccessSensitive, StandardRole... roles) {
        ResultExpectation readExpectation = ResultExpectation.PERMITTED;
        ResultExpectation writeExpectation = canWrite ? ResultExpectation.PERMITTED : ResultExpectation.DENIED;
        testOperation(readExpectation, writeExpectation, UNCONSTRAINED_RESOURCE, UNCONSTRAINED_READWRITE_ATTRIBUTE,
                OLD_VALUE_OF_UNCONSTRAINED_READWRITE_ATTRIBUTE,
                NEW_VALUE_OF_UNCONSTRAINED_READWRITE_ATTRIBUTE, roles);

        readExpectation = canAccessSensitive ? ResultExpectation.PERMITTED : ResultExpectation.DENIED;
        writeExpectation = canAccessSensitive && canWrite ? ResultExpectation.PERMITTED : ResultExpectation.DENIED;
        testOperation(readExpectation, writeExpectation, UNCONSTRAINED_RESOURCE, SENSITIVE_CONSTRAINED_READWRITE_ATTRIBUTE,
                OLD_VALUE_OF_SENSITIVE_CONSTRAINED_READWRITE_ATTRIBUTE,
                NEW_VALUE_OF_SENSITIVE_CONSTRAINED_READWRITE_ATTRIBUTE, roles);

        readExpectation = canAccessSensitive ? ResultExpectation.PERMITTED : ResultExpectation.NO_ACCESS;
        writeExpectation = canAccessSensitive && canWrite ? ResultExpectation.PERMITTED : ResultExpectation.NO_ACCESS;
        if (canAccessSensitive && !canWrite) {
            writeExpectation = ResultExpectation.DENIED;
        }

        testOperation(readExpectation, writeExpectation, SENSITIVE_CONSTRAINED_RESOURCE, UNCONSTRAINED_READWRITE_ATTRIBUTE,
                OLD_VALUE_OF_UNCONSTRAINED_READWRITE_ATTRIBUTE,
                NEW_VALUE_OF_UNCONSTRAINED_READWRITE_ATTRIBUTE, roles);
        testOperation(readExpectation, writeExpectation, SENSITIVE_CONSTRAINED_RESOURCE, SENSITIVE_CONSTRAINED_READWRITE_ATTRIBUTE,
                OLD_VALUE_OF_SENSITIVE_CONSTRAINED_READWRITE_ATTRIBUTE,
                NEW_VALUE_OF_SENSITIVE_CONSTRAINED_READWRITE_ATTRIBUTE, roles);
    }

    // model definition

    private static final SensitivityClassification MY_SENSITIVITY
            = new SensitivityClassification("test", "my-sensitivity", true, true, true);
    private static final AccessConstraintDefinition MY_SENSITIVE_CONSTRAINT
            = new SensitiveTargetAccessConstraintDefinition(MY_SENSITIVITY);

    @Override
    protected void initModel(Resource rootResource, ManagementResourceRegistration registration) {
        GlobalOperationHandlers.registerGlobalOperations(registration, ProcessType.EMBEDDED_SERVER);

        registration.registerSubModel(new TestResourceDefinition(UNCONSTRAINED_RESOURCE));
        registration.registerSubModel(new TestResourceDefinition(SENSITIVE_CONSTRAINED_RESOURCE,
                MY_SENSITIVE_CONSTRAINT));
    }

    private static final class TestResourceDefinition extends SimpleResourceDefinition {
        private final List<AccessConstraintDefinition> constraintDefinitions;

        TestResourceDefinition(String path, AccessConstraintDefinition... constraintDefinitions) {
            super(pathElement(path),
                    new NonResolvingResourceDescriptionResolver(),
                    new AbstractAddStepHandler() {},
                    new AbstractRemoveStepHandler() {}
            );

            this.constraintDefinitions = Collections.unmodifiableList(Arrays.asList(constraintDefinitions));
        }

        @Override
        public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
            super.registerAttributes(resourceRegistration);

            AttributeDefinition attributeDefinition = SimpleAttributeDefinitionBuilder
                    .create(UNCONSTRAINED_READWRITE_ATTRIBUTE, ModelType.STRING)
                    .setDefaultValue(new ModelNode(OLD_VALUE_OF_UNCONSTRAINED_READWRITE_ATTRIBUTE))
                    .build();
            resourceRegistration.registerReadWriteAttribute(attributeDefinition, null,
                    new ModelOnlyWriteAttributeHandler(attributeDefinition));

            attributeDefinition = SimpleAttributeDefinitionBuilder
                    .create(SENSITIVE_CONSTRAINED_READWRITE_ATTRIBUTE, ModelType.STRING)
                    .setDefaultValue(new ModelNode(OLD_VALUE_OF_SENSITIVE_CONSTRAINED_READWRITE_ATTRIBUTE))
                    .setAccessConstraints(MY_SENSITIVE_CONSTRAINT)
                    .build();
            resourceRegistration.registerReadWriteAttribute(attributeDefinition, null,
                    new ModelOnlyWriteAttributeHandler(attributeDefinition));
        }

        @Override
        public List<AccessConstraintDefinition> getAccessConstraints() {
            return constraintDefinitions;
        }
    }
}
