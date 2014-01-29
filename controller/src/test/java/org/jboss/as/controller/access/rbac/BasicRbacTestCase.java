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

import static org.jboss.as.controller.PathAddress.pathAddress;
import static org.jboss.as.controller.PathElement.pathElement;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.access.constraint.ApplicationTypeConfig;
import org.jboss.as.controller.access.constraint.SensitivityClassification;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.access.management.ApplicationTypeAccessConstraintDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.NonResolvingResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.global.GlobalOperationHandlers;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.junit.Before;
import org.junit.Test;

/**
 * Test the basic RBAC permission scheme.
 *
 * TODO audit constraints
 *
 * @author Ladislav Thon <lthon@redhat.com>
 */
public class BasicRbacTestCase extends AbstractRbacTestBase {
    public static final String UNCONSTRAINED_RESOURCE = "unconstrained-resource";
    public static final String APPLICATION_CONSTRAINED_RESOURCE = "application-constrained-resource";
    public static final String SENSITIVE_NON_ADDRESSABLE_RESOURCE = "sensitive-non-addressable-resource";
    public static final String SENSITIVE_ADDRESSABLE_RESOURCE = "sensitive-addressable-resource";
    public static final String SENSITIVE_READ_ONLY_RESOURCE = "sensitive-read-only-resource";

    private static final PathElement CORE_MANAGEMENT = PathElement.pathElement(ModelDescriptionConstants.CORE_SERVICE, ModelDescriptionConstants.MANAGEMENT);
    private static final PathElement ACCESS_AUDIT = PathElement.pathElement(ModelDescriptionConstants.ACCESS, ModelDescriptionConstants.AUDIT);
    private static final PathAddress ACCESS_AUDIT_ADDR = PathAddress.pathAddress(CORE_MANAGEMENT, ACCESS_AUDIT);

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

        operation = Util.createOperation(ADD, pathAddress(SENSITIVE_NON_ADDRESSABLE_RESOURCE, FOO));
        executeWithRole(operation, StandardRole.SUPERUSER);

        operation = Util.createOperation(ADD, pathAddress(SENSITIVE_ADDRESSABLE_RESOURCE, FOO));
        executeWithRole(operation, StandardRole.SUPERUSER);

        operation = Util.createOperation(ADD, pathAddress(SENSITIVE_READ_ONLY_RESOURCE, FOO));
        executeWithRole(operation, StandardRole.SUPERUSER);

        operation = Util.createOperation(ADD, PathAddress.pathAddress(CORE_MANAGEMENT));
        executeWithRole(operation, StandardRole.SUPERUSER);

