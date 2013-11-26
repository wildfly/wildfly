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

import static org.jboss.as.controller.PathAddress.EMPTY_ADDRESS;
import static org.jboss.as.controller.PathAddress.pathAddress;
import static org.jboss.as.controller.PathElement.pathElement;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;

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
import org.jboss.as.controller.access.Action;
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
 * @author Ladislav Thon <lthon@redhat.com>
 */
public class DetailedOperationsTestCase extends AbstractRbacTestBase {
    public static final String UNCONSTRAINED_RESOURCE = "unconstrained-resource";
    public static final String SENSITIVE_CONSTRAINED_RESOURCE = "sensitive-constrained-resource";
    public static final String APPLICATION_CONSTRAINED_RESOURCE = "application-constrained-resource";

    private static final PathElement CORE_MANAGEMENT = PathElement.pathElement(ModelDescriptionConstants.CORE_SERVICE, ModelDescriptionConstants.MANAGEMENT);
    private static final PathElement ACCESS_AUDIT = PathElement.pathElement(ModelDescriptionConstants.ACCESS, ModelDescriptionConstants.AUDIT);
    private static final PathAddress ACCESS_AUDIT_ADDR = PathAddress.pathAddress(CORE_MANAGEMENT, ACCESS_AUDIT);


    public static final String READ_CONFIG_OPERATION = "read-config-operation";
    public static final String READ_RUNTIME_OPERATION = "read-runtime-operation";
    public static final String WRITE_CONFIG_OPERATION = "write-config-operation";
    public static final String WRITE_RUNTIME_OPERATION = "write-runtime-operation";

    public static final String READ_CONFIG_READ_RUNTIME_OPERATION = "read-config-read-runtime-operation";
    public static final String READ_CONFIG_WRITE_RUNTIME_OPERATION = "read-config-write-runtime-operation";
    public static final String WRITE_CONFIG_READ_RUNTIME_OPERATION = "write-config-read-runtime-operation";
    public static final String WRITE_CONFIG_WRITE_RUNTIME_OPERATION = "write-config-write-runtime-operation";

    public static final String READ_RUNTIME_READ_CONFIG_OPERATION = "read-runtime-read-config-operation";
    public static final String WRITE_RUNTIME_READ_CONFIG_OPERATION = "write-runtime-read-config-operation";
    public static final String READ_RUNTIME_WRITE_CONFIG_OPERATION = "read-runtime-write-config-operation";
    public static final String WRITE_RUNTIME_WRITE_CONFIG_OPERATION = "write-runtime-write-config-operation";

    public static final String FOO = "foo";

    @Before
    public void setUp() {
        ModelNode operation = Util.createOperation(ADD, pathAddress(UNCONSTRAINED_RESOURCE, FOO));
        executeWithRoles(operation, StandardRole.SUPERUSER);

        operation = Util.createOperation(ADD, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO));
        executeWithRoles(operation, StandardRole.SUPERUSER);

