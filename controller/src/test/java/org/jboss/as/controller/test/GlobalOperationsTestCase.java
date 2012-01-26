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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILD_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_RUNTIME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.junit.Test;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class GlobalOperationsTestCase extends AbstractGlobalOperationsTestCase {

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

        checkNonRecursiveSubsystem1(result, false);
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
        operation.get(INCLUDE_RUNTIME).set(true);

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
        checkNonRecursiveSubsystem1(subsystem1, true);
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
        try {
            ModelNode result = executeForResult(operation);
            fail("Received invalid successful result " + result.toString());
        } catch (OperationFailedException good) {
            // the correct result
        }

        operation.get(NAME).set("testA1-2");
        ModelNode result = executeForResult(operation);
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

    private void checkNonRecursiveSubsystem1(ModelNode result, boolean includeRuntime) {
        assertEquals(includeRuntime ? 5 : 3, result.keys().size());
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
        if (includeRuntime) {
            assertEquals(ModelType.INT, result.require("metric1").getType());
            assertEquals(ModelType.INT, result.require("metric2").getType());
        }
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
}
