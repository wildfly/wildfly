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
package org.jboss.as.controller.test;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ACCESS_CONTROL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
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
import org.jboss.as.controller.access.constraint.management.AccessConstraintDefinition;
import org.jboss.as.controller.access.constraint.management.ConstrainedResourceDefinition;
import org.jboss.as.controller.access.constraint.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.access.rbac.StandardRole;
import org.jboss.as.controller.descriptions.NonResolvingResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.global.GlobalOperationHandlers;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
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
    private static final PathAddress ONE_B_ADDR = PathAddress.pathAddress(ONE_B);

    private static final PathElement TWO = PathElement.pathElement("two");
    private static final PathElement TWO_A = PathElement.pathElement("two", "a");
    private static final PathElement TWO_B = PathElement.pathElement("two", "b");
    private static final PathAddress TWO_ADDR = PathAddress.pathAddress(ONE, TWO);
    private static final PathAddress ONE_A_TWO_A_ADDR = ONE_A_ADDR.append(TWO_A);
    private static final PathAddress ONE_A_TWO_B_ADDR = ONE_A_ADDR.append(TWO_B);
    private static final PathAddress ONE_B_TWO_A_ADDR = ONE_B_ADDR.append(TWO_A);
    private static final PathAddress ONE_B_TWO_B_ADDR = ONE_B_ADDR.append(TWO_B);
    private static final PathAddress ONE_TWO_ADDR = PathAddress.pathAddress(ONE, TWO);


    private static final String ATTR_ACCESS_READ_WRITE = "access-read-write";
    private static final String ATTR_READ_WRITE = "read-write";
    private static final String ATTR_WRITE = "write";
    private static final String ATTR_READ = "read";
    private static final String ATTR_NONE = "none";

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


    @Test
    public void testNonRecursiveReadRootResourceDefinitionNoSensitivityAsMonitor() throws Exception {
        registerOneChildRootResource();
        ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.MONITOR, false);
        op.get(RECURSIVE).set(false);
        ModelNode result = executeForResult(op);
        Map<PathAddress, ModelNode> map = getResourceAccessControl(result, PathAddress.EMPTY_ADDRESS);
        checkResourcePermissions(map, true, false, true, false);
    }

    @Test
    public void testNonRecursiveReadRootResourceDefinitionNoSensitivityAsMaintainer() throws Exception {
        registerOneChildRootResource();
        ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.MAINTAINER, false);
        op.get(RECURSIVE).set(false);
        ModelNode result = executeForResult(op);
        Map<PathAddress, ModelNode> map = getResourceAccessControl(result, PathAddress.EMPTY_ADDRESS);
        checkResourcePermissions(map, true, true, true, true);
    }

    @Test
    public void testNonRecursiveReadRootResourceDefinitionNoSensitivityAsAdministrator() throws Exception {
        registerOneChildRootResource();
        ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.ADMINISTRATOR, false);
        op.get(RECURSIVE).set(false);
        ModelNode result = executeForResult(op);
        Map<PathAddress, ModelNode> map = getResourceAccessControl(result, PathAddress.EMPTY_ADDRESS);
        checkResourcePermissions(map, true, true, true, true);
    }

    @Test
    public void testRecursiveReadResourceDefinitionNoSensitivityAsMonitor() throws Exception {
        registerOneChildRootResource();
        ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.MONITOR, false);
        ModelNode result = executeForResult(op);
        Map<PathAddress, ModelNode> map = getResourceAccessControl(result, PathAddress.EMPTY_ADDRESS);
        checkResourcePermissions(map, true, false, true, false);
        ModelNode childDesc = getChildDescription(result, ONE);
        map = getResourceAccessControl(childDesc, ONE_ADDR);
        checkResourcePermissions(map, true, false, true, false);
    }

    @Test
    public void testRecursiveReadResourceDefinitionNoSensitivityAsMaintainer() throws Exception {
        registerOneChildRootResource();
        ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.MAINTAINER, false);
        ModelNode result = executeForResult(op);
        Map<PathAddress, ModelNode> map = getResourceAccessControl(result, PathAddress.EMPTY_ADDRESS);
        checkResourcePermissions(map, true, true, true, true);
        ModelNode childDesc = getChildDescription(result, ONE);
        map = getResourceAccessControl(childDesc, ONE_ADDR);
        checkResourcePermissions(map, true, true, true, true);
    }
    @Test
    public void testRecursiveReadResourceDefinitionNoSensitivityAsAdministrator() throws Exception {
        registerOneChildRootResource();
        ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.ADMINISTRATOR, false);
        ModelNode result = executeForResult(op);
        Map<PathAddress, ModelNode> map = getResourceAccessControl(result, PathAddress.EMPTY_ADDRESS);
        checkResourcePermissions(map, true, true, true, true);
        ModelNode childDesc = getChildDescription(result, ONE);
        map = getResourceAccessControl(childDesc, ONE_ADDR);
        checkResourcePermissions(map, true, true, true, true);
    }

    @Test
    public void testDirectReadResourceDefinitionNoSensitivityAsMonitor() throws Exception {
        registerOneChildRootResource();
        ModelNode op = createReadResourceDescriptionOperation(ONE_ADDR, StandardRole.MONITOR, false);
        ModelNode result = getWildcardResourceRegistrationResult(executeForResult(op));
        Map<PathAddress, ModelNode> map = getResourceAccessControl(result, ONE_ADDR);
        checkResourcePermissions(map, true, false, true, false);
    }

    @Test
    public void testDirectReadResourceDefinitionNoSensitivityAsMaintainer() throws Exception {
        registerOneChildRootResource();
        ModelNode op = createReadResourceDescriptionOperation(ONE_ADDR, StandardRole.MAINTAINER, false);
        ModelNode result = getWildcardResourceRegistrationResult(executeForResult(op));
        Map<PathAddress, ModelNode> map = getResourceAccessControl(result, ONE_ADDR);
        checkResourcePermissions(map, true, true, true, true);
    }

    @Test
    public void testDirectReadResourceDefinitionNoSensitivityAsAdministrator() throws Exception {
        registerOneChildRootResource();
        ModelNode op = createReadResourceDescriptionOperation(ONE_ADDR, StandardRole.ADMINISTRATOR, false);
        ModelNode result = getWildcardResourceRegistrationResult(executeForResult(op));
        Map<PathAddress, ModelNode> map = getResourceAccessControl(result, ONE_ADDR);
        checkResourcePermissions(map, true, true, true, true);
    }

    @Test
    public void testRecursiveReadResourceDefinitionAccessReadWriteSensitivityAsMonitor() throws Exception {
        registerOneChildRootResource(createSensitivityConstraint("testRecursiveReadResourceDefinitionAccessReadWriteSensitivityAsMonitor", true, true, true));
        ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.MONITOR, false);
        ModelNode result = executeForResult(op);
        Map<PathAddress, ModelNode> map = getResourceAccessControl(result, PathAddress.EMPTY_ADDRESS);
        checkResourcePermissions(map, true, false, true, false);
        ModelNode childDesc = getChildDescription(result, ONE);
        map = getResourceAccessControl(childDesc); //Since access is restricted, the monitor role cannot see any of the resources
    }

    @Test
    public void testRecursiveReadResourceDefinitionAccessReadWriteSensitivityAsMaintainer() throws Exception {
        registerOneChildRootResource(createSensitivityConstraint("testRecursiveReadResourceDefinitionAccessReadWriteSensitivityAsMaintainer", true, true, true));
        ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.MAINTAINER, false);
        ModelNode result = executeForResult(op);
        Map<PathAddress, ModelNode> map = getResourceAccessControl(result, PathAddress.EMPTY_ADDRESS);
        checkResourcePermissions(map, true, true, true, true);
        ModelNode childDesc = getChildDescription(result, ONE);
        map = getResourceAccessControl(childDesc); //Since access is restricted, the maintainer role cannot see any of the resources
    }

    @Test
    public void testRecursiveReadResourceDefinitionAccessReadWriteSensitivityAsAdministrator() throws Exception {
        registerOneChildRootResource(createSensitivityConstraint("testRecursiveReadResourceDefinitionAccessReadWriteSensitivityAsAdministrator", true, true, true));
        ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.ADMINISTRATOR, false);
        ModelNode result = executeForResult(op);
        Map<PathAddress, ModelNode> map = getResourceAccessControl(result, PathAddress.EMPTY_ADDRESS);
        checkResourcePermissions(map, true, true, true, true);
        ModelNode childDesc = getChildDescription(result, ONE);
        map = getResourceAccessControl(childDesc, ONE_ADDR);
        checkResourcePermissions(map, true, true, true, true);
    }

    @Test
    public void testDirectReadResourceDefinitionAccessReadWriteSensitivityAsMonitor() throws Exception {
        registerOneChildRootResource(createSensitivityConstraint("testDirectReadResourceDefinitionAccessReadWriteSensitivityAsMonitor", true, true, true));
        ModelNode op = createReadResourceDescriptionOperation(ONE_ADDR, StandardRole.MONITOR, false);
        ModelNode result = getWildcardResourceRegistrationResult(executeForResult(op));
        Map<PathAddress, ModelNode> map = getResourceAccessControl(result);//Since access is restricted, the monitor role cannot see any of the resources
    }

    @Test
    public void testDirectReadResourceDefinitionAccessReadWriteSensitivityAsMaintainer() throws Exception {
        registerOneChildRootResource(createSensitivityConstraint("testDirectReadResourceDefinitionAccessReadWriteSensitivityAsMaintainer", true, true, true));
        ModelNode op = createReadResourceDescriptionOperation(ONE_ADDR, StandardRole.MAINTAINER, false);
        ModelNode result = getWildcardResourceRegistrationResult(executeForResult(op));
        Map<PathAddress, ModelNode> map = getResourceAccessControl(result);//Since access is restricted, the maintainer role cannot see any of the resources
    }

    @Test
    public void testDirectReadResourceDefinitionAccessReadWriteSensitivityAsAdministrator() throws Exception {
        registerOneChildRootResource(createSensitivityConstraint("testDirectReadResourceDefinitionAccessReadWriteSensitivityAsAdministrator", true, true, true));
        ModelNode op = createReadResourceDescriptionOperation(ONE_ADDR, StandardRole.ADMINISTRATOR, false);
        ModelNode result = getWildcardResourceRegistrationResult(executeForResult(op));
        Map<PathAddress, ModelNode> map = getResourceAccessControl(result, ONE_ADDR);
        checkResourcePermissions(map, true, true, true, true);
    }

    @Test
    public void testRecursiveReadResourceDefinitionReadWriteSensitivityAsMonitor() throws Exception {
        registerOneChildRootResource(createSensitivityConstraint("testRecursiveReadResourceDefinitionReadWriteSensitivityAsMonitor", false, true, true));
        ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.MONITOR, false);
        ModelNode result = executeForResult(op);
        Map<PathAddress, ModelNode> map = getResourceAccessControl(result, PathAddress.EMPTY_ADDRESS);
        checkResourcePermissions(map, true, false, true, false);
        ModelNode childDesc = getChildDescription(result, ONE);
        map = getResourceAccessControl(childDesc, ONE_ADDR);
        //Reads are sensitive so we cannot read them as the default says we can
        checkResourcePermissions(map, false, false, false, false);
    }

    @Test
    public void testRecursiveReadResourceDefinitionReadWriteSensitivityAsMaintainer() throws Exception {
        registerOneChildRootResource(createSensitivityConstraint("testRecursiveReadResourceDefinitionReadWriteSensitivityAsMaintainer", false, true, true));
        ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.MAINTAINER, false);
        ModelNode result = executeForResult(op);
        Map<PathAddress, ModelNode> map = getResourceAccessControl(result, PathAddress.EMPTY_ADDRESS);
        checkResourcePermissions(map, true, true, true, true);
        ModelNode childDesc = getChildDescription(result, ONE);
        map = getResourceAccessControl(childDesc, ONE_ADDR);
        //Reads and writes are sensitive so we cannot read or write them as the default says we can
        checkResourcePermissions(map, false, false, false, false);
    }

    @Test
    public void testRecursiveReadResourceDefinitionReadWriteSensitivityAsAdministrator() throws Exception {
        registerOneChildRootResource(createSensitivityConstraint("testRecursiveReadResourceDefinitionReadWriteSensitivityAsAdministrator", false, true, true));
        ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.ADMINISTRATOR, false);
        ModelNode result = executeForResult(op);
        Map<PathAddress, ModelNode> map = getResourceAccessControl(result, PathAddress.EMPTY_ADDRESS);
        checkResourcePermissions(map, true, true, true, true);
        ModelNode childDesc = getChildDescription(result, ONE);
        map = getResourceAccessControl(childDesc, ONE_ADDR);
        checkResourcePermissions(map, true, true, true, true);
    }

    @Test
    public void testDirectReadResourceDefinitionReadWriteSensitivityAsMonitor() throws Exception {
        registerOneChildRootResource(createSensitivityConstraint("testDirectReadResourceDefinitionReadWriteSensitivityAsMonitor", false, true, true));
        ModelNode op = createReadResourceDescriptionOperation(ONE_ADDR, StandardRole.MONITOR, false);
        ModelNode result = getWildcardResourceRegistrationResult(executeForResult(op));
        Map<PathAddress, ModelNode> map = getResourceAccessControl(result, ONE_ADDR);
        //Reads are sensitive so we cannot read them as the default says we can
        checkResourcePermissions(map, false, false, false, false);
    }

    @Test
    public void testDirectReadResourceDefinitionReadWriteSensitivityAsMaintainer() throws Exception {
        registerOneChildRootResource(createSensitivityConstraint("testDirectReadResourceDefinitionReadWriteSensitivityAsMaintainer", false, true, true));
        ModelNode op = createReadResourceDescriptionOperation(ONE_ADDR, StandardRole.MAINTAINER, false);
        ModelNode result = getWildcardResourceRegistrationResult(executeForResult(op));
        Map<PathAddress, ModelNode> map = getResourceAccessControl(result, ONE_ADDR);
        //Reads and writes are sensitive so we cannot read or write them as the default says we can
        checkResourcePermissions(map, false, false, false, false);
    }

    @Test
    public void testDirectReadResourceDefinitionReadWriteSensitivityAsAdministrator() throws Exception {
        registerOneChildRootResource(createSensitivityConstraint("testDirectReadResourceDefinitionReadWriteSensitivityAsAdministrator", false, true, true));
        ModelNode op = createReadResourceDescriptionOperation(ONE_ADDR, StandardRole.ADMINISTRATOR, false);
        ModelNode result = getWildcardResourceRegistrationResult(executeForResult(op));
        Map<PathAddress, ModelNode> map = getResourceAccessControl(result, ONE_ADDR);
        checkResourcePermissions(map, true, true, true, true);
    }

    @Test
    public void testRecursiveReadResourceDefinitionWriteSensitivityAsMonitor() throws Exception {
        registerOneChildRootResource(createSensitivityConstraint("testRecursiveReadResourceDefinitionWriteSensitivityAsMonitor", false, false, true));
        ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.MONITOR, false);
        ModelNode result = executeForResult(op);
        Map<PathAddress, ModelNode> map = getResourceAccessControl(result, PathAddress.EMPTY_ADDRESS);
        checkResourcePermissions(map, true, false, true, false);
        ModelNode childDesc = getChildDescription(result, ONE);
        map = getResourceAccessControl(childDesc, ONE_ADDR);
        checkResourcePermissions(map, true, false, true, false);

    }

    @Test
    public void testRecursiveReadResourceDefinitionWriteSensitivityAsMaintainer() throws Exception {
        registerOneChildRootResource(createSensitivityConstraint("testRecursiveReadResourceDefinitionWriteSensitivityAsMaintainer", false, false, true));
        ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.MAINTAINER, false);
        ModelNode result = executeForResult(op);
        Map<PathAddress, ModelNode> map = getResourceAccessControl(result, PathAddress.EMPTY_ADDRESS);
        checkResourcePermissions(map, true, true, true, true);
        ModelNode childDesc = getChildDescription(result, ONE);
        map = getResourceAccessControl(childDesc, ONE_ADDR);
        //Writes are sensitive so we cannot write them as the default says we can
        checkResourcePermissions(map, true, false, true, false);

    }

    @Test
    public void testRecursiveReadResourceDefinitionWriteSensitivityAsAdministrator() throws Exception {
        registerOneChildRootResource(createSensitivityConstraint("testRecursiveReadResourceDefinitionWriteSensitivityAsAdministrator", false, false, true));
        ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.ADMINISTRATOR, false);
        ModelNode result = executeForResult(op);
        Map<PathAddress, ModelNode> map = getResourceAccessControl(result, PathAddress.EMPTY_ADDRESS);
        checkResourcePermissions(map, true, true, true, true);
        ModelNode childDesc = getChildDescription(result, ONE);
        map = getResourceAccessControl(childDesc, ONE_ADDR);
        checkResourcePermissions(map, true, true, true, true);
    }

    @Test
    public void testDirectReadResourceDefinitionWriteSensitivityAsMonitor() throws Exception {
        registerOneChildRootResource(createSensitivityConstraint("testDirectReadResourceDefinitionWriteSensitivityAsMonitor", false, false, true));
        ModelNode op = createReadResourceDescriptionOperation(ONE_ADDR, StandardRole.MONITOR, false);
        ModelNode result = getWildcardResourceRegistrationResult(executeForResult(op));
        Map<PathAddress, ModelNode> map = getResourceAccessControl(result, ONE_ADDR);
        //Reads are sensitive so we cannot read them as the default says we can
        checkResourcePermissions(map, true, false, true, false);
    }

    @Test
    public void testDirectReadResourceDefinitionWriteSensitivityAsMaintainer() throws Exception {
        registerOneChildRootResource(createSensitivityConstraint("testDirectReadResourceDefinitionWriteSensitivityAsMaintainer", false, false, true));
        ModelNode op = createReadResourceDescriptionOperation(ONE_ADDR, StandardRole.MAINTAINER, false);
        ModelNode result = getWildcardResourceRegistrationResult(executeForResult(op));
        Map<PathAddress, ModelNode> map = getResourceAccessControl(result, ONE_ADDR);
        //Writes are sensitive so we cannot write them as the default says we can
        checkResourcePermissions(map, true, false, true, false);
    }

    @Test
    public void testDirectReadResourceDefinitionWriteSensitivityAsAdministrator() throws Exception {
        registerOneChildRootResource(createSensitivityConstraint("testDirectReadResourceDefinitionWriteSensitivityAsAdministrator", false, false, true));
        ModelNode op = createReadResourceDescriptionOperation(ONE_ADDR, StandardRole.ADMINISTRATOR, false);
        ModelNode result = getWildcardResourceRegistrationResult(executeForResult(op));
        Map<PathAddress, ModelNode> map = getResourceAccessControl(result, ONE_ADDR);
        checkResourcePermissions(map, true, true, true, true);
    }

    @Test
    public void testRecursiveReadResourceDefinitionReadSensitivityAsMonitor() throws Exception {
        //Does it really make sense to be able to configure that reads are sensitive but not writes?
        registerOneChildRootResource(createSensitivityConstraint("testRecursiveReadResourceDefinitionReadSensitivityAsMonitor", false, true, false));
        ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.MONITOR, false);
        ModelNode result = executeForResult(op);
        Map<PathAddress, ModelNode> map = getResourceAccessControl(result, PathAddress.EMPTY_ADDRESS);
        checkResourcePermissions(map, true, false, true, false);
        ModelNode childDesc = getChildDescription(result, ONE);
        map = getResourceAccessControl(childDesc, ONE_ADDR);
        //Reads are sensitive so we cannot read them as the default says we can
        checkResourcePermissions(map, false, false, false, false);
    }

    @Test
    public void testRecursiveReadResourceDefinitionReadSensitivityAsMaintainer() throws Exception {
        //Does it really make sense to be able to configure that reads are sensitive but not writes?
        registerOneChildRootResource(createSensitivityConstraint("testRecursiveReadResourceDefinitionReadSensitivityAsMaintainer", false, true, false));
        ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.MAINTAINER, false);
        ModelNode result = executeForResult(op);
        Map<PathAddress, ModelNode> map = getResourceAccessControl(result, PathAddress.EMPTY_ADDRESS);
        checkResourcePermissions(map, true, true, true, true);
        ModelNode childDesc = getChildDescription(result, ONE);
        map = getResourceAccessControl(childDesc, ONE_ADDR);
        //Reads are sensitive so we cannot read them as the default says we can
        checkResourcePermissions(map, false, true, false, true);
    }

    @Test
    public void testRecursiveReadResourceDefinitionReadSensitivityAsAdministrator() throws Exception {
        //Does it really make sense to be able to configure that reads are sensitive but not writes?
        registerOneChildRootResource(createSensitivityConstraint("testRecursiveReadResourceDefinitionReadSensitivityAsAdministrator", false, true, false));
        ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.ADMINISTRATOR, false);
        ModelNode result = executeForResult(op);
        Map<PathAddress, ModelNode> map = getResourceAccessControl(result, PathAddress.EMPTY_ADDRESS);
        checkResourcePermissions(map, true, true, true, true);
        ModelNode childDesc = getChildDescription(result, ONE);
        map = getResourceAccessControl(childDesc, ONE_ADDR);
        checkResourcePermissions(map, true, true, true, true);
    }

    @Test
    public void testDirectReadResourceDefinitionReadSensitivityAsMonitor() throws Exception {
        //Does it really make sense to be able to configure that reads are sensitive but not writes?
        registerOneChildRootResource(createSensitivityConstraint("testDirectReadResourceDefinitionReadSensitivityAsMonitor", false, true, false));
        ModelNode op = createReadResourceDescriptionOperation(ONE_ADDR, StandardRole.MONITOR, false);
        ModelNode result = getWildcardResourceRegistrationResult(executeForResult(op));
        Map<PathAddress, ModelNode> map = getResourceAccessControl(result, ONE_ADDR);
        //Reads are sensitive so we cannot read them as the default says we can
        checkResourcePermissions(map, false, false, false, false);
    }

    @Test
    public void testDirectReadResourceDefinitionReadSensitivityAsMaintainer() throws Exception {
        //Does it really make sense to be able to configure that reads are sensitive but not writes?
        registerOneChildRootResource(createSensitivityConstraint("testDirectReadResourceDefinitionReadSensitivityAsMaintainer", false, true, false));
        ModelNode op = createReadResourceDescriptionOperation(ONE_ADDR, StandardRole.MAINTAINER, false);
        ModelNode result = getWildcardResourceRegistrationResult(executeForResult(op));
        Map<PathAddress, ModelNode> map = getResourceAccessControl(result, ONE_ADDR);
        //Reads are sensitive so we cannot read them as the default says we can
        checkResourcePermissions(map, false, true, false, true);
    }

    @Test
    public void testDirectReadResourceDefinitionReadSensitivityAsAdministrator() throws Exception {
        //Does it really make sense to be able to configure that reads are sensitive but not writes?
        registerOneChildRootResource(createSensitivityConstraint("testDirectReadResourceDefinitionReadSensitivityAsAdministrator", false, true, false));
        ModelNode op = createReadResourceDescriptionOperation(ONE_ADDR, StandardRole.ADMINISTRATOR, false);
        ModelNode result = getWildcardResourceRegistrationResult(executeForResult(op));
        Map<PathAddress, ModelNode> map = getResourceAccessControl(result, ONE_ADDR);
        checkResourcePermissions(map, true, true, true, true);
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

    @Test
    public void testAttributeSensitivityAsMonitor() throws Exception {
        registerAttributeResource("testAttributeSensitivityAsMonitor");

        ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.MONITOR, false);
        ModelNode result = executeForResult(op);
        Map<PathAddress, ModelNode> map = getResourceAccessControl(getChildDescription(result, ONE), ONE_ADDR);
        Map<String, ModelNode> attributes = checkAttributeAccessControlNames(map, ATTR_READ_WRITE, ATTR_WRITE, ATTR_READ, ATTR_NONE);
        checkAttributePermissions(attributes, ATTR_READ_WRITE, false, false, null, null);
        checkAttributePermissions(attributes, ATTR_WRITE, true, false, null, null);
        checkAttributePermissions(attributes, ATTR_READ, false, false, null, null);
        checkAttributePermissions(attributes, ATTR_NONE, true, false, null, null);
    }

    @Test
    public void testAttributeSensitivityAsMaintainer() throws Exception {
        registerAttributeResource("testAttributeSensitivityAsMaintainer");

        ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.MAINTAINER, false);
        ModelNode result = executeForResult(op);
        Map<PathAddress, ModelNode> map = getResourceAccessControl(getChildDescription(result, ONE), ONE_ADDR);
        Map<String, ModelNode> attributes = checkAttributeAccessControlNames(map, ATTR_READ_WRITE, ATTR_WRITE, ATTR_READ, ATTR_NONE);
        checkAttributePermissions(attributes, ATTR_READ_WRITE, false, false, null, null);
        checkAttributePermissions(attributes, ATTR_WRITE, true, false, null, null);
        checkAttributePermissions(attributes, ATTR_READ, false, true, null, null);
        checkAttributePermissions(attributes, ATTR_NONE, true, true, null, null);
    }

    @Test
    public void testAttributeSensitivityAsAdministrator() throws Exception {
        registerAttributeResource("testAttributeSensitivityAsAdministrator");

        ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.ADMINISTRATOR, false);
        ModelNode result = executeForResult(op);
        Map<PathAddress, ModelNode> map = getResourceAccessControl(getChildDescription(result, ONE), ONE_ADDR);
        Map<String, ModelNode> attributes = checkAttributeAccessControlNames(map, ATTR_ACCESS_READ_WRITE, ATTR_READ_WRITE, ATTR_WRITE, ATTR_READ, ATTR_NONE);
        checkAttributePermissions(attributes, ATTR_ACCESS_READ_WRITE, true, true, null, null);
        checkAttributePermissions(attributes, ATTR_READ_WRITE, true, true, null, null);
        checkAttributePermissions(attributes, ATTR_WRITE, true, true, null, null);
        checkAttributePermissions(attributes, ATTR_READ, true, true, null, null);
        checkAttributePermissions(attributes, ATTR_NONE, true, true, null, null);
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

    @Test
    public void testOperationSensitivityAsMonitor() throws Exception {
        registerOperationResource("testOperationSensitivityAsMonitor");
        ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.MONITOR, true);
        ModelNode result = executeForResult(op);
        //TODO check root resource ops
        Map<PathAddress, ModelNode> map = getResourceAccessControl(getChildDescription(result, ONE), ONE_ADDR);
        Map<String, Boolean> operations = checkOperationAccessControlNames(map, ADD, REMOVE,
                OP_CONFIG_RW_READ_WRITE, OP_CONFIG_RW_WRITE, OP_CONFIG_RW_READ, OP_CONFIG_RW_NONE,
                OP_RUNTIME_RW_READ_WRITE, OP_RUNTIME_RW_WRITE, OP_RUNTIME_RW_READ, OP_RUNTIME_RW_NONE,
                OP_CONFIG_RO_READ_WRITE, OP_CONFIG_RO_WRITE, OP_CONFIG_RO_READ, OP_CONFIG_RO_NONE,
                OP_RUNTIME_RO_READ_WRITE, OP_RUNTIME_RO_WRITE, OP_RUNTIME_RO_READ, OP_RUNTIME_RO_NONE);
        Assert.assertEquals(false, operations.get(ADD));
        Assert.assertEquals(false, operations.get(REMOVE));
        Assert.assertEquals(false, operations.get(OP_CONFIG_RW_READ_WRITE));
        Assert.assertEquals(false, operations.get(OP_CONFIG_RW_WRITE));
        Assert.assertEquals(false, operations.get(OP_CONFIG_RW_READ));
        Assert.assertEquals(false, operations.get(OP_CONFIG_RW_NONE));
        Assert.assertEquals(false, operations.get(OP_RUNTIME_RW_READ_WRITE));
        Assert.assertEquals(false, operations.get(OP_RUNTIME_RW_WRITE));
        Assert.assertEquals(false, operations.get(OP_RUNTIME_RW_READ));
        Assert.assertEquals(false, operations.get(OP_RUNTIME_RW_NONE));
        Assert.assertEquals(false, operations.get(OP_CONFIG_RO_READ_WRITE));
        //Although this has a write sensitivity, the operation is read-only so the sensitivity should not be relevant
        Assert.assertEquals(true, operations.get(OP_CONFIG_RO_WRITE));
        Assert.assertEquals(false, operations.get(OP_CONFIG_RO_READ));
        Assert.assertEquals(true, operations.get(OP_CONFIG_RO_NONE));
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
        Map<PathAddress, ModelNode> map = getResourceAccessControl(getChildDescription(result, ONE), ONE_ADDR);
        Map<String, Boolean> operations = checkOperationAccessControlNames(map, ADD, REMOVE,
                OP_CONFIG_RW_READ_WRITE, OP_CONFIG_RW_WRITE, OP_CONFIG_RW_READ, OP_CONFIG_RW_NONE,
                OP_RUNTIME_RW_READ_WRITE, OP_RUNTIME_RW_WRITE, OP_RUNTIME_RW_READ, OP_RUNTIME_RW_NONE,
                OP_CONFIG_RO_READ_WRITE, OP_CONFIG_RO_WRITE, OP_CONFIG_RO_READ, OP_CONFIG_RO_NONE,
                OP_RUNTIME_RO_READ_WRITE, OP_RUNTIME_RO_WRITE, OP_RUNTIME_RO_READ, OP_RUNTIME_RO_NONE);
        Assert.assertEquals(true, operations.get(ADD));
        Assert.assertEquals(true, operations.get(REMOVE));
        Assert.assertEquals(false, operations.get(OP_CONFIG_RW_READ_WRITE));
        Assert.assertEquals(false, operations.get(OP_CONFIG_RW_WRITE));
        Assert.assertEquals(false, operations.get(OP_CONFIG_RW_READ));
        Assert.assertEquals(true, operations.get(OP_CONFIG_RW_NONE));
        Assert.assertEquals(false, operations.get(OP_RUNTIME_RW_READ_WRITE));
        Assert.assertEquals(false, operations.get(OP_RUNTIME_RW_WRITE));
        Assert.assertEquals(false, operations.get(OP_RUNTIME_RW_READ));
        Assert.assertEquals(true, operations.get(OP_RUNTIME_RW_NONE));
        Assert.assertEquals(false, operations.get(OP_CONFIG_RO_READ_WRITE));
        //Although this has a write sensitivity, the operation is read-only so the sensitivity should not be relevant
        Assert.assertEquals(true, operations.get(OP_CONFIG_RO_WRITE));
        Assert.assertEquals(false, operations.get(OP_CONFIG_RO_READ));
        Assert.assertEquals(true, operations.get(OP_CONFIG_RO_NONE));
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
        Map<PathAddress, ModelNode> map = getResourceAccessControl(getChildDescription(result, ONE), ONE_ADDR);
        Map<String, Boolean> operations = checkOperationAccessControlNames(map, ADD, REMOVE,
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

    @Test
    public void testDeepRecursiveReadResourceDefinitionAccessReadWriteSensitivityAsMonitor() throws Exception {
        SensitiveTargetAccessConstraintDefinition constraint = createSensitivityConstraint("testDeepRecursiveReadResourceDefinitionAccessReadWriteSensitivityAsMonitor", true, true, true);
        registerDeepResource(constraint);

        ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.MONITOR, false);
        ModelNode result = executeForResult(op);
        Map<PathAddress, ModelNode> map = getResourceAccessControl(result, PathAddress.EMPTY_ADDRESS);
        checkResourcePermissions(map, true, false, true, false);
        ModelNode childDesc = getChildDescription(result, ONE);
        map = getResourceAccessControl(childDesc); //Since access is restricted, the monitor role cannot see any of the resources
        childDesc = getChildDescription(childDesc, TWO);
        map = getResourceAccessControl(childDesc); //Since access is restricted, the monitor role cannot see any of the resources
    }

    @Test
    public void testDeepRecursiveReadResourceDefinitionAccessReadWriteSensitivityAsMaintainer() throws Exception {
        SensitiveTargetAccessConstraintDefinition constraint = createSensitivityConstraint("testDeepRecursiveReadResourceDefinitionAccessReadWriteSensitivityAsMaintainer", true, true, true);
        registerDeepResource(constraint);

        ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.MAINTAINER, false);
        ModelNode result = executeForResult(op);
        Map<PathAddress, ModelNode> map = getResourceAccessControl(result, PathAddress.EMPTY_ADDRESS);
        checkResourcePermissions(map, true, true, true, true);
        ModelNode childDesc = getChildDescription(result, ONE);
        map = getResourceAccessControl(childDesc); //Since access is restricted, the maintainer role cannot see any of the resources
        childDesc = getChildDescription(childDesc, TWO);
        map = getResourceAccessControl(childDesc); //Since access is restricted, the maintainer role cannot see any of the resources
    }

    @Test
    public void testDeepRecursiveReadResourceDefinitionAccessReadWriteSensitivityAsAdministrator() throws Exception {
        SensitiveTargetAccessConstraintDefinition constraint = createSensitivityConstraint("testDeepRecursiveReadResourceDefinitionAccessReadWriteSensitivityAsAdministrator", true, true, true);
        registerDeepResource(constraint);

        ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.ADMINISTRATOR, false);
        ModelNode result = executeForResult(op);
        Map<PathAddress, ModelNode> map = getResourceAccessControl(result, PathAddress.EMPTY_ADDRESS);
        checkResourcePermissions(map, true, true, true, true);
        ModelNode childDesc = getChildDescription(result, ONE);
        map = getResourceAccessControl(childDesc, ONE_ADDR);
        checkResourcePermissions(map, true, true, true, true);
        childDesc = getChildDescription(childDesc, TWO);
        map = getResourceAccessControl(childDesc, ONE_TWO_ADDR);
        checkResourcePermissions(map, true, true, true, true);
    }

    @Test
    public void testDeepDirectLevelOneReadResourceDefinitionAccessReadWriteSensitivityAsMonitor() throws Exception {
        SensitiveTargetAccessConstraintDefinition constraint = createSensitivityConstraint("testDeepLevelOneReadResourceDefinitionAccessReadWriteSensitivityAsMonitor", true, true, true);
        registerDeepResource(constraint);

        ModelNode op = createReadResourceDescriptionOperation(ONE_ADDR, StandardRole.MONITOR, false);
        ModelNode result = getWildcardResourceRegistrationResult(executeForResult(op));
        Map<PathAddress, ModelNode> map = getResourceAccessControl(result); //Since access is restricted, the monitor role cannot see any of the resources
        ModelNode childDesc = getChildDescription(result, TWO);
        map = getResourceAccessControl(childDesc); //Since access is restricted, the monitor role cannot see any of the resources
    }

    @Test
    public void testDeepDirectLevelOneReadResourceDefinitionAccessReadWriteSensitivityAsMaintainer() throws Exception {
        SensitiveTargetAccessConstraintDefinition constraint = createSensitivityConstraint("testDeepLevelOneReadResourceDefinitionAccessReadWriteSensitivityAsMaintainer", true, true, true);
        registerDeepResource(constraint);

        ModelNode op = createReadResourceDescriptionOperation(ONE_ADDR, StandardRole.MAINTAINER, false);
        ModelNode result = getWildcardResourceRegistrationResult(executeForResult(op));
        Map<PathAddress, ModelNode> map = getResourceAccessControl(result); //Since access is restricted, the monitor role cannot see any of the resources
        ModelNode childDesc = getChildDescription(result, TWO);
        map = getResourceAccessControl(childDesc); //Since access is restricted, the monitor role cannot see any of the resources
    }

    @Test
    public void testDeepDirectLevelOneReadResourceDefinitionAccessReadWriteSensitivityAsAdministrator() throws Exception {
        SensitiveTargetAccessConstraintDefinition constraint = createSensitivityConstraint("testDeepLevelOneReadResourceDefinitionAccessReadWriteSensitivityAsAdministrator", true, true, true);
        registerDeepResource(constraint);

        ModelNode op = createReadResourceDescriptionOperation(ONE_ADDR, StandardRole.ADMINISTRATOR, false);
        ModelNode result = getWildcardResourceRegistrationResult(executeForResult(op));
        Map<PathAddress, ModelNode> map = getResourceAccessControl(result, ONE_ADDR);
        checkResourcePermissions(map, true, true, true, true);
        ModelNode childDesc = getChildDescription(result, TWO);
        map = getResourceAccessControl(childDesc, ONE_TWO_ADDR);
        checkResourcePermissions(map, true, true, true, true);
    }

    @Test
    public void testDeepDirectLevelTwoReadResourceDefinitionAccessReadWriteSensitivityAsMonitor() throws Exception {
        SensitiveTargetAccessConstraintDefinition constraint = createSensitivityConstraint("testDeepDirectLevelTwoReadResourceDefinitionAccessReadWriteSensitivityAsMonitor", true, true, true);
        registerDeepResource(constraint);

        ModelNode op = createReadResourceDescriptionOperation(TWO_ADDR, StandardRole.MONITOR, false);
        ModelNode result = getWildcardResourceRegistrationResult(executeForResult(op));
        Map<PathAddress, ModelNode> map = getResourceAccessControl(result);//Since access is restricted, the monitor role cannot see any of the resources
    }

    @Test
    public void testDeepDirectLevelTwoReadResourceDefinitionAccessReadWriteSensitivityAsMaintainer() throws Exception {
        SensitiveTargetAccessConstraintDefinition constraint = createSensitivityConstraint("testDeepDirectLevelTwoReadResourceDefinitionAccessReadWriteSensitivityAsMaintainer", true, true, true);
        registerDeepResource(constraint);

        ModelNode op = createReadResourceDescriptionOperation(TWO_ADDR, StandardRole.MAINTAINER, false);
        ModelNode result = getWildcardResourceRegistrationResult(executeForResult(op));
        Map<PathAddress, ModelNode> map = getResourceAccessControl(result);//Since access is restricted, the maintainer role cannot see any of the resources
    }

    @Test
    public void testDeepDirectLevelTwoReadResourceDefinitionAccessReadWriteSensitivityAsAdministrator() throws Exception {
        SensitiveTargetAccessConstraintDefinition constraint = createSensitivityConstraint("testDeepDirectLevelTwoReadResourceDefinitionAccessReadWriteSensitivityAsAdministrator", true, true, true);
        registerDeepResource(constraint);

        ModelNode op = createReadResourceDescriptionOperation(TWO_ADDR, StandardRole.ADMINISTRATOR, false);
        ModelNode result = getWildcardResourceRegistrationResult(executeForResult(op));
        Map<PathAddress, ModelNode> map = getResourceAccessControl(result, ONE_TWO_ADDR);
        checkResourcePermissions(map, true, true, true, true);
    }

    @Test
    public void testDeepRecursiveReadResourceDefinitionReadWriteSensitivityAsMonitor() throws Exception {
        SensitiveTargetAccessConstraintDefinition constraint = createSensitivityConstraint("testDeepRecursiveReadResourceDefinitionReadWriteSensitivityAsMonitor", false, true, true);
        registerDeepResource(constraint);

        ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.MONITOR, false);
        ModelNode result = executeForResult(op);
        Map<PathAddress, ModelNode> map = getResourceAccessControl(result, PathAddress.EMPTY_ADDRESS);
        checkResourcePermissions(map, true, false, true, false);
        ModelNode childDesc = getChildDescription(result, ONE);
        map = getResourceAccessControl(childDesc, ONE_ADDR);
        checkResourcePermissions(map, false, false, false, false);
        childDesc = getChildDescription(childDesc, TWO);
        map = getResourceAccessControl(childDesc, ONE_TWO_ADDR);
        checkResourcePermissions(map, false, false, false, false);
    }

    @Test
    public void testDeepRecursiveReadResourceDefinitionReadWriteSensitivityAsMaintainer() throws Exception {
        SensitiveTargetAccessConstraintDefinition constraint = createSensitivityConstraint("testDeepRecursiveReadResourceDefinitionReadWriteSensitivityAsMaintainer", false, true, true);
        registerDeepResource(constraint);

        ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.MAINTAINER, false);
        ModelNode result = executeForResult(op);
        Map<PathAddress, ModelNode> map = getResourceAccessControl(result, PathAddress.EMPTY_ADDRESS);
        checkResourcePermissions(map, true, true, true, true);
        ModelNode childDesc = getChildDescription(result, ONE);
        map = getResourceAccessControl(childDesc, ONE_ADDR);
        checkResourcePermissions(map, false, false, false, false);
        childDesc = getChildDescription(childDesc, TWO);
        map = getResourceAccessControl(childDesc, ONE_TWO_ADDR);
        checkResourcePermissions(map, false, false, false, false);
    }

    @Test
    public void testDeepRecursiveReadResourceDefinitionReadWriteSensitivityAsAdministrator() throws Exception {
        SensitiveTargetAccessConstraintDefinition constraint = createSensitivityConstraint("testDeepRecursiveReadResourceDefinitionReadWriteSensitivityAsAdministrator", false, true, true);
        registerDeepResource(constraint);

        ModelNode op = createReadResourceDescriptionOperation(PathAddress.EMPTY_ADDRESS, StandardRole.ADMINISTRATOR, false);
        ModelNode result = executeForResult(op);
        Map<PathAddress, ModelNode> map = getResourceAccessControl(result, PathAddress.EMPTY_ADDRESS);
        checkResourcePermissions(map, true, true, true, true);
        ModelNode childDesc = getChildDescription(result, ONE);
        map = getResourceAccessControl(childDesc, ONE_ADDR);
        checkResourcePermissions(map, true, true, true, true);
        childDesc = getChildDescription(childDesc, TWO);
        map = getResourceAccessControl(childDesc, ONE_TWO_ADDR);
        checkResourcePermissions(map, true, true, true, true);
    }

    @Test
    public void testDeepDirectLevelOneReadResourceDefinitionReadWriteSensitivityAsMonitor() throws Exception {
        SensitiveTargetAccessConstraintDefinition constraint = createSensitivityConstraint("testDeepDirectLevelOneReadResourceDefinitionReadWriteSensitivityAsMonitor", false, true, true);
        registerDeepResource(constraint);
        ModelNode op = createReadResourceDescriptionOperation(ONE_ADDR, StandardRole.MONITOR, false);
        ModelNode result = getWildcardResourceRegistrationResult(executeForResult(op));
        Map<PathAddress, ModelNode> map = getResourceAccessControl(result, ONE_ADDR);
        checkResourcePermissions(map, false, false, false, false);
        ModelNode childDesc = getChildDescription(result, TWO);
        map = getResourceAccessControl(childDesc, ONE_TWO_ADDR);
        checkResourcePermissions(map, false, false, false, false);
    }

    @Test
    public void testDeepDirectLevelOneReadResourceDefinitionReadWriteSensitivityAsMaintainer() throws Exception {
        SensitiveTargetAccessConstraintDefinition constraint = createSensitivityConstraint("testDeepDirectLevelOneReadResourceDefinitionReadWriteSensitivityAsMaintainer", false, true, true);
        registerDeepResource(constraint);
        ModelNode op = createReadResourceDescriptionOperation(ONE_ADDR, StandardRole.MAINTAINER, false);
        ModelNode result = getWildcardResourceRegistrationResult(executeForResult(op));
        Map<PathAddress, ModelNode> map = getResourceAccessControl(result, ONE_ADDR);
        checkResourcePermissions(map, false, false, false, false);
        ModelNode childDesc = getChildDescription(result, TWO);
        map = getResourceAccessControl(childDesc, ONE_TWO_ADDR);
        checkResourcePermissions(map, false, false, false, false);
    }

    @Test
    public void testDeepDirectLevelOneReadResourceDefinitionReadWriteSensitivityAsAdministrator() throws Exception {
        SensitiveTargetAccessConstraintDefinition constraint = createSensitivityConstraint("testDeepDirectLevelOneReadResourceDefinitionReadWriteSensitivityAsAdministrator", false, true, true);
        registerDeepResource(constraint);
        ModelNode op = createReadResourceDescriptionOperation(ONE_ADDR, StandardRole.ADMINISTRATOR, false);
        ModelNode result = getWildcardResourceRegistrationResult(executeForResult(op));
        Map<PathAddress, ModelNode> map = getResourceAccessControl(result, ONE_ADDR);
        checkResourcePermissions(map, true, true, true, true);
        ModelNode childDesc = getChildDescription(result, TWO);
        map = getResourceAccessControl(childDesc, ONE_TWO_ADDR);
        checkResourcePermissions(map, true, true, true, true);
    }

    @Test
    public void testDeepDirectLevelTwoReadResourceDefinitionReadWriteSensitivityAsMonitor() throws Exception {
        SensitiveTargetAccessConstraintDefinition constraint = createSensitivityConstraint("testDeepDirectLevelTwoReadResourceDefinitionReadWriteSensitivityAsMonitor", false, true, true);
        registerDeepResource(constraint);
        ModelNode op = createReadResourceDescriptionOperation(TWO_ADDR, StandardRole.MONITOR, false);
        ModelNode result = getWildcardResourceRegistrationResult(executeForResult(op));
        Map<PathAddress, ModelNode> map = getResourceAccessControl(result, ONE_TWO_ADDR);
        checkResourcePermissions(map, false, false, false, false);
    }

    @Test
    public void testDeepDirectLevelTwoReadResourceDefinitionReadWriteSensitivityAsMaintainer() throws Exception {
        SensitiveTargetAccessConstraintDefinition constraint = createSensitivityConstraint("testDeepDirectLevelTwoReadResourceDefinitionReadWriteSensitivityAsMaintainer", false, true, true);
        registerDeepResource(constraint);
        ModelNode op = createReadResourceDescriptionOperation(TWO_ADDR, StandardRole.MAINTAINER, false);
        ModelNode result = getWildcardResourceRegistrationResult(executeForResult(op));
        Map<PathAddress, ModelNode> map = getResourceAccessControl(result, ONE_TWO_ADDR);
        checkResourcePermissions(map, false, false, false, false);
    }

    @Test
    public void testDeepDirectLevelTwoReadResourceDefinitionReadWriteSensitivityAsAdministrator() throws Exception {
        SensitiveTargetAccessConstraintDefinition constraint = createSensitivityConstraint("testDeepDirectLevelTwoReadResourceDefinitionReadWriteSensitivityAsAdministrator", false, true, true);
        registerDeepResource(constraint);
        ModelNode op = createReadResourceDescriptionOperation(TWO_ADDR, StandardRole.ADMINISTRATOR, false);
        ModelNode result = getWildcardResourceRegistrationResult(executeForResult(op));
        Map<PathAddress, ModelNode> map = getResourceAccessControl(result, ONE_TWO_ADDR);
        checkResourcePermissions(map, true, true, true, true);
    }

    private SensitiveTargetAccessConstraintDefinition createSensitivityConstraint(String name, boolean access, boolean read, boolean write) {
        SensitivityClassification classification = new SensitivityClassification("test", name, access, read, write);
        return new SensitiveTargetAccessConstraintDefinition(classification);
    }

    private ModelNode getChildDescription(ModelNode description, PathElement element) {
        ModelNode childDesc = description.get(CHILDREN, element.getKey(), MODEL_DESCRIPTION, element.getValue());
        Assert.assertTrue(childDesc.isDefined());
        return childDesc;
    }

    private Map<PathAddress, ModelNode> getResourceAccessControl(ModelNode description, PathAddress...addresses){
        Assert.assertTrue(description.hasDefined(ACCESS_CONTROL));
        Map<PathAddress, ModelNode> map = new HashMap<PathAddress, ModelNode>();
        for (Property prop : description.get(ACCESS_CONTROL).asPropertyList()) {
            if (prop.getValue().isDefined()) {
                //If it was not defined it means access was denied
                //TODO a better way to handle this case
                map.put(PathAddress.pathAddress(ModelNode.fromString(prop.getName())), prop.getValue());
            }
        }
        List<PathAddress> expected = Arrays.asList(addresses);
        Assert.assertTrue(map.keySet().containsAll(expected));
        Assert.assertTrue(expected.containsAll(map.keySet()));
        return map;
    }

    private Map<String, ModelNode> checkAttributeAccessControlNames(Map<PathAddress, ModelNode> map, String...attributeNames) {
        Map<String, ModelNode> attributeMap = null;
        for (ModelNode accessControl : map.values()) {
            Map<String, ModelNode> currentMap = checkAttributeAccessControlNames(accessControl, attributeNames);
            if (attributeMap == null) {
                attributeMap = currentMap;
            } else {
                Assert.assertEquals(attributeMap, currentMap);
            }
        }
        return attributeMap;
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

    private void checkAttributePermissions(Map<String, ModelNode> map, String attrName, Boolean readConfig, Boolean writeConfig, Boolean readRuntime, Boolean writeRuntime) {
        ModelNode attr = map.get(attrName);
        Assert.assertNotNull(attr);
        assertEqualsOrNotDefined(attr, ActionEffect.READ_CONFIG.toString(), readConfig);
        assertEqualsOrNotDefined(attr, ActionEffect.WRITE_CONFIG.toString(), writeConfig);
        assertEqualsOrNotDefined(attr, ActionEffect.READ_RUNTIME.toString(), readRuntime);
        assertEqualsOrNotDefined(attr, ActionEffect.WRITE_RUNTIME.toString(), writeRuntime);
    }

    private Map<String, Boolean> checkOperationAccessControlNames(Map<PathAddress, ModelNode> map, String...operationNames) {
        Map<String, Boolean> operationMap = null;
        for (ModelNode accessControl : map.values()) {
            Map<String, Boolean> currentMap = checkOperationAccessControlNames(accessControl, operationNames);
            if (operationMap == null) {
                operationMap = currentMap;
            } else {
                Assert.assertEquals(operationMap, currentMap);
            }
        }
        return operationMap;
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

    private void checkResourcePermissions(Map<PathAddress, ModelNode> map, boolean readConfig, boolean writeConfig, boolean readRuntime, boolean writeRuntime) {
        for (ModelNode accessControl : map.values()) {
            checkResourcePermissions(accessControl, readConfig, writeConfig, readRuntime, writeRuntime);
        }
    }

    private void checkResourcePermissions(ModelNode accessControl, boolean readConfig, boolean writeConfig, boolean readRuntime, boolean writeRuntime) {
        Assert.assertEquals(readConfig, accessControl.get(ActionEffect.READ_CONFIG.toString()).asBoolean());
        Assert.assertEquals(writeConfig, accessControl.get(ActionEffect.WRITE_CONFIG.toString()).asBoolean());
        Assert.assertEquals(readRuntime, accessControl.get(ActionEffect.READ_RUNTIME.toString()).asBoolean());
        Assert.assertEquals(writeRuntime, accessControl.get(ActionEffect.WRITE_RUNTIME.toString()).asBoolean());
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
        op.get(ACCESS_CONTROL).set(true);
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
}
