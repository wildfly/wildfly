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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTES_OF_TYPE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_NAMED_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_SUB_MODEL_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import org.jboss.as.controller.BasicModelController;
import org.jboss.as.controller.descriptions.common.GlobalDescriptions;
import org.jboss.as.controller.operations.global.GlobalOperationHandlers;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.junit.Test;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class GlobalOperationsTestCase {

    private static final ModelNode MODEL = createTestNode();
    private static final TestModelController CONTROLLER = new TestModelController();

    public static ModelNode createTestNode() {
        ModelNode model = new ModelNode();

        model.get("profile", "profileA", "name").set("Profile A");
        model.get("profile", "profileA", "subsystem", "test1").add(1);
        model.get("profile", "profileA", "subsystem", "test1").add(2);

        model.get("profile", "profileB", "name").set("Profile B");
        model.get("profile", "profileB", "subsystem", "test2", "bigdecimal").set(new BigDecimal(100));
        model.get("profile", "profileB", "subsystem", "test2", "biginteger").set(new BigInteger("101"));
        model.get("profile", "profileB", "subsystem", "test2", "boolean").set(true);
        model.get("profile", "profileB", "subsystem", "test2", "bytes").set(new byte[] {1, 2, 3});
        model.get("profile", "profileB", "subsystem", "test2", "double").set(Double.MAX_VALUE);
        model.get("profile", "profileB", "subsystem", "test2", "expression").setExpression("{expr}");
        model.get("profile", "profileB", "subsystem", "test2", "int").set(102);
        model.get("profile", "profileB", "subsystem", "test2", "list").add("l1A");
        model.get("profile", "profileB", "subsystem", "test2", "list").add("l1B");
        model.get("profile", "profileB", "subsystem", "test2", "long").set(Long.MAX_VALUE);
        model.get("profile", "profileB", "subsystem", "test2", "object", "value").set("objVal");
        model.get("profile", "profileB", "subsystem", "test2", "property").set(new Property("prop1", new ModelNode().set("value1")));
        model.get("profile", "profileB", "subsystem", "test2", "string1").set("s1");
        model.get("profile", "profileB", "subsystem", "test2", "string2").set("s2");
        model.get("profile", "profileB", "subsystem", "test2", "type").set(ModelType.TYPE);

        model.get("profile", "profileC", "name").set("Profile C");

        return model;
    }




    @Test
    public void testRecursiveReadSubModelOperationSimple() throws Exception {
        ModelNode operation = createOperation(READ_SUB_MODEL_OPERATION, "profile", "profileA");
        operation.get(REQUEST_PROPERTIES, RECURSIVE).set(true);

        ModelNode result = CONTROLLER.execute(operation);
        assertNotNull(result);

        assertEquals(2, result.keys().size());
        assertEquals("Profile A", result.require("name").asString());
        ModelNode content = result.require("subsystem", "test1");
        List<ModelNode> list = content.asList();
        assertEquals(2, list.size());
        assertEquals(1, list.get(0).asInt());
        assertEquals(2, list.get(1).asInt());
    }

    @Test
    public void testNonRecursiveReadSubModelOperationSimple() throws Exception {
        ModelNode operation = createOperation(READ_SUB_MODEL_OPERATION, "profile", "profileA");
        operation.get(REQUEST_PROPERTIES, RECURSIVE).set(false);

        ModelNode result = CONTROLLER.execute(operation);
        assertNotNull(result);

        assertEquals(2, result.keys().size());
        assertEquals("Profile A", result.require("name").asString());
        assertFalse(result.require("subsystem").isDefined());
    }

    @Test
    public void testRecursiveReadSubModelOperationComplex() throws Exception {
        ModelNode operation = createOperation(READ_SUB_MODEL_OPERATION, "profile", "profileB", "subsystem", "test2");
        operation.get(REQUEST_PROPERTIES, RECURSIVE).set(true);


        ModelNode result = CONTROLLER.execute(operation);
        assertNotNull(result);
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
        assertEquals("objVal", result.require("object", "value").asString());
        Property prop = result.require("property").asProperty();
        assertEquals("prop1", prop.getName());
        assertEquals("value1", prop.getValue().asString());
        assertEquals("s1", result.require("string1").asString());
        assertEquals("s2", result.require("string2").asString());
        assertEquals(ModelType.TYPE, result.require("type").asType());
    }

    @Test
    public void testNonRecursiveReadSubModelOperationComplex() throws Exception {
        ModelNode operation = createOperation(READ_SUB_MODEL_OPERATION, "profile", "profileB", "subsystem", "test2");
        operation.get(REQUEST_PROPERTIES, RECURSIVE).set(false);


        ModelNode result = CONTROLLER.execute(operation);
        assertNotNull(result);
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

        assertEquals(Long.MAX_VALUE, result.require("long").asLong());
        assertEquals("s1", result.require("string1").asString());
        assertEquals("s2", result.require("string2").asString());
        assertEquals(ModelType.TYPE, result.require("type").asType());

        assertFalse(result.require("list").isDefined());
        assertFalse(result.require("object").isDefined());
        assertFalse(result.require("property").isDefined());
    }

    @Test
    public void testReadNamedAttributeValue() throws Exception {
        ModelNode operation = createOperation(READ_NAMED_ATTRIBUTE_OPERATION, "profile", "profileB", "subsystem", "test2");

        operation.get(REQUEST_PROPERTIES, NAME).set("int");
        ModelNode result = CONTROLLER.execute(operation);
        assertNotNull(result);
        assertEquals(ModelType.INT, result.getType());
        assertEquals(102, result.asInt());

        operation.get(REQUEST_PROPERTIES, NAME).set("string1");
        result = CONTROLLER.execute(operation);
        assertNotNull(result);
        assertEquals(ModelType.STRING, result.getType());
        assertEquals("s1", result.asString());

        operation.get(REQUEST_PROPERTIES, NAME).set("list");
        result = CONTROLLER.execute(operation);
        assertNotNull(result);
        assertEquals(ModelType.LIST, result.getType());
        List<ModelNode> list = result.asList();
        assertEquals(2, list.size());
        assertEquals("l1A", list.get(0).asString());
        assertEquals("l1B", list.get(1).asString());
    }

    @Test
    public void testReadAttributesOfType() throws Exception {
        ModelNode operation = createOperation(READ_ATTRIBUTES_OF_TYPE_OPERATION, "profile", "profileB", "subsystem", "test2");

        operation.get(REQUEST_PROPERTIES, TYPE).set(ModelType.INT);
        ModelNode result = CONTROLLER.execute(operation);
        assertNotNull(result);
        assertEquals(ModelType.LIST, result.getType());
        List<ModelNode> list = result.asList();
        assertEquals(1, list.size());
        assertEquals(ModelType.PROPERTY, list.get(0).getType());
        assertEquals("int", list.get(0).asProperty().getName());
        assertEquals(102, list.get(0).asProperty().getValue().asInt());

        operation.get(REQUEST_PROPERTIES, TYPE).set(ModelType.STRING);
        result = CONTROLLER.execute(operation);
        assertNotNull(result);
        assertEquals(ModelType.LIST, result.getType());
        list = result.asList();
        assertEquals(2, list.size());
        assertEquals(ModelType.PROPERTY, list.get(0).getType());
        assertEquals("string1", list.get(0).asProperty().getName());
        assertEquals("s1", list.get(0).asProperty().getValue().asString());
        assertEquals(ModelType.PROPERTY, list.get(1).getType());
        assertEquals("string2", list.get(1).asProperty().getName());
        assertEquals("s2", list.get(1).asProperty().getValue().asString());

    }

    private ModelNode createOperation(String operationName, String...address) {
        ModelNode operation = new ModelNode();
        operation.get(OPERATION_NAME).set(operationName);
        for (String addr : address) {
            operation.get(ADDRESS).add(addr);
        }

        return operation;
    }

    private static class TestModelController extends BasicModelController {
        protected TestModelController() {
            super(MODEL, null);

            getRegistry().registerOperationHandler(READ_SUB_MODEL_OPERATION, GlobalOperationHandlers.READ_SUB_MODEL, GlobalDescriptions.getReadSubModelOperationDescription(), true);
            getRegistry().registerOperationHandler(READ_NAMED_ATTRIBUTE_OPERATION, GlobalOperationHandlers.READ_NAMED_ATTRIBUTE, GlobalDescriptions.getReadNamedAttributeValueOperationDescription(), true);
            getRegistry().registerOperationHandler(READ_ATTRIBUTES_OF_TYPE_OPERATION, GlobalOperationHandlers.READ_ATTRIBUTE_OF_TYPE, GlobalDescriptions.getReadAttributesOfTypeOperationDescription(), true);
        }
    }

}
