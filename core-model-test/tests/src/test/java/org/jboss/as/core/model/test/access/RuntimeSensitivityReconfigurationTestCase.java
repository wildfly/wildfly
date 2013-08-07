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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTHORIZATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CLASSIFICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONFIGURED_REQUIRES_ADDRESSABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONFIGURED_REQUIRES_READ;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONFIGURED_REQUIRES_WRITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONSTRAINT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SENSITIVITY_CLASSIFICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.access.constraint.SensitivityClassification;
import org.jboss.as.controller.access.rbac.StandardRole;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.core.model.test.AbstractCoreModelTest;
import org.jboss.as.core.model.test.KernelServices;
import org.jboss.as.core.model.test.TestModelType;
import org.jboss.dmr.ModelNode;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Ladislav Thon <lthon@redhat.com>
 */
public class RuntimeSensitivityReconfigurationTestCase extends AbstractCoreModelTest {
    private static final String SOCKET_CONFIG = SensitivityClassification.SOCKET_CONFIG.getName();
    private static final String FOO = "foo";

    private KernelServices kernelServices;

    @Before
    public void setUp() throws Exception {
        kernelServices = createKernelServicesBuilder(TestModelType.STANDALONE)
                .setXmlResource("constraints.xml")
                .validateDescription()
                .build();

        ModelNode operation = Util.createOperation(ADD, pathAddress(INTERFACE, FOO));
        operation.get("any-ipv4-address").set(true);
        executeWithRoles(operation, StandardRole.SUPERUSER);
    }

    @Test
    public void testMonitor() {
        testMonitorOrOperatorOrDeployer(StandardRole.MONITOR);
    }

    @Test
    public void testOperator() {
        testMonitorOrOperatorOrDeployer(StandardRole.OPERATOR);
    }

    private void testMonitorOrOperatorOrDeployer(StandardRole role) {
        assertTrue(kernelServices.isSuccessfulBoot());

        reconfigureSensitivity(SOCKET_CONFIG, true, true, true);
        assertNoAccess(readInterface(FOO, role));
        assertNoAccess(addInterface(role));

        reconfigureSensitivity(SOCKET_CONFIG, false, true, true);
        assertDenied(readInterface(FOO, role));
        assertDenied(addInterface(role));

        reconfigureSensitivity(SOCKET_CONFIG, false, false, true);
        assertPermitted(readInterface(FOO, role));
        assertDenied(addInterface(role));

        reconfigureSensitivity(SOCKET_CONFIG, false, false, false);
        assertPermitted(readInterface(FOO, role));
        assertDenied(addInterface(role));
    }

    @Test
    public void testMaintainer() {
        assertTrue(kernelServices.isSuccessfulBoot());

        StandardRole role = StandardRole.MAINTAINER;

        reconfigureSensitivity(SOCKET_CONFIG, true, true, true);
        assertNoAccess(readInterface(FOO, role));
        assertNoAccess(addInterface(role));

        reconfigureSensitivity(SOCKET_CONFIG, false, true, true);
        assertDenied(readInterface(FOO, role));
        assertDenied(addInterface(role));

        reconfigureSensitivity(SOCKET_CONFIG, false, false, true);
        assertPermitted(readInterface(FOO, role));
        assertDenied(addInterface(role));

        reconfigureSensitivity(SOCKET_CONFIG, false, false, false);
        assertPermitted(readInterface(FOO, role));
        assertPermitted(addInterface(role));
    }

    @Test
    public void testDeployer() {
        testMonitorOrOperatorOrDeployer(StandardRole.DEPLOYER);
    }

    @Test
    public void testAdministrator() {
        testAdministratorOrSuperuser(StandardRole.ADMINISTRATOR);
    }

    private void testAdministratorOrSuperuser(StandardRole role) {
        assertTrue(kernelServices.isSuccessfulBoot());

        reconfigureSensitivity(SOCKET_CONFIG, true, true, true);
        assertPermitted(readInterface(FOO, role));
        assertPermitted(addInterface(role));

        reconfigureSensitivity(SOCKET_CONFIG, false, true, true);
        assertPermitted(readInterface(FOO, role));
        assertPermitted(addInterface(role));

        reconfigureSensitivity(SOCKET_CONFIG, false, false, true);
        assertPermitted(readInterface(FOO, role));
        assertPermitted(addInterface(role));

        reconfigureSensitivity(SOCKET_CONFIG, false, false, false);
        assertPermitted(readInterface(FOO, role));
        assertPermitted(addInterface(role));
    }

    @Test
    public void testAuditor() {
        assertTrue(kernelServices.isSuccessfulBoot());

        StandardRole role = StandardRole.AUDITOR;

        reconfigureSensitivity(SOCKET_CONFIG, true, true, true);
        assertPermitted(readInterface(FOO, role));
        assertDenied(addInterface(role));

        reconfigureSensitivity(SOCKET_CONFIG, false, true, true);
        assertPermitted(readInterface(FOO, role));
        assertDenied(addInterface(role));

        reconfigureSensitivity(SOCKET_CONFIG, false, false, true);
        assertPermitted(readInterface(FOO, role));
        assertDenied(addInterface(role));

        reconfigureSensitivity(SOCKET_CONFIG, false, false, false);
        assertPermitted(readInterface(FOO, role));
        assertDenied(addInterface(role));
    }

    @Test
    public void testSuperuser() {
        testAdministratorOrSuperuser(StandardRole.SUPERUSER);
    }

    // test utils

    private void reconfigureSensitivity(String sensitivity, Boolean requiresAccess, Boolean requiresRead, Boolean requiresWrite) {
        PathAddress address = pathAddress(
                pathElement(CORE_SERVICE, MANAGEMENT),
                pathElement(ACCESS, AUTHORIZATION),
                pathElement(CONSTRAINT, SENSITIVITY_CLASSIFICATION),
                pathElement(TYPE, CORE),
                pathElement(CLASSIFICATION, sensitivity));

        ModelNode operation = Util.createOperation(WRITE_ATTRIBUTE_OPERATION, address);

        if (requiresAccess != null) {
            operation.get(NAME).set(CONFIGURED_REQUIRES_ADDRESSABLE);
            operation.get(VALUE).set(requiresAccess);
            executeWithRoles(operation, StandardRole.SUPERUSER);
        }

        if (requiresRead != null) {
            operation.get(NAME).set(CONFIGURED_REQUIRES_READ);
            operation.get(VALUE).set(requiresRead);
            executeWithRoles(operation, StandardRole.SUPERUSER);
        }

        if (requiresWrite != null) {
            operation.get(NAME).set(CONFIGURED_REQUIRES_WRITE);
            operation.get(VALUE).set(requiresWrite);
            executeWithRoles(operation, StandardRole.SUPERUSER);
        }
    }

    private ModelNode readInterface(String name, StandardRole role) {
        ModelNode operation = Util.createOperation(READ_RESOURCE_OPERATION, pathAddress(INTERFACE, name));
        return executeWithRoles(operation, role);
    }

    private static int counter = 1;

    private ModelNode addInterface(StandardRole role) {
        ModelNode operation = Util.createOperation(ADD, pathAddress(INTERFACE, "test" + (counter++)));
        operation.get("any-ipv4-address").set(true);
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
