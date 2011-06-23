/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ACCESS_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILD_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_RUNTIME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_LENGTH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_OCCURS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODEL_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_RESOURCES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_TYPES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.common.CommonProviders;
import org.jboss.as.controller.operations.global.GlobalOperationHandlers;
import org.jboss.as.controller.operations.global.WriteAttributeHandlers;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.AttributeAccess.AccessType;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.junit.Test;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class GlobalOperationsTestCase extends AbstractControllerTestBase {

    public static DescriptionProvider rootDescriptionProvider = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            ModelNode node = new ModelNode();
            node.get(DESCRIPTION).set("The root node of the test management API");
            node.get(CHILDREN, PROFILE, DESCRIPTION).set("A list of profiles");
            node.get(CHILDREN, PROFILE, MIN_OCCURS).set(1);
            node.get(CHILDREN, PROFILE, MODEL_DESCRIPTION);
            return node;
        }
    };

    public static ModelNode createTestNode() {
        ModelNode model = new ModelNode();

        return model;
    }

    @Test
    public void testRecursiveRead() throws Exception {
        ModelNode operation = createOperation(READ_RESOURCE_OPERATION);
        operation.get(RECURSIVE).set(true);

        ModelNode result = executeForResult(operation);
        assertTrue(result.hasDefined("profile"));
        assertTrue(result.get("profile").hasDefined("profileA"));
    }

    @Test
    public void testRecursiveReadSubModelOperationSimple() throws Exception {
        ModelNode operation = createOperation(READ_RESOURCE_OPERATION, "profile", "profileA", "subsystem", "subsystem1");
        operation.get(RECURSIVE).set(true);

        ModelNode result = executeForResult(operation);
        assertNotNull(result);
        checkRecursiveSubsystem1(result);
        assertFalse(result.get("metric1").isDefined());

        // Query runtime metrics
        operation = createOperation(READ_RESOURCE_OPERATION, "profile", "profileA", "subsystem", "subsystem1");
        operation.get(INCLUDE_RUNTIME).set(true);
        result = executeForResult(operation);

        assertTrue(result.get("metric1").isDefined());
        assertTrue(result.get("metric2").isDefined());
    }

    @Test
    public void testNonRecursiveReadSubModelOperationSimple() throws Exception {
        ModelNode operation = createOperation(READ_RESOURCE_OPERATION, "profile", "profileA", "subsystem", "subsystem1");
        operation.get(RECURSIVE).set(false);

        ModelNode result = executeForResult(operation);
        assertNotNull(result);

        checkNonRecursiveSubsystem1(result);
    }

    @Test
    public void testRecursiveReadSubModelOperationComplex() throws Exception {
        ModelNode operation = createOperation(READ_RESOURCE_OPERATION, "profile", "profileA", "subsystem", "subsystem2");
        operation.get(RECURSIVE).set(true);


        ModelNode result = executeForResult(operation);
        assertNotNull(result);
        checkRecursiveSubsystem2(result);
    }

    @Test
    public void testReadAttributeValue() throws Exception {
        ModelNode operation = createOperation(READ_ATTRIBUTE_OPERATION, "profile", "profileA", "subsystem", "subsystem2");

        operation.get(NAME).set("int");
        ModelNode result = executeForResult(operation);
        assertNotNull(result);
        assertEquals(ModelType.INT, result.getType());
        assertEquals(102, result.asInt());

        operation.get(NAME).set("string1");
        result = executeForResult(operation);
        assertNotNull(result);
        assertEquals(ModelType.STRING, result.getType());
        assertEquals("s1", result.asString());

        operation.get(NAME).set("list");
        result = executeForResult(operation);
        assertNotNull(result);
        assertEquals(ModelType.LIST, result.getType());
        List<ModelNode> list = result.asList();
        assertEquals(2, list.size());
        assertEquals("l1A", list.get(0).asString());
        assertEquals("l1B", list.get(1).asString());

        operation.get(NAME).set("non-existant-attribute");
        try {
            result = executeForResult(operation);
            fail("Expected error for non-existant attribute");
        } catch (OperationFailedException expected) {
        }

        operation.get(NAME).set("string2");
        result = executeForResult(operation);
        assertNotNull(result);
        assertEquals(ModelType.STRING, result.getType());
        assertEquals("s2", result.asString());

        operation = createOperation(READ_ATTRIBUTE_OPERATION, "profile", "profileC", "subsystem", "subsystem4");
        operation.get(NAME).set("name");
        result = executeForResult(operation);
        assertNotNull(result);
        assertFalse(result.isDefined());

        operation = createOperation(READ_ATTRIBUTE_OPERATION, "profile", "profileC", "subsystem", "subsystem5");
        operation.get(NAME).set("name");
        result = executeForResult(operation);
        assertNotNull(result);
        assertEquals("Overridden by special read handler", result.asString());

        operation = createOperation(READ_ATTRIBUTE_OPERATION, "profile", "profileA", "subsystem", "subsystem1");
        operation.get(NAME).set("metric1");
        result = executeForResult(operation);
        assertNotNull(result);
        assertEquals(ModelType.INT, result.getType());
    }

    @Test
    public void testWriteAttributeValue() throws Exception {
        ModelNode read = createOperation(READ_ATTRIBUTE_OPERATION, "profile", "profileA", "subsystem", "subsystem2");
        read.get(NAME).set("long");
        ModelNode result = executeForResult(read);
        assertEquals(ModelType.LONG, result.getType());
        long original = result.asLong();

        ModelNode write = createOperation(WRITE_ATTRIBUTE_OPERATION, "profile", "profileA", "subsystem", "subsystem2");
        write.get(NAME).set("long");
        try {
            write.get(VALUE).set(99999L);
            executeForResult(write);

            result = executeForResult(read);
            assertEquals(ModelType.LONG, result.getType());
            assertEquals(99999L, result.asLong());

            write.get(VALUE).set("Not Valid");
            try {
                executeForResult(write);
                fail("Expected error setting long property to string");
            } catch (Exception expected) {
            }

            //TODO How to set a value to null?
        } finally {
            write.get(VALUE).set(original);
            executeForResult(write);
            result = executeForResult(read);
            assertEquals(ModelType.LONG, result.getType());
            assertEquals(original, result.asLong());
        }

        write.get(NAME).set("string1");
        write.get(VALUE).set("Hello");
        try {
            executeForResult(write);
            fail("Expected error setting property with no write handler");
        } catch (Exception expected) {
        }

    }


    @Test
    public void testReadChildrenNames() throws Exception {
        ModelNode operation = createOperation(READ_CHILDREN_NAMES_OPERATION, "profile", "profileA");
        operation.get(CHILD_TYPE).set("subsystem");

        ModelNode result = executeForResult(operation);
        assertNotNull(result);
        assertEquals(ModelType.LIST, result.getType());
        assertEquals(2, result.asList().size());
        List<String> names =  modelNodeListToStringList(result.asList());
        assertTrue(names.contains("subsystem1"));
        assertTrue(names.contains("subsystem2"));

        operation = createOperation(READ_CHILDREN_NAMES_OPERATION, "profile", "profileA", "subsystem", "subsystem1");
        operation.get(CHILD_TYPE).set("type2");
        result = executeForResult(operation);
        assertNotNull(result);
        assertEquals(ModelType.LIST, result.getType());
        names = modelNodeListToStringList(result.asList());
        assertEquals(1, names.size());
        assertTrue(names.contains("other"));


        operation.get(CHILD_TYPE).set("non-existant-child");
        try {
            result = executeForResult(operation);
            fail("Expected error for non-existant child");
        } catch (OperationFailedException expected) {
        }

        operation = createOperation(READ_CHILDREN_NAMES_OPERATION, "profile", "profileC", "subsystem", "subsystem4");
        operation.get(CHILD_TYPE).set("type1");
        // BES 2011/06/06 These assertions make no sense; as they are no different from "non-existant-child" case
        // Replacing with a fail check
//          result = executeForResult(operation);
//        assertNotNull(result);
//        assertEquals(ModelType.LIST, result.getType());
//        assertTrue(result.asList().isEmpty());
        try {
            result = executeForResult(operation);
            fail("Expected error for type1 child under subsystem4");
        } catch (OperationFailedException expected) {
        }

        operation = createOperation(READ_CHILDREN_NAMES_OPERATION, "profile", "profileC", "subsystem", "subsystem5");
        operation.get(CHILD_TYPE).set("type1");
        // BES 2011/06/06 see comment above
        result = executeForResult(operation);
        assertNotNull(result);
        assertEquals(ModelType.LIST, result.getType());
        assertTrue(result.asList().isEmpty());
//        try {
//            result = executeForResult(operation);
//            fail("Expected error for type1 child under subsystem5");
//        } catch (OperationFailedException expected) {
//        }
    }

    @Test
    public void testReadChildrenTypes() throws Exception {
        ModelNode operation = createOperation(READ_CHILDREN_TYPES_OPERATION, "profile", "profileA");

        ModelNode result = executeForResult(operation);
        assertNotNull(result);
        assertEquals(ModelType.LIST, result.getType());
        assertEquals(1, result.asList().size());
        assertEquals("subsystem", result.asList().get(0).asString());

        operation = createOperation(READ_CHILDREN_TYPES_OPERATION, "profile", "profileA", "subsystem", "subsystem1");
        result = executeForResult(operation);
        assertNotNull(result);
        assertEquals(ModelType.LIST, result.getType());
        assertEquals(2, result.asList().size());
        List<String> stringList = modelNodeListToStringList(result.asList());
        assertTrue(Arrays.asList("type1", "type2").containsAll(stringList));


        operation = createOperation(READ_CHILDREN_TYPES_OPERATION, "profile", "profileA", "subsystem", "non-existant");
        try {
            result = executeForResult(operation);
            fail("Expected error for non-existant child");
        } catch (OperationFailedException expected) {
        }

        operation = createOperation(READ_CHILDREN_TYPES_OPERATION, "profile", "profileC", "subsystem", "subsystem4");
        result = executeForResult(operation);
        assertNotNull(result);
        assertEquals(ModelType.LIST, result.getType());
        assertTrue(result.asList().isEmpty());

        operation = createOperation(READ_CHILDREN_TYPES_OPERATION, "profile", "profileC", "subsystem", "subsystem5");
        result = executeForResult(operation);
        assertNotNull(result);
        assertEquals(ModelType.LIST, result.getType());
        assertEquals(1, result.asList().size());
        assertEquals("type1", result.asList().get(0).asString());
    }

    @Test
    public void testReadChildrenResources() throws Exception {
        ModelNode operation = createOperation(READ_CHILDREN_RESOURCES_OPERATION, "profile", "profileA");
        operation.get(CHILD_TYPE).set("subsystem");

        ModelNode result = executeForResult(operation);
        assertNotNull(result);
        assertEquals(ModelType.OBJECT, result.getType());
        assertEquals(2, result.asList().size());
        ModelNode subsystem1 = null;
        ModelNode subsystem2 = null;
        for(Property property : result.asPropertyList()) {
            if("subsystem1".equals(property.getName())) {
                subsystem1 = property.getValue();
            } else if("subsystem2".equals(property.getName())) {
                subsystem2 = property.getValue();
            }
        }
        assertNotNull(subsystem1);
        checkNonRecursiveSubsystem1(subsystem1);
        assertNotNull(subsystem2);
        checkRecursiveSubsystem2(subsystem2);

        operation = createOperation(READ_CHILDREN_RESOURCES_OPERATION, "profile", "profileA", "subsystem", "subsystem1");
        operation.get(CHILD_TYPE).set("type2");
        result = executeForResult(operation);
        assertNotNull(result);
        assertEquals(ModelType.OBJECT, result.getType());
        assertEquals(1, result.asList().size());
        ModelNode other = null;
        for(Property property : result.asPropertyList()) {
            if("other".equals(property.getName())) {
                other = property.getValue();
            }
        }
        assertNotNull(other);
        assertEquals("Name2", other.require(NAME).asString());

        operation.get(CHILD_TYPE).set("non-existant-child");
        try {
            result = executeForResult(operation);
            fail("Expected error for non-existant child");
        } catch (OperationFailedException expected) {
        }

        operation = createOperation(READ_CHILDREN_RESOURCES_OPERATION, "profile", "profileC", "subsystem", "subsystem4");
        operation.get(CHILD_TYPE).set("type1");
        // BES 2011/06/06 These assertions make no sense; as they are no different from "non-existant-child" case
        // Replacing with a fail check
//          result = executeForResult(operation);
//        assertNotNull(result);
//        assertEquals(ModelType.LIST, result.getType());
//        assertTrue(result.asList().isEmpty());
        try {
            result = executeForResult(operation);
            fail("Expected error for type1 child under subsystem4");
        } catch (OperationFailedException expected) {
        }

        operation = createOperation(READ_CHILDREN_RESOURCES_OPERATION, "profile", "profileC", "subsystem", "subsystem5");
        operation.get(CHILD_TYPE).set("type1");
        result = executeForResult(operation);
        assertNotNull(result);
        assertEquals(ModelType.OBJECT, result.getType());
        assertTrue(result.asList().isEmpty());
    }

    @Test
    public void testReadChildrenResourcesRecursive() throws Exception {
        ModelNode operation = createOperation(READ_CHILDREN_RESOURCES_OPERATION, "profile", "profileA");
        operation.get(CHILD_TYPE).set("subsystem");
        operation.get(RECURSIVE).set(true);

        ModelNode result = executeForResult(operation);
        assertNotNull(result);
        assertEquals(ModelType.OBJECT, result.getType());
        assertEquals(2, result.asList().size());
        ModelNode subsystem1 = null;
        ModelNode subsystem2 = null;
        for(Property property : result.asPropertyList()) {
            if("subsystem1".equals(property.getName())) {
                subsystem1 = property.getValue();
            } else if("subsystem2".equals(property.getName())) {
                subsystem2 = property.getValue();
            }
        }
        assertNotNull(subsystem1);
        checkRecursiveSubsystem1(subsystem1);
        assertNotNull(subsystem2);
        checkRecursiveSubsystem2(subsystem2);

        operation = createOperation(READ_CHILDREN_RESOURCES_OPERATION, "profile", "profileA", "subsystem", "subsystem1");
        operation.get(CHILD_TYPE).set("type2");
        result = executeForResult(operation);
        assertNotNull(result);
        assertEquals(ModelType.OBJECT, result.getType());
        assertEquals(1, result.asList().size());
        ModelNode other = null;
        for(Property property : result.asPropertyList()) {
            if("other".equals(property.getName())) {
                other = property.getValue();
            }
        }
        assertNotNull(other);
        assertEquals("Name2", other.require(NAME).asString());

        operation.get(CHILD_TYPE).set("non-existant-child");
        try {
            result = executeForResult(operation);
            fail("Expected error for non-existant child");
        } catch (OperationFailedException expected) {
        }

        operation = createOperation(READ_CHILDREN_RESOURCES_OPERATION, "profile", "profileC", "subsystem", "subsystem4");
        operation.get(CHILD_TYPE).set("type1");
        // BES 2011/06/06 These assertions make no sense; as they are no different from "non-existant-child" case
        // Replacing with a fail check
//          result = executeForResult(operation);
//        assertNotNull(result);
//        assertEquals(ModelType.LIST, result.getType());
//        assertTrue(result.asList().isEmpty());
        try {
            result = executeForResult(operation);
            fail("Expected error for type1 child under subsystem4");
        } catch (OperationFailedException expected) {
        }

        operation = createOperation(READ_CHILDREN_RESOURCES_OPERATION, "profile", "profileC", "subsystem", "subsystem5");
        operation.get(CHILD_TYPE).set("type1");
        result = executeForResult(operation);
        assertNotNull(result);
        assertEquals(ModelType.OBJECT, result.getType());
        assertTrue(result.asList().isEmpty());
    }

    @Test
    public void testReadOperationNamesOperation() throws Exception {
        ModelNode operation = createOperation(READ_OPERATION_NAMES_OPERATION, "profile", "profileA", "subsystem", "subsystem1");
        ModelNode result = executeForResult(operation);

        assertEquals(ModelType.LIST, result.getType());
        assertEquals(11, result.asList().size());
        List<String> names = modelNodeListToStringList(result.asList());
        assertTrue(names.contains("testA1-1"));
        assertTrue(names.contains("testA1-2"));
        assertTrue(names.contains(READ_RESOURCE_OPERATION));
        assertTrue(names.contains(READ_ATTRIBUTE_OPERATION));
        assertTrue(names.contains(READ_RESOURCE_DESCRIPTION_OPERATION));
        assertTrue(names.contains(READ_CHILDREN_NAMES_OPERATION));
        assertTrue(names.contains(READ_CHILDREN_TYPES_OPERATION));
        assertTrue(names.contains(READ_CHILDREN_RESOURCES_OPERATION));
        assertTrue(names.contains(READ_OPERATION_NAMES_OPERATION));
        assertTrue(names.contains(READ_OPERATION_DESCRIPTION_OPERATION));
        assertTrue(names.contains(WRITE_ATTRIBUTE_OPERATION));


        operation = createOperation(READ_OPERATION_NAMES_OPERATION, "profile", "profileA", "subsystem", "subsystem2");

        result = executeForResult(operation);
        assertEquals(ModelType.LIST, result.getType());
        assertEquals(10, result.asList().size());
        names = modelNodeListToStringList(result.asList());
        assertTrue(names.contains("testA2"));
        assertTrue(names.contains(READ_RESOURCE_OPERATION));
        assertTrue(names.contains(READ_ATTRIBUTE_OPERATION));
        assertTrue(names.contains(READ_RESOURCE_DESCRIPTION_OPERATION));
        assertTrue(names.contains(READ_CHILDREN_NAMES_OPERATION));
        assertTrue(names.contains(READ_CHILDREN_TYPES_OPERATION));
        assertTrue(names.contains(READ_CHILDREN_RESOURCES_OPERATION));
        assertTrue(names.contains(READ_OPERATION_NAMES_OPERATION));
        assertTrue(names.contains(READ_OPERATION_DESCRIPTION_OPERATION));
        assertTrue(names.contains(WRITE_ATTRIBUTE_OPERATION));

        operation = createOperation(READ_OPERATION_NAMES_OPERATION, "profile", "profileB");
        result = executeForResult(operation);
        assertEquals(ModelType.LIST, result.getType());
        assertEquals(9, result.asList().size());
        assertTrue(names.contains(READ_RESOURCE_OPERATION));
        assertTrue(names.contains(READ_ATTRIBUTE_OPERATION));
        assertTrue(names.contains(READ_RESOURCE_DESCRIPTION_OPERATION));
        assertTrue(names.contains(READ_CHILDREN_NAMES_OPERATION));
        assertTrue(names.contains(READ_CHILDREN_TYPES_OPERATION));
        assertTrue(names.contains(READ_CHILDREN_RESOURCES_OPERATION));
        assertTrue(names.contains(READ_OPERATION_NAMES_OPERATION));
        assertTrue(names.contains(READ_OPERATION_DESCRIPTION_OPERATION));
        assertTrue(names.contains(WRITE_ATTRIBUTE_OPERATION));
    }

    @Test
    public void testReadOperationDescriptionOperation() throws Exception {
        ModelNode operation = createOperation(READ_OPERATION_DESCRIPTION_OPERATION, "profile", "profileA", "subsystem", "subsystem1");

        operation.get(NAME).set("Nothing");
        ModelNode result = executeForResult(operation);
        assertFalse(result.isDefined());

        operation.get(NAME).set("testA1-2");
        result = executeForResult(operation);
        assertEquals(ModelType.OBJECT, result.getType());
        assertEquals("testA2", result.require(OPERATION_NAME).asString());
        assertEquals(ModelType.STRING, result.require(REQUEST_PROPERTIES).require("paramA2").require(TYPE).asType());
    }

    @Test
    public void testReadResourceDescriptionOperation() throws Exception {
        ModelNode operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION);
        ModelNode result = executeForResult(operation);
        checkRootNodeDescription(result, false, false);
        assertFalse(result.get(OPERATIONS).isDefined());

        operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, "profile", "profileA");
        result = executeForResult(operation);
        checkProfileNodeDescription(result, false, false);

        //TODO this is not possible - the wildcard address does not correspond to anything in the real model
        //operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, "profile", "*");
        //result = execute(operation);
        //checkProfileNodeDescription(result, false);

        operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, "profile", "profileA", "subsystem", "subsystem1");
        result = executeForResult(operation);
        checkSubsystem1Description(result, false, false);
        assertFalse(result.get(OPERATIONS).isDefined());

        operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, "profile", "profileA", "subsystem", "subsystem1", "type1", "thing1");
        result = executeForResult(operation);
        checkType1Description(result);
        assertFalse(result.get(OPERATIONS).isDefined());

        operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, "profile", "profileA", "subsystem", "subsystem1", "type1", "thing2");
        result = executeForResult(operation);
        checkType1Description(result);
        assertFalse(result.get(OPERATIONS).isDefined());

        operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, "profile", "profileA", "subsystem", "subsystem1", "type2", "other");
        result = executeForResult(operation);
        checkType2Description(result);
        assertFalse(result.get(OPERATIONS).isDefined());
    }

    @Test
    public void testReadRecursiveResourceDescriptionOperation() throws Exception {
        ModelNode operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION);
        operation.get(RECURSIVE).set(true);
        ModelNode result = executeForResult(operation);
        checkRootNodeDescription(result, true, false);
        assertFalse(result.get(OPERATIONS).isDefined());

        operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, "profile", "profileA");
        operation.get(RECURSIVE).set(true);
        result = executeForResult(operation);
        checkProfileNodeDescription(result, true, false);

        //TODO this is not possible - the wildcard address does not correspond to anything in the real model
        //operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, "profile", "*");
        //operation.get(RECURSIVE).set(true);
        //result = execute(operation);
        //checkProfileNodeDescription(result, false);

        operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, "profile", "profileA", "subsystem", "subsystem1");
        operation.get(RECURSIVE).set(true);
        result = executeForResult(operation);
        checkSubsystem1Description(result, true, false);
        assertFalse(result.get(OPERATIONS).isDefined());

        operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, "profile", "profileA", "subsystem", "subsystem1", "type1", "thing1");
        operation.get(RECURSIVE).set(true);
        result = executeForResult(operation);
        checkType1Description(result);
        assertFalse(result.get(OPERATIONS).isDefined());

        operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, "profile", "profileA", "subsystem", "subsystem1", "type1", "thing2");
        operation.get(RECURSIVE).set(true);
        result = executeForResult(operation);
        checkType1Description(result);
        assertFalse(result.get(OPERATIONS).isDefined());

        operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, "profile", "profileA", "subsystem", "subsystem1", "type2", "other");
        operation.get(RECURSIVE).set(true);
        result = executeForResult(operation);
        checkType2Description(result);
        assertFalse(result.get(OPERATIONS).isDefined());
    }

    @Test
    public void testReadResourceDescriptionWithOperationsOperation() throws Exception {
        ModelNode operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION);
        operation.get(OPERATIONS).set(true);
        ModelNode result = executeForResult(operation);
        checkRootNodeDescription(result, false, true);
        assertTrue(result.require(OPERATIONS).isDefined());
        Set<String> ops = result.require(OPERATIONS).keys();
        assertTrue(ops.contains(READ_ATTRIBUTE_OPERATION));
        assertTrue(ops.contains(READ_CHILDREN_NAMES_OPERATION));
        assertTrue(ops.contains(READ_CHILDREN_TYPES_OPERATION));
        assertTrue(ops.contains(READ_OPERATION_DESCRIPTION_OPERATION));
        assertTrue(ops.contains(READ_OPERATION_NAMES_OPERATION));
        assertTrue(ops.contains(READ_RESOURCE_DESCRIPTION_OPERATION));
        assertTrue(ops.contains(READ_RESOURCE_OPERATION));
        for (String op : ops) {
            assertEquals(op, result.require(OPERATIONS).require(op).require(OPERATION_NAME).asString());
        }

        operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, "profile", "profileA", "subsystem", "subsystem1");
        operation.get(OPERATIONS).set(true);
        result = executeForResult(operation);
        checkSubsystem1Description(result, false, true);

        operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, "profile", "profileA", "subsystem", "subsystem1", "type1", "thing1");
        operation.get(OPERATIONS).set(true);
        result = executeForResult(operation);
        checkType1Description(result);

        operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, "profile", "profileA", "subsystem", "subsystem1", "type1", "thing2");
        operation.get(OPERATIONS).set(true);
        result = executeForResult(operation);
        checkType1Description(result);

        operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, "profile", "profileA", "subsystem", "subsystem1", "type2", "other");
        operation.get(OPERATIONS).set(true);
        result = executeForResult(operation);
        checkType2Description(result);
    }

    @Test
    public void testRecursiveReadResourceDescriptionWithOperationsOperation() throws Exception {
        ModelNode operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION);
        operation.get(OPERATIONS).set(true);
        operation.get(RECURSIVE).set(true);
        ModelNode result = executeForResult(operation);
        checkRootNodeDescription(result, true, true);


        operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, "profile", "profileA", "subsystem", "subsystem1");
        operation.get(OPERATIONS).set(true);
        operation.get(RECURSIVE).set(true);
        result = executeForResult(operation);
        checkSubsystem1Description(result, true, true);

        operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, "profile", "profileA", "subsystem", "subsystem1", "type1", "thing1");
        operation.get(OPERATIONS).set(true);
        operation.get(RECURSIVE).set(true);
        result = executeForResult(operation);
        checkType1Description(result);

        operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, "profile", "profileA", "subsystem", "subsystem1", "type1", "thing2");
        operation.get(OPERATIONS).set(true);
        operation.get(RECURSIVE).set(true);
        result = executeForResult(operation);
        checkType1Description(result);

        operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, "profile", "profileA", "subsystem", "subsystem1", "type2", "other");
        operation.get(OPERATIONS).set(true);
        operation.get(RECURSIVE).set(true);
        result = executeForResult(operation);
        checkType2Description(result);
    }

    private void checkNonRecursiveSubsystem1(ModelNode result) {
        assertEquals(3, result.keys().size());
        ModelNode content = result.require("attr1");
        List<ModelNode> list = content.asList();
        assertEquals(2, list.size());
        assertEquals(1, list.get(0).asInt());
        assertEquals(2, list.get(1).asInt());
        assertEquals(2, result.require("type1").keys().size());
        assertTrue(result.require("type1").has("thing1"));
        assertFalse(result.require("type1").get("thing1").isDefined());
        assertTrue(result.require("type1").has("thing2"));
        assertFalse(result.require("type1").get("thing2").isDefined());
        assertEquals(1, result.require("type2").keys().size());
        assertTrue(result.require("type2").has("other"));
        assertFalse(result.require("type2").get("other").isDefined());
    }

    private void checkRecursiveSubsystem1(ModelNode result) {
        assertEquals(3, result.keys().size());
        List<ModelNode> list = result.require("attr1").asList();
        assertEquals(2, list.size());
        assertEquals(1, list.get(0).asInt());
        assertEquals(2, list.get(1).asInt());
        assertEquals("Name11", result.require("type1").require("thing1").require("name").asString());
        assertEquals(201, result.require("type1").require("thing1").require("value").asInt());
        assertEquals("Name12", result.require("type1").require("thing2").require("name").asString());
        assertEquals(202, result.require("type1").require("thing2").require("value").asInt());
        assertEquals("Name2", result.require("type2").require("other").require("name").asString());
    }

    private void checkRecursiveSubsystem2(ModelNode result) {
        assertEquals(14, result.keys().size());

        assertEquals(new BigDecimal(100), result.require("bigdecimal").asBigDecimal());
        assertEquals(new BigInteger("101"), result.require("biginteger").asBigInteger());
        assertTrue(result.require("boolean").asBoolean());
        assertEquals(3, result.require("bytes").asBytes().length);
        assertEquals(1, result.require("bytes").asBytes()[0]);
        assertEquals(2, result.require("bytes").asBytes()[1]);
        assertEquals(3, result.require("bytes").asBytes()[2]);
        assertEquals(Double.MAX_VALUE, result.require("double").asDouble());
        assertEquals("{expr}", result.require("expression").asString());
        assertEquals(102, result.require("int").asInt());
        List<ModelNode> list = result.require("list").asList();
        assertEquals(2, list.size());
        assertEquals("l1A", list.get(0).asString());
        assertEquals("l1B", list.get(1).asString());
        assertEquals(Long.MAX_VALUE, result.require("long").asLong());
        assertEquals("objVal", result.require("object").require("value").asString());
        Property prop = result.require("property").asProperty();
        assertEquals("prop1", prop.getName());
        assertEquals("value1", prop.getValue().asString());
        assertEquals("s1", result.require("string1").asString());
        assertEquals("s2", result.require("string2").asString());
        assertEquals(ModelType.TYPE, result.require("type").asType());
    }

    private void checkRootNodeDescription(ModelNode result, boolean recursive, boolean operations) {
        assertEquals("The root node of the test management API", result.require(DESCRIPTION).asString());
        assertEquals("A list of profiles", result.require(CHILDREN).require(PROFILE).require(DESCRIPTION).asString());
        assertEquals(1, result.require(CHILDREN).require(PROFILE).require(MIN_OCCURS).asInt());

        if (operations) {
            assertTrue(result.require(OPERATIONS).isDefined());
            Set<String> ops = result.require(OPERATIONS).keys();
            assertTrue(ops.contains(READ_ATTRIBUTE_OPERATION));
            assertTrue(ops.contains(READ_CHILDREN_NAMES_OPERATION));
            assertTrue(ops.contains(READ_CHILDREN_TYPES_OPERATION));
            assertTrue(ops.contains(READ_OPERATION_DESCRIPTION_OPERATION));
            assertTrue(ops.contains(READ_OPERATION_NAMES_OPERATION));
            assertTrue(ops.contains(READ_RESOURCE_DESCRIPTION_OPERATION));
            assertTrue(ops.contains(READ_RESOURCE_OPERATION));
            assertTrue(ops.contains(WRITE_ATTRIBUTE_OPERATION));
            for (String op : ops) {
                assertEquals(op, result.require(OPERATIONS).require(op).require(OPERATION_NAME).asString());
            }
        } else {
            assertFalse(result.get(OPERATIONS).isDefined());
        }


        if (!recursive) {
            assertFalse(result.require(CHILDREN).require(PROFILE).require(MODEL_DESCRIPTION).isDefined());
            return;
        }
        assertTrue(result.require(CHILDREN).require(PROFILE).require(MODEL_DESCRIPTION).isDefined());
        assertEquals(1, result.require(CHILDREN).require(PROFILE).require(MODEL_DESCRIPTION).keys().size());
        checkProfileNodeDescription(result.require(CHILDREN).require(PROFILE).require(MODEL_DESCRIPTION).require("*"), true, operations);
    }

    private void checkProfileNodeDescription(ModelNode result, boolean recursive, boolean operations) {
        assertEquals("A named set of subsystem configs", result.require(DESCRIPTION).asString());
        assertEquals(ModelType.STRING, result.require(ATTRIBUTES).require(NAME).require(TYPE).asType());
        assertEquals("The name of the profile", result.require(ATTRIBUTES).require(NAME).require(DESCRIPTION).asString());
        assertEquals(true, result.require(ATTRIBUTES).require(NAME).require(REQUIRED).asBoolean());
        assertEquals(1, result.require(ATTRIBUTES).require(NAME).require(MIN_LENGTH).asInt());
        assertEquals("The subsystems that make up the profile", result.require(CHILDREN).require(SUBSYSTEM).require(DESCRIPTION).asString());
        assertEquals(1, result.require(CHILDREN).require(SUBSYSTEM).require(MIN_OCCURS).asInt());
        if (!recursive) {
            assertFalse(result.require(CHILDREN).require(SUBSYSTEM).require(MODEL_DESCRIPTION).isDefined());
            return;
        }
        assertTrue(result.require(CHILDREN).require(SUBSYSTEM).require(MODEL_DESCRIPTION).isDefined());
        assertEquals(5, result.require(CHILDREN).require(SUBSYSTEM).require(MODEL_DESCRIPTION).keys().size());
        checkSubsystem1Description(result.require(CHILDREN).require(SUBSYSTEM).require(MODEL_DESCRIPTION).require("subsystem1"), recursive, operations);
    }

    private void checkSubsystem1Description(ModelNode result, boolean recursive, boolean operations) {
        assertNotNull(result);

        assertEquals("A test subsystem 1", result.require(DESCRIPTION).asString());
        assertEquals(ModelType.LIST, result.require(ATTRIBUTES).require("attr1").require(TYPE).asType());
        assertEquals(ModelType.INT, result.require(ATTRIBUTES).require("attr1").require(VALUE_TYPE).asType());
        assertEquals("The values", result.require(ATTRIBUTES).require("attr1").require(DESCRIPTION).asString());
        assertTrue(result.require(ATTRIBUTES).require("attr1").require(REQUIRED).asBoolean());
        assertEquals(AccessType.READ_ONLY.toString(), result.require(ATTRIBUTES).require("attr1").get(ACCESS_TYPE).asString());
        assertEquals(ModelType.INT, result.require(ATTRIBUTES).require("read-only").require(TYPE).asType());
        assertEquals("A r/o int", result.require(ATTRIBUTES).require("read-only").require(DESCRIPTION).asString());
        assertFalse(result.require(ATTRIBUTES).require("read-only").require(REQUIRED).asBoolean());
        assertEquals(AccessType.READ_ONLY.toString(), result.require(ATTRIBUTES).require("read-only").get(ACCESS_TYPE).asString());
        assertEquals(ModelType.INT, result.require(ATTRIBUTES).require("metric1").require(TYPE).asType());
        assertEquals("A random metric", result.require(ATTRIBUTES).require("metric1").require(DESCRIPTION).asString());
        assertEquals(AccessType.METRIC.toString(), result.require(ATTRIBUTES).require("metric1").get(ACCESS_TYPE).asString());
        assertEquals(AccessType.METRIC.toString(), result.require(ATTRIBUTES).require("metric2").get(ACCESS_TYPE).asString());
        assertEquals(ModelType.INT, result.require(ATTRIBUTES).require("read-write").require(TYPE).asType());
        assertEquals("A r/w int", result.require(ATTRIBUTES).require("read-write").require(DESCRIPTION).asString());
        assertFalse(result.require(ATTRIBUTES).require("read-write").require(REQUIRED).asBoolean());
        assertEquals(AccessType.READ_WRITE.toString(), result.require(ATTRIBUTES).require("read-write").get(ACCESS_TYPE).asString());

        assertEquals("The children1", result.require(CHILDREN).require("type1").require(DESCRIPTION).asString());
        assertEquals(1, result.require(CHILDREN).require("type1").require(MIN_OCCURS).asInt());

        assertEquals("The children1", result.require(CHILDREN).require("type1").require(DESCRIPTION).asString());
        assertEquals("The children2", result.require(CHILDREN).require("type2").require(DESCRIPTION).asString());
        assertEquals(1, result.require(CHILDREN).require("type2").require(MIN_OCCURS).asInt());
        assertEquals(1, result.require(CHILDREN).require("type2").require(MIN_OCCURS).asInt());

        if (operations) {
            assertTrue(result.require(OPERATIONS).isDefined());
            Set<String> ops = result.require(OPERATIONS).keys();
            assertEquals(11, ops.size());
            assertTrue(ops.contains("testA1-1"));
            assertTrue(ops.contains("testA1-2"));
            assertTrue(ops.contains(READ_RESOURCE_OPERATION));
            assertTrue(ops.contains(READ_ATTRIBUTE_OPERATION));
            assertTrue(ops.contains(READ_RESOURCE_DESCRIPTION_OPERATION));
            assertTrue(ops.contains(READ_CHILDREN_NAMES_OPERATION));
            assertTrue(ops.contains(READ_CHILDREN_TYPES_OPERATION));
            assertTrue(ops.contains(READ_CHILDREN_RESOURCES_OPERATION));
            assertTrue(ops.contains(READ_OPERATION_NAMES_OPERATION));
            assertTrue(ops.contains(READ_OPERATION_DESCRIPTION_OPERATION));
            assertTrue(ops.contains(WRITE_ATTRIBUTE_OPERATION));

        } else {
            assertFalse(result.get(OPERATIONS).isDefined());
        }

        if (!recursive) {
            assertFalse(result.require(CHILDREN).require("type1").require(MODEL_DESCRIPTION).isDefined());
            assertFalse(result.require(CHILDREN).require("type2").require(MODEL_DESCRIPTION).isDefined());
            return;
        }

        checkType1Description(result.require(CHILDREN).require("type1").require(MODEL_DESCRIPTION).require("*"));
        checkType2Description(result.require(CHILDREN).require("type2").require(MODEL_DESCRIPTION).require("other"));
    }

    private void checkType1Description(ModelNode result) {
        assertNotNull(result);
        assertEquals("A type 1", result.require(DESCRIPTION).asString());
        assertEquals(ModelType.STRING, result.require(ATTRIBUTES).require("name").require(TYPE).asType());
        assertEquals("The name of the thing", result.require(ATTRIBUTES).require("name").require(DESCRIPTION).asString());
        assertTrue(result.require(ATTRIBUTES).require("name").require(REQUIRED).asBoolean());
        assertEquals(ModelType.INT, result.require(ATTRIBUTES).require("value").require(TYPE).asType());
        assertEquals("The value of the thing", result.require(ATTRIBUTES).require("value").require(DESCRIPTION).asString());
        assertTrue(result.require(ATTRIBUTES).require("value").require(REQUIRED).asBoolean());
        //TODO should the inherited ops be picked up?
        if (result.has(OPERATIONS)) {
            assertTrue(result.require(OPERATIONS).isDefined());
            Set<String> ops = result.require(OPERATIONS).keys();
            assertEquals(9, ops.size());
            assertTrue(ops.contains(READ_RESOURCE_OPERATION));
            assertTrue(ops.contains(READ_ATTRIBUTE_OPERATION));
            assertTrue(ops.contains(READ_RESOURCE_DESCRIPTION_OPERATION));
            assertTrue(ops.contains(READ_CHILDREN_NAMES_OPERATION));
            assertTrue(ops.contains(READ_CHILDREN_TYPES_OPERATION));
            assertTrue(ops.contains(READ_CHILDREN_RESOURCES_OPERATION));
            assertTrue(ops.contains(READ_OPERATION_NAMES_OPERATION));
            assertTrue(ops.contains(READ_OPERATION_DESCRIPTION_OPERATION));
            assertTrue(ops.contains(WRITE_ATTRIBUTE_OPERATION));

        }

    }

    private void checkType2Description(ModelNode result) {
        assertNotNull(result);
        assertEquals("A type 2", result.require(DESCRIPTION).asString());
        assertEquals(ModelType.STRING, result.require(ATTRIBUTES).require("name").require(TYPE).asType());
        assertEquals("The name of the thing", result.require(ATTRIBUTES).require("name").require(DESCRIPTION).asString());
        assertTrue(result.require(ATTRIBUTES).require("name").require(REQUIRED).asBoolean());
        //TODO should the inherited ops be picked up?
        if (result.has(OPERATIONS)) {
            assertTrue(result.require(OPERATIONS).isDefined());
            Set<String> ops = result.require(OPERATIONS).keys();
            assertEquals(9, ops.size());
            assertTrue(ops.contains(READ_RESOURCE_OPERATION));
            assertTrue(ops.contains(READ_ATTRIBUTE_OPERATION));
            assertTrue(ops.contains(READ_RESOURCE_DESCRIPTION_OPERATION));
            assertTrue(ops.contains(READ_CHILDREN_NAMES_OPERATION));
            assertTrue(ops.contains(READ_CHILDREN_TYPES_OPERATION));
            assertTrue(ops.contains(READ_CHILDREN_RESOURCES_OPERATION));
            assertTrue(ops.contains(READ_OPERATION_NAMES_OPERATION));
            assertTrue(ops.contains(READ_OPERATION_DESCRIPTION_OPERATION));
            assertTrue(ops.contains(WRITE_ATTRIBUTE_OPERATION));
        }
    }

    private ModelNode createOperation(String operationName, String...address) {
        ModelNode operation = new ModelNode();
        operation.get(OP).set(operationName);
        if (address.length > 0) {
            for (String addr : address) {
                operation.get(OP_ADDR).add(addr);
            }
        } else {
            operation.get(OP_ADDR).setEmptyList();
        }

        return operation;
    }

    private List<String> modelNodeListToStringList(List<ModelNode> nodes){
        List<String> result = new ArrayList<String>();
        for (ModelNode node : nodes) {
            result.add(node.asString());
        }
        return result;
    }

    @Override
    DescriptionProvider getRootDescriptionProvider() {
        return rootDescriptionProvider;
    }

    @Override
    protected ModelNode createCoreModel() {
        return createTestNode();
    }

    @Override
    protected void initModel(ManagementResourceRegistration rootRegistration) {
        rootRegistration.registerOperationHandler(READ_RESOURCE_OPERATION, GlobalOperationHandlers.READ_RESOURCE, CommonProviders.READ_RESOURCE_PROVIDER, true);
        rootRegistration.registerOperationHandler(READ_ATTRIBUTE_OPERATION, GlobalOperationHandlers.READ_ATTRIBUTE, CommonProviders.READ_ATTRIBUTE_PROVIDER, true);
        rootRegistration.registerOperationHandler(READ_RESOURCE_DESCRIPTION_OPERATION, GlobalOperationHandlers.READ_RESOURCE_DESCRIPTION, CommonProviders.READ_RESOURCE_DESCRIPTION_PROVIDER, true);
        rootRegistration.registerOperationHandler(READ_CHILDREN_NAMES_OPERATION, GlobalOperationHandlers.READ_CHILDREN_NAMES, CommonProviders.READ_CHILDREN_NAMES_PROVIDER, true);
        rootRegistration.registerOperationHandler(READ_CHILDREN_TYPES_OPERATION, GlobalOperationHandlers.READ_CHILDREN_TYPES, CommonProviders.READ_CHILDREN_TYPES_PROVIDER, true);
        rootRegistration.registerOperationHandler(READ_CHILDREN_RESOURCES_OPERATION, GlobalOperationHandlers.READ_CHILDREN_RESOURCES, CommonProviders.READ_CHILDREN_RESOURCES_PROVIDER, true);
        rootRegistration.registerOperationHandler(READ_OPERATION_NAMES_OPERATION, GlobalOperationHandlers.READ_OPERATION_NAMES, CommonProviders.READ_OPERATION_NAMES_PROVIDER, true);
        rootRegistration.registerOperationHandler(READ_OPERATION_DESCRIPTION_OPERATION, GlobalOperationHandlers.READ_OPERATION_DESCRIPTION, CommonProviders.READ_OPERATION_PROVIDER, true);
        rootRegistration.registerOperationHandler(WRITE_ATTRIBUTE_OPERATION, GlobalOperationHandlers.WRITE_ATTRIBUTE, CommonProviders.WRITE_ATTRIBUTE_PROVIDER, true);
        rootRegistration.registerOperationHandler("setup", new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                final ModelNode model = new ModelNode();
                //Atttributes
                model.get("profile", "profileA", "subsystem", "subsystem1", "attr1").add(1);
                model.get("profile", "profileA", "subsystem", "subsystem1", "attr1").add(2);
                //Children
                model.get("profile", "profileA", "subsystem", "subsystem1", "type1", "thing1", "name").set("Name11");
                model.get("profile", "profileA", "subsystem", "subsystem1", "type1", "thing1", "value").set("201");
                model.get("profile", "profileA", "subsystem", "subsystem1", "type1", "thing2", "name").set("Name12");
                model.get("profile", "profileA", "subsystem", "subsystem1", "type1", "thing2", "value").set("202");
                model.get("profile", "profileA", "subsystem", "subsystem1", "type2", "other", "name").set("Name2");


                model.get("profile", "profileA", "subsystem", "subsystem2", "bigdecimal").set(new BigDecimal(100));
                model.get("profile", "profileA", "subsystem", "subsystem2", "biginteger").set(new BigInteger("101"));
                model.get("profile", "profileA", "subsystem", "subsystem2", "boolean").set(true);
                model.get("profile", "profileA", "subsystem", "subsystem2", "bytes").set(new byte[] {1, 2, 3});
                model.get("profile", "profileA", "subsystem", "subsystem2", "double").set(Double.MAX_VALUE);
                model.get("profile", "profileA", "subsystem", "subsystem2", "expression").setExpression("{expr}");
                model.get("profile", "profileA", "subsystem", "subsystem2", "int").set(102);
                model.get("profile", "profileA", "subsystem", "subsystem2", "list").add("l1A");
                model.get("profile", "profileA", "subsystem", "subsystem2", "list").add("l1B");
                model.get("profile", "profileA", "subsystem", "subsystem2", "long").set(Long.MAX_VALUE);
                model.get("profile", "profileA", "subsystem", "subsystem2", "object", "value").set("objVal");
                model.get("profile", "profileA", "subsystem", "subsystem2", "property").set(new Property("prop1", new ModelNode().set("value1")));
                model.get("profile", "profileA", "subsystem", "subsystem2", "string1").set("s1");
                model.get("profile", "profileA", "subsystem", "subsystem2", "string2").set("s2");
                model.get("profile", "profileA", "subsystem", "subsystem2", "type").set(ModelType.TYPE);


                model.get("profile", "profileB", "name").set("Profile B");

                model.get("profile", "profileC", "subsystem", "subsystem4");
                model.get("profile", "profileC", "subsystem", "subsystem5", "name").set("Test");

                createModel(context, model);

                context.completeStep();
            }
        }, new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return new ModelNode();
            }
        }, false, OperationEntry.EntryType.PRIVATE);

        ManagementResourceRegistration profileReg = rootRegistration.registerSubModel(PathElement.pathElement("profile", "*"), new DescriptionProvider() {

            @Override
            public ModelNode getModelDescription(Locale locale) {
                ModelNode node = new ModelNode();
                node.get(DESCRIPTION).set("A named set of subsystem configs");
                node.get(ATTRIBUTES, NAME, TYPE).set(ModelType.STRING);
                node.get(ATTRIBUTES, NAME, DESCRIPTION).set("The name of the profile");
                node.get(ATTRIBUTES, NAME, REQUIRED).set(true);
                node.get(ATTRIBUTES, NAME, MIN_LENGTH).set(1);
                node.get(CHILDREN, SUBSYSTEM, DESCRIPTION).set("The subsystems that make up the profile");
                node.get(CHILDREN, SUBSYSTEM, MIN_OCCURS).set(1);
                node.get(CHILDREN, SUBSYSTEM, MODEL_DESCRIPTION);
                return node;
            }
        });

        ManagementResourceRegistration profileSub1Reg = profileReg.registerSubModel(PathElement.pathElement("subsystem", "subsystem1"), new DescriptionProvider() {

            @Override
            public ModelNode getModelDescription(Locale locale) {
                ModelNode node = new ModelNode();
                node.get(DESCRIPTION).set("A test subsystem 1");
                node.get(ATTRIBUTES, "attr1", TYPE).set(ModelType.LIST);
                node.get(ATTRIBUTES, "attr1", VALUE_TYPE).set(ModelType.INT);
                node.get(ATTRIBUTES, "attr1", DESCRIPTION).set("The values");
                node.get(ATTRIBUTES, "attr1", REQUIRED).set(true);
                node.get(ATTRIBUTES, "read-only", TYPE).set(ModelType.INT);
                node.get(ATTRIBUTES, "read-only", DESCRIPTION).set("A r/o int");
                node.get(ATTRIBUTES, "read-only", REQUIRED).set(false);
                node.get(ATTRIBUTES, "metric1", TYPE).set(ModelType.INT);
                node.get(ATTRIBUTES, "metric1", DESCRIPTION).set("A random metric");
                node.get(ATTRIBUTES, "read-write", TYPE).set(ModelType.INT);
                node.get(ATTRIBUTES, "metric2", TYPE).set(ModelType.INT);
                node.get(ATTRIBUTES, "read-write", DESCRIPTION).set("A r/w int");
                node.get(ATTRIBUTES, "read-write", REQUIRED).set(false);
                node.get(CHILDREN, "type1", DESCRIPTION).set("The children1");
                node.get(CHILDREN, "type1", MIN_OCCURS).set(1);
                node.get(CHILDREN, "type1", MODEL_DESCRIPTION);
                node.get(CHILDREN, "type2", DESCRIPTION).set("The children2");
                node.get(CHILDREN, "type2", MIN_OCCURS).set(1);
                node.get(CHILDREN, "type2", MODEL_DESCRIPTION);
                return node;
            }
        });

        profileSub1Reg.registerReadOnlyAttribute("read-only", null, AttributeAccess.Storage.CONFIGURATION);
        profileSub1Reg.registerReadWriteAttribute("read-write", null, new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.INT), AttributeAccess.Storage.CONFIGURATION);
        profileSub1Reg.registerMetric("metric1", TestMetricHandler.INSTANCE);
        profileSub1Reg.registerMetric("metric2", TestMetricHandler.INSTANCE);
        //TODO Validation if we try to set a handler for an attribute that does not exist in model?

        DescriptionProvider thingProvider = new DescriptionProvider() {

            @Override
            public ModelNode getModelDescription(Locale locale) {
                ModelNode node = new ModelNode();
                node.get(DESCRIPTION).set("A type 1");
                node.get(ATTRIBUTES, "name", TYPE).set(ModelType.STRING);
                node.get(ATTRIBUTES, "name", DESCRIPTION).set("The name of the thing");
                node.get(ATTRIBUTES, "name", REQUIRED).set(true);
                node.get(ATTRIBUTES, "value", TYPE).set(ModelType.INT);
                node.get(ATTRIBUTES, "value", DESCRIPTION).set("The value of the thing");
                node.get(ATTRIBUTES, "value", REQUIRED).set(true);
                return node;
            }
        };

        ManagementResourceRegistration profileSub1RegChildType11 = profileSub1Reg.registerSubModel(PathElement.pathElement("type1", "*"), thingProvider);
        ManagementResourceRegistration profileSub1RegChildType2 = profileSub1Reg.registerSubModel(PathElement.pathElement("type2", "other"), new DescriptionProvider() {

            @Override
            public ModelNode getModelDescription(Locale locale) {
                ModelNode node = new ModelNode();
                node.get(DESCRIPTION).set("A type 2");
                node.get(ATTRIBUTES, "name", TYPE).set(ModelType.STRING);
                node.get(ATTRIBUTES, "name", DESCRIPTION).set("The name of the thing");
                node.get(ATTRIBUTES, "name", REQUIRED).set(true);
                return node;
            }
        });

        ManagementResourceRegistration profileASub2Reg = profileReg.registerSubModel(PathElement.pathElement("subsystem", "subsystem2"), new DescriptionProvider() {

            @Override
            public ModelNode getModelDescription(Locale locale) {
                ModelNode node = new ModelNode();
                node.get(DESCRIPTION).set("A test subsystem 2");
                node.get(ATTRIBUTES, "bigdecimal", TYPE).set(ModelType.BIG_DECIMAL);
                node.get(ATTRIBUTES, "bigdecimal", DESCRIPTION).set("A big decimal");
                node.get(ATTRIBUTES, "bigdecimal", REQUIRED).set(true);
                node.get(ATTRIBUTES, "biginteger", TYPE).set(ModelType.BIG_DECIMAL);
                node.get(ATTRIBUTES, "biginteger", DESCRIPTION).set("A big integer");
                node.get(ATTRIBUTES, "biginteger", REQUIRED).set(true);
                node.get(ATTRIBUTES, "boolean", TYPE).set(ModelType.BOOLEAN);
                node.get(ATTRIBUTES, "boolean", DESCRIPTION).set("A boolean");
                node.get(ATTRIBUTES, "boolean", REQUIRED).set(true);
                node.get(ATTRIBUTES, "bytes", TYPE).set(ModelType.BYTES);
                node.get(ATTRIBUTES, "bytes", DESCRIPTION).set("A bytes");
                node.get(ATTRIBUTES, "bytes", REQUIRED).set(true);
                node.get(ATTRIBUTES, "double", TYPE).set(ModelType.DOUBLE);
                node.get(ATTRIBUTES, "double", DESCRIPTION).set("A double");
                node.get(ATTRIBUTES, "double", REQUIRED).set(true);
                node.get(ATTRIBUTES, "expression", TYPE).set(ModelType.EXPRESSION);
                node.get(ATTRIBUTES, "expression", DESCRIPTION).set("A double");
                node.get(ATTRIBUTES, "expression", REQUIRED).set(true);
                node.get(ATTRIBUTES, "int", TYPE).set(ModelType.INT);
                node.get(ATTRIBUTES, "int", DESCRIPTION).set("A int");
                node.get(ATTRIBUTES, "int", REQUIRED).set(true);
                node.get(ATTRIBUTES, "list", TYPE).set(ModelType.LIST);
                node.get(ATTRIBUTES, "list", VALUE_TYPE).set(ModelType.STRING);
                node.get(ATTRIBUTES, "list", DESCRIPTION).set("A list");
                node.get(ATTRIBUTES, "list", REQUIRED).set(true);
                node.get(ATTRIBUTES, "long", TYPE).set(ModelType.LONG);
                node.get(ATTRIBUTES, "long", DESCRIPTION).set("A long");
                node.get(ATTRIBUTES, "long", REQUIRED).set(true);
                node.get(ATTRIBUTES, "object", TYPE).set(ModelType.OBJECT);
                node.get(ATTRIBUTES, "object", VALUE_TYPE).set(ModelType.STRING);
                node.get(ATTRIBUTES, "object", DESCRIPTION).set("A object");
                node.get(ATTRIBUTES, "object", REQUIRED).set(true);
                node.get(ATTRIBUTES, "property", TYPE).set(ModelType.PROPERTY);
                node.get(ATTRIBUTES, "property", VALUE_TYPE).set(ModelType.STRING);
                node.get(ATTRIBUTES, "property", DESCRIPTION).set("A property");
                node.get(ATTRIBUTES, "property", REQUIRED).set(true);
                node.get(ATTRIBUTES, "string1", TYPE).set(ModelType.STRING);
                node.get(ATTRIBUTES, "string1", DESCRIPTION).set("A string");
                node.get(ATTRIBUTES, "string1", REQUIRED).set(true);
                node.get(ATTRIBUTES, "string2", TYPE).set(ModelType.STRING);
                node.get(ATTRIBUTES, "string2", DESCRIPTION).set("A string");
                node.get(ATTRIBUTES, "string2", REQUIRED).set(true);
                node.get(ATTRIBUTES, "type", TYPE).set(ModelType.TYPE);
                node.get(ATTRIBUTES, "type", DESCRIPTION).set("A type");
                node.get(ATTRIBUTES, "type", REQUIRED).set(true);


                return node;
            }
        });

        profileASub2Reg.registerReadWriteAttribute("long", null, new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.LONG, false), AttributeAccess.Storage.CONFIGURATION);

        ManagementResourceRegistration profileBSub3Reg = profileReg.registerSubModel(PathElement.pathElement("subsystem", "subsystem3"), new DescriptionProvider() {

            @Override
            public ModelNode getModelDescription(Locale locale) {
                ModelNode node = new ModelNode();
                node.get(DESCRIPTION).set("A test subsystem 1");
                node.get(ATTRIBUTES, "attr1", TYPE).set(ModelType.INT);
                node.get(ATTRIBUTES, "attr1", DESCRIPTION).set("The value");
                node.get(ATTRIBUTES, "attr1", REQUIRED).set(true);
                node.get(CHILDREN).setEmptyObject();
                return node;
            }
        });

        profileSub1Reg.registerOperationHandler("testA1-1",
                new OperationStepHandler() {
                    @Override
                    public void execute(OperationContext context, ModelNode operation) {
                        return;
                    }
                },
                new DescriptionProvider() {

                    @Override
                    public ModelNode getModelDescription(Locale locale) {
                        ModelNode node = new ModelNode();
                        node.get(OPERATION_NAME).set("testA1");
                        node.get(REQUEST_PROPERTIES, "paramA1", TYPE).set(ModelType.INT);
                        return node;
                    }
                },
                false);
        profileSub1Reg.registerOperationHandler("testA1-2",
                new OperationStepHandler() {

                    @Override
                    public void execute(OperationContext context, ModelNode operation) {
                        return;
                    }
                },
                new DescriptionProvider() {

                    @Override
                    public ModelNode getModelDescription(Locale locale) {
                        ModelNode node = new ModelNode();
                        node.get(OPERATION_NAME).set("testA2");
                        node.get(REQUEST_PROPERTIES, "paramA2", TYPE).set(ModelType.STRING);
                        return node;
                    }
                },
                false);


        profileASub2Reg.registerOperationHandler("testA2",
                new OperationStepHandler() {

                    @Override
                    public void execute(OperationContext context, ModelNode operation) {
                        return;
                    }
                },
                new DescriptionProvider() {

                    @Override
                    public ModelNode getModelDescription(Locale locale) {
                        ModelNode node = new ModelNode();
                        node.get(OPERATION_NAME).set("testB");
                        node.get(REQUEST_PROPERTIES, "paramB", TYPE).set(ModelType.LONG);
                        return node;
                    }
                },
                false);

        ManagementResourceRegistration profileCSub4Reg = profileReg.registerSubModel(PathElement.pathElement("subsystem", "subsystem4"), new DescriptionProvider() {

            @Override
            public ModelNode getModelDescription(Locale locale) {
                ModelNode node = new ModelNode();
                node.get(DESCRIPTION).set("A subsystem");
                node.get(ATTRIBUTES, "name", TYPE).set(ModelType.STRING);
                node.get(ATTRIBUTES, "name", DESCRIPTION).set("The name of the thing");
                node.get(ATTRIBUTES, "name", REQUIRED).set(false);
                node.get(CHILDREN, "type1", DESCRIPTION).set("The children1");
                node.get(CHILDREN, "type1", MIN_OCCURS).set(0);
                node.get(CHILDREN, "type1", MODEL_DESCRIPTION);
                return node;
            }
        });


        ManagementResourceRegistration profileCSub5Reg = profileReg.registerSubModel(PathElement.pathElement("subsystem", "subsystem5"), new DescriptionProvider() {

            @Override
            public ModelNode getModelDescription(Locale locale) {
                ModelNode node = new ModelNode();
                node.get(DESCRIPTION).set("A subsystem");
                node.get(ATTRIBUTES, "name", TYPE).set(ModelType.STRING);
                node.get(ATTRIBUTES, "name", DESCRIPTION).set("The name of the thing");
                node.get(ATTRIBUTES, "name", REQUIRED).set(false);
                node.get(CHILDREN, "type1", DESCRIPTION).set("The children1");
                node.get(CHILDREN, "type1", MIN_OCCURS).set(0);
                node.get(CHILDREN, "type1", MODEL_DESCRIPTION);
                return node;
            }
        });
        profileCSub5Reg.registerReadOnlyAttribute("name", new OperationStepHandler() {

            @Override
            public void execute(OperationContext context, ModelNode operation) {
                context.getResult().set("Overridden by special read handler");
                context.completeStep();
            }
        }, AttributeAccess.Storage.CONFIGURATION);


        ManagementResourceRegistration profileCSub5Type1Reg = profileCSub5Reg.registerSubModel(PathElement.pathElement("type1", "thing1"), new DescriptionProvider() {

            @Override
            public ModelNode getModelDescription(Locale locale) {
                ModelNode node = new ModelNode();
                node.get(DESCRIPTION).set("A subsystem");
                return node;
            }
        });
    }

    /**
     * Override to get the actual result from the response.
     */
    public ModelNode executeForResult(ModelNode operation) throws OperationFailedException {
        ModelNode rsp = getController().execute(operation, null, null, null);
        if (FAILED.equals(rsp.get(OUTCOME).asString())) {
            throw new OperationFailedException(rsp.get(FAILURE_DESCRIPTION));
        }
        return rsp.get(RESULT);
    }

    static class TestMetricHandler implements OperationStepHandler {
        static final TestMetricHandler INSTANCE = new TestMetricHandler();
        private static final Random random = new Random();
        @Override
        public void execute(final OperationContext context, final ModelNode operation) {
            context.getResult().set(random.nextInt());
            context.completeStep();
        }

    }

}
