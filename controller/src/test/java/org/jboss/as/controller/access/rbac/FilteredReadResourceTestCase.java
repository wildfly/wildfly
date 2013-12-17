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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ACCESS_CONTROL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FILTERED_CHILDREN_TYPES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESPONSE_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UNREADABLE_CHILDREN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.access.constraint.SensitivityClassification;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.descriptions.NonResolvingResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.global.GlobalOperationHandlers;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests filtering of output from {@code read-resource} requests.
 *
 * @author Ladislav Thon <lthon@redhat.com>
 */
public class FilteredReadResourceTestCase extends AbstractRbacTestBase {
    public static final String UNCONSTRAINED_RESOURCE = "unconstrained-resource";
    public static final String SENSITIVE_CONSTRAINED_RESOURCE = "sensitive-constrained-resource";

    public static final String FOO = "foo";
    public static final String BAR = "bar";

    @Before
    public void setup() {
        ModelNode operation = Util.createOperation(ADD, pathAddress(UNCONSTRAINED_RESOURCE, FOO));
        executeWithRoles(operation, StandardRole.SUPERUSER);
        operation = Util.createOperation(ADD, pathAddress(UNCONSTRAINED_RESOURCE, BAR));
        executeWithRoles(operation, StandardRole.SUPERUSER);

        operation = Util.createOperation(ADD, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, FOO));
        executeWithRoles(operation, StandardRole.SUPERUSER);
        operation = Util.createOperation(ADD, pathAddress(SENSITIVE_CONSTRAINED_RESOURCE, BAR));
        executeWithRoles(operation, StandardRole.SUPERUSER);

        MY_SENSITIVITY.setConfiguredRequiresAccessPermission(true);
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
    public void testMonitorOperator() {
        test(false, StandardRole.MONITOR, StandardRole.OPERATOR);
    }

    @Test
    public void testMonitorAdministrator() {
        test(true, StandardRole.MONITOR, StandardRole.ADMINISTRATOR);
    }

    @Test
    public void testAdministratorAuditor() {
        test(true, StandardRole.ADMINISTRATOR, StandardRole.AUDITOR);
    }

    /** Test for WFLY-2444 */
    @Test
    public void testWildcardFiltering() {
        PathAddress wildcardAddress = PathAddress.pathAddress(PathElement.pathElement(SENSITIVE_CONSTRAINED_RESOURCE));
        ModelNode operation = Util.createOperation(READ_RESOURCE_OPERATION, wildcardAddress);
        ModelNode result = executeWithRole(operation, StandardRole.MONITOR);
        assertEquals(SUCCESS, result.get(OUTCOME).asString());
        assertEquals(ModelType.LIST, result.get(RESULT).getType());
        assertEquals(0, result.get(RESULT).asInt());
        assertTrue(result.hasDefined(RESPONSE_HEADERS));
        assertTrue(result.get(RESPONSE_HEADERS).hasDefined(ACCESS_CONTROL));
        assertEquals(1, result.get(RESPONSE_HEADERS, ACCESS_CONTROL).asInt());
        ModelNode accessControl = result.get(RESPONSE_HEADERS, ACCESS_CONTROL).get(0);
        assertTrue(accessControl.hasDefined(FILTERED_CHILDREN_TYPES));
        ModelNode filteredTypes = accessControl.get(FILTERED_CHILDREN_TYPES);
        assertEquals(1, filteredTypes.asInt());
        assertEquals(SENSITIVE_CONSTRAINED_RESOURCE, filteredTypes.get(0).asString());

        MY_SENSITIVITY.setConfiguredRequiresAccessPermission(false);
        result = executeWithRole(operation, StandardRole.MONITOR);
        assertEquals(SUCCESS, result.get(OUTCOME).asString());
        assertEquals(ModelType.LIST, result.get(RESULT).getType());
        assertEquals(0, result.get(RESULT).asInt());
        assertTrue(result.hasDefined(RESPONSE_HEADERS));
        assertTrue(result.get(RESPONSE_HEADERS).hasDefined(ACCESS_CONTROL));
        assertEquals(1, result.get(RESPONSE_HEADERS, ACCESS_CONTROL).asInt());
        accessControl = result.get(RESPONSE_HEADERS, ACCESS_CONTROL).get(0);
        assertTrue(accessControl.hasDefined(UNREADABLE_CHILDREN));
        ModelNode unreadable = accessControl.get(UNREADABLE_CHILDREN);
        assertEquals(2, unreadable.asInt());
        Set<String> children = new HashSet<String>(Arrays.asList(FOO, BAR));
        for (Property prop : unreadable.asPropertyList()) {
            assertEquals(SENSITIVE_CONSTRAINED_RESOURCE, prop.getName());
            String name = prop.getValue().asString();
            assertTrue(name, children.remove(name));
        }
        assertEquals(0, children.size());
    }

    private void test(boolean sensitiveResourceVisible, StandardRole... roles) {
        ModelNode operation = Util.createOperation(READ_RESOURCE_OPERATION, EMPTY_ADDRESS);
        operation.get(RECURSIVE).set(true);
        ModelNode result = executeWithRoles(operation, roles);

        assertEquals(SUCCESS, result.get(OUTCOME).asString());
        assertTrue(result.get(RESULT, UNCONSTRAINED_RESOURCE).has(FOO));
        assertTrue(result.get(RESULT, UNCONSTRAINED_RESOURCE).has(BAR));
        assertEquals(sensitiveResourceVisible, result.get(RESULT, SENSITIVE_CONSTRAINED_RESOURCE).has(FOO));
        assertEquals(sensitiveResourceVisible, result.get(RESULT, SENSITIVE_CONSTRAINED_RESOURCE).has(BAR));

        // lthon asks: is this format stable? testing it isn't that important anyway...
        // BES 2013/07/08 Yes, it's stable and needs testing as automated clients will be relying on it
        assertEquals(!sensitiveResourceVisible, result.get(RESPONSE_HEADERS, ACCESS_CONTROL).get(0)
                .get("filtered-children-types").get(0).asString().equals(SENSITIVE_CONSTRAINED_RESOURCE));
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
        public List<AccessConstraintDefinition> getAccessConstraints() {
            return constraintDefinitions;
        }
    }
}
