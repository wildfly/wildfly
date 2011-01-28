/*
* JBoss, Home of Professional Open Source.
* Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILD_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_LENGTH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_OCCURS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODEL_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import java.io.OutputStream;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.jboss.as.controller.BasicModelController;
import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.OperationHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.common.CommonProviders;
import org.jboss.as.controller.operations.global.GlobalOperationHandlers;
import org.jboss.as.controller.operations.global.WriteAttributeHandlers;
import org.jboss.as.controller.persistence.ConfigurationPersistenceException;
import org.jboss.as.controller.persistence.NewConfigurationPersister;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public abstract class AbstractProxyControllerTest {

    private ModelNode proxyModel;
    private ModelNode mainModel;
    private TestMainModelController mainController;
    private TestProxyModelController proxyController;

    @Before
    public void setupNodes() {
        proxyModel = new ModelNode();
        proxyModel.get("hostchild", "hcA", "name").set("hostA");
        proxyModel.get("hostchild", "hcA", "child", "childA", "name").set("childName");
        proxyModel.get("hostchild", "hcA", "child", "childA", "value").set("childValue");

        mainModel = new ModelNode();
        mainModel.get("host", "hostA");  //Create an empty node to be got from the proxied model
        mainModel.get("profile", "profileA").get(NAME).set("Profile A");

        mainController = new TestMainModelController(mainModel);
        proxyController = new TestProxyModelController(proxyModel);
    }

    @Test
    public void testRecursiveReadResourceDescriptionOperation() throws Exception {
        ModelNode operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION);
        operation.get(RECURSIVE).set(true);

        ModelNode result = proxyController.execute(operation);

        checkHostSubModelDescription(result, false);

        result = mainController.execute(operation);
        checkRootSubModelDescription(result, false);
    }

    @Test
    public void testRecursiveReadResourceDescriptionOperationForAddressInOtherController() throws Exception {
        ModelNode operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, "host", "hostA");
        operation.get(RECURSIVE).set(true);

        ModelNode result = mainController.execute(operation);

        checkHostSubModelDescription(result, false);

        result = mainController.execute(operation);
        checkHostSubModelDescription(result, false);
    }

    @Test
    public void testRecursiveReadResourceDescriptionWithOperations() throws Exception {
        ModelNode operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION);
        operation.get(RECURSIVE).set(true);
        operation.get(OPERATIONS).set(true);

        ModelNode result = mainController.execute(operation);
        checkRootSubModelDescription(result, true);
    }

    @Test
    public void testRecursiveReadSubModelOperation() throws Exception {
        ModelNode operation = createOperation(READ_RESOURCE_OPERATION);
        operation.get(RECURSIVE).set(true);

        ModelNode result = proxyController.execute(operation);
        checkHostNode(result);

        result = mainController.execute(operation);
        checkRootNode(result);
    }

    @Test
    public void testRecursiveReadSubModelOperationForAddressInOtherController() throws Exception {
        ModelNode operation = createOperation(READ_RESOURCE_OPERATION, "host", "hostA");
        operation.get(RECURSIVE).set(true);

        ModelNode result = mainController.execute(operation);
        checkHostNode(result);
    }

    @Test
    public void testWriteAttributeOperation() throws Exception {
        ModelNode write = createOperation(WRITE_ATTRIBUTE_OPERATION, "hostchild", "hcA", "child", "childA");
        write.get(NAME).set("value");
        write.get(VALUE).set("NewValue");
        proxyController.execute(write);

        assertEquals("NewValue", proxyModel.get("hostchild", "hcA", "child", "childA", "value").asString());

        ModelNode read = createOperation(READ_RESOURCE_OPERATION, "hostchild", "hcA", "child", "childA");
        read.get(RECURSIVE).set(true);
        ModelNode result = proxyController.execute(read);
        assertEquals("NewValue", result.get("value").asString());

        read = createOperation(READ_RESOURCE_OPERATION, "host", "hostA", "hostchild", "hcA", "child", "childA");
        read.get(RECURSIVE).set(true);
        result = mainController.execute(read);
        assertEquals("NewValue", result.get("value").asString());
    }

    @Test
    public void testWriteAttributeOperationInChildController() throws Exception {
        ModelNode write = createOperation(WRITE_ATTRIBUTE_OPERATION, "host", "hostA", "hostchild", "hcA", "child", "childA");
        write.get(NAME).set("value");
        write.get(VALUE).set("NewValue2");
        mainController.execute(write);

        assertEquals("NewValue2", proxyModel.get("hostchild", "hcA", "child", "childA", "value").asString());

        ModelNode read = createOperation(READ_RESOURCE_OPERATION, "hostchild", "hcA", "child", "childA");
        read.get(RECURSIVE).set(true);
        ModelNode result = proxyController.execute(read);
        assertEquals("NewValue2", result.get("value").asString());

        read = createOperation(READ_RESOURCE_OPERATION, "host", "hostA", "hostchild", "hcA", "child", "childA");
        read.get(RECURSIVE).set(true);
        result = mainController.execute(read);
        assertEquals("NewValue2", result.get("value").asString());
    }

    @Test
    public void testReadAttributeOperationInChildController() throws Exception {
        ModelNode read = createOperation(READ_ATTRIBUTE_OPERATION, "hostchild", "hcA", "child", "childA");
        read.get(NAME).set("name");
        ModelNode result = proxyController.execute(read);
        assertEquals("childName", result.asString());

        read.get(NAME).set("metric");
        result = proxyController.execute(read);
        assertEquals(ModelType.INT, result.getType());
    }

    @Test
    public void testReadOperationNames() throws Exception {
        ModelNode read = createOperation(READ_OPERATION_NAMES_OPERATION);
        ModelNode result = mainController.execute(read);
        checkOperationNames(result);

        read = createOperation(READ_OPERATION_NAMES_OPERATION, HOST, "hostA");
        result = mainController.execute(read);
        checkOperationNames(result);

        read = createOperation(READ_OPERATION_NAMES_OPERATION,  HOST, "hostA", "hostchild", "hcA");
        result = mainController.execute(read);
        assertTrue(result.asList().isEmpty());
    }

    @Test
    public void testReadOperationDescription() throws Exception {
        ModelNode read = createOperation(READ_OPERATION_DESCRIPTION_OPERATION);
        read.get(NAME).set(READ_ATTRIBUTE_OPERATION);
        ModelNode result = mainController.execute(read);
        checkReadAttributeOperationDescription(result);

        read = createOperation(READ_OPERATION_DESCRIPTION_OPERATION, HOST, "hostA");
        read.get(NAME).set(READ_ATTRIBUTE_OPERATION);
        result = mainController.execute(read);
        checkReadAttributeOperationDescription(result);

        read = createOperation(READ_OPERATION_DESCRIPTION_OPERATION, HOST, "hostA", "hostchild", "hcA");
        read.get(NAME).set(READ_ATTRIBUTE_OPERATION);
        result = mainController.execute(read);
        assertFalse(result.isDefined());
    }

    @Test
    public void testReadChildNames() throws Exception {
        ModelNode read = createOperation(READ_CHILDREN_NAMES_OPERATION);
        read.get(CHILD_TYPE).set("host");
        ModelNode result = mainController.execute(read);
        assertNotNull(result);
        assertEquals(ModelType.LIST, result.getType());
        assertEquals(1, result.asList().size());
        assertEquals("hostA", result.get(0).asString());

        read = createOperation(READ_CHILDREN_NAMES_OPERATION, HOST, "hostA");
        read.get(CHILD_TYPE).set("hostchild");
        result = mainController.execute(read);
        assertNotNull(result);
        assertEquals(ModelType.LIST, result.getType());
        assertEquals(1, result.asList().size());
        assertEquals("hcA", result.get(0).asString());

        read = createOperation(READ_CHILDREN_NAMES_OPERATION, HOST, "hostA", "hostchild", "hcA");
        read.get(CHILD_TYPE).set("child");
        result = mainController.execute(read);
        assertNotNull(result);
        assertEquals(ModelType.LIST, result.getType());
        assertEquals(1, result.asList().size());
        assertEquals("childA", result.get(0).asString());

    }

    private void checkReadAttributeOperationDescription(ModelNode result) {
        assertEquals(READ_ATTRIBUTE_OPERATION, result.get(OPERATION_NAME).asString());
        assertEquals(ModelType.STRING, result.require(REQUEST_PROPERTIES).require(NAME).require(TYPE).asType());
        assertEquals(ModelType.OBJECT, result.require(REPLY_PROPERTIES).require(TYPE).asType());
    }

    private void checkOperationNames(ModelNode operationNamesList) {
        assertTrue(operationNamesList.isDefined());
        assertEquals(ModelType.LIST, operationNamesList.getType());
        assertEquals(7, operationNamesList.asList().size());
    }

    private void checkRootSubModelDescription(ModelNode result, boolean operations) {
        assertEquals("The root node of the test management API", result.get(DESCRIPTION).asString());
        assertEquals("A list of hosts", result.get(CHILDREN, HOST, DESCRIPTION).asString());
        assertEquals(1, result.get(CHILDREN, HOST, MIN_OCCURS).asInt());
        assertEquals(1, result.get(CHILDREN, HOST, MODEL_DESCRIPTION).keys().size());
        assertEquals("A list of profiles", result.get(CHILDREN, PROFILE, DESCRIPTION).asString());
        assertEquals(1, result.get(CHILDREN, PROFILE, MODEL_DESCRIPTION).keys().size());

        if (!operations) {
            assertFalse(result.has(OPERATIONS));
        } else {
            Set<String> ops = result.require(OPERATIONS).keys();
            assertTrue(ops.contains(READ_ATTRIBUTE_OPERATION));
            assertTrue(ops.contains(READ_CHILDREN_NAMES_OPERATION));
            assertTrue(ops.contains(READ_OPERATION_DESCRIPTION_OPERATION));
            assertTrue(ops.contains(READ_OPERATION_NAMES_OPERATION));
            assertTrue(ops.contains(READ_RESOURCE_DESCRIPTION_OPERATION));
            assertTrue(ops.contains(READ_RESOURCE_OPERATION));
            assertTrue(ops.contains(WRITE_ATTRIBUTE_OPERATION));
            for (String op : ops) {
                assertEquals(op, result.require(OPERATIONS).require(op).require(OPERATION_NAME).asString());
            }
        }

        ModelNode proxy = result.get(CHILDREN, HOST, MODEL_DESCRIPTION, "hostA");

        checkHostSubModelDescription(proxy, operations);
    }

    private void checkHostSubModelDescription(ModelNode result, boolean operations) {
        int expectedChildren = operations ? 3 : 2;
        assertEquals(expectedChildren, result.keys().size());
        assertEquals("A host", result.get(DESCRIPTION).asString());
        assertEquals("A list of children", result.get(CHILDREN, "hostchild", DESCRIPTION).asString());
        assertEquals(1, result.get(CHILDREN, "hostchild", MIN_OCCURS).asInt());
        assertEquals(1, result.get(CHILDREN, "hostchild", MODEL_DESCRIPTION).keys().size());

        if (!operations) {
            assertFalse(result.has(OPERATIONS));
        } else {
            Set<String> ops = result.require(OPERATIONS).keys();
            assertTrue(ops.contains(READ_ATTRIBUTE_OPERATION));
            assertTrue(ops.contains(READ_CHILDREN_NAMES_OPERATION));
            assertTrue(ops.contains(READ_OPERATION_DESCRIPTION_OPERATION));
            assertTrue(ops.contains(READ_OPERATION_NAMES_OPERATION));
            assertTrue(ops.contains(READ_RESOURCE_DESCRIPTION_OPERATION));
            assertTrue(ops.contains(READ_RESOURCE_OPERATION));
            assertTrue(ops.contains(WRITE_ATTRIBUTE_OPERATION));
            for (String op : ops) {
                assertEquals(op, result.require(OPERATIONS).require(op).require(OPERATION_NAME).asString());
            }
        }

        checkHostChildSubModelDescription(result.get(CHILDREN, "hostchild", MODEL_DESCRIPTION, "*"), operations);
    }

    private void checkHostChildSubModelDescription(ModelNode result, boolean operations) {
        int expectedChildren = operations ? 4 : 3;
        assertEquals(expectedChildren, result.keys().size());
        assertEquals("A host child", result.get(DESCRIPTION).asString());
        assertEquals(1, result.get(ATTRIBUTES).keys().size());
        assertEquals(ModelType.STRING, result.get(ATTRIBUTES, "name", TYPE).asType());
        assertEquals("The name of the host child", result.get(ATTRIBUTES, "name", DESCRIPTION).asString());
        assertTrue(result.get(ATTRIBUTES, "name", REQUIRED).asBoolean());
        assertEquals(1, result.get(ATTRIBUTES, "name", MIN_LENGTH).asInt());

        assertEquals(1, result.get(CHILDREN).keys().size());
        assertEquals("The children of the host child", result.get(CHILDREN, "child", DESCRIPTION).asString());
        assertEquals(1, result.get(CHILDREN, "child", MIN_OCCURS).asInt());
        assertEquals(1, result.get(CHILDREN, "child", MODEL_DESCRIPTION).keys().size());

        if (!operations) {
            assertFalse(result.has(OPERATIONS));
        } else {
            assertEquals(0, result.require(OPERATIONS).asList().size());
        }


        checkHostChildChildSubModelDescription(result.get(CHILDREN, "child", MODEL_DESCRIPTION, "*"), operations);
    }

    private void checkHostChildChildSubModelDescription(ModelNode result, boolean operations) {
        int expectedChildren = operations ? 3 : 2;
        assertEquals(expectedChildren, result.keys().size());
        assertEquals("A named set of children", result.get(DESCRIPTION).asString());
        assertEquals(2, result.get(ATTRIBUTES).keys().size());
        assertEquals(ModelType.STRING, result.get(ATTRIBUTES, "name", TYPE).asType());
        assertEquals("The name of the child", result.get(ATTRIBUTES, "name", DESCRIPTION).asString());
        assertTrue(result.get(ATTRIBUTES, "name", REQUIRED).asBoolean());
        assertEquals(1, result.get(ATTRIBUTES, "name", MIN_LENGTH).asInt());
        assertEquals(ModelType.STRING, result.get(ATTRIBUTES, "value", TYPE).asType());
        assertEquals("The value of the child", result.get(ATTRIBUTES, "value", DESCRIPTION).asString());
        assertTrue(result.get(ATTRIBUTES, "value", REQUIRED).asBoolean());
        assertEquals(1, result.get(ATTRIBUTES, "value", MIN_LENGTH).asInt());

        if (!operations) {
            assertFalse(result.has(OPERATIONS));
        } else {
            Set<String> ops = result.require(OPERATIONS).keys();
            assertTrue(ops.contains("test-op"));
            for (String op : ops) {
                assertEquals(op, result.require(OPERATIONS).require(op).require(OPERATION_NAME).asString());
            }
        }

    }

    private void checkRootNode(ModelNode result){
        assertEquals(2, result.keys().size());
        assertEquals(1, result.get("host").keys().size());
        checkHostNode(result.get("host", "hostA"));
        assertEquals(1, result.get("profile").keys().size());
        assertEquals("Profile A", result.get("profile", "profileA", NAME).asString());
    }

    private void checkHostNode(ModelNode result){
        assertEquals(1, result.keys().size());
        assertEquals("hostA", result.require("hostchild").require("hcA").require("name").asString());
        assertEquals("childName", result.get("hostchild", "hcA", "child", "childA", "name").asString());
        assertEquals("childValue", result.get("hostchild", "hcA", "child", "childA", "value").asString());
    }

    private ModelNode createOperation(String operationName, String...address) {
        ModelNode operation = new ModelNode();
        operation.get(OP).set(operationName);
        if (address.length > 0) {
            for (String addr : address) {
                operation.get(ADDRESS).add(addr);
            }
        } else {
            operation.get(ADDRESS).setEmptyList();
        }

        return operation;
    }

    protected abstract ProxyController createProxyController(ModelController targetController, PathAddress proxyNodeAddress);

    private static class NullConfigurationPersister implements NewConfigurationPersister{

        @Override
        public void store(ModelNode model) throws ConfigurationPersistenceException {
        }

        @Override
        public void marshallAsXml(ModelNode model, OutputStream output) throws ConfigurationPersistenceException {
        }

        @Override
        public List<ModelNode> load() throws ConfigurationPersistenceException {
            return null;
        }

    }


    class TestMainModelController extends BasicModelController {
        protected TestMainModelController(ModelNode model) {
            super(model, new NullConfigurationPersister(), new DescriptionProvider() {
                @Override
                public ModelNode getModelDescription(Locale locale) {
                    ModelNode node = new ModelNode();
                    node.get(DESCRIPTION).set("The root node of the test management API");
                    node.get(CHILDREN, HOST, DESCRIPTION).set("A list of hosts");
                    node.get(CHILDREN, HOST, MIN_OCCURS).set(1);
                    node.get(CHILDREN, HOST, MODEL_DESCRIPTION);
                    node.get(CHILDREN, PROFILE, DESCRIPTION).set("A list of profiles");
                    node.get(CHILDREN, PROFILE, MODEL_DESCRIPTION);
                    return node;
                }
            });

            getRegistry().registerOperationHandler(READ_RESOURCE_OPERATION, GlobalOperationHandlers.READ_RESOURCE, CommonProviders.READ_RESOURCE_PROVIDER, true);
            getRegistry().registerOperationHandler(READ_ATTRIBUTE_OPERATION, GlobalOperationHandlers.READ_ATTRIBUTE, CommonProviders.READ_ATTRIBUTE_PROVIDER, true);
            getRegistry().registerOperationHandler(READ_RESOURCE_DESCRIPTION_OPERATION, GlobalOperationHandlers.READ_RESOURCE_DESCRIPTION, CommonProviders.READ_RESOURCE_DESCRIPTION_PROVIDER, true);
            getRegistry().registerOperationHandler(READ_CHILDREN_NAMES_OPERATION, GlobalOperationHandlers.READ_CHILDREN_NAMES, CommonProviders.READ_CHILDREN_NAMES_PROVIDER, true);
            getRegistry().registerOperationHandler(READ_OPERATION_NAMES_OPERATION, GlobalOperationHandlers.READ_OPERATION_NAMES, CommonProviders.READ_OPERATION_NAMES_PROVIDER, true);
            getRegistry().registerOperationHandler(READ_OPERATION_DESCRIPTION_OPERATION, GlobalOperationHandlers.READ_OPERATION_DESCRIPTION, CommonProviders.READ_OPERATION_PROVIDER, true);
            getRegistry().registerOperationHandler(WRITE_ATTRIBUTE_OPERATION, GlobalOperationHandlers.WRITE_ATTRIBUTE, CommonProviders.WRITE_ATTRIBUTE_PROVIDER, true);

            ModelNodeRegistration profileReg = getRegistry().registerSubModel(PathElement.pathElement("profile", "*"), new DescriptionProvider() {

                @Override
                public ModelNode getModelDescription(Locale locale) {
                    ModelNode node = new ModelNode();
                    node.get(DESCRIPTION).set("A host child");
                    node.get(ATTRIBUTES, NAME, TYPE).set(ModelType.STRING);
                    node.get(ATTRIBUTES, NAME, DESCRIPTION).set("The name of the profile");
                    node.get(ATTRIBUTES, NAME, REQUIRED).set(true);
                    return node;
                }
            });


        }

        @Override
        public ModelNodeRegistration getRegistry() {
            return super.getRegistry();
        }
    }

    class TestProxyModelController extends BasicModelController {
        protected TestProxyModelController(ModelNode model) {
            super(model, new NullConfigurationPersister(), new DescriptionProvider() {
                @Override
                public ModelNode getModelDescription(Locale locale) {
                    ModelNode node = new ModelNode();
                    node.get(DESCRIPTION).set("A host");
                    node.get(CHILDREN, "hostchild", DESCRIPTION).set("A list of children");
                    node.get(CHILDREN, "hostchild", MIN_OCCURS).set(1);
                    node.get(CHILDREN, "hostchild", MODEL_DESCRIPTION);
                    return node;
                }
            });

            getRegistry().registerOperationHandler(READ_RESOURCE_OPERATION, GlobalOperationHandlers.READ_RESOURCE, CommonProviders.READ_RESOURCE_PROVIDER, true);
            getRegistry().registerOperationHandler(READ_ATTRIBUTE_OPERATION, GlobalOperationHandlers.READ_ATTRIBUTE, CommonProviders.READ_ATTRIBUTE_PROVIDER, true);
            getRegistry().registerOperationHandler(READ_RESOURCE_DESCRIPTION_OPERATION, GlobalOperationHandlers.READ_RESOURCE_DESCRIPTION, CommonProviders.READ_RESOURCE_DESCRIPTION_PROVIDER, true);
            getRegistry().registerOperationHandler(READ_CHILDREN_NAMES_OPERATION, GlobalOperationHandlers.READ_CHILDREN_NAMES, CommonProviders.READ_CHILDREN_NAMES_PROVIDER, true);
            getRegistry().registerOperationHandler(READ_OPERATION_NAMES_OPERATION, GlobalOperationHandlers.READ_OPERATION_NAMES, CommonProviders.READ_OPERATION_NAMES_PROVIDER, true);
            getRegistry().registerOperationHandler(READ_OPERATION_DESCRIPTION_OPERATION, GlobalOperationHandlers.READ_OPERATION_DESCRIPTION, CommonProviders.READ_OPERATION_PROVIDER, true);
            getRegistry().registerOperationHandler(WRITE_ATTRIBUTE_OPERATION, GlobalOperationHandlers.WRITE_ATTRIBUTE, CommonProviders.WRITE_ATTRIBUTE_PROVIDER, true);

            PathElement hostAElement = PathElement.pathElement(HOST, "hostA");
            mainController.getRegistry().registerProxyController(hostAElement, createProxyController(this, PathAddress.pathAddress(hostAElement)));

            ModelNodeRegistration hostReg = getRegistry().registerSubModel(PathElement.pathElement("hostchild", "*"), new DescriptionProvider() {

                @Override
                public ModelNode getModelDescription(Locale locale) {
                    ModelNode node = new ModelNode();
                    node.get(DESCRIPTION).set("A host child");
                    node.get(ATTRIBUTES, NAME, TYPE).set(ModelType.STRING);
                    node.get(ATTRIBUTES, NAME, DESCRIPTION).set("The name of the host child");
                    node.get(ATTRIBUTES, NAME, REQUIRED).set(true);
                    node.get(ATTRIBUTES, NAME, MIN_LENGTH).set(1);
                    node.get(CHILDREN, "child", DESCRIPTION).set("The children of the host child");
                    node.get(CHILDREN, "child", MIN_OCCURS).set(1);
                    node.get(CHILDREN, "child", MODEL_DESCRIPTION);
                    return node;
                }
            });

            ModelNodeRegistration hostChildReg = hostReg.registerSubModel(PathElement.pathElement("child", "*"), new DescriptionProvider() {

                @Override
                public ModelNode getModelDescription(Locale locale) {
                    ModelNode node = new ModelNode();
                    node.get(DESCRIPTION).set("A named set of children");
                    node.get(ATTRIBUTES, NAME, TYPE).set(ModelType.STRING);
                    node.get(ATTRIBUTES, NAME, DESCRIPTION).set("The name of the child");
                    node.get(ATTRIBUTES, NAME, REQUIRED).set(true);
                    node.get(ATTRIBUTES, NAME, MIN_LENGTH).set(1);
                    node.get(ATTRIBUTES, VALUE, TYPE).set(ModelType.STRING);
                    node.get(ATTRIBUTES, VALUE, DESCRIPTION).set("The value of the child");
                    node.get(ATTRIBUTES, VALUE, REQUIRED).set(true);
                    node.get(ATTRIBUTES, VALUE, MIN_LENGTH).set(1);
                    return node;
                }
            });
            hostChildReg.registerReadWriteAttribute("value", null, new WriteAttributeHandlers.ValidatingWriteAttributeOperationHandler(ModelType.STRING), AttributeAccess.Storage.CONFIGURATION);
            hostChildReg.registerMetric("metric", GlobalOperationsTestCase.TestMetricHandler.INSTANCE);

            hostChildReg.registerOperationHandler("test-op",
                    new OperationHandler() {

                        @Override
                        public Cancellable execute(NewOperationContext context, ModelNode operation, ResultHandler resultHandler) {
                            return null;
                        }
                    },
                    new DescriptionProvider() {
                        @Override
                        public ModelNode getModelDescription(Locale locale) {
                            ModelNode node = new ModelNode();
                            node.get(OPERATION_NAME).set("test-op");
                            node.get(REQUEST_PROPERTIES).setEmptyObject();
                            node.get(REPLY_PROPERTIES, DESCRIPTION).setEmptyObject();
                            node.protect();

                            return node;
                        }
                    },
                    false);
        }
    }
}
