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

package org.jboss.as.core.model.test.access;

import static org.jboss.as.controller.PathAddress.pathAddress;
import static org.jboss.as.controller.PathElement.pathElement;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ACCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.APPLICATION_CLASSIFICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTHORIZATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.BYTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CLASSIFICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONFIGURED_APPLICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONSTRAINT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.access.constraint.ApplicationTypeConfig;
import org.jboss.as.controller.access.rbac.StandardRole;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.core.model.test.AbstractCoreModelTest;
import org.jboss.as.core.model.test.KernelServices;
import org.jboss.as.core.model.test.TestModelType;
import org.jboss.as.domain.management.CoreManagementResourceDefinition;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.dmr.ModelNode;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Ladislav Thon <lthon@redhat.com>
 */
public class RuntimeApplicationTypeReconfigurationTestCase extends AbstractCoreModelTest {
    private static final String DEPLOYMENT = ApplicationTypeConfig.DEPLOYMENT.getName();
    private static final String FOO = "foo";

    private KernelServices kernelServices;

    @Before
    public void setUp() throws Exception {
        kernelServices = createKernelServicesBuilder(TestModelType.STANDALONE)
                .setXmlResource("constraints.xml")
                .validateDescription()
                .build();

        ModelNode operation = Util.createOperation(ADD, pathAddress(DEPLOYMENT, FOO));
        operation.get(CONTENT).get(0).get(BYTES).set(new byte[]{0x00, 0x11, 0x22, 0x33, 0x44});
        executeWithRoles(operation, StandardRole.SUPERUSER);
    }

    @Test
    public void testMonitor() {
        testMonitorOrOperatorOrAuditor(StandardRole.MONITOR);
    }

    @Test
    public void testOperator() {
        testMonitorOrOperatorOrAuditor(StandardRole.OPERATOR);
    }

    private void testMonitorOrOperatorOrAuditor(StandardRole role) {
        assertTrue(kernelServices.isSuccessfulBoot());

        reconfigureApplicationType(DEPLOYMENT, false);
        assertPermitted(readDeployment(FOO, role));
        assertDenied(deploySomething(role));

        reconfigureApplicationType(DEPLOYMENT, true);
        assertPermitted(readDeployment(FOO, role));
        assertDenied(deploySomething(role));
    }

    @Test
    public void testMaintainer() {
        testMaintainerOrAdministrator(StandardRole.MAINTAINER);
    }

    private void testMaintainerOrAdministrator(StandardRole role) {
        assertTrue(kernelServices.isSuccessfulBoot());

        reconfigureApplicationType(DEPLOYMENT, false);
        assertPermitted(readDeployment(FOO, role));
        assertPermitted(deploySomething(role));

        reconfigureApplicationType(DEPLOYMENT, true);
        assertPermitted(readDeployment(FOO, role));
        assertPermitted(deploySomething(role));
    }

    @Test
    public void testDeployer() {
        assertTrue(kernelServices.isSuccessfulBoot());

        StandardRole role = StandardRole.DEPLOYER;

        reconfigureApplicationType(DEPLOYMENT, false);
        assertPermitted(readDeployment(FOO, role));
        assertDenied(deploySomething(role));

        reconfigureApplicationType(DEPLOYMENT, true);
        assertPermitted(readDeployment(FOO, role));
        assertPermitted(deploySomething(role));
    }

    @Test
    public void testAdministrator() {
        testMaintainerOrAdministrator(StandardRole.ADMINISTRATOR);
    }

    @Test
    public void testAuditor() {
        testMonitorOrOperatorOrAuditor(StandardRole.AUDITOR);
    }

    @Test
    public void testSuperuser() {
        testMaintainerOrAdministrator(StandardRole.SUPERUSER);
    }

    // test utils

    private void reconfigureApplicationType(String applicationType, Boolean isApplication) {
        PathAddress address = pathAddress(
                CoreManagementResourceDefinition.PATH_ELEMENT,
                pathElement(ACCESS, AUTHORIZATION),
                pathElement(CONSTRAINT, APPLICATION_CLASSIFICATION),
                pathElement(TYPE, CORE),
                pathElement(CLASSIFICATION, applicationType));

        ModelNode operation = Util.createOperation(WRITE_ATTRIBUTE_OPERATION, address);

        if (isApplication != null) {
            operation.get(NAME).set(CONFIGURED_APPLICATION);
            operation.get(VALUE).set(isApplication);
            ModelTestUtils.checkOutcome(executeWithRoles(operation, StandardRole.SUPERUSER));
        }
    }

    private ModelNode readDeployment(String name, StandardRole role) {
        ModelNode operation = Util.createOperation(READ_RESOURCE_OPERATION, pathAddress(DEPLOYMENT, name));
        return executeWithRoles(operation, role);
    }

    private static int counter = 1;

    private ModelNode deploySomething(StandardRole role) {
        ModelNode operation = Util.createOperation(ADD, pathAddress(DEPLOYMENT, "test" + (counter++)));
        operation.get(CONTENT).get(0).get(BYTES).set(new byte[]{0x00, 0x11, 0x22, 0x33, 0x44});
        return executeWithRoles(operation, role);
    }

    private ModelNode executeWithRoles(ModelNode operation, StandardRole... roles) {
        for (StandardRole role : roles) {
            operation.get(OPERATION_HEADERS, "roles").add(role.name());
        }
        return kernelServices.executeOperation(operation);
    }

    protected static void assertPermitted(ModelNode operationResult) {
        assertEquals(SUCCESS, operationResult.get(OUTCOME).asString());
    }

    protected static void assertDenied(ModelNode operationResult) {
        assertEquals(FAILED, operationResult.get(OUTCOME).asString());
        assertTrue(operationResult.get(FAILURE_DESCRIPTION).asString().contains("Permission denied"));
    }

    protected static void assertNoAccess(ModelNode operationResult) {
        assertEquals(FAILED, operationResult.get(OUTCOME).asString());
        assertTrue(operationResult.get(FAILURE_DESCRIPTION).asString().contains("not found"));
    }
}