        operation = Util.createOperation(ADD, PathAddress.pathAddress(ACCESS_AUDIT_ADDR));
        executeWithRole(operation, StandardRole.SUPERUSER);
    }

    // monitor

    @Test
    public void testMonitorNonAddressable() throws Exception {
        noAddress(READ_RESOURCE_OPERATION, pathAddress(SENSITIVE_NON_ADDRESSABLE_RESOURCE, FOO), StandardRole.MONITOR);
    }

    @Test
    public void testMonitorReadConfigDenied() throws Exception {
        denied(READ_RESOURCE_OPERATION, pathAddress(SENSITIVE_ADDRESSABLE_RESOURCE, FOO), StandardRole.MONITOR);
        denied(READ_RESOURCE_OPERATION, ACCESS_AUDIT_ADDR, StandardRole.MONITOR);
    }

    @Test
    public void testMonitorReadConfigPermitted() throws Exception {
        permitted(READ_RESOURCE_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), StandardRole.MONITOR);
        permitted(READ_RESOURCE_OPERATION, pathAddress(SENSITIVE_READ_ONLY_RESOURCE, FOO), StandardRole.MONITOR);
        permitted(READ_RESOURCE_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), StandardRole.MONITOR);
    }

    @Test
    public void testMonitorWriteConfigDenied() throws Exception {
        denied(ADD, pathAddress(UNCONSTRAINED_RESOURCE, BAR), StandardRole.MONITOR);
        denied(ADD, pathAddress(SENSITIVE_READ_ONLY_RESOURCE, BAR), StandardRole.MONITOR);
        denied(ADD, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, BAR), StandardRole.MONITOR);
    }

    @Test
    public void testMonitorReadRuntimeDenied() throws Exception {
        denied(READONLY_OPERATION, pathAddress(SENSITIVE_ADDRESSABLE_RESOURCE, FOO), StandardRole.MONITOR);
    }

    @Test
    public void testMonitorReadRuntimePermitted() throws Exception {
        permitted(READONLY_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), StandardRole.MONITOR);
        permitted(READONLY_OPERATION, pathAddress(SENSITIVE_READ_ONLY_RESOURCE, FOO), StandardRole.MONITOR);
        permitted(READONLY_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), StandardRole.MONITOR);
    }

    @Test
    public void testMonitorWriteRuntimeDenied() throws Exception {
        denied(READWRITE_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), StandardRole.MONITOR);
        denied(READWRITE_OPERATION, pathAddress(SENSITIVE_READ_ONLY_RESOURCE, FOO), StandardRole.MONITOR);
        denied(READWRITE_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), StandardRole.MONITOR);
    }

    // operator

    @Test
    public void testOperatorNonAddressable() throws Exception {
        noAddress(READ_RESOURCE_OPERATION, pathAddress(SENSITIVE_NON_ADDRESSABLE_RESOURCE, FOO), StandardRole.OPERATOR);
    }

    @Test
    public void testOperatorReadConfigDenied() throws Exception {
        denied(READ_RESOURCE_OPERATION, pathAddress(SENSITIVE_ADDRESSABLE_RESOURCE, FOO), StandardRole.OPERATOR);
        denied(READ_RESOURCE_OPERATION, ACCESS_AUDIT_ADDR, StandardRole.OPERATOR);
    }

    @Test
    public void testOperatorReadConfigPermitted() throws Exception {
        permitted(READ_RESOURCE_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), StandardRole.OPERATOR);
        permitted(READ_RESOURCE_OPERATION, pathAddress(SENSITIVE_READ_ONLY_RESOURCE, FOO), StandardRole.OPERATOR);
        permitted(READ_RESOURCE_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), StandardRole.OPERATOR);
    }

    @Test
    public void testOperatorWriteConfigDenied() throws Exception {
        denied(ADD, pathAddress(UNCONSTRAINED_RESOURCE, BAR), StandardRole.OPERATOR);
        denied(ADD, pathAddress(SENSITIVE_READ_ONLY_RESOURCE, BAR), StandardRole.OPERATOR);
        denied(ADD, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, BAR), StandardRole.OPERATOR);
    }

    @Test
    public void testOperatorReadRuntimeDenied() throws Exception {
        denied(READONLY_OPERATION, pathAddress(SENSITIVE_ADDRESSABLE_RESOURCE, FOO), StandardRole.OPERATOR);
    }

    @Test
    public void testOperatorReadRuntimePermitted() throws Exception {
        permitted(READONLY_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), StandardRole.OPERATOR);
        permitted(READONLY_OPERATION, pathAddress(SENSITIVE_READ_ONLY_RESOURCE, FOO), StandardRole.OPERATOR);
        permitted(READONLY_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), StandardRole.OPERATOR);
    }

    @Test
    public void testOperatorWriteRuntimeDenied() throws Exception {
        denied(READWRITE_OPERATION, pathAddress(SENSITIVE_READ_ONLY_RESOURCE, BAR), StandardRole.OPERATOR);
    }

    @Test
    public void testOperatorWriteRuntimePermitted() throws Exception {
        permitted(READWRITE_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), StandardRole.OPERATOR);
        permitted(READWRITE_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), StandardRole.OPERATOR);
    }

    // maintainer

    @Test
    public void testMaintainerNonAddressable() throws Exception {
        noAddress(READ_RESOURCE_OPERATION, pathAddress(SENSITIVE_NON_ADDRESSABLE_RESOURCE, FOO), StandardRole.MAINTAINER);
    }

    @Test
    public void testMaintainerReadConfigDenied() throws Exception {
        denied(READ_RESOURCE_OPERATION, pathAddress(SENSITIVE_ADDRESSABLE_RESOURCE, FOO), StandardRole.MAINTAINER);
        denied(READ_RESOURCE_OPERATION, ACCESS_AUDIT_ADDR, StandardRole.MAINTAINER);
    }

    @Test
    public void testMaintainerReadConfigPermitted() throws Exception {
        permitted(READ_RESOURCE_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), StandardRole.MAINTAINER);
        permitted(READ_RESOURCE_OPERATION, pathAddress(SENSITIVE_READ_ONLY_RESOURCE, FOO), StandardRole.MAINTAINER);
        permitted(READ_RESOURCE_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), StandardRole.MAINTAINER);
    }

    @Test
    public void testMaintainerWriteConfigPermitted() throws Exception {
        permitted(ADD, pathAddress(UNCONSTRAINED_RESOURCE, BAR), StandardRole.MAINTAINER);
        permitted(ADD, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, BAR), StandardRole.MAINTAINER);
    }

    @Test
    public void testMaintainerReadRuntimePermitted() throws Exception {
        permitted(READONLY_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), StandardRole.MAINTAINER);
        permitted(READONLY_OPERATION, pathAddress(SENSITIVE_READ_ONLY_RESOURCE, FOO), StandardRole.MAINTAINER);
        permitted(READONLY_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), StandardRole.MAINTAINER);
    }

    @Test
    public void testMaintainerWriteRuntimePermitted() throws Exception {
        permitted(READWRITE_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), StandardRole.MAINTAINER);
        permitted(READWRITE_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), StandardRole.MAINTAINER);
    }

    // deployer

    @Test
    public void testDeployerNoAccess() throws Exception {
        noAddress(READ_RESOURCE_OPERATION, pathAddress(SENSITIVE_NON_ADDRESSABLE_RESOURCE, FOO), StandardRole.DEPLOYER);
    }

    @Test
    public void testDeployerReadConfigDenied() throws Exception {
        denied(READ_RESOURCE_OPERATION, pathAddress(SENSITIVE_ADDRESSABLE_RESOURCE, FOO), StandardRole.DEPLOYER);
        denied(READ_RESOURCE_OPERATION, ACCESS_AUDIT_ADDR, StandardRole.DEPLOYER);
    }

    @Test
    public void testDeployerReadConfigPermitted() throws Exception {
        permitted(READ_RESOURCE_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), StandardRole.DEPLOYER);
        permitted(READ_RESOURCE_OPERATION, pathAddress(SENSITIVE_READ_ONLY_RESOURCE, FOO), StandardRole.DEPLOYER);
        permitted(READ_RESOURCE_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), StandardRole.DEPLOYER);
    }

    @Test
    public void testDeployerWriteConfigDenied() throws Exception {
        denied(ADD, pathAddress(UNCONSTRAINED_RESOURCE, BAR), StandardRole.DEPLOYER);
        denied(ADD, pathAddress(SENSITIVE_READ_ONLY_RESOURCE, BAR), StandardRole.DEPLOYER);
    }

    @Test
    public void testDeployerWriteConfigPermitted() throws Exception {
        permitted(ADD, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, BAR), StandardRole.DEPLOYER);
    }

    @Test
    public void testDeployerReadRuntimePermitted() throws Exception {
        permitted(READONLY_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), StandardRole.DEPLOYER);
        permitted(READONLY_OPERATION, pathAddress(SENSITIVE_READ_ONLY_RESOURCE, FOO), StandardRole.DEPLOYER);
        permitted(READONLY_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), StandardRole.DEPLOYER);
    }

    @Test
    public void testDeployerWriteRuntimeDenied() throws Exception {
        denied(READWRITE_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, BAR), StandardRole.DEPLOYER);
        denied(READWRITE_OPERATION, pathAddress(SENSITIVE_READ_ONLY_RESOURCE, BAR), StandardRole.DEPLOYER);
    }

    @Test
    public void testDeployerWriteRuntimePermitted() throws Exception {
        permitted(READWRITE_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), StandardRole.DEPLOYER);
    }

    // administrator

    @Test
    public void testAdministratorReadConfigDenied() throws Exception {
        denied(READ_RESOURCE_OPERATION, ACCESS_AUDIT_ADDR, StandardRole.ADMINISTRATOR);
    }

    @Test
    public void testAdministratorReadConfigPermitted() throws Exception {
        permitted(READ_RESOURCE_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), StandardRole.ADMINISTRATOR);
        permitted(READ_RESOURCE_OPERATION, pathAddress(SENSITIVE_NON_ADDRESSABLE_RESOURCE, FOO), StandardRole.ADMINISTRATOR);
        permitted(READ_RESOURCE_OPERATION, pathAddress(SENSITIVE_ADDRESSABLE_RESOURCE, FOO), StandardRole.ADMINISTRATOR);
        permitted(READ_RESOURCE_OPERATION, pathAddress(SENSITIVE_READ_ONLY_RESOURCE, FOO), StandardRole.ADMINISTRATOR);
        permitted(READ_RESOURCE_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), StandardRole.ADMINISTRATOR);
    }

    @Test
    public void testAdministratorWriteConfigPermitted() throws Exception {
        permitted(ADD, pathAddress(UNCONSTRAINED_RESOURCE, BAR), StandardRole.ADMINISTRATOR);
        permitted(ADD, pathAddress(SENSITIVE_NON_ADDRESSABLE_RESOURCE, BAR), StandardRole.ADMINISTRATOR);
        permitted(ADD, pathAddress(SENSITIVE_ADDRESSABLE_RESOURCE, BAR), StandardRole.ADMINISTRATOR);
        permitted(ADD, pathAddress(SENSITIVE_READ_ONLY_RESOURCE, BAR), StandardRole.ADMINISTRATOR);
        permitted(ADD, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, BAR), StandardRole.ADMINISTRATOR);
    }

    @Test
    public void testAdministratorReadRuntimePermitted() throws Exception {
        permitted(READONLY_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), StandardRole.ADMINISTRATOR);
        permitted(READONLY_OPERATION, pathAddress(SENSITIVE_NON_ADDRESSABLE_RESOURCE, FOO), StandardRole.ADMINISTRATOR);
        permitted(READONLY_OPERATION, pathAddress(SENSITIVE_ADDRESSABLE_RESOURCE, FOO), StandardRole.ADMINISTRATOR);
        permitted(READONLY_OPERATION, pathAddress(SENSITIVE_READ_ONLY_RESOURCE, FOO), StandardRole.ADMINISTRATOR);
        permitted(READONLY_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), StandardRole.ADMINISTRATOR);
    }

    @Test
    public void testAdministratorWriteRuntimePermitted() throws Exception {
        permitted(READWRITE_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, BAR), StandardRole.ADMINISTRATOR);
        permitted(READWRITE_OPERATION, pathAddress(SENSITIVE_NON_ADDRESSABLE_RESOURCE, BAR), StandardRole.ADMINISTRATOR);
        permitted(READWRITE_OPERATION, pathAddress(SENSITIVE_ADDRESSABLE_RESOURCE, BAR), StandardRole.ADMINISTRATOR);
        permitted(READWRITE_OPERATION, pathAddress(SENSITIVE_READ_ONLY_RESOURCE, BAR), StandardRole.ADMINISTRATOR);
        permitted(READWRITE_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, BAR), StandardRole.ADMINISTRATOR);
    }

    // auditor

    @Test
    public void testAuditorReadConfigPermitted() throws Exception {
        permitted(READ_RESOURCE_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), StandardRole.AUDITOR);
        permitted(READ_RESOURCE_OPERATION, pathAddress(SENSITIVE_NON_ADDRESSABLE_RESOURCE, FOO), StandardRole.AUDITOR);
        permitted(READ_RESOURCE_OPERATION, pathAddress(SENSITIVE_ADDRESSABLE_RESOURCE, FOO), StandardRole.AUDITOR);
        permitted(READ_RESOURCE_OPERATION, pathAddress(SENSITIVE_READ_ONLY_RESOURCE, FOO), StandardRole.AUDITOR);
        permitted(READ_RESOURCE_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), StandardRole.AUDITOR);
        permitted(READ_RESOURCE_OPERATION, ACCESS_AUDIT_ADDR, StandardRole.AUDITOR);
    }

    @Test
    public void testAuditorWriteConfigDenied() throws Exception {
        denied(ADD, pathAddress(UNCONSTRAINED_RESOURCE, BAR), StandardRole.AUDITOR);
        denied(ADD, pathAddress(SENSITIVE_READ_ONLY_RESOURCE, BAR), StandardRole.AUDITOR);
        denied(ADD, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, BAR), StandardRole.AUDITOR);
    }

    @Test
    public void testAuditorWriteConfigPermitted() throws Exception {
        permitted(REMOVE, ACCESS_AUDIT_ADDR, StandardRole.AUDITOR);
        permitted(ADD, ACCESS_AUDIT_ADDR, StandardRole.AUDITOR);
    }

    @Test
    public void testAuditorReadRuntimePermitted() throws Exception {
        permitted(READONLY_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), StandardRole.AUDITOR);
        permitted(READONLY_OPERATION, pathAddress(SENSITIVE_NON_ADDRESSABLE_RESOURCE, FOO), StandardRole.AUDITOR);
        permitted(READONLY_OPERATION, pathAddress(SENSITIVE_ADDRESSABLE_RESOURCE, FOO), StandardRole.AUDITOR);
        permitted(READONLY_OPERATION, pathAddress(SENSITIVE_READ_ONLY_RESOURCE, FOO), StandardRole.AUDITOR);
        permitted(READONLY_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), StandardRole.AUDITOR);
        permitted(READONLY_OPERATION, ACCESS_AUDIT_ADDR, StandardRole.AUDITOR);
    }

    @Test
    public void testAuditorWriteRuntimeDenied() throws Exception {
        denied(READWRITE_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), StandardRole.AUDITOR);
        denied(READWRITE_OPERATION, pathAddress(SENSITIVE_READ_ONLY_RESOURCE, FOO), StandardRole.AUDITOR);
        denied(READWRITE_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), StandardRole.AUDITOR);
        permitted(READWRITE_OPERATION, ACCESS_AUDIT_ADDR, StandardRole.AUDITOR);
    }

    // superuser

    @Test
    public void testSuperUserReadConfigPermitted() throws Exception {
        permitted(READ_RESOURCE_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), StandardRole.SUPERUSER);
        permitted(READ_RESOURCE_OPERATION, pathAddress(SENSITIVE_NON_ADDRESSABLE_RESOURCE, FOO), StandardRole.SUPERUSER);
        permitted(READ_RESOURCE_OPERATION, pathAddress(SENSITIVE_ADDRESSABLE_RESOURCE, FOO), StandardRole.SUPERUSER);
        permitted(READ_RESOURCE_OPERATION, pathAddress(SENSITIVE_READ_ONLY_RESOURCE, FOO), StandardRole.SUPERUSER);
        permitted(READ_RESOURCE_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), StandardRole.SUPERUSER);
        permitted(READ_RESOURCE_OPERATION, ACCESS_AUDIT_ADDR, StandardRole.AUDITOR);
    }

    @Test
    public void testSuperUserWriteConfigPermitted() throws Exception {
        permitted(ADD, pathAddress(UNCONSTRAINED_RESOURCE, BAR), StandardRole.SUPERUSER);
        permitted(ADD, pathAddress(SENSITIVE_NON_ADDRESSABLE_RESOURCE, BAR), StandardRole.SUPERUSER);
        permitted(ADD, pathAddress(SENSITIVE_ADDRESSABLE_RESOURCE, BAR), StandardRole.SUPERUSER);
        permitted(ADD, pathAddress(SENSITIVE_READ_ONLY_RESOURCE, BAR), StandardRole.SUPERUSER);
        permitted(ADD, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, BAR), StandardRole.SUPERUSER);
        permitted(REMOVE, ACCESS_AUDIT_ADDR, StandardRole.SUPERUSER);
        permitted(ADD, ACCESS_AUDIT_ADDR, StandardRole.SUPERUSER);
    }

    @Test
    public void testSuperUserReadRuntimePermitted() throws Exception {
        permitted(READONLY_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), StandardRole.SUPERUSER);
        permitted(READONLY_OPERATION, pathAddress(SENSITIVE_NON_ADDRESSABLE_RESOURCE, FOO), StandardRole.SUPERUSER);
        permitted(READONLY_OPERATION, pathAddress(SENSITIVE_ADDRESSABLE_RESOURCE, FOO), StandardRole.SUPERUSER);
        permitted(READONLY_OPERATION, pathAddress(SENSITIVE_READ_ONLY_RESOURCE, FOO), StandardRole.SUPERUSER);
        permitted(READONLY_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), StandardRole.SUPERUSER);
        permitted(READONLY_OPERATION, ACCESS_AUDIT_ADDR, StandardRole.AUDITOR);
    }

    @Test
    public void testSuperUserWriteRuntimePermitted() throws Exception {
        permitted(READWRITE_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, BAR), StandardRole.SUPERUSER);
        permitted(READWRITE_OPERATION, pathAddress(SENSITIVE_NON_ADDRESSABLE_RESOURCE, BAR), StandardRole.SUPERUSER);
        permitted(READWRITE_OPERATION, pathAddress(SENSITIVE_ADDRESSABLE_RESOURCE, BAR), StandardRole.SUPERUSER);
        permitted(READWRITE_OPERATION, pathAddress(SENSITIVE_READ_ONLY_RESOURCE, BAR), StandardRole.SUPERUSER);
        permitted(READWRITE_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, BAR), StandardRole.SUPERUSER);
        permitted(READWRITE_OPERATION, ACCESS_AUDIT_ADDR, StandardRole.AUDITOR);
    }

    // Bad role

    @Test
    public void testBadRole() throws Exception {
        ModelNode operation = Util.createOperation(READONLY_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO));
        operation.get(OPERATION_HEADERS, "roles").add("Minataur");
        executeForFailure(operation);
    }

    // model definition

    private static final SensitivityClassification ADDRESSABLE_SENSITIVITY
            = new SensitivityClassification("test", "addressable-sensitivity", true, true, true);
    private static final AccessConstraintDefinition ADDRESSABLE_SENSITIVITY_CONSTRAINT
            = new SensitiveTargetAccessConstraintDefinition(ADDRESSABLE_SENSITIVITY);

    private static final SensitivityClassification READ_SENSITIVITY
            = new SensitivityClassification("test", "read-sensitivity", false, true, true);
    private static final AccessConstraintDefinition READ_SENSITIVITY_CONSTRAINT
            = new SensitiveTargetAccessConstraintDefinition(READ_SENSITIVITY);

    private static final SensitivityClassification WRITE_SENSITIVITY
            = new SensitivityClassification("test", "write-sensitivity", false, false, true);
    private static final AccessConstraintDefinition WRITE_SENSITIVITY_CONSTRAINT
            = new SensitiveTargetAccessConstraintDefinition(WRITE_SENSITIVITY);

    private static final ApplicationTypeConfig MY_APPLICATION_TYPE
            = new ApplicationTypeConfig("test", "my-application-type", true);
    private static final AccessConstraintDefinition MY_APPLICATION_CONSTRAINT
            = new ApplicationTypeAccessConstraintDefinition(MY_APPLICATION_TYPE);

    @Override
    protected void initModel(Resource rootResource, ManagementResourceRegistration registration) {
        GlobalOperationHandlers.registerGlobalOperations(registration, ProcessType.EMBEDDED_SERVER);

        registration.registerSubModel(new TestResourceDefinition(UNCONSTRAINED_RESOURCE));
        registration.registerSubModel(new TestResourceDefinition(SENSITIVE_NON_ADDRESSABLE_RESOURCE,
                ADDRESSABLE_SENSITIVITY_CONSTRAINT));
        registration.registerSubModel(new TestResourceDefinition(SENSITIVE_ADDRESSABLE_RESOURCE,
                READ_SENSITIVITY_CONSTRAINT));
        registration.registerSubModel(new TestResourceDefinition(SENSITIVE_READ_ONLY_RESOURCE,
                WRITE_SENSITIVITY_CONSTRAINT));
        registration.registerSubModel(new TestResourceDefinition(APPLICATION_CONSTRAINED_RESOURCE,
                MY_APPLICATION_CONSTRAINT));

        ManagementResourceRegistration mgmt = registration.registerSubModel(new TestResourceDefinition(CORE_MANAGEMENT));
        mgmt.registerSubModel(new TestResourceDefinition(ACCESS_AUDIT));
    }

    private static final class TestResourceDefinition extends SimpleResourceDefinition {
        private final List<AccessConstraintDefinition> constraintDefinitions;

        TestResourceDefinition(String path, AccessConstraintDefinition... constraintDefinitions) {
            this(pathElement(path), constraintDefinitions);
        }

        TestResourceDefinition(PathElement element, AccessConstraintDefinition... constraintDefinitions) {
            super(element,
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
