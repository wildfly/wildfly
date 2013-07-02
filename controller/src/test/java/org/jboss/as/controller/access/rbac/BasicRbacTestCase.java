package org.jboss.as.controller.access.rbac;

import org.jboss.as.controller.*;
import org.jboss.as.controller.access.constraint.ApplicationTypeConfig;
import org.jboss.as.controller.access.constraint.SensitivityClassification;
import org.jboss.as.controller.access.constraint.management.AccessConstraintDefinition;
import org.jboss.as.controller.access.constraint.management.ApplicationTypeAccessConstraintDefinition;
import org.jboss.as.controller.access.constraint.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.descriptions.NonResolvingResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.global.GlobalOperationHandlers;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.test.AbstractControllerTestBase;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.jboss.as.controller.PathAddress.pathAddress;
import static org.jboss.as.controller.PathElement.pathElement;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Ladislav Thon <lthon@redhat.com>
 */
public class BasicRbacTestCase extends AbstractControllerTestBase {
    public static final String UNCONSTRAINED_RESOURCE = "unconstrained-resource";
    public static final String APPLICATION_CONSTRAINED_RESOURCE = "application-constrained-resource";
    public static final String SENSITIVE_CONSTRAINED_RESOURCE = "sensitive-constrained-resource";

    public static final String READONLY_OPERATION = "readonly-operation";
    public static final String READWRITE_OPERATION = "readwrite-operation";

    public static final String FOO = "foo";
    public static final String BAR = "bar";

    // what can be tested: NA -- no access, P -- permitted, D -- denied
    //
    //               | access | read-config | write-config | read-runtime | write-runtime
    // --------------+--------+-------------+--------------+--------------+--------------
    // monitor       |   NA          P             D               P              D
    // operator      |   NA          P             D               P              P
    // maintainer    |   NA          P             P               P              P
    // deployer      |   NA          P             P               P              P
    // administrator |   --          P             P               P              P
    // auditor       |   --          P             D               P              D
    // superuser     |   --          P             P               P              P

    @Before
    public void setup() {
        ModelNode operation = Util.createOperation(ADD, pathAddress(UNCONSTRAINED_RESOURCE, FOO));
        executeWithRole(operation, StandardRole.SUPERUSER);

        operation = Util.createOperation(ADD, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO));
        executeWithRole(operation, StandardRole.SUPERUSER);

