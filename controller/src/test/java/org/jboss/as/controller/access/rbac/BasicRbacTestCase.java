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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
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
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Ladislav Thon <lthon@redhat.com>
 */
public class BasicRbacTestCase extends AbstractRbacTestBase {
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