        operation = Util.createOperation(ADD, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO));
        executeWithRoles(operation, StandardRole.SUPERUSER);

        operation = Util.createOperation(ADD, PathAddress.pathAddress(CORE_MANAGEMENT));
        executeWithRoles(operation, StandardRole.SUPERUSER);

        operation = Util.createOperation(ADD, ACCESS_AUDIT_ADDR);
        executeWithRoles(operation, StandardRole.SUPERUSER);
    }

    @Test
    public void testMonitor() {
        StandardRole role = StandardRole.MONITOR;

        permitted(READ_CONFIG_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        noAddress(READ_CONFIG_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        permitted(READ_CONFIG_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        denied(READ_CONFIG_OPERATION, ACCESS_AUDIT_ADDR, role);

        permitted(READ_RUNTIME_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        noAddress(READ_RUNTIME_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        permitted(READ_RUNTIME_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        denied(READ_RUNTIME_OPERATION, ACCESS_AUDIT_ADDR, role);

        denied(WRITE_CONFIG_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        noAddress(WRITE_CONFIG_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        denied(WRITE_CONFIG_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        denied(WRITE_CONFIG_OPERATION, ACCESS_AUDIT_ADDR, role);

        denied(WRITE_RUNTIME_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        noAddress(WRITE_RUNTIME_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        denied(WRITE_RUNTIME_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        denied(WRITE_RUNTIME_OPERATION, ACCESS_AUDIT_ADDR, role);

        permitted(READ_CONFIG_READ_RUNTIME_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        noAddress(READ_CONFIG_READ_RUNTIME_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        permitted(READ_CONFIG_READ_RUNTIME_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        denied(READ_CONFIG_READ_RUNTIME_OPERATION, ACCESS_AUDIT_ADDR, role);

        denied(READ_CONFIG_WRITE_RUNTIME_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        noAddress(READ_CONFIG_WRITE_RUNTIME_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        denied(READ_CONFIG_WRITE_RUNTIME_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        denied(READ_CONFIG_WRITE_RUNTIME_OPERATION, ACCESS_AUDIT_ADDR, role);

        denied(WRITE_CONFIG_READ_RUNTIME_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        noAddress(WRITE_CONFIG_READ_RUNTIME_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        denied(WRITE_CONFIG_READ_RUNTIME_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        denied(WRITE_CONFIG_READ_RUNTIME_OPERATION, ACCESS_AUDIT_ADDR, role);

        denied(WRITE_CONFIG_WRITE_RUNTIME_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        noAddress(WRITE_CONFIG_WRITE_RUNTIME_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        denied(WRITE_CONFIG_WRITE_RUNTIME_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        denied(WRITE_CONFIG_WRITE_RUNTIME_OPERATION, ACCESS_AUDIT_ADDR, role);

        permitted(READ_RUNTIME_READ_CONFIG_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        noAddress(READ_RUNTIME_READ_CONFIG_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        permitted(READ_RUNTIME_READ_CONFIG_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        denied(READ_RUNTIME_READ_CONFIG_OPERATION, ACCESS_AUDIT_ADDR, role);

        denied(WRITE_RUNTIME_READ_CONFIG_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        noAddress(WRITE_RUNTIME_READ_CONFIG_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        denied(WRITE_RUNTIME_READ_CONFIG_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        denied(WRITE_RUNTIME_READ_CONFIG_OPERATION, ACCESS_AUDIT_ADDR, role);

        denied(READ_RUNTIME_WRITE_CONFIG_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        noAddress(READ_RUNTIME_WRITE_CONFIG_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        denied(READ_RUNTIME_WRITE_CONFIG_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        denied(READ_RUNTIME_WRITE_CONFIG_OPERATION, ACCESS_AUDIT_ADDR, role);

        denied(WRITE_RUNTIME_WRITE_CONFIG_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        noAddress(WRITE_RUNTIME_WRITE_CONFIG_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        denied(WRITE_RUNTIME_WRITE_CONFIG_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        denied(WRITE_RUNTIME_WRITE_CONFIG_OPERATION, ACCESS_AUDIT_ADDR, role);
    }

    @Test
    public void testOperator() {
        StandardRole role = StandardRole.OPERATOR;

        permitted(READ_CONFIG_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        noAddress(READ_CONFIG_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        permitted(READ_CONFIG_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        denied(READ_CONFIG_OPERATION, ACCESS_AUDIT_ADDR, role);

        permitted(READ_RUNTIME_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        noAddress(READ_RUNTIME_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        permitted(READ_RUNTIME_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        denied(READ_RUNTIME_OPERATION, ACCESS_AUDIT_ADDR, role);

        denied(WRITE_CONFIG_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        noAddress(WRITE_CONFIG_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        denied(WRITE_CONFIG_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        denied(WRITE_CONFIG_OPERATION, ACCESS_AUDIT_ADDR, role);

        permitted(WRITE_RUNTIME_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        noAddress(WRITE_RUNTIME_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        permitted(WRITE_RUNTIME_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        denied(WRITE_RUNTIME_OPERATION, ACCESS_AUDIT_ADDR, role);

        permitted(READ_CONFIG_READ_RUNTIME_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        noAddress(READ_CONFIG_READ_RUNTIME_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        permitted(READ_CONFIG_READ_RUNTIME_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        denied(READ_CONFIG_READ_RUNTIME_OPERATION, ACCESS_AUDIT_ADDR, role);

        permitted(READ_CONFIG_WRITE_RUNTIME_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        noAddress(READ_CONFIG_WRITE_RUNTIME_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        permitted(READ_CONFIG_WRITE_RUNTIME_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        denied(READ_CONFIG_WRITE_RUNTIME_OPERATION, ACCESS_AUDIT_ADDR, role);

        denied(WRITE_CONFIG_READ_RUNTIME_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        noAddress(WRITE_CONFIG_READ_RUNTIME_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        denied(WRITE_CONFIG_READ_RUNTIME_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        denied(WRITE_CONFIG_READ_RUNTIME_OPERATION, ACCESS_AUDIT_ADDR, role);

        denied(WRITE_CONFIG_WRITE_RUNTIME_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        noAddress(WRITE_CONFIG_WRITE_RUNTIME_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        denied(WRITE_CONFIG_WRITE_RUNTIME_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        denied(WRITE_CONFIG_WRITE_RUNTIME_OPERATION, ACCESS_AUDIT_ADDR, role);

        permitted(READ_RUNTIME_READ_CONFIG_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        noAddress(READ_RUNTIME_READ_CONFIG_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        permitted(READ_RUNTIME_READ_CONFIG_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        denied(READ_RUNTIME_READ_CONFIG_OPERATION, ACCESS_AUDIT_ADDR, role);

        permitted(WRITE_RUNTIME_READ_CONFIG_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        noAddress(WRITE_RUNTIME_READ_CONFIG_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        permitted(WRITE_RUNTIME_READ_CONFIG_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        denied(WRITE_RUNTIME_READ_CONFIG_OPERATION, ACCESS_AUDIT_ADDR, role);

        denied(READ_RUNTIME_WRITE_CONFIG_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        noAddress(READ_RUNTIME_WRITE_CONFIG_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        denied(READ_RUNTIME_WRITE_CONFIG_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        denied(READ_RUNTIME_WRITE_CONFIG_OPERATION, ACCESS_AUDIT_ADDR, role);

        denied(WRITE_RUNTIME_WRITE_CONFIG_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        noAddress(WRITE_RUNTIME_WRITE_CONFIG_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        denied(WRITE_RUNTIME_WRITE_CONFIG_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        denied(WRITE_RUNTIME_WRITE_CONFIG_OPERATION, ACCESS_AUDIT_ADDR, role);
    }

    @Test
    public void testMaintainer() {
        StandardRole role = StandardRole.MAINTAINER;

        permitted(READ_CONFIG_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        noAddress(READ_CONFIG_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        permitted(READ_CONFIG_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        denied(READ_CONFIG_OPERATION, ACCESS_AUDIT_ADDR, role);

        permitted(READ_RUNTIME_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        noAddress(READ_RUNTIME_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        permitted(READ_RUNTIME_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        denied(READ_RUNTIME_OPERATION, ACCESS_AUDIT_ADDR, role);

        permitted(WRITE_CONFIG_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        noAddress(WRITE_CONFIG_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        permitted(WRITE_CONFIG_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        denied(WRITE_CONFIG_OPERATION, ACCESS_AUDIT_ADDR, role);

        permitted(WRITE_RUNTIME_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        noAddress(WRITE_RUNTIME_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        permitted(WRITE_RUNTIME_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        denied(WRITE_RUNTIME_OPERATION, ACCESS_AUDIT_ADDR, role);

        permitted(READ_CONFIG_READ_RUNTIME_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        noAddress(READ_CONFIG_READ_RUNTIME_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        permitted(READ_CONFIG_READ_RUNTIME_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        denied(READ_CONFIG_READ_RUNTIME_OPERATION, ACCESS_AUDIT_ADDR, role);

        permitted(READ_CONFIG_WRITE_RUNTIME_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        noAddress(READ_CONFIG_WRITE_RUNTIME_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        permitted(READ_CONFIG_WRITE_RUNTIME_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        denied(READ_CONFIG_WRITE_RUNTIME_OPERATION, ACCESS_AUDIT_ADDR, role);

        permitted(WRITE_CONFIG_READ_RUNTIME_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        noAddress(WRITE_CONFIG_READ_RUNTIME_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        permitted(WRITE_CONFIG_READ_RUNTIME_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        denied(WRITE_CONFIG_READ_RUNTIME_OPERATION, ACCESS_AUDIT_ADDR, role);

        permitted(WRITE_CONFIG_WRITE_RUNTIME_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        noAddress(WRITE_CONFIG_WRITE_RUNTIME_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        permitted(WRITE_CONFIG_WRITE_RUNTIME_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        denied(WRITE_CONFIG_WRITE_RUNTIME_OPERATION, ACCESS_AUDIT_ADDR, role);

        permitted(READ_RUNTIME_READ_CONFIG_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        noAddress(READ_RUNTIME_READ_CONFIG_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        permitted(READ_RUNTIME_READ_CONFIG_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        denied(READ_RUNTIME_READ_CONFIG_OPERATION, ACCESS_AUDIT_ADDR, role);

        permitted(WRITE_RUNTIME_READ_CONFIG_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        noAddress(WRITE_RUNTIME_READ_CONFIG_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        permitted(WRITE_RUNTIME_READ_CONFIG_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        denied(WRITE_RUNTIME_READ_CONFIG_OPERATION, ACCESS_AUDIT_ADDR, role);

        permitted(READ_RUNTIME_WRITE_CONFIG_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        noAddress(READ_RUNTIME_WRITE_CONFIG_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        permitted(READ_RUNTIME_WRITE_CONFIG_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        denied(READ_RUNTIME_WRITE_CONFIG_OPERATION, ACCESS_AUDIT_ADDR, role);

        permitted(WRITE_RUNTIME_WRITE_CONFIG_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        noAddress(WRITE_RUNTIME_WRITE_CONFIG_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        permitted(WRITE_RUNTIME_WRITE_CONFIG_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        denied(WRITE_RUNTIME_WRITE_CONFIG_OPERATION, ACCESS_AUDIT_ADDR, role);
    }

    @Test
    public void testDeployer() {
        StandardRole role = StandardRole.DEPLOYER;

        permitted(READ_CONFIG_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        noAddress(READ_CONFIG_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        permitted(READ_CONFIG_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        denied(READ_CONFIG_OPERATION, ACCESS_AUDIT_ADDR, role);

        permitted(READ_RUNTIME_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        noAddress(READ_RUNTIME_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        permitted(READ_RUNTIME_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        denied(READ_RUNTIME_OPERATION, ACCESS_AUDIT_ADDR, role);

        denied(WRITE_CONFIG_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        noAddress(WRITE_CONFIG_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        permitted(WRITE_CONFIG_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        denied(WRITE_CONFIG_OPERATION, ACCESS_AUDIT_ADDR, role);

        denied(WRITE_RUNTIME_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        noAddress(WRITE_RUNTIME_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        permitted(WRITE_RUNTIME_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        denied(WRITE_RUNTIME_OPERATION, ACCESS_AUDIT_ADDR, role);

        permitted(READ_CONFIG_READ_RUNTIME_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        noAddress(READ_CONFIG_READ_RUNTIME_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        permitted(READ_CONFIG_READ_RUNTIME_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        denied(READ_CONFIG_READ_RUNTIME_OPERATION, ACCESS_AUDIT_ADDR, role);

        denied(READ_CONFIG_WRITE_RUNTIME_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        noAddress(READ_CONFIG_WRITE_RUNTIME_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        permitted(READ_CONFIG_WRITE_RUNTIME_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        denied(READ_CONFIG_WRITE_RUNTIME_OPERATION, ACCESS_AUDIT_ADDR, role);

        denied(WRITE_CONFIG_READ_RUNTIME_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        noAddress(WRITE_CONFIG_READ_RUNTIME_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        permitted(WRITE_CONFIG_READ_RUNTIME_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        denied(WRITE_CONFIG_READ_RUNTIME_OPERATION, ACCESS_AUDIT_ADDR, role);

        denied(WRITE_CONFIG_WRITE_RUNTIME_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        noAddress(WRITE_CONFIG_WRITE_RUNTIME_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        permitted(WRITE_CONFIG_WRITE_RUNTIME_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        denied(WRITE_CONFIG_WRITE_RUNTIME_OPERATION, ACCESS_AUDIT_ADDR, role);

        permitted(READ_RUNTIME_READ_CONFIG_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        noAddress(READ_RUNTIME_READ_CONFIG_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        permitted(READ_RUNTIME_READ_CONFIG_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        denied(READ_RUNTIME_READ_CONFIG_OPERATION, ACCESS_AUDIT_ADDR, role);

        denied(WRITE_RUNTIME_READ_CONFIG_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        noAddress(WRITE_RUNTIME_READ_CONFIG_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        permitted(WRITE_RUNTIME_READ_CONFIG_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        denied(WRITE_RUNTIME_READ_CONFIG_OPERATION, ACCESS_AUDIT_ADDR, role);

        denied(READ_RUNTIME_WRITE_CONFIG_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        noAddress(READ_RUNTIME_WRITE_CONFIG_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        permitted(READ_RUNTIME_WRITE_CONFIG_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        denied(READ_RUNTIME_WRITE_CONFIG_OPERATION, ACCESS_AUDIT_ADDR, role);

        denied(WRITE_RUNTIME_WRITE_CONFIG_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        noAddress(WRITE_RUNTIME_WRITE_CONFIG_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        permitted(WRITE_RUNTIME_WRITE_CONFIG_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        denied(WRITE_RUNTIME_WRITE_CONFIG_OPERATION, ACCESS_AUDIT_ADDR, role);
    }

    @Test
    public void testAdministrator() {
        StandardRole role = StandardRole.ADMINISTRATOR;

        permitted(READ_CONFIG_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        permitted(READ_CONFIG_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        permitted(READ_CONFIG_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        denied(READ_CONFIG_OPERATION, ACCESS_AUDIT_ADDR, role);

        permitted(READ_RUNTIME_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        permitted(READ_RUNTIME_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        permitted(READ_RUNTIME_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        denied(READ_RUNTIME_OPERATION, ACCESS_AUDIT_ADDR, role);

        permitted(WRITE_CONFIG_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        permitted(WRITE_CONFIG_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        permitted(WRITE_CONFIG_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        denied(WRITE_CONFIG_OPERATION, ACCESS_AUDIT_ADDR, role);

        permitted(WRITE_RUNTIME_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        permitted(WRITE_RUNTIME_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        permitted(WRITE_RUNTIME_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        denied(WRITE_RUNTIME_OPERATION, ACCESS_AUDIT_ADDR, role);

        permitted(READ_CONFIG_READ_RUNTIME_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        permitted(READ_CONFIG_READ_RUNTIME_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        permitted(READ_CONFIG_READ_RUNTIME_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        denied(READ_CONFIG_READ_RUNTIME_OPERATION, ACCESS_AUDIT_ADDR, role);

        permitted(READ_CONFIG_WRITE_RUNTIME_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        permitted(READ_CONFIG_WRITE_RUNTIME_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        permitted(READ_CONFIG_WRITE_RUNTIME_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        denied(READ_CONFIG_WRITE_RUNTIME_OPERATION, ACCESS_AUDIT_ADDR, role);

        permitted(WRITE_CONFIG_READ_RUNTIME_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        permitted(WRITE_CONFIG_READ_RUNTIME_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        permitted(WRITE_CONFIG_READ_RUNTIME_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        denied(WRITE_CONFIG_READ_RUNTIME_OPERATION, ACCESS_AUDIT_ADDR, role);

        permitted(WRITE_CONFIG_WRITE_RUNTIME_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        permitted(WRITE_CONFIG_WRITE_RUNTIME_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        permitted(WRITE_CONFIG_WRITE_RUNTIME_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        denied(WRITE_CONFIG_WRITE_RUNTIME_OPERATION, ACCESS_AUDIT_ADDR, role);

        permitted(READ_RUNTIME_READ_CONFIG_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        permitted(READ_RUNTIME_READ_CONFIG_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        permitted(READ_RUNTIME_READ_CONFIG_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        denied(READ_RUNTIME_READ_CONFIG_OPERATION, ACCESS_AUDIT_ADDR, role);

        permitted(WRITE_RUNTIME_READ_CONFIG_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        permitted(WRITE_RUNTIME_READ_CONFIG_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        permitted(WRITE_RUNTIME_READ_CONFIG_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        denied(WRITE_RUNTIME_READ_CONFIG_OPERATION, ACCESS_AUDIT_ADDR, role);

        permitted(READ_RUNTIME_WRITE_CONFIG_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        permitted(READ_RUNTIME_WRITE_CONFIG_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        permitted(READ_RUNTIME_WRITE_CONFIG_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        denied(READ_RUNTIME_WRITE_CONFIG_OPERATION, ACCESS_AUDIT_ADDR, role);

        permitted(WRITE_RUNTIME_WRITE_CONFIG_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        permitted(WRITE_RUNTIME_WRITE_CONFIG_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        permitted(WRITE_RUNTIME_WRITE_CONFIG_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        denied(WRITE_RUNTIME_WRITE_CONFIG_OPERATION, ACCESS_AUDIT_ADDR, role);
    }

    @Test
    public void testAuditor() {
        StandardRole role = StandardRole.AUDITOR;

        permitted(READ_CONFIG_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        permitted(READ_CONFIG_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        permitted(READ_CONFIG_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        permitted(READ_CONFIG_OPERATION, ACCESS_AUDIT_ADDR, role);

        permitted(READ_RUNTIME_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        permitted(READ_RUNTIME_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        permitted(READ_RUNTIME_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        permitted(READ_RUNTIME_OPERATION, ACCESS_AUDIT_ADDR, role);

        denied(WRITE_CONFIG_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        denied(WRITE_CONFIG_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        denied(WRITE_CONFIG_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        permitted(WRITE_CONFIG_OPERATION, ACCESS_AUDIT_ADDR, role);

        denied(WRITE_RUNTIME_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        denied(WRITE_RUNTIME_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        denied(WRITE_RUNTIME_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        permitted(WRITE_RUNTIME_OPERATION, ACCESS_AUDIT_ADDR, role);

        permitted(READ_CONFIG_READ_RUNTIME_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        permitted(READ_CONFIG_READ_RUNTIME_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        permitted(READ_CONFIG_READ_RUNTIME_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        permitted(READ_CONFIG_READ_RUNTIME_OPERATION, ACCESS_AUDIT_ADDR, role);

        denied(READ_CONFIG_WRITE_RUNTIME_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        denied(READ_CONFIG_WRITE_RUNTIME_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        denied(READ_CONFIG_WRITE_RUNTIME_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        permitted(READ_CONFIG_WRITE_RUNTIME_OPERATION, ACCESS_AUDIT_ADDR, role);

        denied(WRITE_CONFIG_READ_RUNTIME_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        denied(WRITE_CONFIG_READ_RUNTIME_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        denied(WRITE_CONFIG_READ_RUNTIME_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        permitted(WRITE_CONFIG_READ_RUNTIME_OPERATION, ACCESS_AUDIT_ADDR, role);

        denied(WRITE_CONFIG_WRITE_RUNTIME_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        denied(WRITE_CONFIG_WRITE_RUNTIME_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        denied(WRITE_CONFIG_WRITE_RUNTIME_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        permitted(WRITE_CONFIG_WRITE_RUNTIME_OPERATION, ACCESS_AUDIT_ADDR, role);

        permitted(READ_RUNTIME_READ_CONFIG_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        permitted(READ_RUNTIME_READ_CONFIG_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        permitted(READ_RUNTIME_READ_CONFIG_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        permitted(READ_RUNTIME_READ_CONFIG_OPERATION, ACCESS_AUDIT_ADDR, role);

        denied(WRITE_RUNTIME_READ_CONFIG_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        denied(WRITE_RUNTIME_READ_CONFIG_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        denied(WRITE_RUNTIME_READ_CONFIG_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        permitted(WRITE_RUNTIME_READ_CONFIG_OPERATION, ACCESS_AUDIT_ADDR, role);

        denied(READ_RUNTIME_WRITE_CONFIG_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        denied(READ_RUNTIME_WRITE_CONFIG_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        denied(READ_RUNTIME_WRITE_CONFIG_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        permitted(READ_RUNTIME_WRITE_CONFIG_OPERATION, ACCESS_AUDIT_ADDR, role);

        denied(WRITE_RUNTIME_WRITE_CONFIG_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        denied(WRITE_RUNTIME_WRITE_CONFIG_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        denied(WRITE_RUNTIME_WRITE_CONFIG_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        permitted(WRITE_RUNTIME_WRITE_CONFIG_OPERATION, ACCESS_AUDIT_ADDR, role);
    }

    @Test
    public void testSuperUser() {
        StandardRole role = StandardRole.SUPERUSER;

        permitted(READ_CONFIG_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        permitted(READ_CONFIG_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        permitted(READ_CONFIG_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        permitted(READ_CONFIG_OPERATION, ACCESS_AUDIT_ADDR, role);

        permitted(READ_RUNTIME_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        permitted(READ_RUNTIME_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        permitted(READ_RUNTIME_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        permitted(READ_RUNTIME_OPERATION, ACCESS_AUDIT_ADDR, role);

        permitted(WRITE_CONFIG_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        permitted(WRITE_CONFIG_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        permitted(WRITE_CONFIG_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        permitted(WRITE_CONFIG_OPERATION, ACCESS_AUDIT_ADDR, role);

        permitted(WRITE_RUNTIME_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        permitted(WRITE_RUNTIME_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        permitted(WRITE_RUNTIME_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        permitted(WRITE_RUNTIME_OPERATION, ACCESS_AUDIT_ADDR, role);

        permitted(READ_CONFIG_READ_RUNTIME_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        permitted(READ_CONFIG_READ_RUNTIME_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        permitted(READ_CONFIG_READ_RUNTIME_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        permitted(READ_CONFIG_READ_RUNTIME_OPERATION, ACCESS_AUDIT_ADDR, role);

        permitted(READ_CONFIG_WRITE_RUNTIME_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        permitted(READ_CONFIG_WRITE_RUNTIME_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        permitted(READ_CONFIG_WRITE_RUNTIME_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        permitted(READ_CONFIG_WRITE_RUNTIME_OPERATION, ACCESS_AUDIT_ADDR, role);

        permitted(WRITE_CONFIG_READ_RUNTIME_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        permitted(WRITE_CONFIG_READ_RUNTIME_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        permitted(WRITE_CONFIG_READ_RUNTIME_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        permitted(WRITE_CONFIG_READ_RUNTIME_OPERATION, ACCESS_AUDIT_ADDR, role);

        permitted(WRITE_CONFIG_WRITE_RUNTIME_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        permitted(WRITE_CONFIG_WRITE_RUNTIME_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        permitted(WRITE_CONFIG_WRITE_RUNTIME_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        permitted(WRITE_CONFIG_WRITE_RUNTIME_OPERATION, ACCESS_AUDIT_ADDR, role);

        permitted(READ_RUNTIME_READ_CONFIG_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        permitted(READ_RUNTIME_READ_CONFIG_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        permitted(READ_RUNTIME_READ_CONFIG_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        permitted(READ_RUNTIME_READ_CONFIG_OPERATION, ACCESS_AUDIT_ADDR, role);

        permitted(WRITE_RUNTIME_READ_CONFIG_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        permitted(WRITE_RUNTIME_READ_CONFIG_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        permitted(WRITE_RUNTIME_READ_CONFIG_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        permitted(WRITE_RUNTIME_READ_CONFIG_OPERATION, ACCESS_AUDIT_ADDR, role);

        permitted(READ_RUNTIME_WRITE_CONFIG_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        permitted(READ_RUNTIME_WRITE_CONFIG_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        permitted(READ_RUNTIME_WRITE_CONFIG_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        permitted(READ_RUNTIME_WRITE_CONFIG_OPERATION, ACCESS_AUDIT_ADDR, role);

        permitted(WRITE_RUNTIME_WRITE_CONFIG_OPERATION, pathAddress(UNCONSTRAINED_RESOURCE, FOO), role);
        permitted(WRITE_RUNTIME_WRITE_CONFIG_OPERATION, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO), role);
        permitted(WRITE_RUNTIME_WRITE_CONFIG_OPERATION, pathAddress(APPLICATION_CONSTRAINED_RESOURCE, FOO), role);
        permitted(WRITE_RUNTIME_WRITE_CONFIG_OPERATION, ACCESS_AUDIT_ADDR, role);
    }
    // auditor, -- TODO AuditConstraint

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

            resourceRegistration.registerOperationHandler(TestOperationStepHandler.READ_CONFIG_DEFINITION,
                    new TestOperationStepHandler(Action.ActionEffect.READ_CONFIG));
            resourceRegistration.registerOperationHandler(TestOperationStepHandler.READ_RUNTIME_DEFINITION,
                    new TestOperationStepHandler(Action.ActionEffect.READ_RUNTIME));
            resourceRegistration.registerOperationHandler(TestOperationStepHandler.WRITE_CONFIG_DEFINITION,
                    new TestOperationStepHandler(Action.ActionEffect.WRITE_CONFIG));
            resourceRegistration.registerOperationHandler(TestOperationStepHandler.WRITE_RUNTIME_DEFINITION,
                    new TestOperationStepHandler(Action.ActionEffect.WRITE_RUNTIME));

            resourceRegistration.registerOperationHandler(TestOperationStepHandler.READ_CONFIG_READ_RUNTIME_DEFINITION,
                    new TestOperationStepHandler(Action.ActionEffect.READ_CONFIG, Action.ActionEffect.READ_RUNTIME));
            resourceRegistration.registerOperationHandler(TestOperationStepHandler.READ_CONFIG_WRITE_RUNTIME_DEFINITION,
                    new TestOperationStepHandler(Action.ActionEffect.READ_CONFIG, Action.ActionEffect.WRITE_RUNTIME));
            resourceRegistration.registerOperationHandler(TestOperationStepHandler.WRITE_CONFIG_READ_RUNTIME_DEFINITION,
                    new TestOperationStepHandler(Action.ActionEffect.WRITE_CONFIG, Action.ActionEffect.READ_RUNTIME));
            resourceRegistration.registerOperationHandler(TestOperationStepHandler.WRITE_CONFIG_WRITE_RUNTIME_DEFINITION,
                    new TestOperationStepHandler(Action.ActionEffect.WRITE_CONFIG, Action.ActionEffect.WRITE_RUNTIME));

            resourceRegistration.registerOperationHandler(TestOperationStepHandler.READ_RUNTIME_READ_CONFIG_DEFINITION,
                    new TestOperationStepHandler(Action.ActionEffect.READ_RUNTIME, Action.ActionEffect.READ_CONFIG));
            resourceRegistration.registerOperationHandler(TestOperationStepHandler.WRITE_RUNTIME_READ_CONFIG_DEFINITION,
                    new TestOperationStepHandler(Action.ActionEffect.WRITE_RUNTIME, Action.ActionEffect.READ_CONFIG));
            resourceRegistration.registerOperationHandler(TestOperationStepHandler.READ_RUNTIME_WRITE_CONFIG_DEFINITION,
                    new TestOperationStepHandler(Action.ActionEffect.READ_RUNTIME, Action.ActionEffect.WRITE_CONFIG));
            resourceRegistration.registerOperationHandler(TestOperationStepHandler.WRITE_RUNTIME_WRITE_CONFIG_DEFINITION,
                    new TestOperationStepHandler(Action.ActionEffect.WRITE_RUNTIME, Action.ActionEffect.WRITE_CONFIG));
        }

        @Override
        public List<AccessConstraintDefinition> getAccessConstraints() {
            return constraintDefinitions;
        }
    }

    private static final class TestOperationStepHandler implements OperationStepHandler {
        private static SimpleOperationDefinition definition(String name) {
            return new SimpleOperationDefinitionBuilder(name, new NonResolvingResourceDescriptionResolver())
                    .setReplyType(ModelType.INT)
                    .build();
        }

        private static final SimpleOperationDefinition READ_CONFIG_DEFINITION = definition(READ_CONFIG_OPERATION);
        private static final SimpleOperationDefinition READ_RUNTIME_DEFINITION = definition(READ_RUNTIME_OPERATION);
        private static final SimpleOperationDefinition WRITE_CONFIG_DEFINITION = definition(WRITE_CONFIG_OPERATION);
        private static final SimpleOperationDefinition WRITE_RUNTIME_DEFINITION = definition(WRITE_RUNTIME_OPERATION);

        private static final SimpleOperationDefinition READ_CONFIG_READ_RUNTIME_DEFINITION = definition(READ_CONFIG_READ_RUNTIME_OPERATION);
        private static final SimpleOperationDefinition READ_CONFIG_WRITE_RUNTIME_DEFINITION = definition(READ_CONFIG_WRITE_RUNTIME_OPERATION);
        private static final SimpleOperationDefinition WRITE_CONFIG_READ_RUNTIME_DEFINITION = definition(WRITE_CONFIG_READ_RUNTIME_OPERATION);
        private static final SimpleOperationDefinition WRITE_CONFIG_WRITE_RUNTIME_DEFINITION = definition(WRITE_CONFIG_WRITE_RUNTIME_OPERATION);

        private static final SimpleOperationDefinition READ_RUNTIME_READ_CONFIG_DEFINITION = definition(READ_RUNTIME_READ_CONFIG_OPERATION);
        private static final SimpleOperationDefinition WRITE_RUNTIME_READ_CONFIG_DEFINITION = definition(WRITE_RUNTIME_READ_CONFIG_OPERATION);
        private static final SimpleOperationDefinition READ_RUNTIME_WRITE_CONFIG_DEFINITION = definition(READ_RUNTIME_WRITE_CONFIG_OPERATION);
        private static final SimpleOperationDefinition WRITE_RUNTIME_WRITE_CONFIG_DEFINITION = definition(WRITE_RUNTIME_WRITE_CONFIG_OPERATION);

        private List<Action.ActionEffect> actionEffects;

        private TestOperationStepHandler(Action.ActionEffect... actionEffects) {
            this.actionEffects = Collections.unmodifiableList(Arrays.asList(actionEffects));
        }

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            for (Action.ActionEffect actionEffect : actionEffects) {
                if (actionEffect == Action.ActionEffect.READ_CONFIG) {
                    context.readResource(EMPTY_ADDRESS); // causes read-config auth
                }
                if (actionEffect == Action.ActionEffect.WRITE_CONFIG) {
                    context.getResourceRegistrationForUpdate(); // causes read-config+write-config auth
                }
                if (actionEffect == Action.ActionEffect.READ_RUNTIME) {
                    context.getServiceRegistry(false); // causes read-runtime auth
                }
                if (actionEffect == Action.ActionEffect.WRITE_RUNTIME) {
                    context.getServiceRegistry(true); // causes read-runtime+write-runtime auth
                }
            }

            context.getResult().set(new Random().nextInt());
            context.stepCompleted();
        }
    }
}
