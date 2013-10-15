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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ACCESS_CONTROL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEFAULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXCEPTIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXECUTE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INHERITED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODEL_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelOnlyWriteAttributeHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.access.Action.ActionEffect;
import org.jboss.as.controller.access.constraint.SensitivityClassification;
import org.jboss.as.controller.access.constraint.VaultExpressionSensitivityConfig;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.access.management.ConstrainedResourceDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.NonResolvingResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.global.GlobalOperationHandlers;
import org.jboss.as.controller.operations.global.ReadResourceDescriptionHandler;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.test.AbstractControllerTestBase;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ReadResourceDescriptionAccessControlTestCase extends AbstractControllerTestBase {

    private static final PathElement ONE = PathElement.pathElement("one");
    private static final PathElement ONE_A = PathElement.pathElement("one", "a");
    private static final PathElement ONE_B = PathElement.pathElement("one", "b");
    private static final PathAddress ONE_ADDR = PathAddress.pathAddress(ONE);
    private static final PathAddress ONE_A_ADDR = PathAddress.pathAddress(ONE_A);

    private static final PathElement TWO = PathElement.pathElement("two");
    private static final PathElement TWO_A = PathElement.pathElement("two", "a");
    private static final PathElement TWO_B = PathElement.pathElement("two", "b");
    private static final PathAddress TWO_ADDR = PathAddress.pathAddress(ONE, TWO);
    private static final PathAddress ONE_A_TWO_A_ADDR = ONE_A_ADDR.append(TWO_A);

    private static final String ATTR_ACCESS_READ_WRITE = "access-read-write";
    private static final String ATTR_READ_WRITE = "read-write";
    private static final String ATTR_WRITE = "write";
    private static final String ATTR_READ = "read";
    private static final String ATTR_NONE = "none";
    private static final String ATTR_VAULT = "attr-vault";

    private static final String OP_CONFIG_RW_ACCESS_READ_WRITE = "config-rw-access-read-write";
    private static final String OP_CONFIG_RW_READ_WRITE = "config-rw-read-write";
    private static final String OP_CONFIG_RW_WRITE = "config-rw-write";
    private static final String OP_CONFIG_RW_READ = "config-rw-read";
    private static final String OP_CONFIG_RW_NONE = "config-rw-none";
    private static final String OP_RUNTIME_RW_ACCESS_READ_WRITE = "runtime-rw-access-read-write";
    private static final String OP_RUNTIME_RW_READ_WRITE = "runtime-rw-read-write";
    private static final String OP_RUNTIME_RW_WRITE = "runtime-rw-write";
    private static final String OP_RUNTIME_RW_READ = "runtime-rw-read";
    private static final String OP_RUNTIME_RW_NONE = "runtime-rw-none";
    private static final String OP_CONFIG_RO_ACCESS_READ_WRITE = "config-ro-access-read-write";
    private static final String OP_CONFIG_RO_READ_WRITE = "config-ro-read-write";
    private static final String OP_CONFIG_RO_WRITE = "config-ro-write";
    private static final String OP_CONFIG_RO_READ = "config-ro-read";
    private static final String OP_CONFIG_RO_NONE = "config-ro-none";
    private static final String OP_RUNTIME_RO_ACCESS_READ_WRITE = "runtime-ro-access-read-write";
    private static final String OP_RUNTIME_RO_READ_WRITE = "runtime-ro-read-write";
    private static final String OP_RUNTIME_RO_WRITE = "runtime-ro-write";
    private static final String OP_RUNTIME_RO_READ = "runtime-ro-read";
    private static final String OP_RUNTIME_RO_NONE = "runtime-ro-none";

    private volatile ManagementResourceRegistration rootRegistration;
    private volatile Resource rootResource;

    // These three are the same for different roles
    @Test
    public void testNonRecursiveReadRootResourceDefinitionNoSensitivityAsMonitor() throws Exception {
        registerOneChildRootResource();
        ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.MONITOR, false);
        op.get(RECURSIVE).set(false);
        ModelNode result = executeForResult(op);
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        checkResourcePermissions(accessControl.defaultControl, true, false);
    }

    @Test
    public void testNonRecursiveReadRootResourceDefinitionNoSensitivityAsMaintainer() throws Exception {
        registerOneChildRootResource();
        ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.MAINTAINER, false);
        op.get(RECURSIVE).set(false);
        ModelNode result = executeForResult(op);
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        checkResourcePermissions(accessControl.defaultControl, true, true);
    }

    @Test
    public void testNonRecursiveReadRootResourceDefinitionNoSensitivityAsAdministrator() throws Exception {
        registerOneChildRootResource();
        ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.ADMINISTRATOR, false);
        op.get(RECURSIVE).set(false);
        ModelNode result = executeForResult(op);
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        checkResourcePermissions(accessControl.defaultControl, true, true);
    }

    // These three are the same for different roles
    @Test
    public void testDirectWildcardReadResourceDefinitionNoSensitivityNoResourceAsMonitor() throws Exception {
        registerOneChildRootResource();
        //Remove the childred to make sure the access-control=default part gets populated anyway
        rootResource.removeChild(ONE_A);
        rootResource.removeChild(ONE_B);
        ModelNode op = createReadResourceDescriptionOperation(ONE_ADDR, StandardRole.MONITOR, false);
        ModelNode result = getWildcardResourceRegistrationResult(executeForResult(op));
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        checkResourcePermissions(accessControl.defaultControl, true, false);
    }

    @Test
    public void testDirectWildcardReadResourceDefinitionNoSensitivityNoResourceAsMaintainer() throws Exception {
        registerOneChildRootResource();
        //Remove the childred to make sure the access-control=default part gets populated anyway
        rootResource.removeChild(ONE_A);
        rootResource.removeChild(ONE_B);
        ModelNode op = createReadResourceDescriptionOperation(ONE_ADDR, StandardRole.MAINTAINER, false);
        ModelNode result = getWildcardResourceRegistrationResult(executeForResult(op));
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        checkResourcePermissions(accessControl.defaultControl, true, true);
    }

    @Test
    public void testDirectWildcardReadResourceDefinitionNoSensitivityNoResourceAsAdministrator() throws Exception {
        registerOneChildRootResource();
        //Remove the childred to make sure the access-control=default part gets populated anyway
        rootResource.removeChild(ONE_A);
        rootResource.removeChild(ONE_B);
        ModelNode op = createReadResourceDescriptionOperation(ONE_ADDR, StandardRole.ADMINISTRATOR, false);
        ModelNode result = getWildcardResourceRegistrationResult(executeForResult(op));
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        checkResourcePermissions(accessControl.defaultControl, true, true);
    }

    // These three are the same for different roles
    @Test
    public void testRecursiveReadResourceDefinitionNoSensitivityAsMonitor() throws Exception {
        registerOneChildRootResource();
        ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.MONITOR, false);
        ModelNode result = executeForResult(op);
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        checkResourcePermissions(accessControl.defaultControl, true, false);
        ModelNode childDesc = getChildDescription(result, ONE);
        accessControl = getResourceAccessControl(childDesc);
        checkResourcePermissions(accessControl.defaultControl, true, false);
    }

    @Test
    public void testRecursiveReadResourceDefinitionNoSensitivityAsMaintainer() throws Exception {
        registerOneChildRootResource();
        ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.MAINTAINER, false);
        ModelNode result = executeForResult(op);
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        checkResourcePermissions(accessControl.defaultControl, true, true);
        ModelNode childDesc = getChildDescription(result, ONE);
        accessControl = getResourceAccessControl(childDesc);
        checkResourcePermissions(accessControl.defaultControl, true, true);
    }
    @Test
    public void testRecursiveReadResourceDefinitionNoSensitivityAsAdministrator() throws Exception {
        registerOneChildRootResource();
        ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.ADMINISTRATOR, false);
        ModelNode result = executeForResult(op);
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        checkResourcePermissions(accessControl.defaultControl, true, true);
        ModelNode childDesc = getChildDescription(result, ONE);
        accessControl = getResourceAccessControl(childDesc);
        checkResourcePermissions(accessControl.defaultControl, true, true);
    }

    // These three are the same for different roles
    @Test
    public void testDirectWildcardReadResourceDefinitionNoSensitivityAsMonitor() throws Exception {
        registerOneChildRootResource();
        ModelNode op = createReadResourceDescriptionOperation(ONE_ADDR, StandardRole.MONITOR, false);
        ModelNode result = getWildcardResourceRegistrationResult(executeForResult(op));
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        checkResourcePermissions(accessControl.defaultControl, true, false);
    }

    @Test
    public void testDirectWildcardReadResourceDefinitionNoSensitivityAsMaintainer() throws Exception {
        registerOneChildRootResource();
        ModelNode op = createReadResourceDescriptionOperation(ONE_ADDR, StandardRole.MAINTAINER, false);
        ModelNode result = getWildcardResourceRegistrationResult(executeForResult(op));
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        checkResourcePermissions(accessControl.defaultControl, true, true);
    }

    @Test
    public void testDirectWildcardReadResourceDefinitionNoSensitivityAsAdministrator() throws Exception {
        registerOneChildRootResource();
        ModelNode op = createReadResourceDescriptionOperation(ONE_ADDR, StandardRole.ADMINISTRATOR, false);
        ModelNode result = getWildcardResourceRegistrationResult(executeForResult(op));
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        checkResourcePermissions(accessControl.defaultControl, true, true);
    }

    // These three are the same for different roles - similar to the testDirectWildcardReadResourceDefinitionNoSensitivityXXX() ones but for a fixed resource

    @Test
    public void testDirectReadResourceDefinitionNoSensitivityAsMonitor() throws Exception {
        registerOneChildRootResource();
        ModelNode op = createReadResourceDescriptionOperation(ONE_A_ADDR, StandardRole.MONITOR, false);
        ModelNode result = executeForResult(op);
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        checkResourcePermissions(accessControl.defaultControl, true, false);
    }

    @Test
    public void testDirectReadResourceDefinitionNoSensitivityAsMaintainer() throws Exception {
        registerOneChildRootResource();
        ModelNode op = createReadResourceDescriptionOperation(ONE_A_ADDR, StandardRole.MAINTAINER, false);
        ModelNode result = executeForResult(op);
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        checkResourcePermissions(accessControl.defaultControl, true, true);
    }

    @Test
    public void testDirectReadResourceDefinitionNoSensitivityAsAdministrator() throws Exception {
        registerOneChildRootResource();
        ModelNode op = createReadResourceDescriptionOperation(ONE_A_ADDR, StandardRole.ADMINISTRATOR, false);
        ModelNode result = executeForResult(op);
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        checkResourcePermissions(accessControl.defaultControl, true, true);
    }

    // These three are the same for different roles

    @Test
    public void testRecursiveReadResourceDefinitionAccessReadWriteSensitivityAsMonitor() throws Exception {
        registerOneChildRootResource(createSensitivityConstraint("testRecursiveReadResourceDefinitionAccessReadWriteSensitivityAsMonitor", true, true, true));
        ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.MONITOR, false);
        ModelNode result = executeForResult(op);
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        checkResourcePermissions(accessControl.defaultControl, true, false);
        ModelNode childDesc = getChildDescription(result, ONE);
        assertNonAccessibleDefaultAccessControl(childDesc); //Since access is restricted, the maintainer role cannot access the resources
    }

    @Test
    public void testRecursiveReadResourceDefinitionAccessReadWriteSensitivityAsMaintainer() throws Exception {
        registerOneChildRootResource(createSensitivityConstraint("testRecursiveReadResourceDefinitionAccessReadWriteSensitivityAsMaintainer", true, true, true));
        ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.MAINTAINER, false);
        ModelNode result = executeForResult(op);
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        checkResourcePermissions(accessControl.defaultControl, true, true);
        ModelNode childDesc = getChildDescription(result, ONE);
        assertNonAccessibleDefaultAccessControl(childDesc); //Since access is restricted, the maintainer role cannot access the resources
    }

    @Test
    public void testRecursiveReadResourceDefinitionAccessReadWriteSensitivityAsAdministrator() throws Exception {
        registerOneChildRootResource(createSensitivityConstraint("testRecursiveReadResourceDefinitionAccessReadWriteSensitivityAsAdministrator", true, true, true));
        ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.ADMINISTRATOR, false);
        ModelNode result = executeForResult(op);
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        checkResourcePermissions(accessControl.defaultControl, true, true);
        ModelNode childDesc = getChildDescription(result, ONE);
        accessControl = getResourceAccessControl(childDesc);
        checkResourcePermissions(accessControl.defaultControl, true, true);
    }

    // These three are the same for different roles

    @Test
    public void testDirectWildcardReadResourceDefinitionAccessReadWriteSensitivityAsMonitor() throws Exception {
        registerOneChildRootResource(createSensitivityConstraint("testDirectWildcardReadResourceDefinitionAccessReadWriteSensitivityAsMonitor", true, true, true));
        ModelNode op = createReadResourceDescriptionOperation(ONE_ADDR, StandardRole.MONITOR, false);
        ModelNode result = getWildcardResourceRegistrationResult(executeForResult(op));
        assertNonAccessibleDefaultAccessControl(result);//Since access is restricted, the monitor role should not be able to access the resource
    }

    @Test
    public void testDirectWildcardReadResourceDefinitionAccessReadWriteSensitivityAsMaintainer() throws Exception {
        registerOneChildRootResource(createSensitivityConstraint("testDirectWildcardReadResourceDefinitionAccessReadWriteSensitivityAsMaintainer", true, true, true));
        ModelNode op = createReadResourceDescriptionOperation(ONE_ADDR, StandardRole.MAINTAINER, false);
        ModelNode result = getWildcardResourceRegistrationResult(executeForResult(op));
        assertNonAccessibleDefaultAccessControl(result);//Since access is restricted, the maintainer role should not be able to access the resource
    }

    @Test
    public void testDirectWildcardReadResourceDefinitionAccessReadWriteSensitivityAsAdministrator() throws Exception {
        registerOneChildRootResource(createSensitivityConstraint("testDirectWildcardReadResourceDefinitionAccessReadWriteSensitivityAsAdministrator", true, true, true));
        ModelNode op = createReadResourceDescriptionOperation(ONE_ADDR, StandardRole.ADMINISTRATOR, false);
        ModelNode result = getWildcardResourceRegistrationResult(executeForResult(op));
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        checkResourcePermissions(accessControl.defaultControl, true, true);
    }

    // These three are the same for different roles - similar to the testDirectWildcardReadResourceDefinitionAccessReadWriteSensitivityAsXXXX() ones but for a fixed resource

    @Test
    public void testDirectReadResourceDefinitionAccessReadWriteSensitivityAsMonitor() throws Exception {
        registerOneChildRootResource(createSensitivityConstraint("testDirectReadResourceDefinitionAccessReadWriteSensitivityAsMonitor", true, true, true));
        ModelNode op = createReadResourceDescriptionOperation(ONE_A_ADDR, StandardRole.MONITOR, false);
        ModelNode result = executeForResult(op);
        assertNonAccessibleDefaultAccessControl(result);//Since access is restricted, the monitor role should not be able to access the resource
    }

    @Test
    public void testDirectReadResourceDefinitionAccessReadWriteSensitivityAsMaintainer() throws Exception {
        registerOneChildRootResource(createSensitivityConstraint("testDirectReadResourceDefinitionAccessReadWriteSensitivityAsMaintainer", true, true, true));
        ModelNode op = createReadResourceDescriptionOperation(ONE_A_ADDR, StandardRole.MAINTAINER, false);
        ModelNode result = executeForResult(op);
        assertNonAccessibleDefaultAccessControl(result);//Since access is restricted, the maintainer role should not be able to access the resource
    }

    @Test
    public void testDirectReadResourceDefinitionAccessReadWriteSensitivityAsAdministrator() throws Exception {
        registerOneChildRootResource(createSensitivityConstraint("testDirectReadResourceDefinitionAccessReadWriteSensitivityAsAdministrator", true, true, true));
        ModelNode op = createReadResourceDescriptionOperation(ONE_A_ADDR, StandardRole.ADMINISTRATOR, false);
        ModelNode result = executeForResult(op);
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        checkResourcePermissions(accessControl.defaultControl, true, true);
    }

    // These three are the same for different roles

    @Test
    public void testRecursiveReadResourceDefinitionReadWriteSensitivityAsMonitor() throws Exception {
        registerOneChildRootResource(createSensitivityConstraint("testRecursiveReadResourceDefinitionReadWriteSensitivityAsMonitor", false, true, true));
        ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.MONITOR, false);
        ModelNode result = executeForResult(op);
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        checkResourcePermissions(accessControl.defaultControl, true, false);
        ModelNode childDesc = getChildDescription(result, ONE);
        accessControl = getResourceAccessControl(childDesc);
        //Reads are sensitive so we cannot read them as the default says we can
        checkResourcePermissions(accessControl.defaultControl, false, false);
    }

    @Test
    public void testRecursiveReadResourceDefinitionReadWriteSensitivityAsMaintainer() throws Exception {
        registerOneChildRootResource(createSensitivityConstraint("testRecursiveReadResourceDefinitionReadWriteSensitivityAsMaintainer", false, true, true));
        ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.MAINTAINER, false);
        ModelNode result = executeForResult(op);
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        checkResourcePermissions(accessControl.defaultControl, true, true);
        ModelNode childDesc = getChildDescription(result, ONE);
        accessControl = getResourceAccessControl(childDesc);
        //Reads and writes are sensitive so we cannot read or write them as the default says we can
        checkResourcePermissions(accessControl.defaultControl, false, false);
    }

    @Test
    public void testRecursiveReadResourceDefinitionReadWriteSensitivityAsAdministrator() throws Exception {
        registerOneChildRootResource(createSensitivityConstraint("testRecursiveReadResourceDefinitionReadWriteSensitivityAsAdministrator", false, true, true));
        ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.ADMINISTRATOR, false);
        ModelNode result = executeForResult(op);
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        checkResourcePermissions(accessControl.defaultControl, true, true);
        ModelNode childDesc = getChildDescription(result, ONE);
        accessControl = getResourceAccessControl(childDesc);
        checkResourcePermissions(accessControl.defaultControl, true, true);
    }

    // These three are the same for different roles

    @Test
    public void testDirectWildcardReadResourceDefinitionReadWriteSensitivityAsMonitor() throws Exception {
        registerOneChildRootResource(createSensitivityConstraint("testDirectWildcardReadResourceDefinitionReadWriteSensitivityAsMonitor", false, true, true));
        ModelNode op = createReadResourceDescriptionOperation(ONE_ADDR, StandardRole.MONITOR, false);
        ModelNode result = getWildcardResourceRegistrationResult(executeForResult(op));
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        //Reads are sensitive so we cannot read them as the default says we can
        checkResourcePermissions(accessControl.defaultControl, false, false);
    }

    @Test
    public void testDirectWildcardReadResourceDefinitionReadWriteSensitivityAsMaintainer() throws Exception {
        registerOneChildRootResource(createSensitivityConstraint("testDirectWildcardReadResourceDefinitionReadWriteSensitivityAsMaintainer", false, true, true));
        ModelNode op = createReadResourceDescriptionOperation(ONE_ADDR, StandardRole.MAINTAINER, false);
        ModelNode result = getWildcardResourceRegistrationResult(executeForResult(op));
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        //Reads and writes are sensitive so we cannot read or write them as the default says we can
        checkResourcePermissions(accessControl.defaultControl, false, false);
    }

    @Test
    public void testDirectWildcardReadResourceDefinitionReadWriteSensitivityAsAdministrator() throws Exception {
        registerOneChildRootResource(createSensitivityConstraint("testDirectWildcardReadResourceDefinitionReadWriteSensitivityAsAdministrator", false, true, true));
        ModelNode op = createReadResourceDescriptionOperation(ONE_ADDR, StandardRole.ADMINISTRATOR, false);
        ModelNode result = getWildcardResourceRegistrationResult(executeForResult(op));
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        checkResourcePermissions(accessControl.defaultControl, true, true);
    }

    // These three are the same for different roles  - similar to the testDirectWildcardReadResourceDefinitionReadWriteSensitivityAsXXX() ones but for a fixed resource
    @Test
    public void testDirectReadResourceDefinitionReadWriteSensitivityAsMonitor() throws Exception {
        registerOneChildRootResource(createSensitivityConstraint("testDirectReadResourceDefinitionReadWriteSensitivityAsMonitor", false, true, true));
        ModelNode op = createReadResourceDescriptionOperation(ONE_A_ADDR, StandardRole.MONITOR, false);
        ModelNode result = executeForResult(op);
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        //Reads are sensitive so we cannot read them as the default says we can
        checkResourcePermissions(accessControl.defaultControl, false, false);
    }

    @Test
    public void testDirectReadResourceDefinitionReadWriteSensitivityAsMaintainer() throws Exception {
        registerOneChildRootResource(createSensitivityConstraint("testDirectReadResourceDefinitionReadWriteSensitivityAsMaintainer", false, true, true));
        ModelNode op = createReadResourceDescriptionOperation(ONE_A_ADDR, StandardRole.MAINTAINER, false);
        ModelNode result = executeForResult(op);
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        //Reads and writes are sensitive so we cannot read or write them as the default says we can
        checkResourcePermissions(accessControl.defaultControl, false, false);
    }

    @Test
    public void testDirectReadResourceDefinitionReadWriteSensitivityAsAdministrator() throws Exception {
        registerOneChildRootResource(createSensitivityConstraint("testDirectReadResourceDefinitionReadWriteSensitivityAsAdministrator", false, true, true));
        ModelNode op = createReadResourceDescriptionOperation(ONE_A_ADDR, StandardRole.ADMINISTRATOR, false);
        ModelNode result = executeForResult(op);
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        checkResourcePermissions(accessControl.defaultControl, true, true);
    }

    // These three are the same for different roles

    @Test
    public void testRecursiveReadResourceDefinitionWriteSensitivityAsMonitor() throws Exception {
        registerOneChildRootResource(createSensitivityConstraint("testRecursiveReadResourceDefinitionWriteSensitivityAsMonitor", false, false, true));
        ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.MONITOR, false);
        ModelNode result = executeForResult(op);
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        checkResourcePermissions(accessControl.defaultControl, true, false);
        ModelNode childDesc = getChildDescription(result, ONE);
        accessControl = getResourceAccessControl(childDesc);
        checkResourcePermissions(accessControl.defaultControl, true, false);

    }

    @Test
    public void testRecursiveReadResourceDefinitionWriteSensitivityAsMaintainer() throws Exception {
        registerOneChildRootResource(createSensitivityConstraint("testRecursiveReadResourceDefinitionWriteSensitivityAsMaintainer", false, false, true));
        ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.MAINTAINER, false);
        ModelNode result = executeForResult(op);
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        checkResourcePermissions(accessControl.defaultControl, true, true);
        ModelNode childDesc = getChildDescription(result, ONE);
        accessControl = getResourceAccessControl(childDesc);
        //Writes are sensitive so we cannot write them as the default says we can
        checkResourcePermissions(accessControl.defaultControl, true, false);

    }

    @Test
    public void testRecursiveReadResourceDefinitionWriteSensitivityAsAdministrator() throws Exception {
        registerOneChildRootResource(createSensitivityConstraint("testRecursiveReadResourceDefinitionWriteSensitivityAsAdministrator", false, false, true));
        ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.ADMINISTRATOR, false);
        ModelNode result = executeForResult(op);
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        checkResourcePermissions(accessControl.defaultControl, true, true);
        ModelNode childDesc = getChildDescription(result, ONE);
        accessControl = getResourceAccessControl(childDesc);
        checkResourcePermissions(accessControl.defaultControl, true, true);
    }

    // These three are the same for different roles

    @Test
    public void testDirectWildcardReadResourceDefinitionWriteSensitivityAsMonitor() throws Exception {
        registerOneChildRootResource(createSensitivityConstraint("testDirectWildcardReadResourceDefinitionWriteSensitivityAsMonitor", false, false, true));
        ModelNode op = createReadResourceDescriptionOperation(ONE_ADDR, StandardRole.MONITOR, false);
        ModelNode result = getWildcardResourceRegistrationResult(executeForResult(op));
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        //Reads are sensitive so we cannot read them as the default says we can
        checkResourcePermissions(accessControl.defaultControl, true, false);
    }

    @Test
    public void testDirectWildcardReadResourceDefinitionWriteSensitivityAsMaintainer() throws Exception {
        registerOneChildRootResource(createSensitivityConstraint("testDirectWildcardReadResourceDefinitionWriteSensitivityAsMaintainer", false, false, true));
        ModelNode op = createReadResourceDescriptionOperation(ONE_ADDR, StandardRole.MAINTAINER, false);
        ModelNode result = getWildcardResourceRegistrationResult(executeForResult(op));
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        //Writes are sensitive so we cannot write them as the default says we can
        checkResourcePermissions(accessControl.defaultControl, true, false);
    }

    @Test
    public void testDirectWildcardReadResourceDefinitionWriteSensitivityAsAdministrator() throws Exception {
        registerOneChildRootResource(createSensitivityConstraint("testDirectWildcardReadResourceDefinitionWriteSensitivityAsAdministrator", false, false, true));
        ModelNode op = createReadResourceDescriptionOperation(ONE_ADDR, StandardRole.ADMINISTRATOR, false);
        ModelNode result = getWildcardResourceRegistrationResult(executeForResult(op));
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        checkResourcePermissions(accessControl.defaultControl, true, true);
    }

    // These three are the same for different roles  - similar to the testDirectWildcardReadResourceDefinitionWriteSensitivityAsXXX() ones but for a fixed resource

    @Test
    public void testDirectReadResourceDefinitionWriteSensitivityAsMonitor() throws Exception {
        registerOneChildRootResource(createSensitivityConstraint("testDirectReadResourceDefinitionWriteSensitivityAsMonitor", false, false, true));
        ModelNode op = createReadResourceDescriptionOperation(ONE_A_ADDR, StandardRole.MONITOR, false);
        ModelNode result = executeForResult(op);
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        //Reads are sensitive so we cannot read them as the default says we can
        checkResourcePermissions(accessControl.defaultControl, true, false);
    }

    @Test
    public void testDirectReadResourceDefinitionWriteSensitivityAsMaintainer() throws Exception {
        registerOneChildRootResource(createSensitivityConstraint("testDirectReadResourceDefinitionWriteSensitivityAsMaintainer", false, false, true));
        ModelNode op = createReadResourceDescriptionOperation(ONE_A_ADDR, StandardRole.MAINTAINER, false);
        ModelNode result = executeForResult(op);
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        //Writes are sensitive so we cannot write them as the default says we can
        checkResourcePermissions(accessControl.defaultControl, true, false);
    }

    @Test
    public void testDirectReadResourceDefinitionWriteSensitivityAsAdministrator() throws Exception {
        registerOneChildRootResource(createSensitivityConstraint("testDirectReadResourceDefinitionWriteSensitivityAsAdministrator", false, false, true));
        ModelNode op = createReadResourceDescriptionOperation(ONE_A_ADDR, StandardRole.ADMINISTRATOR, false);
        ModelNode result = executeForResult(op);
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        checkResourcePermissions(accessControl.defaultControl, true, true);
    }

    // These three are the same for different roles

    @Test
    public void testRecursiveReadResourceDefinitionReadSensitivityAsMonitor() throws Exception {
        //Does it really make sense to be able to configure that reads are sensitive but not writes?
        registerOneChildRootResource(createSensitivityConstraint("testRecursiveReadResourceDefinitionReadSensitivityAsMonitor", false, true, false));
        ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.MONITOR, false);
        ModelNode result = executeForResult(op);
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        checkResourcePermissions(accessControl.defaultControl, true, false);
        ModelNode childDesc = getChildDescription(result, ONE);
        accessControl = getResourceAccessControl(childDesc);
        //Reads are sensitive so we cannot read them as the default says we can
        checkResourcePermissions(accessControl.defaultControl, false, false);
    }

    @Test
    public void testRecursiveReadResourceDefinitionReadSensitivityAsMaintainer() throws Exception {
        //Does it really make sense to be able to configure that reads are sensitive but not writes?
        registerOneChildRootResource(createSensitivityConstraint("testRecursiveReadResourceDefinitionReadSensitivityAsMaintainer", false, true, false));
        ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.MAINTAINER, false);
        ModelNode result = executeForResult(op);
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        checkResourcePermissions(accessControl.defaultControl, true, true);
        ModelNode childDesc = getChildDescription(result, ONE);
        accessControl = getResourceAccessControl(childDesc);
        //Reads are sensitive so we cannot read them as the default says we can
        checkResourcePermissions(accessControl.defaultControl, false, true);
    }

    @Test
    public void testRecursiveReadResourceDefinitionReadSensitivityAsAdministrator() throws Exception {
        //Does it really make sense to be able to configure that reads are sensitive but not writes?
        registerOneChildRootResource(createSensitivityConstraint("testRecursiveReadResourceDefinitionReadSensitivityAsAdministrator", false, true, false));
        ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.ADMINISTRATOR, false);
        ModelNode result = executeForResult(op);
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        checkResourcePermissions(accessControl.defaultControl, true, true);
        ModelNode childDesc = getChildDescription(result, ONE);
        accessControl = getResourceAccessControl(childDesc);
        checkResourcePermissions(accessControl.defaultControl, true, true);
    }

    // These three are the same for different roles

    @Test
    public void testDirectWildcardReadResourceDefinitionReadSensitivityAsMonitor() throws Exception {
        //Does it really make sense to be able to configure that reads are sensitive but not writes?
        registerOneChildRootResource(createSensitivityConstraint("testDirectWildcardReadResourceDefinitionReadSensitivityAsMonitor", false, true, false));
        ModelNode op = createReadResourceDescriptionOperation(ONE_ADDR, StandardRole.MONITOR, false);
        ModelNode result = getWildcardResourceRegistrationResult(executeForResult(op));
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        //Reads are sensitive so we cannot read them as the default says we can
        checkResourcePermissions(accessControl.defaultControl, false, false);
    }

    @Test
    public void testDirectWildcardReadResourceDefinitionReadSensitivityAsMaintainer() throws Exception {
        //Does it really make sense to be able to configure that reads are sensitive but not writes?
        registerOneChildRootResource(createSensitivityConstraint("testDirectWildcardReadResourceDefinitionReadSensitivityAsMaintainer", false, true, false));
        ModelNode op = createReadResourceDescriptionOperation(ONE_ADDR, StandardRole.MAINTAINER, false);
        ModelNode result = getWildcardResourceRegistrationResult(executeForResult(op));
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        //Reads are sensitive so we cannot read them as the default says we can
        checkResourcePermissions(accessControl.defaultControl, false, true);
    }

    @Test
    public void testDirectWildcardReadResourceDefinitionReadSensitivityAsAdministrator() throws Exception {
        //Does it really make sense to be able to configure that reads are sensitive but not writes?
        registerOneChildRootResource(createSensitivityConstraint("testDirectWildcardReadResourceDefinitionReadSensitivityAsAdministrator", false, true, false));
        ModelNode op = createReadResourceDescriptionOperation(ONE_ADDR, StandardRole.ADMINISTRATOR, false);
        ModelNode result = getWildcardResourceRegistrationResult(executeForResult(op));
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        checkResourcePermissions(accessControl.defaultControl, true, true);
    }

    // These three are the same for different roles  - similar to the testDirectWildcardReadResourceDefinitionReadSensitivityAsXXX() ones but for a fixed resource

    @Test
    public void testDirectReadResourceDefinitionReadSensitivityAsMonitor() throws Exception {
        //Does it really make sense to be able to configure that reads are sensitive but not writes?
        registerOneChildRootResource(createSensitivityConstraint("testDirectReadResourceDefinitionReadSensitivityAsMonitor", false, true, false));
        ModelNode op = createReadResourceDescriptionOperation(ONE_A_ADDR, StandardRole.MONITOR, false);
        ModelNode result = executeForResult(op);
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        //Reads are sensitive so we cannot read them as the default says we can
        checkResourcePermissions(accessControl.defaultControl, false, false);
    }

    @Test
    public void testDirectReadResourceDefinitionReadSensitivityAsMaintainer() throws Exception {
        //Does it really make sense to be able to configure that reads are sensitive but not writes?
        registerOneChildRootResource(createSensitivityConstraint("testDirectReadResourceDefinitionReadSensitivityAsMaintainer", false, true, false));
        ModelNode op = createReadResourceDescriptionOperation(ONE_A_ADDR, StandardRole.MAINTAINER, false);
        ModelNode result = executeForResult(op);
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        //Reads are sensitive so we cannot read them as the default says we can
        checkResourcePermissions(accessControl.defaultControl, false, true);
    }

    @Test
    public void testDirectReadResourceDefinitionReadSensitivityAsAdministrator() throws Exception {
        //Does it really make sense to be able to configure that reads are sensitive but not writes?
        registerOneChildRootResource(createSensitivityConstraint("testDirectReadResourceDefinitionReadSensitivityAsAdministrator", false, true, false));
        ModelNode op = createReadResourceDescriptionOperation(ONE_A_ADDR, StandardRole.ADMINISTRATOR, false);
        ModelNode result = executeForResult(op);
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        checkResourcePermissions(accessControl.defaultControl, true, true);
    }

    private void registerOneChildRootResource(SensitiveTargetAccessConstraintDefinition...sensitivityConstraints) {
        ChildResourceDefinition oneChild = new ChildResourceDefinition(ONE, sensitivityConstraints);
        oneChild.addAttribute("attr1");
        rootRegistration.registerSubModel(oneChild);
        Resource resourceA = Resource.Factory.create();
        resourceA.getModel().get("attr1").set("test-a");
        rootResource.registerChild(ONE_A, resourceA);
        Resource resourceB = Resource.Factory.create();
        resourceB.getModel().get("attr1").set("test-a");
        rootResource.registerChild(ONE_B, resourceB);
    }

    // These three are the same for different roles

    @Test
    public void testAttributeSensitivityAsMonitor() throws Exception {
        registerAttributeResource("testAttributeSensitivityAsMonitor");

        ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.MONITOR, false);
        ModelNode result = executeForResult(op);
        ResourceAccessControl accessControl = getResourceAccessControl(getChildDescription(result, ONE));
        Map<String, ModelNode> attributes = checkAttributeAccessControlNames(accessControl.defaultControl, ATTR_ACCESS_READ_WRITE, ATTR_READ_WRITE, ATTR_WRITE, ATTR_READ, ATTR_NONE);
        checkAttributePermissions(attributes, ATTR_ACCESS_READ_WRITE, false, false);
        checkAttributePermissions(attributes, ATTR_READ_WRITE, false, false);
        checkAttributePermissions(attributes, ATTR_WRITE, true, false);
        checkAttributePermissions(attributes, ATTR_READ, false, false);
        checkAttributePermissions(attributes, ATTR_NONE, true, false);
    }

    @Test
    public void testAttributeSensitivityAsMaintainer() throws Exception {
        registerAttributeResource("testAttributeSensitivityAsMaintainer");

        ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.MAINTAINER, false);
        ModelNode result = executeForResult(op);
        ResourceAccessControl accessControl = getResourceAccessControl(getChildDescription(result, ONE));
        Map<String, ModelNode> attributes = checkAttributeAccessControlNames(accessControl.defaultControl,
                ATTR_ACCESS_READ_WRITE, ATTR_READ_WRITE, ATTR_WRITE, ATTR_READ, ATTR_NONE);
        checkAttributePermissions(attributes, ATTR_ACCESS_READ_WRITE, false, false);
        checkAttributePermissions(attributes, ATTR_READ_WRITE, false, false);
        checkAttributePermissions(attributes, ATTR_WRITE, true, false);
        checkAttributePermissions(attributes, ATTR_READ, false, true);
        checkAttributePermissions(attributes, ATTR_NONE, true, true);
    }

    @Test
    public void testAttributeSensitivityAsAdministrator() throws Exception {
        registerAttributeResource("testAttributeSensitivityAsAdministrator");

        ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.ADMINISTRATOR, false);
        ModelNode result = executeForResult(op);
        ResourceAccessControl accessControl = getResourceAccessControl(getChildDescription(result, ONE));
        Map<String, ModelNode> attributes = checkAttributeAccessControlNames(accessControl.defaultControl, ATTR_ACCESS_READ_WRITE, ATTR_READ_WRITE, ATTR_WRITE, ATTR_READ, ATTR_NONE);
        checkAttributePermissions(attributes, ATTR_ACCESS_READ_WRITE, true, true);
        checkAttributePermissions(attributes, ATTR_READ_WRITE, true, true);
        checkAttributePermissions(attributes, ATTR_WRITE, true, true);
        checkAttributePermissions(attributes, ATTR_READ, true, true);
        checkAttributePermissions(attributes, ATTR_NONE, true, true);
    }

    private void registerAttributeResource(String testName) {
        ChildResourceDefinition oneChild = new ChildResourceDefinition(ONE);
        oneChild.addAttribute(ATTR_ACCESS_READ_WRITE, createSensitivityConstraint(testName + "-access-read-write", true, true, true));
        oneChild.addAttribute(ATTR_READ_WRITE, createSensitivityConstraint(testName + "-read-write", false, true, true));
        oneChild.addAttribute(ATTR_WRITE, createSensitivityConstraint(testName + "-write", false, false, true));
        oneChild.addAttribute(ATTR_READ, createSensitivityConstraint(testName + "-read", false, true, false));
        oneChild.addAttribute(ATTR_NONE);
        rootRegistration.registerSubModel(oneChild);
        Resource resourceA = Resource.Factory.create();
        ModelNode modelA = resourceA.getModel();
        modelA.get(ATTR_ACCESS_READ_WRITE).set("test1");
        modelA.get(ATTR_READ_WRITE).set("test2");
        modelA.get(ATTR_WRITE).set("test3");
        modelA.get(ATTR_READ).set("test4");
        modelA.get(ATTR_NONE).set("test5");
        rootResource.registerChild(ONE_A, resourceA);
        rootResource.registerChild(ONE_B, Resource.Factory.create());
    }

    // These three are the same for different roles

    @Test
    public void testReadOnlyAttributeSensitivityAsMonitor() throws Exception {
        registerReadOnlyAttributeResource("testReadOnlyAttributeSensitivityAsMonitor");

        ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.MONITOR, false);
        ModelNode result = executeForResult(op);
        ResourceAccessControl accessControl = getResourceAccessControl(getChildDescription(result, ONE));
        Map<String, ModelNode> attributes = checkAttributeAccessControlNames(accessControl.defaultControl, ATTR_ACCESS_READ_WRITE, ATTR_READ_WRITE, ATTR_WRITE, ATTR_READ, ATTR_NONE);
        checkAttributePermissions(attributes, ATTR_ACCESS_READ_WRITE, false, false);
        checkAttributePermissions(attributes, ATTR_READ_WRITE, false, false);
        checkAttributePermissions(attributes, ATTR_WRITE, true, false);
        checkAttributePermissions(attributes, ATTR_READ, false, false);
        checkAttributePermissions(attributes, ATTR_NONE, true, false);
    }

    @Test
    public void testReadOnlyAttributeSensitivityAsMaintainer() throws Exception {
        registerReadOnlyAttributeResource("testReadOnlyAttributeSensitivityAsMaintainer");

        ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.MAINTAINER, false);
        ModelNode result = executeForResult(op);
        ResourceAccessControl accessControl = getResourceAccessControl(getChildDescription(result, ONE));
        Map<String, ModelNode> attributes = checkAttributeAccessControlNames(accessControl.defaultControl,
                ATTR_ACCESS_READ_WRITE, ATTR_READ_WRITE, ATTR_WRITE, ATTR_READ, ATTR_NONE);
        checkAttributePermissions(attributes, ATTR_ACCESS_READ_WRITE, false, false);
        checkAttributePermissions(attributes, ATTR_READ_WRITE, false, false);
        checkAttributePermissions(attributes, ATTR_WRITE, true, false);
        checkAttributePermissions(attributes, ATTR_READ, false, true);
        checkAttributePermissions(attributes, ATTR_NONE, true, true);
    }

    @Test
    public void testReadOnlyAttributeSensitivityAsAdministrator() throws Exception {
        registerReadOnlyAttributeResource("testReadOnlyAttributeSensitivityAsAdministrator");

        ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.ADMINISTRATOR, false);
        ModelNode result = executeForResult(op);
        ResourceAccessControl accessControl = getResourceAccessControl(getChildDescription(result, ONE));
        Map<String, ModelNode> attributes = checkAttributeAccessControlNames(accessControl.defaultControl, ATTR_ACCESS_READ_WRITE, ATTR_READ_WRITE, ATTR_WRITE, ATTR_READ, ATTR_NONE);
        checkAttributePermissions(attributes, ATTR_ACCESS_READ_WRITE, true, true);
        checkAttributePermissions(attributes, ATTR_READ_WRITE, true, true);
        checkAttributePermissions(attributes, ATTR_WRITE, true, true);
        checkAttributePermissions(attributes, ATTR_READ, true, true);
        checkAttributePermissions(attributes, ATTR_NONE, true, true);
    }


    private void registerReadOnlyAttributeResource(String testName) {
        ChildResourceDefinition oneChild = new ChildResourceDefinition(ONE);
        oneChild.addReadOnlyAttribute(ATTR_ACCESS_READ_WRITE, createSensitivityConstraint(testName + "-access-read-write", true, true, true));
        oneChild.addReadOnlyAttribute(ATTR_READ_WRITE, createSensitivityConstraint(testName + "-read-write", false, true, true));
        oneChild.addReadOnlyAttribute(ATTR_WRITE, createSensitivityConstraint(testName + "-write", false, false, true));
        oneChild.addReadOnlyAttribute(ATTR_READ, createSensitivityConstraint(testName + "-read", false, true, false));
        oneChild.addReadOnlyAttribute(ATTR_NONE);
        rootRegistration.registerSubModel(oneChild);
        Resource resourceA = Resource.Factory.create();
        ModelNode modelA = resourceA.getModel();
        modelA.get(ATTR_ACCESS_READ_WRITE).set("test1");
        modelA.get(ATTR_READ_WRITE).set("test2");
        modelA.get(ATTR_WRITE).set("test3");
        modelA.get(ATTR_READ).set("test4");
        modelA.get(ATTR_NONE).set("test5");
        rootResource.registerChild(ONE_A, resourceA);
        rootResource.registerChild(ONE_B, Resource.Factory.create());
    }

    // These three are the same for different roles

    @Test
    public void testOperationSensitivityAsMonitor() throws Exception {
        registerOperationResource("testOperationSensitivityAsMonitor");
        ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.MONITOR, true);
        ModelNode result = executeForResult(op);
        //TODO check root resource ops
        ResourceAccessControl accessControl = getResourceAccessControl(getChildDescription(result, ONE));
        Map<String, Boolean> operations = checkOperationAccessControlNames(accessControl.defaultControl, ADD, REMOVE,
                OP_CONFIG_RW_ACCESS_READ_WRITE, OP_CONFIG_RW_READ_WRITE, OP_CONFIG_RW_WRITE, OP_CONFIG_RW_READ, OP_CONFIG_RW_NONE,
                OP_RUNTIME_RW_ACCESS_READ_WRITE, OP_RUNTIME_RW_READ_WRITE, OP_RUNTIME_RW_WRITE, OP_RUNTIME_RW_READ, OP_RUNTIME_RW_NONE,
                OP_CONFIG_RO_ACCESS_READ_WRITE, OP_CONFIG_RO_READ_WRITE, OP_CONFIG_RO_WRITE, OP_CONFIG_RO_READ, OP_CONFIG_RO_NONE,
                OP_RUNTIME_RO_ACCESS_READ_WRITE, OP_RUNTIME_RO_READ_WRITE, OP_RUNTIME_RO_WRITE, OP_RUNTIME_RO_READ, OP_RUNTIME_RO_NONE);
        Assert.assertEquals(false, operations.get(ADD));
        Assert.assertEquals(false, operations.get(REMOVE));
        Assert.assertEquals(false, operations.get(OP_CONFIG_RW_ACCESS_READ_WRITE));
        Assert.assertEquals(false, operations.get(OP_CONFIG_RW_READ_WRITE));
        Assert.assertEquals(false, operations.get(OP_CONFIG_RW_WRITE));
        Assert.assertEquals(false, operations.get(OP_CONFIG_RW_READ));
        Assert.assertEquals(false, operations.get(OP_CONFIG_RW_NONE));
        Assert.assertEquals(false, operations.get(OP_RUNTIME_RW_ACCESS_READ_WRITE));
        Assert.assertEquals(false, operations.get(OP_RUNTIME_RW_READ_WRITE));
        Assert.assertEquals(false, operations.get(OP_RUNTIME_RW_WRITE));
        Assert.assertEquals(false, operations.get(OP_RUNTIME_RW_READ));
        Assert.assertEquals(false, operations.get(OP_RUNTIME_RW_NONE));
        Assert.assertEquals(false, operations.get(OP_CONFIG_RO_ACCESS_READ_WRITE));
        Assert.assertEquals(false, operations.get(OP_CONFIG_RO_READ_WRITE));
        //Although this has a write sensitivity, the operation is read-only so the sensitivity should not be relevant
        Assert.assertEquals(true, operations.get(OP_CONFIG_RO_WRITE));
        Assert.assertEquals(false, operations.get(OP_CONFIG_RO_READ));
        Assert.assertEquals(true, operations.get(OP_CONFIG_RO_NONE));
        Assert.assertEquals(false, operations.get(OP_RUNTIME_RO_ACCESS_READ_WRITE));
        Assert.assertEquals(false, operations.get(OP_RUNTIME_RO_READ_WRITE));
        //Although this has a write sensitivity, the operation is read-only so the sensitivity should not be relevant
        Assert.assertEquals(true, operations.get(OP_RUNTIME_RO_WRITE));
        Assert.assertEquals(false, operations.get(OP_RUNTIME_RO_READ));
        Assert.assertEquals(true, operations.get(OP_RUNTIME_RO_NONE));
    }

    @Test
    public void testOperationSensitivityAsMaintainer() throws Exception {
        registerOperationResource("testOperationSensitivityAsMaintainer");
        ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.MAINTAINER, true);
        ModelNode result = executeForResult(op);
        //TODO check root resource ops
        ResourceAccessControl accessControl = getResourceAccessControl(getChildDescription(result, ONE));
        Map<String, Boolean> operations = checkOperationAccessControlNames(accessControl.defaultControl, ADD, REMOVE,
                OP_CONFIG_RW_ACCESS_READ_WRITE, OP_CONFIG_RW_READ_WRITE, OP_CONFIG_RW_WRITE, OP_CONFIG_RW_READ, OP_CONFIG_RW_NONE,
                OP_RUNTIME_RW_ACCESS_READ_WRITE, OP_RUNTIME_RW_READ_WRITE, OP_RUNTIME_RW_WRITE, OP_RUNTIME_RW_READ, OP_RUNTIME_RW_NONE,
                OP_CONFIG_RO_ACCESS_READ_WRITE, OP_CONFIG_RO_READ_WRITE, OP_CONFIG_RO_WRITE, OP_CONFIG_RO_READ, OP_CONFIG_RO_NONE,
                OP_RUNTIME_RO_ACCESS_READ_WRITE, OP_RUNTIME_RO_READ_WRITE, OP_RUNTIME_RO_WRITE, OP_RUNTIME_RO_READ, OP_RUNTIME_RO_NONE);
        Assert.assertEquals(true, operations.get(ADD));
        Assert.assertEquals(true, operations.get(REMOVE));
        Assert.assertEquals(false, operations.get(OP_CONFIG_RW_ACCESS_READ_WRITE));
        Assert.assertEquals(false, operations.get(OP_CONFIG_RW_READ_WRITE));
        Assert.assertEquals(false, operations.get(OP_CONFIG_RW_WRITE));
        Assert.assertEquals(false, operations.get(OP_CONFIG_RW_READ));
        Assert.assertEquals(true, operations.get(OP_CONFIG_RW_NONE));
        Assert.assertEquals(false, operations.get(OP_RUNTIME_RW_ACCESS_READ_WRITE));
        Assert.assertEquals(false, operations.get(OP_RUNTIME_RW_READ_WRITE));
        Assert.assertEquals(false, operations.get(OP_RUNTIME_RW_WRITE));
        Assert.assertEquals(false, operations.get(OP_RUNTIME_RW_READ));
        Assert.assertEquals(true, operations.get(OP_RUNTIME_RW_NONE));
        Assert.assertEquals(false, operations.get(OP_CONFIG_RO_ACCESS_READ_WRITE));
        Assert.assertEquals(false, operations.get(OP_CONFIG_RO_READ_WRITE));
        //Although this has a write sensitivity, the operation is read-only so the sensitivity should not be relevant
        Assert.assertEquals(true, operations.get(OP_CONFIG_RO_WRITE));
        Assert.assertEquals(false, operations.get(OP_CONFIG_RO_READ));
        Assert.assertEquals(true, operations.get(OP_CONFIG_RO_NONE));
        Assert.assertEquals(false, operations.get(OP_RUNTIME_RO_ACCESS_READ_WRITE));
        Assert.assertEquals(false, operations.get(OP_RUNTIME_RO_READ_WRITE));
        //Although this has a write sensitivity, the operation is read-only so the sensitivity should not be relevant
        Assert.assertEquals(true, operations.get(OP_RUNTIME_RO_WRITE));
        Assert.assertEquals(false, operations.get(OP_RUNTIME_RO_READ));
        Assert.assertEquals(true, operations.get(OP_RUNTIME_RO_NONE));
    }

    @Test
    public void testOperationSensitivityAsAdministrator() throws Exception {
        registerOperationResource("testOperationSensitivityAsAdministrator");
        ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.ADMINISTRATOR, true);
        ModelNode result = executeForResult(op);
        //TODO check root resource ops
        ResourceAccessControl accessControl = getResourceAccessControl(getChildDescription(result, ONE));
        Map<String, Boolean> operations = checkOperationAccessControlNames(accessControl.defaultControl, ADD, REMOVE,
                OP_CONFIG_RW_ACCESS_READ_WRITE, OP_CONFIG_RW_READ_WRITE, OP_CONFIG_RW_WRITE, OP_CONFIG_RW_READ, OP_CONFIG_RW_NONE,
                OP_RUNTIME_RW_ACCESS_READ_WRITE, OP_RUNTIME_RW_READ_WRITE, OP_RUNTIME_RW_WRITE, OP_RUNTIME_RW_READ, OP_RUNTIME_RW_NONE,
                OP_CONFIG_RO_ACCESS_READ_WRITE, OP_CONFIG_RO_READ_WRITE, OP_CONFIG_RO_WRITE, OP_CONFIG_RO_READ, OP_CONFIG_RO_NONE,
                OP_RUNTIME_RO_ACCESS_READ_WRITE, OP_RUNTIME_RO_READ_WRITE, OP_RUNTIME_RO_WRITE, OP_RUNTIME_RO_READ, OP_RUNTIME_RO_NONE);
        Assert.assertEquals(true, operations.get(ADD));
        Assert.assertEquals(true, operations.get(REMOVE));
        Assert.assertEquals(true, operations.get(OP_CONFIG_RW_ACCESS_READ_WRITE));
        Assert.assertEquals(true, operations.get(OP_CONFIG_RW_READ_WRITE));
        Assert.assertEquals(true, operations.get(OP_CONFIG_RW_WRITE));
        Assert.assertEquals(true, operations.get(OP_CONFIG_RW_READ));
        Assert.assertEquals(true, operations.get(OP_CONFIG_RW_NONE));
        Assert.assertEquals(true, operations.get(OP_RUNTIME_RW_ACCESS_READ_WRITE));
        Assert.assertEquals(true, operations.get(OP_RUNTIME_RW_READ_WRITE));
        Assert.assertEquals(true, operations.get(OP_RUNTIME_RW_WRITE));
        Assert.assertEquals(true, operations.get(OP_RUNTIME_RW_READ));
        Assert.assertEquals(true, operations.get(OP_RUNTIME_RW_NONE));
        Assert.assertEquals(true, operations.get(OP_CONFIG_RO_ACCESS_READ_WRITE));
        Assert.assertEquals(true, operations.get(OP_CONFIG_RO_READ_WRITE));
        Assert.assertEquals(true, operations.get(OP_CONFIG_RO_WRITE));
        Assert.assertEquals(true, operations.get(OP_CONFIG_RO_READ));
        Assert.assertEquals(true, operations.get(OP_CONFIG_RO_NONE));
        Assert.assertEquals(true, operations.get(OP_RUNTIME_RO_ACCESS_READ_WRITE));
        Assert.assertEquals(true, operations.get(OP_RUNTIME_RO_READ_WRITE));
        Assert.assertEquals(true, operations.get(OP_RUNTIME_RO_WRITE));
        Assert.assertEquals(true, operations.get(OP_RUNTIME_RO_READ));
        Assert.assertEquals(true, operations.get(OP_RUNTIME_RO_NONE));
    }

    private void registerOperationResource(String testName) {
        ChildResourceDefinition oneChild = new ChildResourceDefinition(ONE);
        SensitiveTargetAccessConstraintDefinition accessReadWriteSensitivity = createSensitivityConstraint(testName + "-access-read-write", true, true, true);
        SensitiveTargetAccessConstraintDefinition readWriteSensitivity = createSensitivityConstraint(testName + "-read-write", false, true, true);
        SensitiveTargetAccessConstraintDefinition writeSensitivity = createSensitivityConstraint(testName + "-write", false, false, true);
        SensitiveTargetAccessConstraintDefinition readSensitivity = createSensitivityConstraint(testName + "-read", false, true, false);
        oneChild.addOperation(OP_CONFIG_RW_ACCESS_READ_WRITE, false, false, accessReadWriteSensitivity);
        oneChild.addOperation(OP_CONFIG_RW_READ_WRITE, false, false, readWriteSensitivity);
        oneChild.addOperation(OP_CONFIG_RW_WRITE, false, false, writeSensitivity);
        oneChild.addOperation(OP_CONFIG_RW_READ, false, false, readSensitivity);
        oneChild.addOperation(OP_CONFIG_RW_NONE, false, false);
        oneChild.addOperation(OP_RUNTIME_RW_ACCESS_READ_WRITE, false, true, accessReadWriteSensitivity);
        oneChild.addOperation(OP_RUNTIME_RW_READ_WRITE, false, true, readWriteSensitivity);
        oneChild.addOperation(OP_RUNTIME_RW_WRITE, false, true, writeSensitivity);
        oneChild.addOperation(OP_RUNTIME_RW_READ, false, true, readSensitivity);
        oneChild.addOperation(OP_RUNTIME_RW_NONE, false, true);
        oneChild.addOperation(OP_CONFIG_RO_ACCESS_READ_WRITE, true, false, accessReadWriteSensitivity);
        oneChild.addOperation(OP_CONFIG_RO_READ_WRITE, true, false, readWriteSensitivity);
        oneChild.addOperation(OP_CONFIG_RO_WRITE, true, false, writeSensitivity);
        oneChild.addOperation(OP_CONFIG_RO_READ, true, false, readSensitivity);
        oneChild.addOperation(OP_CONFIG_RO_NONE, true, false);
        oneChild.addOperation(OP_RUNTIME_RO_ACCESS_READ_WRITE, true, true, accessReadWriteSensitivity);
        oneChild.addOperation(OP_RUNTIME_RO_READ_WRITE, true, true, readWriteSensitivity);
        oneChild.addOperation(OP_RUNTIME_RO_WRITE, true, true, writeSensitivity);
        oneChild.addOperation(OP_RUNTIME_RO_READ, true, true, readSensitivity);
        oneChild.addOperation(OP_RUNTIME_RO_NONE, true, true);

        rootRegistration.registerSubModel(oneChild);
        rootResource.registerChild(ONE_A, Resource.Factory.create());
        rootResource.registerChild(ONE_B, Resource.Factory.create());
    }

    private void registerDeepResource(SensitiveTargetAccessConstraintDefinition constraint) {
        ChildResourceDefinition oneChild = new ChildResourceDefinition(ONE, constraint);
        ManagementResourceRegistration childOneReg = rootRegistration.registerSubModel(oneChild);
        rootResource.registerChild(ONE_A, Resource.Factory.create());
        rootResource.registerChild(ONE_B, Resource.Factory.create());

        ChildResourceDefinition twoChild = new ChildResourceDefinition(TWO, constraint);
        childOneReg.registerSubModel(twoChild);
        rootResource.requireChild(ONE_A).registerChild(TWO_A, Resource.Factory.create());
        rootResource.requireChild(ONE_A).registerChild(TWO_B, Resource.Factory.create());
        rootResource.requireChild(ONE_B).registerChild(TWO_A, Resource.Factory.create());
        rootResource.requireChild(ONE_B).registerChild(TWO_B, Resource.Factory.create());
    }

    // These three are the same for different roles

    @Test
    public void testDeepRecursiveReadResourceDefinitionAccessReadWriteSensitivityAsMonitor() throws Exception {
        SensitiveTargetAccessConstraintDefinition constraint = createSensitivityConstraint("testDeepRecursiveReadResourceDefinitionAccessReadWriteSensitivityAsMonitor", true, true, true);
        registerDeepResource(constraint);

        ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.MONITOR, false);
        ModelNode result = executeForResult(op);
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        checkResourcePermissions(accessControl.defaultControl, true, false);
        ModelNode childDesc = getChildDescription(result, ONE);
        assertNonAccessibleDefaultAccessControl(childDesc);//Since access is restricted, the monitor role access any of the resources
        childDesc = getChildDescription(childDesc, TWO);
        assertNonAccessibleDefaultAccessControl(childDesc);//Since access is restricted, the monitor role access any of the resources
    }

    @Test
    public void testDeepRecursiveReadResourceDefinitionAccessReadWriteSensitivityAsMaintainer() throws Exception {
        SensitiveTargetAccessConstraintDefinition constraint = createSensitivityConstraint("testDeepRecursiveReadResourceDefinitionAccessReadWriteSensitivityAsMaintainer", true, true, true);
        registerDeepResource(constraint);

        ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.MAINTAINER, false);
        ModelNode result = executeForResult(op);
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        checkResourcePermissions(accessControl.defaultControl, true, true);
        ModelNode childDesc = getChildDescription(result, ONE);
        assertNonAccessibleDefaultAccessControl(childDesc); //Since access is restricted, the maintainer role cannot access the resources
        childDesc = getChildDescription(childDesc, TWO);
        assertNonAccessibleDefaultAccessControl(childDesc); //Since access is restricted, the maintainer role cannot access the resources
    }

    @Test
    public void testDeepRecursiveReadResourceDefinitionAccessReadWriteSensitivityAsAdministrator() throws Exception {
        SensitiveTargetAccessConstraintDefinition constraint = createSensitivityConstraint("testDeepRecursiveReadResourceDefinitionAccessReadWriteSensitivityAsAdministrator", true, true, true);
        registerDeepResource(constraint);

        ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.ADMINISTRATOR, false);
        ModelNode result = executeForResult(op);
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        checkResourcePermissions(accessControl.defaultControl, true, true);
        ModelNode childDesc = getChildDescription(result, ONE);
        accessControl = getResourceAccessControl(childDesc);
        checkResourcePermissions(accessControl.defaultControl, true, true);
        childDesc = getChildDescription(childDesc, TWO);
        accessControl = getResourceAccessControl(childDesc);
        checkResourcePermissions(accessControl.defaultControl, true, true);
    }

    // These three are the same for different roles

    @Test
    public void testDeepDirectWildcardLevelOneReadResourceDefinitionAccessReadWriteSensitivityAsMonitor() throws Exception {
        SensitiveTargetAccessConstraintDefinition constraint = createSensitivityConstraint("testDeepDirectWildcardLevelOneReadResourceDefinitionAccessReadWriteSensitivityAsMonitor", true, true, true);
        registerDeepResource(constraint);

        ModelNode op = createReadResourceDescriptionOperation(ONE_ADDR, StandardRole.MONITOR, false);
        ModelNode result = getWildcardResourceRegistrationResult(executeForResult(op));
        assertNonAccessibleDefaultAccessControl(result); //Since access is restricted, the monitor role cannot access the resources
        ModelNode childDesc = getChildDescription(result, TWO);
        assertNonAccessibleDefaultAccessControl(childDesc); //Since access is restricted, the monitor role cannot access the resources
    }

    @Test
    public void testDeepDirectWildcardLevelOneReadResourceDefinitionAccessReadWriteSensitivityAsMaintainer() throws Exception {
        SensitiveTargetAccessConstraintDefinition constraint = createSensitivityConstraint("testDeepDirectWildcardLevelOneReadResourceDefinitionAccessReadWriteSensitivityAsMaintainer", true, true, true);
        registerDeepResource(constraint);

        ModelNode op = createReadResourceDescriptionOperation(ONE_ADDR, StandardRole.MAINTAINER, false);
        ModelNode result = getWildcardResourceRegistrationResult(executeForResult(op));
        assertNonAccessibleDefaultAccessControl(result); //Since access is restricted, the monitor role cannot access the resources
        ModelNode childDesc = getChildDescription(result, TWO);
        assertNonAccessibleDefaultAccessControl(childDesc); //Since access is restricted, the monitor role cannot access the resources
    }

    @Test
    public void testDeepDirectWildcardLevelOneReadResourceDefinitionAccessReadWriteSensitivityAsAdministrator() throws Exception {
        SensitiveTargetAccessConstraintDefinition constraint = createSensitivityConstraint("testDeepDirectWildcardLevelOneReadResourceDefinitionAccessReadWriteSensitivityAsAdministrator", true, true, true);
        registerDeepResource(constraint);

        ModelNode op = createReadResourceDescriptionOperation(ONE_ADDR, StandardRole.ADMINISTRATOR, false);
        ModelNode result = getWildcardResourceRegistrationResult(executeForResult(op));
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        checkResourcePermissions(accessControl.defaultControl, true, true);
        ModelNode childDesc = getChildDescription(result, TWO);
        accessControl = getResourceAccessControl(childDesc);
        checkResourcePermissions(accessControl.defaultControl, true, true);
    }

    // These three are the same for different roles  - similar to the testDeepDirectWildcardLevelOneReadResourceDefinitionAccessReadWriteSensitivityAsXXXX() ones but for a fixed resource

    @Test
    public void testDeepDirectLevelOneReadResourceDefinitionAccessReadWriteSensitivityAsMonitor() throws Exception {
        SensitiveTargetAccessConstraintDefinition constraint = createSensitivityConstraint("testDeepDirectLevelOneReadResourceDefinitionAccessReadWriteSensitivityAsMonitor", true, true, true);
        registerDeepResource(constraint);

        ModelNode op = createReadResourceDescriptionOperation(ONE_A_ADDR, StandardRole.MONITOR, false);
        ModelNode result = executeForResult(op);
        assertNonAccessibleDefaultAccessControl(result); //Since access is restricted, the monitor role cannot access the resources
        ModelNode childDesc = getChildDescription(result, TWO);
        assertNonAccessibleDefaultAccessControl(childDesc); //Since access is restricted, the monitor role cannot access the resources
    }

    @Test
    public void testDeepDirectLevelOneReadResourceDefinitionAccessReadWriteSensitivityAsMaintainer() throws Exception {
        SensitiveTargetAccessConstraintDefinition constraint = createSensitivityConstraint("testDeepDirectLevelOneReadResourceDefinitionAccessReadWriteSensitivityAsMaintainer", true, true, true);
        registerDeepResource(constraint);

        ModelNode op = createReadResourceDescriptionOperation(ONE_A_ADDR, StandardRole.MAINTAINER, false);
        ModelNode result = executeForResult(op);
        assertNonAccessibleDefaultAccessControl(result); //Since access is restricted, the monitor role cannot access the resources
        ModelNode childDesc = getChildDescription(result, TWO);
        assertNonAccessibleDefaultAccessControl(childDesc); //Since access is restricted, the monitor role cannot access the resources
    }

    @Test
    public void testDeepDirectLevelOneReadResourceDefinitionAccessReadWriteSensitivityAsAdministrator() throws Exception {
        SensitiveTargetAccessConstraintDefinition constraint = createSensitivityConstraint("testDeepDirectLevelOneReadResourceDefinitionAccessReadWriteSensitivityAsAdministrator", true, true, true);
        registerDeepResource(constraint);

        ModelNode op = createReadResourceDescriptionOperation(ONE_A_ADDR, StandardRole.ADMINISTRATOR, false);
        ModelNode result = executeForResult(op);
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        checkResourcePermissions(accessControl.defaultControl, true, true);
        ModelNode childDesc = getChildDescription(result, TWO);
        accessControl = getResourceAccessControl(childDesc);
        checkResourcePermissions(accessControl.defaultControl, true, true);
    }

    // These three are the same for different roles
    @Test
    public void testDeepDirectWildcardLevelTwoReadResourceDefinitionAccessReadWriteSensitivityAsMonitor() throws Exception {
        SensitiveTargetAccessConstraintDefinition constraint = createSensitivityConstraint("testDeepDirectWildcardLevelTwoReadResourceDefinitionAccessReadWriteSensitivityAsMonitor", true, true, true);
        registerDeepResource(constraint);

        ModelNode op = createReadResourceDescriptionOperation(TWO_ADDR, StandardRole.MONITOR, false);
        ModelNode result = getWildcardResourceRegistrationResult(executeForResult(op));
        assertNonAccessibleDefaultAccessControl(result); //Since access is restricted, the monitor role cannot access the resources
    }

    @Test
    public void testDeepDirectWildcardLevelTwoReadResourceDefinitionAccessReadWriteSensitivityAsMaintainer() throws Exception {
        SensitiveTargetAccessConstraintDefinition constraint = createSensitivityConstraint("testDeepDirectWildcardLevelTwoReadResourceDefinitionAccessReadWriteSensitivityAsMaintainer", true, true, true);
        registerDeepResource(constraint);

        ModelNode op = createReadResourceDescriptionOperation(TWO_ADDR, StandardRole.MAINTAINER, false);
        ModelNode result = getWildcardResourceRegistrationResult(executeForResult(op));
        assertNonAccessibleDefaultAccessControl(result); //Since access is restricted, the maintainer role cannot access the resources
    }

    @Test
    public void testDeepDirectWildcardLevelTwoReadResourceDefinitionAccessReadWriteSensitivityAsAdministrator() throws Exception {
        SensitiveTargetAccessConstraintDefinition constraint = createSensitivityConstraint("testDeepDirectWildcardLevelTwoReadResourceDefinitionAccessReadWriteSensitivityAsAdministrator", true, true, true);
        registerDeepResource(constraint);

        ModelNode op = createReadResourceDescriptionOperation(TWO_ADDR, StandardRole.ADMINISTRATOR, false);
        ModelNode result = getWildcardResourceRegistrationResult(executeForResult(op));
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        checkResourcePermissions(accessControl.defaultControl, true, true);
    }

    // These three are the same for different roles  - similar to the testDeepDirectWildcardLevelTwoReadResourceDefinitionAccessReadWriteSensitivityAsXXXX() ones but for a fixed resource

    @Test
    public void testDeepDirectLevelTwoReadResourceDefinitionAccessReadWriteSensitivityAsMonitor() throws Exception {
        SensitiveTargetAccessConstraintDefinition constraint = createSensitivityConstraint("testDeepDirectLevelTwoReadResourceDefinitionAccessReadWriteSensitivityAsMonitor", true, true, true);
        registerDeepResource(constraint);

        ModelNode op = createReadResourceDescriptionOperation(ONE_A_TWO_A_ADDR, StandardRole.MONITOR, false);
        ModelNode result = executeForResult(op);
        assertNonAccessibleDefaultAccessControl(result); //Since access is restricted, the monitor role cannot access the resources
    }

    @Test
    public void testDeepDirectLevelTwoReadResourceDefinitionAccessReadWriteSensitivityAsMaintainer() throws Exception {
        SensitiveTargetAccessConstraintDefinition constraint = createSensitivityConstraint("testDeepDirectLevelTwoReadResourceDefinitionAccessReadWriteSensitivityAsMaintainer", true, true, true);
        registerDeepResource(constraint);

        ModelNode op = createReadResourceDescriptionOperation(ONE_A_TWO_A_ADDR, StandardRole.MAINTAINER, false);
        ModelNode result = executeForResult(op);
        assertNonAccessibleDefaultAccessControl(result); //Since access is restricted, the maintainer role cannot access the resources
    }

    @Test
    public void testDeepDirectLevelTwoReadResourceDefinitionAccessReadWriteSensitivityAsAdministrator() throws Exception {
        SensitiveTargetAccessConstraintDefinition constraint = createSensitivityConstraint("testDeepDirectLevelTwoReadResourceDefinitionAccessReadWriteSensitivityAsAdministrator", true, true, true);
        registerDeepResource(constraint);

        ModelNode op = createReadResourceDescriptionOperation(ONE_A_TWO_A_ADDR, StandardRole.ADMINISTRATOR, false);
        ModelNode result = executeForResult(op);
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        checkResourcePermissions(accessControl.defaultControl, true, true);
    }

    // These three are the same for different roles
    @Test
    public void testDeepRecursiveReadResourceDefinitionReadWriteSensitivityAsMonitor() throws Exception {
        SensitiveTargetAccessConstraintDefinition constraint = createSensitivityConstraint("testDeepRecursiveReadResourceDefinitionReadWriteSensitivityAsMonitor", false, true, true);
        registerDeepResource(constraint);

        ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.MONITOR, false);
        ModelNode result = executeForResult(op);
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        checkResourcePermissions(accessControl.defaultControl, true, false);
        ModelNode childDesc = getChildDescription(result, ONE);
        accessControl = getResourceAccessControl(childDesc);
        checkResourcePermissions(accessControl.defaultControl, false, false);
        childDesc = getChildDescription(childDesc, TWO);
        accessControl = getResourceAccessControl(childDesc);
        checkResourcePermissions(accessControl.defaultControl, false, false);
    }

    @Test
    public void testDeepRecursiveReadResourceDefinitionReadWriteSensitivityAsMaintainer() throws Exception {
        SensitiveTargetAccessConstraintDefinition constraint = createSensitivityConstraint("testDeepRecursiveReadResourceDefinitionReadWriteSensitivityAsMaintainer", false, true, true);
        registerDeepResource(constraint);

        ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.MAINTAINER, false);
        ModelNode result = executeForResult(op);
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        checkResourcePermissions(accessControl.defaultControl, true, true);
        ModelNode childDesc = getChildDescription(result, ONE);
        accessControl = getResourceAccessControl(childDesc);
        checkResourcePermissions(accessControl.defaultControl, false, false);
        childDesc = getChildDescription(childDesc, TWO);
        accessControl = getResourceAccessControl(childDesc);
        checkResourcePermissions(accessControl.defaultControl, false, false);
    }

    @Test
    public void testDeepRecursiveReadResourceDefinitionReadWriteSensitivityAsAdministrator() throws Exception {
        SensitiveTargetAccessConstraintDefinition constraint = createSensitivityConstraint("testDeepRecursiveReadResourceDefinitionReadWriteSensitivityAsAdministrator", false, true, true);
        registerDeepResource(constraint);

        ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.ADMINISTRATOR, false);
        ModelNode result = executeForResult(op);
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        checkResourcePermissions(accessControl.defaultControl, true, true);
        ModelNode childDesc = getChildDescription(result, ONE);
        accessControl = getResourceAccessControl(childDesc);
        checkResourcePermissions(accessControl.defaultControl, true, true);
        childDesc = getChildDescription(childDesc, TWO);
        accessControl = getResourceAccessControl(childDesc);
        checkResourcePermissions(accessControl.defaultControl, true, true);
    }

    // These three are the same for different roles
    @Test
    public void testDeepDirectWildcardLevelOneReadResourceDefinitionReadWriteSensitivityAsMonitor() throws Exception {
        SensitiveTargetAccessConstraintDefinition constraint = createSensitivityConstraint("testDeepDirectWildcardLevelOneReadResourceDefinitionReadWriteSensitivityAsMonitor", false, true, true);
        registerDeepResource(constraint);
        ModelNode op = createReadResourceDescriptionOperation(ONE_ADDR, StandardRole.MONITOR, false);
        ModelNode result = getWildcardResourceRegistrationResult(executeForResult(op));
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        checkResourcePermissions(accessControl.defaultControl, false, false);
        ModelNode childDesc = getChildDescription(result, TWO);
        accessControl = getResourceAccessControl(childDesc);
        checkResourcePermissions(accessControl.defaultControl, false, false);
    }

    @Test
    public void testDeepDirectWildcardLevelOneReadResourceDefinitionReadWriteSensitivityAsMaintainer() throws Exception {
        SensitiveTargetAccessConstraintDefinition constraint = createSensitivityConstraint("testDeepDirectWildcardLevelOneReadResourceDefinitionReadWriteSensitivityAsMaintainer", false, true, true);
        registerDeepResource(constraint);
        ModelNode op = createReadResourceDescriptionOperation(ONE_ADDR, StandardRole.MAINTAINER, false);
        ModelNode result = getWildcardResourceRegistrationResult(executeForResult(op));
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        checkResourcePermissions(accessControl.defaultControl, false, false);
        ModelNode childDesc = getChildDescription(result, TWO);
        accessControl = getResourceAccessControl(childDesc);
        checkResourcePermissions(accessControl.defaultControl, false, false);
    }

    @Test
    public void testDeepDirectWildcardLevelOneReadResourceDefinitionReadWriteSensitivityAsAdministrator() throws Exception {
        SensitiveTargetAccessConstraintDefinition constraint = createSensitivityConstraint("testDeepDirectWildcardLevelOneReadResourceDefinitionReadWriteSensitivityAsAdministrator", false, true, true);
        registerDeepResource(constraint);
        ModelNode op = createReadResourceDescriptionOperation(ONE_ADDR, StandardRole.ADMINISTRATOR, false);
        ModelNode result = getWildcardResourceRegistrationResult(executeForResult(op));
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        checkResourcePermissions(accessControl.defaultControl, true, true);
        ModelNode childDesc = getChildDescription(result, TWO);
        accessControl = getResourceAccessControl(childDesc);
        checkResourcePermissions(accessControl.defaultControl, true, true);
    }

    // These three are the same for different roles  - similar to the testDeepDirectWildcardLevelOneReadResourceDefinitionReadWriteSensitivityAsXXXX() ones but for a fixed resource
    @Test
    public void testDeepDirectLevelOneReadResourceDefinitionReadWriteSensitivityAsMonitor() throws Exception {
        SensitiveTargetAccessConstraintDefinition constraint = createSensitivityConstraint("testDeepDirectLevelOneReadResourceDefinitionReadWriteSensitivityAsMonitor", false, true, true);
        registerDeepResource(constraint);
        ModelNode op = createReadResourceDescriptionOperation(ONE_A_ADDR, StandardRole.MONITOR, false);
        ModelNode result = executeForResult(op);
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        checkResourcePermissions(accessControl.defaultControl, false, false);
        ModelNode childDesc = getChildDescription(result, TWO);
        accessControl = getResourceAccessControl(childDesc);
        checkResourcePermissions(accessControl.defaultControl, false, false);
    }

    @Test
    public void testDeepDirectLevelOneReadResourceDefinitionReadWriteSensitivityAsMaintainer() throws Exception {
        SensitiveTargetAccessConstraintDefinition constraint = createSensitivityConstraint("testDeepDirectLevelOneReadResourceDefinitionReadWriteSensitivityAsMaintainer", false, true, true);
        registerDeepResource(constraint);
        ModelNode op = createReadResourceDescriptionOperation(ONE_A_ADDR, StandardRole.MAINTAINER, false);
        ModelNode result = executeForResult(op);
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        checkResourcePermissions(accessControl.defaultControl, false, false);
        ModelNode childDesc = getChildDescription(result, TWO);
        accessControl = getResourceAccessControl(childDesc);
        checkResourcePermissions(accessControl.defaultControl, false, false);
    }

    @Test
    public void testDeepDirectLevelOneReadResourceDefinitionReadWriteSensitivityAsAdministrator() throws Exception {
        SensitiveTargetAccessConstraintDefinition constraint = createSensitivityConstraint("testDeepDirectLevelOneReadResourceDefinitionReadWriteSensitivityAsAdministrator", false, true, true);
        registerDeepResource(constraint);
        ModelNode op = createReadResourceDescriptionOperation(ONE_A_ADDR, StandardRole.ADMINISTRATOR, false);
        ModelNode result = executeForResult(op);
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        checkResourcePermissions(accessControl.defaultControl, true, true);
        ModelNode childDesc = getChildDescription(result, TWO);
        accessControl = getResourceAccessControl(childDesc);
        checkResourcePermissions(accessControl.defaultControl, true, true);
    }

    // These three are the same for different roles
    @Test
    public void testDeepDirectWildcardLevelTwoReadResourceDefinitionReadWriteSensitivityAsMonitor() throws Exception {
        SensitiveTargetAccessConstraintDefinition constraint = createSensitivityConstraint("testDeepDirectWildcardLevelTwoReadResourceDefinitionReadWriteSensitivityAsMonitor", false, true, true);
        registerDeepResource(constraint);
        ModelNode op = createReadResourceDescriptionOperation(TWO_ADDR, StandardRole.MONITOR, false);
        ModelNode result = getWildcardResourceRegistrationResult(executeForResult(op));
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        checkResourcePermissions(accessControl.defaultControl, false, false);
    }

    @Test
    public void testDeepDirectWildcardLevelTwoReadResourceDefinitionReadWriteSensitivityAsMaintainer() throws Exception {
        SensitiveTargetAccessConstraintDefinition constraint = createSensitivityConstraint("testDeepDirectWildcardLevelTwoReadResourceDefinitionReadWriteSensitivityAsMaintainer", false, true, true);
        registerDeepResource(constraint);
        ModelNode op = createReadResourceDescriptionOperation(TWO_ADDR, StandardRole.MAINTAINER, false);
        ModelNode result = getWildcardResourceRegistrationResult(executeForResult(op));
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        checkResourcePermissions(accessControl.defaultControl, false, false);
    }

    @Test
    public void testDeepDirectWildcardLevelTwoReadResourceDefinitionReadWriteSensitivityAsAdministrator() throws Exception {
        SensitiveTargetAccessConstraintDefinition constraint = createSensitivityConstraint("testDeepDirectWildcardLevelTwoReadResourceDefinitionReadWriteSensitivityAsAdministrator", false, true, true);
        registerDeepResource(constraint);
        ModelNode op = createReadResourceDescriptionOperation(TWO_ADDR, StandardRole.ADMINISTRATOR, false);
        ModelNode result = getWildcardResourceRegistrationResult(executeForResult(op));
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        checkResourcePermissions(accessControl.defaultControl, true, true);
    }

    // These three are the same for different roles  - similar to the testDeepDirectWildcardLevelTwoReadResourceDefinitionReadWriteSensitivityAsXXXX() ones but for a fixed resource
    @Test
    public void testDeepDirectLevelTwoReadResourceDefinitionReadWriteSensitivityAsMonitor() throws Exception {
        SensitiveTargetAccessConstraintDefinition constraint = createSensitivityConstraint("testDeepDirectLevelTwoReadResourceDefinitionReadWriteSensitivityAsMonitor", false, true, true);
        registerDeepResource(constraint);
        ModelNode op = createReadResourceDescriptionOperation(ONE_A_TWO_A_ADDR, StandardRole.MONITOR, false);
        ModelNode result = executeForResult(op);
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        checkResourcePermissions(accessControl.defaultControl, false, false);
    }

    @Test
    public void testDeepDirectLevelTwoReadResourceDefinitionReadWriteSensitivityAsMaintainer() throws Exception {
        SensitiveTargetAccessConstraintDefinition constraint = createSensitivityConstraint("testDeepDirectLevelTwoReadResourceDefinitionReadWriteSensitivityAsMaintainer", false, true, true);
        registerDeepResource(constraint);
        ModelNode op = createReadResourceDescriptionOperation(ONE_A_TWO_A_ADDR, StandardRole.MAINTAINER, false);
        ModelNode result = executeForResult(op);
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        checkResourcePermissions(accessControl.defaultControl, false, false);
    }

    @Test
    public void testDeepDirectLevelTwoReadResourceDefinitionReadWriteSensitivityAsAdministrator() throws Exception {
        SensitiveTargetAccessConstraintDefinition constraint = createSensitivityConstraint("testDeepDirectLevelTwoReadResourceDefinitionReadWriteSensitivityAsAdministrator", false, true, true);
        registerDeepResource(constraint);
        ModelNode op = createReadResourceDescriptionOperation(ONE_A_TWO_A_ADDR, StandardRole.ADMINISTRATOR, false);
        ModelNode result = executeForResult(op);
        ResourceAccessControl accessControl = getResourceAccessControl(result);
        checkResourcePermissions(accessControl.defaultControl, true, true);
    }

    // These three are the same for different roles

    @Test
    public void testAttributeSensitivityWithVaultExpressionVaultAccessReadWriteAsMonitor() throws Exception {
        registerAttributeVaultSensitivityResource();

        VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresAccessPermission(true);
        VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresReadPermission(true);
        VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresWritePermission(true);
        try {
            ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.MONITOR, true);
            ModelNode result = executeForResult(op);

            ResourceAccessControl accessControl = getResourceAccessControl(getChildDescription(result, ONE), ONE_A_ADDR);
            Map<String, ModelNode> defaultAttributes = checkAttributeAccessControlNames(accessControl.defaultControl, ATTR_NONE, ATTR_VAULT);
            checkAttributePermissions(defaultAttributes, ATTR_NONE, true, false);
            checkAttributePermissions(defaultAttributes, ATTR_VAULT, true, false);
            Map<String, ModelNode> exceptionAttributes = checkAttributeAccessControlNames(accessControl.exceptions.get(ONE_A_ADDR), ATTR_NONE, ATTR_VAULT);
            checkAttributePermissions(exceptionAttributes, ATTR_NONE, true, false);
            checkAttributePermissions(exceptionAttributes, ATTR_VAULT, false, false);

            Map<String, Boolean> defaultOps = checkOperationAccessControlNames(accessControl.defaultControl, ADD, REMOVE);
            Assert.assertEquals(false, defaultOps.get(ADD));
            Assert.assertEquals(false, defaultOps.get(REMOVE));
            Map<String, Boolean> exceptionOps = checkOperationAccessControlNames(accessControl.exceptions.get(ONE_A_ADDR), ADD, REMOVE);
            Assert.assertEquals(false, exceptionOps.get(ADD));
            Assert.assertEquals(false, exceptionOps.get(REMOVE));
        } finally {
            VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresAccessPermission(null);
            VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresReadPermission(null);
            VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresWritePermission(null);
        }
    }

    @Test
    public void testAttributeSensitivityWithVaultExpressionVaultAccessReadWriteAsMaintainer() throws Exception {
        registerAttributeVaultSensitivityResource();

        VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresAccessPermission(true);
        VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresReadPermission(true);
        VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresWritePermission(true);
        try {
            ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.MAINTAINER, true);
            ModelNode result = executeForResult(op);

            ResourceAccessControl accessControl = getResourceAccessControl(getChildDescription(result, ONE), ONE_A_ADDR);
            Map<String, ModelNode> defaultAttributes = checkAttributeAccessControlNames(accessControl.defaultControl, ATTR_NONE, ATTR_VAULT);
            checkAttributePermissions(defaultAttributes, ATTR_NONE, true, true);
            checkAttributePermissions(defaultAttributes, ATTR_VAULT, true, true);
            Map<String, ModelNode> exceptionAttributes = checkAttributeAccessControlNames(accessControl.exceptions.get(ONE_A_ADDR), ATTR_NONE, ATTR_VAULT);
            checkAttributePermissions(exceptionAttributes, ATTR_NONE, true, true);
            checkAttributePermissions(exceptionAttributes, ATTR_VAULT, false, false);

            Map<String, Boolean> defaultOps = checkOperationAccessControlNames(accessControl.defaultControl, ADD, REMOVE);
            Assert.assertEquals(true, defaultOps.get(ADD));
            Assert.assertEquals(true, defaultOps.get(REMOVE));
            Map<String, Boolean> exceptionOps = checkOperationAccessControlNames(accessControl.exceptions.get(ONE_A_ADDR), ADD, REMOVE);
            // Vault expressions in an existing resource do not mean add should not be executable
            Assert.assertEquals(true, exceptionOps.get(ADD));
            Assert.assertEquals(true, exceptionOps.get(REMOVE));

        } finally {
            VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresAccessPermission(null);
            VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresReadPermission(null);
            VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresWritePermission(null);
        }
    }

    @Test
    public void testAttributeSensitivityWithVaultExpressionVaultAccessReadWriteAsAdministrator() throws Exception {
        registerAttributeVaultSensitivityResource();

        VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresAccessPermission(true);
        VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresReadPermission(true);
        VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresWritePermission(true);
        try {
            ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.ADMINISTRATOR, true);
            ModelNode result = executeForResult(op);

            ResourceAccessControl accessControl = getResourceAccessControl(getChildDescription(result, ONE));
            Map<String, ModelNode> defaultAttributes = checkAttributeAccessControlNames(accessControl.defaultControl, ATTR_NONE, ATTR_VAULT);
            checkAttributePermissions(defaultAttributes, ATTR_NONE, true, true);
            checkAttributePermissions(defaultAttributes, ATTR_VAULT, true, true);
            Assert.assertTrue(accessControl.exceptions.isEmpty());

            Map<String, Boolean> defaultOps = checkOperationAccessControlNames(accessControl.defaultControl, ADD, REMOVE);
            Assert.assertEquals(true, defaultOps.get(ADD));
            Assert.assertEquals(true, defaultOps.get(REMOVE));
        } finally {
            VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresAccessPermission(null);
            VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresReadPermission(null);
            VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresWritePermission(null);
        }
    }

    // These three are the same for different roles

    @Test
    public void testAttributeSensitivityWithVaultExpressionVaultReadAsMonitor() throws Exception {
        registerAttributeVaultSensitivityResource();

        VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresAccessPermission(false);
        VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresReadPermission(true);
        VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresWritePermission(false);
        try {
            ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.MONITOR, true);
            ModelNode result = executeForResult(op);

            ResourceAccessControl accessControl = getResourceAccessControl(getChildDescription(result, ONE), ONE_A_ADDR);
            Map<String, ModelNode> defaultAttributes = checkAttributeAccessControlNames(accessControl.defaultControl, ATTR_NONE, ATTR_VAULT);
            checkAttributePermissions(defaultAttributes, ATTR_NONE, true, false);
            checkAttributePermissions(defaultAttributes, ATTR_VAULT, true, false);
            Map<String, ModelNode> exceptionAttributes = checkAttributeAccessControlNames(accessControl.exceptions.get(ONE_A_ADDR), ATTR_NONE, ATTR_VAULT);
            checkAttributePermissions(exceptionAttributes, ATTR_NONE, true, false);
            checkAttributePermissions(exceptionAttributes, ATTR_VAULT, false, false);

            Map<String, Boolean> defaultOps = checkOperationAccessControlNames(accessControl.defaultControl, ADD, REMOVE);
            Assert.assertEquals(false, defaultOps.get(ADD));
            Assert.assertEquals(false, defaultOps.get(REMOVE));
            Map<String, Boolean> exceptionOps = checkOperationAccessControlNames(accessControl.exceptions.get(ONE_A_ADDR), ADD, REMOVE);
            Assert.assertEquals(false, exceptionOps.get(ADD));
            Assert.assertEquals(false, exceptionOps.get(REMOVE));

        } finally {
            VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresAccessPermission(null);
            VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresReadPermission(null);
            VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresWritePermission(null);
        }
    }

    @Test
    public void testAttributeSensitivityWithVaultExpressionVaultReadAsMaintainer() throws Exception {
        registerAttributeVaultSensitivityResource();

        VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresAccessPermission(false);
        VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresReadPermission(true);
        VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresWritePermission(false);
        try {
            ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.MAINTAINER, true);
            ModelNode result = executeForResult(op);

            ResourceAccessControl accessControl = getResourceAccessControl(getChildDescription(result, ONE), ONE_A_ADDR);
            Map<String, ModelNode> defaultAttributes = checkAttributeAccessControlNames(accessControl.defaultControl, ATTR_NONE, ATTR_VAULT);
            checkAttributePermissions(defaultAttributes, ATTR_NONE, true, true);
            checkAttributePermissions(defaultAttributes, ATTR_VAULT, true, true);
            Map<String, ModelNode> exceptionAttributes = checkAttributeAccessControlNames(accessControl.exceptions.get(ONE_A_ADDR), ATTR_NONE, ATTR_VAULT);
            checkAttributePermissions(exceptionAttributes, ATTR_NONE, true, true);
            checkAttributePermissions(exceptionAttributes, ATTR_VAULT, false, true);

            Map<String, Boolean> defaultOps = checkOperationAccessControlNames(accessControl.defaultControl, ADD, REMOVE);
            Assert.assertEquals(true, defaultOps.get(ADD));
            Assert.assertEquals(true, defaultOps.get(REMOVE));
            Map<String, Boolean> exceptionOps = checkOperationAccessControlNames(accessControl.exceptions.get(ONE_A_ADDR), ADD, REMOVE);
            // Vault expressions in an existing resource do not mean add should not be executable
            Assert.assertEquals(true, exceptionOps.get(ADD));
            Assert.assertEquals(true, exceptionOps.get(REMOVE));

        } finally {
            VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresAccessPermission(null);
            VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresReadPermission(null);
            VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresWritePermission(null);
        }
    }

    @Test
    public void testAttributeSensitivityWithVaultExpressionVaultReadAsAdministrator() throws Exception {
        registerAttributeVaultSensitivityResource();

        VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresAccessPermission(false);
        VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresReadPermission(true);
        VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresWritePermission(false);
        try {
            ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.ADMINISTRATOR, true);
            ModelNode result = executeForResult(op);

            ResourceAccessControl accessControl = getResourceAccessControl(getChildDescription(result, ONE));
            Map<String, ModelNode> defaultAttributes = checkAttributeAccessControlNames(accessControl.defaultControl, ATTR_NONE, ATTR_VAULT);
            checkAttributePermissions(defaultAttributes, ATTR_NONE, true, true);
            checkAttributePermissions(defaultAttributes, ATTR_VAULT, true, true);

            Map<String, Boolean> defaultOps = checkOperationAccessControlNames(accessControl.defaultControl, ADD, REMOVE);
            Assert.assertEquals(true, defaultOps.get(ADD));
            Assert.assertEquals(true, defaultOps.get(REMOVE));
        } finally {
            VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresAccessPermission(null);
            VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresReadPermission(null);
            VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresWritePermission(null);
        }
    }

    // These three are the same for different roles

    @Test
    public void testAttributeSensitivityWithVaultExpressionVaultWriteAsMonitor() throws Exception {
        registerAttributeVaultSensitivityResource();

        VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresAccessPermission(false);
        VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresReadPermission(false);
        VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresWritePermission(true);
        try {
            ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.MONITOR, true);
            ModelNode result = executeForResult(op);

            ResourceAccessControl accessControl = getResourceAccessControl(getChildDescription(result, ONE));
            Map<String, ModelNode> defaultAttributes = checkAttributeAccessControlNames(accessControl.defaultControl, ATTR_NONE, ATTR_VAULT);
            checkAttributePermissions(defaultAttributes, ATTR_NONE, true, false);
            checkAttributePermissions(defaultAttributes, ATTR_VAULT, true, false);

            Map<String, Boolean> defaultOps = checkOperationAccessControlNames(accessControl.defaultControl, ADD, REMOVE);
            Assert.assertEquals(false, defaultOps.get(ADD));
            Assert.assertEquals(false, defaultOps.get(REMOVE));
        } finally {
            VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresAccessPermission(null);
            VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresReadPermission(null);
            VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresWritePermission(null);
        }
    }

    @Test
    public void testAttributeSensitivityWithVaultExpressionVaultWriteAsMaintainer() throws Exception {
        registerAttributeVaultSensitivityResource();

        VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresAccessPermission(false);
        VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresReadPermission(false);
        VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresWritePermission(true);
        try {
            ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.MAINTAINER, true);
            ModelNode result = executeForResult(op);

            ResourceAccessControl accessControl = getResourceAccessControl(getChildDescription(result, ONE), ONE_A_ADDR);
            Map<String, ModelNode> defaultAttributes = checkAttributeAccessControlNames(accessControl.defaultControl, ATTR_NONE, ATTR_VAULT);
            checkAttributePermissions(defaultAttributes, ATTR_NONE, true, true);
            checkAttributePermissions(defaultAttributes, ATTR_VAULT, true, true);
            Map<String, ModelNode> exceptionAttributes = checkAttributeAccessControlNames(accessControl.exceptions.get(ONE_A_ADDR), ATTR_NONE, ATTR_VAULT);
            checkAttributePermissions(exceptionAttributes, ATTR_NONE, true, true);
            checkAttributePermissions(exceptionAttributes, ATTR_VAULT, true, false);

            Map<String, Boolean> defaultOps = checkOperationAccessControlNames(accessControl.defaultControl, ADD, REMOVE);
            Assert.assertEquals(true, defaultOps.get(ADD));
            Assert.assertEquals(true, defaultOps.get(REMOVE));
            Map<String, Boolean> exceptionOps = checkOperationAccessControlNames(accessControl.exceptions.get(ONE_A_ADDR), ADD, REMOVE);
            // Vault expressions in an existing resource do not mean add should not be executable
            Assert.assertEquals(true, exceptionOps.get(ADD));
            Assert.assertEquals(true, exceptionOps.get(REMOVE));

        } finally {
            VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresAccessPermission(null);
            VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresReadPermission(null);
            VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresWritePermission(null);
        }
    }

    @Test
    public void testAttributeSensitivityWithVaultExpressionVaultWriteAsAdministrator() throws Exception {
        registerAttributeVaultSensitivityResource();

        VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresAccessPermission(false);
        VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresReadPermission(false);
        VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresWritePermission(true);
        try {
            ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.ADMINISTRATOR, true);
            ModelNode result = executeForResult(op);

            ResourceAccessControl accessControl = getResourceAccessControl(getChildDescription(result, ONE));
            Map<String, ModelNode> defaultAttributes = checkAttributeAccessControlNames(accessControl.defaultControl, ATTR_NONE, ATTR_VAULT);
            checkAttributePermissions(defaultAttributes, ATTR_NONE, true, true);
            checkAttributePermissions(defaultAttributes, ATTR_VAULT, true, true);

            Map<String, Boolean> defaultOps = checkOperationAccessControlNames(accessControl.defaultControl, ADD, REMOVE);
            Assert.assertEquals(true, defaultOps.get(ADD));
            Assert.assertEquals(true, defaultOps.get(REMOVE));
        } finally {
            VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresAccessPermission(null);
            VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresReadPermission(null);
            VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresWritePermission(null);
        }
    }

    private void registerAttributeVaultSensitivityResource() {
        ChildResourceDefinition oneChild = new ChildResourceDefinition(ONE);
        oneChild.addAttribute(ATTR_NONE);
        oneChild.addAttribute(ATTR_VAULT);
        rootRegistration.registerSubModel(oneChild);
        Resource resourceA = Resource.Factory.create();
        ModelNode modelA = resourceA.getModel();
        modelA.get(ATTR_NONE).set("hello");
        modelA.get(ATTR_VAULT).set("${VAULT::AA::bb::cc}");
        rootResource.registerChild(ONE_A, resourceA);
        rootResource.registerChild(ONE_B, Resource.Factory.create());
    }

    // These two are related to see that a fixed resource gets included as a default and also listed as an exception if it is sensitive due to vault expressions
    @Test
    public void testFixedChildWithNoSensitivityAsMaintainer() throws Exception {
        registerAttributeVaultSensitivityFixedResource();
        VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresAccessPermission(false);
        VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresReadPermission(false);
        VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresWritePermission(false);
        try {
            ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.MAINTAINER, true);
            ModelNode result = executeForResult(op);

            ResourceAccessControl accessControl = getResourceAccessControl(getChildDescription(result, ONE_A));
            Map<String, ModelNode> defaultAttributes = checkAttributeAccessControlNames(accessControl.defaultControl, ATTR_NONE, ATTR_VAULT);
            checkAttributePermissions(defaultAttributes, ATTR_NONE, true, true);
            checkAttributePermissions(defaultAttributes, ATTR_VAULT, true, true);

            Map<String, Boolean> defaultOps = checkOperationAccessControlNames(accessControl.defaultControl, ADD, REMOVE);
            Assert.assertEquals(true, defaultOps.get(ADD));
            Assert.assertEquals(true, defaultOps.get(REMOVE));

        } finally {
            VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresAccessPermission(null);
            VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresReadPermission(null);
            VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresWritePermission(null);
        }
    }

    @Test
    public void testFixedChildWithVaultWriteSensitivityAsMaintainer() throws Exception {
        registerAttributeVaultSensitivityFixedResource();
        VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresAccessPermission(false);
        VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresReadPermission(false);
        VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresWritePermission(true);
        try {
            ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.MAINTAINER, true);
            ModelNode result = executeForResult(op);

            ResourceAccessControl accessControl = getResourceAccessControl(getChildDescription(result, ONE_A), ONE_A_ADDR);
            Map<String, ModelNode> defaultAttributes = checkAttributeAccessControlNames(accessControl.defaultControl, ATTR_NONE, ATTR_VAULT);
            checkAttributePermissions(defaultAttributes, ATTR_NONE, true, true);
            checkAttributePermissions(defaultAttributes, ATTR_VAULT, true, true);
            Map<String, ModelNode> exceptionAttributes = checkAttributeAccessControlNames(accessControl.exceptions.get(ONE_A_ADDR), ATTR_NONE, ATTR_VAULT);
            checkAttributePermissions(exceptionAttributes, ATTR_NONE, true, true);
            checkAttributePermissions(exceptionAttributes, ATTR_VAULT, true, false);


            Map<String, Boolean> defaultOps = checkOperationAccessControlNames(accessControl.defaultControl, ADD, REMOVE);
            Assert.assertEquals(true, defaultOps.get(ADD));
            Assert.assertEquals(true, defaultOps.get(REMOVE));
            Map<String, Boolean> exceptionOps = checkOperationAccessControlNames(accessControl.exceptions.get(ONE_A_ADDR), ADD, REMOVE);
            // Vault expressions in an existing resource do not mean add should not be executable
            Assert.assertEquals(true, exceptionOps.get(ADD));
            Assert.assertEquals(true, exceptionOps.get(REMOVE));
        } finally {
            VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresAccessPermission(null);
            VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresReadPermission(null);
            VaultExpressionSensitivityConfig.INSTANCE.setConfiguredRequiresWritePermission(null);
        }
    }

    private void registerAttributeVaultSensitivityFixedResource() {
        ChildResourceDefinition oneChild = new ChildResourceDefinition(ONE_A);
        oneChild.addAttribute(ATTR_NONE);
        oneChild.addAttribute(ATTR_VAULT);
        rootRegistration.registerSubModel(oneChild);
        Resource resourceA = Resource.Factory.create();
        ModelNode modelA = resourceA.getModel();
        modelA.get(ATTR_NONE).set("hello");
        modelA.get(ATTR_VAULT).set("${VAULT::AA::bb::cc}");
        rootResource.registerChild(ONE_A, resourceA);
    }

    //These three test the impact of the access-control parameter
    @Test
    public void testAccessControlNone() throws Exception  {
        //These should have no access-control element but the normal descriptions
        ModelNode desc = readModelDescriptionWithAccessControlParameter(ReadResourceDescriptionHandler.AccessControl.NONE);
        Assert.assertFalse(desc.has(ACCESS_CONTROL));

        Assert.assertEquals("description", desc.get(ModelDescriptionConstants.DESCRIPTION).asString());
        Assert.assertTrue(desc.get(CHILDREN, TWO.getKey(), ModelDescriptionConstants.DESCRIPTION).isDefined());

        Assert.assertTrue(desc.hasDefined(ModelDescriptionConstants.ATTRIBUTES));
        Set<String> attributes = desc.get(ModelDescriptionConstants.ATTRIBUTES).keys();
        Assert.assertEquals(1, attributes.size());
        Assert.assertTrue(attributes.contains(ATTR_NONE));

        Assert.assertTrue(desc.hasDefined(ModelDescriptionConstants.OPERATIONS));
        Set<String> ops = desc.get(ModelDescriptionConstants.OPERATIONS).keys();
        Assert.assertEquals(3, ops.size());
        Assert.assertTrue(ops.contains(ModelDescriptionConstants.ADD));
        Assert.assertTrue(ops.contains(ModelDescriptionConstants.REMOVE));
        Assert.assertTrue(ops.contains(OP_CONFIG_RW_NONE));

        desc = getChildDescription(desc, TWO);
        Assert.assertFalse(desc.has(ACCESS_CONTROL));

        Assert.assertEquals("description", desc.get(ModelDescriptionConstants.DESCRIPTION).asString());

        Assert.assertTrue(desc.hasDefined(ModelDescriptionConstants.ATTRIBUTES));
        attributes = desc.get(ModelDescriptionConstants.ATTRIBUTES).keys();
        Assert.assertEquals(1, attributes.size());
        Assert.assertTrue(attributes.contains(ATTR_NONE));

        Assert.assertTrue(desc.hasDefined(ModelDescriptionConstants.OPERATIONS));
        ops = desc.get(ModelDescriptionConstants.OPERATIONS).keys();
        Assert.assertEquals(3, ops.size());
        Assert.assertTrue(ops.contains(ModelDescriptionConstants.ADD));
        Assert.assertTrue(ops.contains(ModelDescriptionConstants.REMOVE));
        Assert.assertTrue(ops.contains(OP_CONFIG_RW_NONE));
    }

    @Test
    public void testAccessControlCombinedDescriptions() throws Exception  {
        //These should have the access-control element and the normal descriptions
        ModelNode desc = readModelDescriptionWithAccessControlParameter(ReadResourceDescriptionHandler.AccessControl.COMBINED_DESCRIPTIONS);
        ResourceAccessControl accessControl = getResourceAccessControl(desc);
        Map<String, ModelNode> defaultAttributes = checkAttributeAccessControlNames(accessControl.defaultControl, ATTR_NONE);
        checkAttributePermissions(defaultAttributes, ATTR_NONE, true, true);
        Map<String, Boolean> defaultOperations = checkOperationAccessControlNames(accessControl.defaultControl, ModelDescriptionConstants.ADD, ModelDescriptionConstants.REMOVE, OP_CONFIG_RW_NONE);
        Assert.assertEquals(true, defaultOperations.get(ADD));
        Assert.assertEquals(true, defaultOperations.get(REMOVE));
        Assert.assertEquals(true, defaultOperations.get(OP_CONFIG_RW_NONE));

        Assert.assertEquals("description", desc.get(ModelDescriptionConstants.DESCRIPTION).asString());
        Assert.assertTrue(desc.get(CHILDREN, TWO.getKey(), ModelDescriptionConstants.DESCRIPTION).isDefined());

        Assert.assertTrue(desc.hasDefined(ModelDescriptionConstants.ATTRIBUTES));
        Set<String> attributes = desc.get(ModelDescriptionConstants.ATTRIBUTES).keys();
        Assert.assertEquals(1, attributes.size());
        Assert.assertTrue(attributes.contains(ATTR_NONE));

        Assert.assertTrue(desc.hasDefined(ModelDescriptionConstants.OPERATIONS));
        Set<String> ops = desc.get(ModelDescriptionConstants.OPERATIONS).keys();
        Assert.assertEquals(3, ops.size());
        Assert.assertTrue(ops.contains(ModelDescriptionConstants.ADD));
        Assert.assertTrue(ops.contains(ModelDescriptionConstants.REMOVE));
        Assert.assertTrue(ops.contains(OP_CONFIG_RW_NONE));

        desc = getChildDescription(desc, TWO);
        accessControl = getResourceAccessControl(desc);
        defaultAttributes = checkAttributeAccessControlNames(accessControl.defaultControl, ATTR_NONE);
        checkAttributePermissions(defaultAttributes, ATTR_NONE, true, true);
        defaultOperations = checkOperationAccessControlNames(accessControl.defaultControl, ModelDescriptionConstants.ADD, ModelDescriptionConstants.REMOVE, OP_CONFIG_RW_NONE);
        Assert.assertEquals(true, defaultOperations.get(ADD));
        Assert.assertEquals(true, defaultOperations.get(REMOVE));
        Assert.assertEquals(true, defaultOperations.get(OP_CONFIG_RW_NONE));

        Assert.assertEquals("description", desc.get(ModelDescriptionConstants.DESCRIPTION).asString());

        Assert.assertTrue(desc.hasDefined(ModelDescriptionConstants.ATTRIBUTES));
        attributes = desc.get(ModelDescriptionConstants.ATTRIBUTES).keys();
        Assert.assertEquals(1, attributes.size());
        Assert.assertTrue(attributes.contains(ATTR_NONE));

        Assert.assertTrue(desc.hasDefined(ModelDescriptionConstants.OPERATIONS));
        ops = desc.get(ModelDescriptionConstants.OPERATIONS).keys();
        Assert.assertEquals(3, ops.size());
        Assert.assertTrue(ops.contains(ModelDescriptionConstants.ADD));
        Assert.assertTrue(ops.contains(ModelDescriptionConstants.REMOVE));
        Assert.assertTrue(ops.contains(OP_CONFIG_RW_NONE));
    }

    @Test
    public void testAccessControlTrimDescriptions() throws Exception  {
        //These should have the access-control element but trim the normal descriptions
        ModelNode desc = readModelDescriptionWithAccessControlParameter(ReadResourceDescriptionHandler.AccessControl.TRIM_DESCRIPTONS);
        ResourceAccessControl accessControl = getResourceAccessControl(desc);
        Map<String, ModelNode> defaultAttributes = checkAttributeAccessControlNames(accessControl.defaultControl, ATTR_NONE);
        checkAttributePermissions(defaultAttributes, ATTR_NONE, true, true);
        Map<String, Boolean> defaultOperations = checkOperationAccessControlNames(accessControl.defaultControl, ModelDescriptionConstants.ADD, ModelDescriptionConstants.REMOVE, OP_CONFIG_RW_NONE);
        Assert.assertEquals(true, defaultOperations.get(ADD));
        Assert.assertEquals(true, defaultOperations.get(REMOVE));
        Assert.assertEquals(true, defaultOperations.get(OP_CONFIG_RW_NONE));

        Assert.assertFalse(desc.hasDefined(ModelDescriptionConstants.DESCRIPTION));
        Assert.assertFalse(desc.hasDefined(ModelDescriptionConstants.ATTRIBUTES));
        Assert.assertFalse(desc.hasDefined(ModelDescriptionConstants.OPERATIONS));
        Assert.assertFalse(desc.get(CHILDREN, TWO.getKey(), ModelDescriptionConstants.DESCRIPTION).isDefined());


        desc = getChildDescription(desc, TWO);
        accessControl = getResourceAccessControl(desc);
        defaultAttributes = checkAttributeAccessControlNames(accessControl.defaultControl, ATTR_NONE);
        checkAttributePermissions(defaultAttributes, ATTR_NONE, true, true);
        defaultOperations = checkOperationAccessControlNames(accessControl.defaultControl, ModelDescriptionConstants.ADD, ModelDescriptionConstants.REMOVE, OP_CONFIG_RW_NONE);
        Assert.assertEquals(true, defaultOperations.get(ADD));
        Assert.assertEquals(true, defaultOperations.get(REMOVE));
        Assert.assertEquals(true, defaultOperations.get(OP_CONFIG_RW_NONE));

        Assert.assertFalse(desc.hasDefined(ModelDescriptionConstants.DESCRIPTION));
        Assert.assertFalse(desc.hasDefined(ModelDescriptionConstants.ATTRIBUTES));
        Assert.assertFalse(desc.hasDefined(ModelDescriptionConstants.OPERATIONS));
    }

    private ModelNode readModelDescriptionWithAccessControlParameter(ReadResourceDescriptionHandler.AccessControl accessControl) throws Exception {
        ChildResourceDefinition oneChild = new ChildResourceDefinition(ONE);
        oneChild.addAttribute(ATTR_NONE);
        oneChild.addOperation(OP_CONFIG_RW_NONE, false, false);
        ManagementResourceRegistration oneReg = rootRegistration.registerSubModel(oneChild);
        Resource resourceOneA = Resource.Factory.create();
        ModelNode modelOneA = resourceOneA.getModel();
        modelOneA.get(ATTR_NONE).set("uno");
        rootResource.registerChild(ONE_A, resourceOneA);

        ChildResourceDefinition twoChild = new ChildResourceDefinition(TWO);
        twoChild.addAttribute(ATTR_NONE);
        twoChild.addOperation(OP_CONFIG_RW_NONE, false, false);
        oneReg.registerSubModel(twoChild);
        Resource resourceTwoA = Resource.Factory.create();
        ModelNode modelTwoA = resourceOneA.getModel();
        modelTwoA.get(ATTR_NONE).set("dos");
        resourceOneA.registerChild(TWO_A, resourceTwoA);

        ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.ADMINISTRATOR, true);
        op.get(ACCESS_CONTROL).set(accessControl.toString());
        ModelNode desc = executeForResult(op);
        return getChildDescription(desc, ONE);
    }

    private SensitiveTargetAccessConstraintDefinition createSensitivityConstraint(String name, boolean access, boolean read, boolean write) {
        SensitivityClassification classification = new SensitivityClassification("test", name, access, read, write);
        return new SensitiveTargetAccessConstraintDefinition(classification);
    }

    private ModelNode getChildDescription(ModelNode description, PathElement element) {
        ModelNode childDesc = description.get(CHILDREN, element.getKey(), MODEL_DESCRIPTION, element.getValue());
        Assert.assertTrue(description.toString(), childDesc.isDefined());
        return childDesc;
    }

    private void assertNonAccessibleDefaultAccessControl(ModelNode description) {
        ResourceAccessControl accessControl = getResourceAccessControl(description);
        Assert.assertEquals(1, accessControl.defaultControl.keys().size());
        Assert.assertTrue(accessControl.defaultControl.hasDefined(ActionEffect.ADDRESS.toString()));
        Assert.assertFalse(accessControl.defaultControl.get(ActionEffect.ADDRESS.toString()).asBoolean());
    }

    private ResourceAccessControl getResourceAccessControl(ModelNode description, PathAddress...exceptions){
        Assert.assertTrue(description.hasDefined(ACCESS_CONTROL));

        ModelNode defaultControl = description.get(ACCESS_CONTROL, DEFAULT);
        Assert.assertTrue(defaultControl.isDefined());

        Map<PathAddress, ModelNode> exceptionsMap = new HashMap<PathAddress, ModelNode>();
        for (Property prop : description.get(ACCESS_CONTROL, EXCEPTIONS).asPropertyList()) {
            Assert.assertTrue(prop.getValue().isDefined());
            exceptionsMap.put(PathAddress.pathAddress(ModelNode.fromString(prop.getName())), prop.getValue());
        }
        List<PathAddress> expected = Arrays.asList(exceptions);
        Assert.assertTrue(exceptionsMap.keySet().toString(), exceptionsMap.keySet().containsAll(expected));
        Assert.assertTrue(expected.containsAll(exceptionsMap.keySet()));
        return new ResourceAccessControl(defaultControl, exceptionsMap);
    }

    private Map<String, ModelNode> checkAttributeAccessControlNames(ModelNode accessControl, String...attributeNames) {
        Map<String, ModelNode> map = new HashMap<String, ModelNode>();
        ModelNode attributes = accessControl.get(ATTRIBUTES);
        Assert.assertTrue(attributes.isDefined());
        for (Property prop : attributes.asPropertyList()) {
            map.put(prop.getName(), prop.getValue());
        }
        List<String> names = Arrays.asList(attributeNames);
        Assert.assertTrue(names.containsAll(map.keySet()));
        Assert.assertTrue(map.keySet().containsAll(names));

        return map;
    }

    private void checkAttributePermissions(Map<String, ModelNode> map, String attrName, Boolean read, Boolean write) {
        ModelNode attr = map.get(attrName);
        Assert.assertNotNull(attr);
        assertEqualsOrNotDefined(attr, ModelDescriptionConstants.READ, read);
        assertEqualsOrNotDefined(attr, ModelDescriptionConstants.WRITE, write);
    }

    private Map<String, Boolean> checkOperationAccessControlNames(ModelNode accessControl, String...operationNames) {
        Map<String, Boolean> map = new HashMap<String, Boolean>();
        ModelNode operations = accessControl.get(OPERATIONS);
        Assert.assertTrue(operations.isDefined());
        for (Property prop : operations.asPropertyList()) {
            ModelNode opCtrl = prop.getValue();
            Assert.assertTrue(opCtrl.hasDefined(EXECUTE));
            map.put(prop.getName(), prop.getValue().get(EXECUTE).asBoolean());
        }
        List<String> names = Arrays.asList(operationNames);
        Assert.assertEquals(names.size(), map.keySet().size());
        Assert.assertTrue(names.containsAll(map.keySet()));
        Assert.assertTrue(map.keySet().containsAll(names));

        return map;
    }

    private void assertEqualsOrNotDefined(ModelNode node, String name, Boolean expected) {
        if (expected != null) {
            Assert.assertEquals(expected.booleanValue(), node.get(name).asBoolean());
        } else {
            Assert.assertFalse(node.hasDefined(name));
        }
    }

    private ModelNode getWildcardResourceRegistrationResult(ModelNode result) {
        Assert.assertEquals(ModelType.LIST, result.getType());
        List<ModelNode> list = result.asList();
        Assert.assertEquals(1, list.size());
        ModelNode realResult = list.get(0);
        return realResult.get(RESULT);
    }

    private void checkResourcePermissions(ModelNode accessControl, boolean read, boolean write) {
        Assert.assertEquals(read, accessControl.get(ModelDescriptionConstants.READ).asBoolean());
        Assert.assertEquals(write, accessControl.get(ModelDescriptionConstants.WRITE).asBoolean());
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

    private ModelNode createReadResourceDescriptionOperation(PathAddress address, StandardRole role, boolean operations) {
        ModelNode op = Util.createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, address);
        op.get(RECURSIVE).set(true);
        op.get(ACCESS_CONTROL).set(ReadResourceDescriptionHandler.AccessControl.COMBINED_DESCRIPTIONS.toString());
        if (operations) {
            op.get(OPERATIONS).set(true);
            op.get(INHERITED).set(false);
        }

        op.get(OPERATION_HEADERS).get("roles").set(role.toString());

        return op;
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
        private final List<AttributeDefinition> readOnlyAttributes = Collections.synchronizedList(new ArrayList<AttributeDefinition>());
        private final List<OperationDefinition> operations = Collections.synchronizedList(new ArrayList<OperationDefinition>());

        ChildResourceDefinition(PathElement element, AccessConstraintDefinition...constraints){
            super(element);
            this.constraints = Collections.unmodifiableList(Arrays.asList(constraints));
        }

        void addAttribute(String name, AccessConstraintDefinition...constraints) {
            SimpleAttributeDefinitionBuilder builder = new SimpleAttributeDefinitionBuilder(name, ModelType.STRING);
            if (constraints != null) {
                builder.setAccessConstraints(constraints);
            }
            attributes.add(builder.build());
        }

        void addReadOnlyAttribute(String name, AccessConstraintDefinition...constraints) {
            SimpleAttributeDefinitionBuilder builder = new SimpleAttributeDefinitionBuilder(name, ModelType.STRING);
            if (constraints != null) {
                builder.setAccessConstraints(constraints);
            }
            readOnlyAttributes.add(builder.build());
        }

        void addOperation(String name, boolean readOnly, boolean runtimeOnly, AccessConstraintDefinition...constraints) {
            SimpleOperationDefinitionBuilder builder = new SimpleOperationDefinitionBuilder(name, new NonResolvingResourceDescriptionResolver());
            if (constraints != null) {
                builder.setAccessConstraints(constraints);
            }
            if (readOnly) {
                builder.setReadOnly();
            }
            if (runtimeOnly) {
                builder.setRuntimeOnly();
            }
            operations.add(builder.build());
        }

        @Override
        public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
            for (AttributeDefinition attribute : attributes) {
                resourceRegistration.registerReadWriteAttribute(attribute, null, new ModelOnlyWriteAttributeHandler(attribute));
            }
            for (AttributeDefinition attribute : readOnlyAttributes) {
                resourceRegistration.registerReadOnlyAttribute(attribute, null);
            }
        }

        @Override
        public void registerOperations(ManagementResourceRegistration resourceRegistration) {
            super.registerOperations(resourceRegistration);
            for (OperationDefinition op : operations) {
                resourceRegistration.registerOperationHandler(op, TestOperationStepHandler.INSTANCE);
            }
        }

        @Override
        public List<AccessConstraintDefinition> getAccessConstraints() {
            return constraints;
        }
    }

    private static class TestOperationStepHandler implements OperationStepHandler {
        static final TestOperationStepHandler INSTANCE = new TestOperationStepHandler();

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            context.stepCompleted();
        }
    }

    private static class ResourceAccessControl {
        private final ModelNode defaultControl;
        private final Map<PathAddress, ModelNode> exceptions;

        public ResourceAccessControl(ModelNode defaultControl, Map<PathAddress, ModelNode> exceptions) {
            this.defaultControl = defaultControl;
            this.exceptions = exceptions;
        }
    }
}