        operation = Util.createOperation(ADD, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO));
        executeWithRole(operation, StandardRole.SUPERUSER);
    }

    // monitor

    @Test
    public void testMonitorNoAccess() throws Exception {
        noAccess(READ_RESOURCE_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), StandardRole.MONITOR);
    }

    @Test
    public void testMonitorReadConfigPermitted() throws Exception {
        permitted(READ_RESOURCE_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), StandardRole.MONITOR);
    }

    @Test
    public void testMonitorWriteConfigDenied() throws Exception {
        denied(ADD, pathAddress(UNCONSTRAINED_RESOURCE, BAR), StandardRole.MONITOR);
    }

    @Test
    public void testMonitorReadRuntimePermitted() throws Exception {
        permitted(READONLY_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), StandardRole.MONITOR);
    }

    @Test
    public void testMonitorWriteRuntimeDenied() throws Exception {
        denied(READWRITE_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), StandardRole.MONITOR);
    }

    // operator

    @Test
    public void testOperatorNoAccess() throws Exception {
        noAccess(READ_RESOURCE_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), StandardRole.OPERATOR);
    }

    @Test
    public void testOperatorReadConfigPermitted() throws Exception {
        permitted(READ_RESOURCE_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), StandardRole.OPERATOR);
    }

    @Test
    public void testOperatorWriteConfigDenied() throws Exception {
        denied(ADD, pathAddress(UNCONSTRAINED_RESOURCE, BAR), StandardRole.OPERATOR);
    }

    @Test
    public void testOperatorReadRuntimePermitted() throws Exception {
        permitted(READONLY_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), StandardRole.OPERATOR);
    }

    @Test
    public void testOperatorWriteRuntimePermitted() throws Exception {
        permitted(READWRITE_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), StandardRole.OPERATOR);
    }

    // maintainer

    @Test
    public void testMaintainerNoAccess() throws Exception {
        noAccess(READ_RESOURCE_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), StandardRole.MAINTAINER);
    }

    @Test
    public void testMaintainerReadConfigPermitted() throws Exception {
        permitted(READ_RESOURCE_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), StandardRole.MAINTAINER);
    }

    @Test
    public void testMaintainerWriteConfigPermitted() throws Exception {
        permitted(ADD, pathAddress(UNCONSTRAINED_RESOURCE, BAR), StandardRole.MAINTAINER);
    }

    @Test
    public void testMaintainerReadRuntimePermitted() throws Exception {
        permitted(READONLY_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), StandardRole.MAINTAINER);
    }

    @Test
    public void testMaintainerWriteRuntimePermitted() throws Exception {
        permitted(READWRITE_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), StandardRole.MAINTAINER);
    }

    // deployer

    @Test
    public void testDeployerNoAccess() throws Exception {
        noAccess(READ_RESOURCE_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), StandardRole.DEPLOYER);
    }

    @Test
    public void testDeployerReadConfigPermitted() throws Exception {
        permitted(READ_RESOURCE_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), StandardRole.DEPLOYER);
    }

    @Test
    public void testDeployerWriteConfigPermitted() throws Exception {
        permitted(ADD, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, BAR), StandardRole.DEPLOYER);
    }

    @Test
    public void testDeployerReadRuntimePermitted() throws Exception {
        permitted(READONLY_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), StandardRole.DEPLOYER);
    }

    @Test
    public void testDeployerWriteRuntimePermitted() throws Exception {
        permitted(READWRITE_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), StandardRole.DEPLOYER);
    }

    // administrator

    @Test
    public void testAdministratorReadConfigPermitted() throws Exception {
        permitted(READ_RESOURCE_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), StandardRole.ADMINISTRATOR);
    }

    @Test
    public void testAdministratorWriteConfigPermitted() throws Exception {
        permitted(ADD, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, BAR), StandardRole.ADMINISTRATOR);
    }

    @Test
    public void testAdministratorReadRuntimePermitted() throws Exception {
        permitted(READONLY_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), StandardRole.ADMINISTRATOR);
    }

    @Test
    public void testAdministratorWriteRuntimePermitted() throws Exception {
        permitted(READWRITE_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), StandardRole.ADMINISTRATOR);
    }

    // auditor, superuser -- TODO AuditContraint

    // ---
    // testing utils

    private void permitted(String operation, PathAddress pathAddress, StandardRole role) {
        assertPermitted(executeWithRole(Util.createOperation(operation, pathAddress), role));
    }

    private void denied(String operation, PathAddress pathAddress, StandardRole role) {
        assertDenied(executeWithRole(Util.createOperation(operation, pathAddress), role));
    }

    private void noAccess(String operation, PathAddress pathAddress, StandardRole role) {
        assertNoAccess(executeWithRole(Util.createOperation(operation, pathAddress), role));
    }

    protected ModelNode executeWithRole(ModelNode operation, StandardRole role) {
        operation.get(OPERATION_HEADERS, "roles").add(role.name());
        return getController().execute(operation, null, null, null);
    }

    private static void assertPermitted(ModelNode operationResult) {
        assertEquals(SUCCESS, operationResult.get(OUTCOME).asString());
    }

    private static void assertDenied(ModelNode operationResult) {
        assertEquals(FAILED, operationResult.get(OUTCOME).asString());
        assertTrue(operationResult.get(FAILURE_DESCRIPTION).asString().contains("Permission denied"));
    }

    private static void assertNoAccess(ModelNode operationResult) {
        assertEquals(FAILED, operationResult.get(OUTCOME).asString());
        assertTrue(operationResult.get(FAILURE_DESCRIPTION).asString().contains("not found"));
    }

    // model definition

    private static final SensitivityClassification MY_SENSITIVITY
            = new SensitivityClassification("test", "my-sensitivity", true, true, true);
    private static final AccessConstraintDefinition MY_SENSITIVE_CONSTRAINT
            = new SensitiveTargetAccessConstraintDefinition(MY_SENSITIVITY);

    private static final ApplicationTypeConfig MY_APPLICATION_TYPE
            = new ApplicationTypeConfig("test", "my-application-type", true);
    private static final AccessConstraintDefinition MY_APPLICATION_CONSTRAINT
            = new ApplicationTypeAccessConstraintDefinition(MY_APPLICATION_TYPE);

    @Override
    protected void initModel(Resource rootResource, ManagementResourceRegistration registration) {
        GlobalOperationHandlers.registerGlobalOperations(registration, ProcessType.EMBEDDED_SERVER);

        registration.registerSubModel(new TestResourceDefinition(UNCONSTRAINED_RESOURCE));
        registration.registerSubModel(new TestResourceDefinition(SENSITIVE_CONSTRAINED_RESOURCE,
                MY_SENSITIVE_CONSTRAINT));
        registration.registerSubModel(new TestResourceDefinition(APPLICATION_CONSTRAINED_RESOURCE,
                MY_APPLICATION_CONSTRAINT));
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
        public void registerOperations(ManagementResourceRegistration resourceRegistration) {
            super.registerOperations(resourceRegistration);

            resourceRegistration.registerOperationHandler(TestOperationStepHandler.RO_DEFINITION,
                    new TestOperationStepHandler(false));
            resourceRegistration.registerOperationHandler(TestOperationStepHandler.RW_DEFINITION,
                    new TestOperationStepHandler(true));
        }

        @Override
        public List<AccessConstraintDefinition> getAccessConstraints() {
            return constraintDefinitions;
        }
    }

    private static final class TestOperationStepHandler implements OperationStepHandler {
        private static final SimpleOperationDefinition RO_DEFINITION
                = new SimpleOperationDefinitionBuilder(READONLY_OPERATION, new NonResolvingResourceDescriptionResolver())
                .setReplyType(ModelType.INT)
                .build();

        private static final SimpleOperationDefinition RW_DEFINITION
                = new SimpleOperationDefinitionBuilder(READWRITE_OPERATION, new NonResolvingResourceDescriptionResolver())
                .setReplyType(ModelType.INT)
                .build();

        private final boolean modify;

        private TestOperationStepHandler(boolean modify) {
            this.modify = modify;
        }

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            context.getServiceRegistry(modify); // causes read-runtime/write-runtime auth, otherwise ignored
            context.getResult().set(new Random().nextInt());
            context.stepCompleted();
        }
    }
}
