/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ACCESS_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILD_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_ALIASES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_RUNTIME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INHERITED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_OCCURS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODEL_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_RESOURCES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_TYPES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STORAGE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import junit.framework.Assert;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationContext.Stage;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.operations.global.GlobalOperationHandlers;
import org.jboss.as.controller.operations.global.ReadAttributeHandler;
import org.jboss.as.controller.operations.global.WriteAttributeHandler;
import org.jboss.as.controller.operations.global.WriteAttributeHandlers;
import org.jboss.as.controller.registry.AliasEntry;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.junit.Test;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class AliasResourceTestCase extends AbstractControllerTestBase {

    private static final String CORE = "core";
    private static final String ALIASED = "aliased";
    static final String MODEL = "model";
    private static final String CHILD = "child";
    private static final String KID_MODEL = "kid";
    private static final String KID_ALIASED = "kid-aliased";

    @Test
    public void checkReadResourceDescription() throws Exception {
        //Recursive
        ModelNode op = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION);
        op.get(RECURSIVE).set(true);
        op.get(OPERATIONS).set(true);
        op.get(INHERITED).set(false);
        ModelNode result = executeForResult(op);
        Assert.assertTrue(result.get(CHILDREN, CORE, MODEL_DESCRIPTION, MODEL).isDefined());
        Assert.assertFalse(result.get(CHILDREN, ALIASED, MODEL_DESCRIPTION, MODEL).isDefined());

        op = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION);
        op.get(RECURSIVE).set(true);
        op.get(OPERATIONS).set(true);
        op.get(INCLUDE_ALIASES).set(true);
        op.get(INHERITED).set(false);
        result = executeForResult(op);
        Assert.assertTrue(result.get(CHILDREN, CORE, MODEL_DESCRIPTION, MODEL).isDefined());
        Assert.assertTrue(result.get(CHILDREN, ALIASED, MODEL_DESCRIPTION, MODEL).isDefined());

        checkReadResourceDescription(result.get(CHILDREN, CORE, MODEL_DESCRIPTION, MODEL), true);
        checkReadResourceDescription(result.get(CHILDREN, ALIASED, MODEL_DESCRIPTION, MODEL), true);

        //Then check each thing on its own
        op = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, CORE, MODEL);
        op.get(RECURSIVE).set(true);
        op.get(OPERATIONS).set(true);
        op.get(INHERITED).set(false);
        result = executeForResult(op);
        Assert.assertTrue(result.isDefined());
        checkReadResourceDescription(result, false);

        op = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, CORE, MODEL);
        op.get(RECURSIVE).set(true);
        op.get(OPERATIONS).set(true);
        op.get(INHERITED).set(false);
        op.get(INCLUDE_ALIASES).set(true);
        result = executeForResult(op);
        Assert.assertTrue(result.isDefined());
        checkReadResourceDescription(result, true);

        op = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, ALIASED, MODEL);
        op.get(RECURSIVE).set(true);
        op.get(OPERATIONS).set(true);
        op.get(INHERITED).set(false);
        result = executeForResult(op);
        Assert.assertTrue(result.isDefined());
        checkReadResourceDescription(result, false);

        op = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, ALIASED, MODEL);
        op.get(RECURSIVE).set(true);
        op.get(OPERATIONS).set(true);
        op.get(INHERITED).set(false);
        op.get(INCLUDE_ALIASES).set(true);
        result = executeForResult(op);
        Assert.assertTrue(result.isDefined());
        checkReadResourceDescription(result, true);

        op = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, CORE, MODEL, CHILD, KID_MODEL);
        op.get(RECURSIVE).set(true);
        op.get(OPERATIONS).set(true);
        op.get(INHERITED).set(false);
        result = executeForResult(op);
        Assert.assertTrue(result.isDefined());
        checkChildResourceDescription(result);

        op = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, ALIASED, MODEL, CHILD, KID_MODEL);
        op.get(RECURSIVE).set(true);
        op.get(OPERATIONS).set(true);
        op.get(INHERITED).set(false);
        result = executeForResult(op);
        Assert.assertTrue(result.isDefined());
        checkChildResourceDescription(result);

        op = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, CORE, MODEL, CHILD, KID_ALIASED);
        op.get(RECURSIVE).set(true);
        op.get(OPERATIONS).set(true);
        op.get(INHERITED).set(false);
        result = executeForResult(op);
        Assert.assertTrue(result.isDefined());
        checkChildResourceDescription(result);

        op = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, ALIASED, MODEL, CHILD, KID_ALIASED);
        op.get(RECURSIVE).set(true);
        op.get(OPERATIONS).set(true);
        op.get(INHERITED).set(false);
        result = executeForResult(op);
        Assert.assertTrue(result.isDefined());
        checkChildResourceDescription(result);
    }

    @Test
    public void testAliasedAttributes() throws Exception {

    }

    @Test
    public void testAliasedResourceAndAttributeReadWriteFromCore() throws Exception {
        testAliasedResourceAndAttributeReadWrite(CORE, ALIASED);
    }

    @Test
    public void testAliasedResourceAndAttributeReadWriteFromAliased() throws Exception {
        testAliasedResourceAndAttributeReadWrite(ALIASED, CORE);
    }

    @Test
    public void testInvokeOpFromCore() throws Exception {
        checkInvokeOp(CORE, ALIASED);
    }

    @Test
    public void testInvokeOpFromAliased() throws Exception {
        checkInvokeOp(ALIASED, CORE);

    }

    @Test
    public void readChildrenTypes() throws Exception {
        ModelNode op = createOperation(READ_CHILDREN_TYPES_OPERATION);
        ModelNode result = executeForResult(op);
        List<ModelNode> list = result.asList();
        Assert.assertEquals(2, list.size());
        Assert.assertTrue(list.contains(new ModelNode(CORE)));
        Assert.assertTrue(list.contains(new ModelNode(ALIASED)));

        op.get(OP_ADDR).setEmptyList().add(CORE, MODEL);
        result = executeForResult(op);
        list = result.asList();
        Assert.assertEquals(1, list.size());
        Assert.assertTrue(list.contains(new ModelNode(CHILD)));

        op.get(OP_ADDR).setEmptyList().add(ALIASED, MODEL);
        result = executeForResult(op);
        list = result.asList();
        Assert.assertEquals(1, list.size());
        Assert.assertTrue(list.contains(new ModelNode(CHILD)));
    }

    @Test
    public void testReadChildresNamesNoModel() throws Exception {
        ModelNode op = createOperation(READ_CHILDREN_NAMES_OPERATION);
        op.get(CHILD_TYPE).set(CORE);
        ModelNode result = executeForResult(op);
        List<ModelNode> list = result.asList();
        Assert.assertEquals(0, list.size());

        op.get(CHILD_TYPE).set(ALIASED);
        result = executeForResult(op);
        list = result.asList();
        Assert.assertEquals(0, list.size());
    }

    @Test
    public void testReadChildresNamesWithModel() throws Exception {
        addCore(CORE);
        addChild(CORE);
        ModelNode op = createOperation(READ_CHILDREN_NAMES_OPERATION);
        op.get(CHILD_TYPE).set(CORE);
        ModelNode result = executeForResult(op);
        List<ModelNode> list = result.asList();
        Assert.assertEquals(1, list.size());
        Assert.assertTrue(list.contains(new ModelNode(MODEL)));

        op.get(CHILD_TYPE).set(ALIASED);
        result = executeForResult(op);
        list = result.asList();
        Assert.assertEquals(1, list.size());
        Assert.assertTrue(list.contains(new ModelNode(MODEL)));

        op = createOperation(READ_CHILDREN_NAMES_OPERATION, CORE, MODEL);
        op.get(CHILD_TYPE).set(CHILD);
        result = executeForResult(op);
        list = result.asList();
        Assert.assertEquals(2, list.size());
        Assert.assertTrue(list.contains(new ModelNode(KID_MODEL)));
        Assert.assertTrue(list.contains(new ModelNode(KID_ALIASED)));

        op = createOperation(READ_CHILDREN_NAMES_OPERATION, ALIASED, MODEL);
        op.get(CHILD_TYPE).set(CHILD);
        result = executeForResult(op);
        list = result.asList();
        Assert.assertEquals(2, list.size());
        Assert.assertTrue(list.contains(new ModelNode(KID_MODEL)));
        Assert.assertTrue(list.contains(new ModelNode(KID_ALIASED)));
    }

    @Test
    public void testReadChildrenResources() throws Exception {
        addCore(CORE);
        addChild(CORE);
        ModelNode op = createOperation(READ_CHILDREN_RESOURCES_OPERATION);
        op.get(CHILD_TYPE).set(CORE);
        ModelNode result = executeForResult(op);
        Assert.assertTrue(result.hasDefined(MODEL));
        checkResource(false, result.get(MODEL), "R/O", "R/W", null, null);

        op.get(CHILD_TYPE).set(ALIASED);
        result = executeForResult(op);
        Assert.assertTrue(result.hasDefined(MODEL));
        checkResource(false, result.get(MODEL), "R/O", "R/W", null, null);

        op = createOperation(READ_CHILDREN_RESOURCES_OPERATION);
        op.get(CHILD_TYPE).set(CORE);
        op.get(RECURSIVE).set(true);
        result = executeForResult(op);
        Assert.assertTrue(result.hasDefined(MODEL));
        checkResource(false, result.get(MODEL), "R/O", "R/W", null, "R/W 2");

        op.get(CHILD_TYPE).set(ALIASED);
        result = executeForResult(op);
        Assert.assertTrue(result.hasDefined(MODEL));
        checkResource(false, result.get(MODEL), "R/O", "R/W", null, "R/W 2");

        op = createOperation(READ_CHILDREN_RESOURCES_OPERATION, CORE, MODEL);
        op.get(CHILD_TYPE).set(CHILD);
        result = executeForResult(op);
        Assert.assertEquals("R/W 2", result.get(KID_MODEL, "rw").asString());

        op = createOperation(READ_CHILDREN_RESOURCES_OPERATION, ALIASED, MODEL);
        op.get(CHILD_TYPE).set(CHILD);
        result = executeForResult(op);
        Assert.assertEquals("R/W 2", result.get(KID_MODEL, "rw").asString());
    }

    @Test
    public void testReadOperationNamesCore() throws Exception {
        readOperationNames(CORE);
    }

    @Test
    public void testReadOperationNamesAliased() throws Exception {
        readOperationNames(ALIASED);
    }

    @Test
    public void testReadOperationDescriptionCore() throws Exception {
        readOperationDescription(CORE);
    }

    @Test
    public void testReadOperationDescriptionAliased() throws Exception {
        readOperationDescription(ALIASED);
    }

    @Test
    public void testDescribeHandler() throws Exception {
       addCore(CORE);
       addChild(CORE);

       ModelNode op = createOperation(DESCRIBE);
       ModelNode result = executeForResult(op);

       List<ModelNode> ops = result.asList();
       Assert.assertEquals(3, ops.size());

       op = ops.get(0);
       Assert.assertEquals(2, op.keys().size());
       Assert.assertEquals(ADD, op.get(OP).asString());
       Assert.assertEquals(new ModelNode().setEmptyList(), op.get(OP_ADDR));

       op = ops.get(1);
       Assert.assertEquals(4, op.keys().size());
       Assert.assertEquals(ADD, op.get(OP).asString());
       Assert.assertEquals(new ModelNode().add(CORE, MODEL), op.get(OP_ADDR));
       Assert.assertEquals("R/W", op.get("rw").asString());
       Assert.assertEquals("R/O", op.get("ro").asString());

       op = ops.get(2);
       Assert.assertEquals(3, op.keys().size());
       Assert.assertEquals(ADD, op.get(OP).asString());
       Assert.assertEquals(new ModelNode().add(CORE, MODEL).add(CHILD, KID_MODEL), op.get(OP_ADDR));
       Assert.assertEquals("R/W 2", op.get("rw").asString());
    }


    private void testAliasedResourceAndAttributeReadWrite(String main, String other) throws Exception {
        readResource(true, true, null, null, null, null);

        addCore(main);

        readResource(true, false, "R/O", "R/W", null, null);
        readResource(true, true, "R/O", "R/W", "runtime", null);

        addChild(main);

        readResource(true, false, "R/O", "R/W", null, "R/W 2");
        readResource(true, true, "R/O", "R/W", "runtime", "R/W 2");

        readResource(true, false, "R/O", "R/W", null, "R/W 2", other, MODEL);
        readResource(true, true, "R/O", "R/W", "runtime", "R/W 2", other, MODEL);

        writeAttribute("rw", "abc", other, MODEL);
        Assert.assertEquals("abc", readAttribute("rw", other, MODEL));
        Assert.assertEquals("abc", readAttribute("rwa", other, MODEL));

        writeAttribute("rwa", "123", other, MODEL);
        Assert.assertEquals("123", readAttribute("rw", other, MODEL));
        Assert.assertEquals("123", readAttribute("rwa", other, MODEL));

        undefineAttribute("rw", other, MODEL);
        Assert.assertNull(readAttribute("rw", other, MODEL));
        Assert.assertNull(readAttribute("rwa", other, MODEL));

        writeAttribute("rw", "aliased", other, MODEL, CHILD, KID_ALIASED);
        Assert.assertEquals("aliased", readAttribute("rw", other, MODEL, CHILD, KID_MODEL));

        writeAttribute("rw", "main", other, MODEL, CHILD, KID_MODEL);
        Assert.assertEquals("main", readAttribute("rw", other, MODEL, CHILD, KID_ALIASED));

        ModelNode op = createOperation(REMOVE, main, MODEL);
        executeForResult(op);

        readResource(true, true, null, null, null, null);
    }

    private void checkInvokeOp(String main, String other) throws Exception {
        addCore(main);
        addChild(main);

        ModelNode op = createOperation("core-test", main, MODEL);
        ModelNode result = executeForResult(op);
        Assert.assertEquals("core", result.asString());

        op = createOperation("child-test", other, MODEL, CHILD, KID_MODEL);
        result = executeForResult(op);
        Assert.assertEquals("child", result.asString());

        op = createOperation("child-test", other, MODEL, CHILD, KID_ALIASED);
        result = executeForResult(op);
        Assert.assertEquals("child", result.asString());
    }

    private void addCore(String main) throws Exception {
        ModelNode op = createOperation(ADD, main, MODEL);
        op.get("rw").set("R/W");
        op.get("ro").set("R/O");
        executeForResult(op);
    }

    private void addChild(String main) throws Exception {
        ModelNode op = createOperation(ADD, main, MODEL, CHILD, KID_MODEL);
        op.get("rw").set("R/W 2");
        executeForResult(op);
    }

    private void readResource(boolean includeRuntime, boolean includeAlias, String ro, String rw, String rt, String childRw) throws Exception {
        ModelNode op = createOperation(READ_RESOURCE_OPERATION);
        op.get(RECURSIVE).set(true);
        if (includeRuntime) {
            op.get(INCLUDE_RUNTIME).set(true);
        }
        if (includeAlias) {
            op.get(INCLUDE_ALIASES).set(true);
        }
        ModelNode result = executeForResult(op);

        Assert.assertEquals(includeAlias ? 2 : 1, result.keys().size());
        Assert.assertTrue(result.keys().contains(CORE));
        checkResource(includeAlias, result.get(CORE, MODEL), ro, rw, rt, childRw);
        if (includeAlias) {
            Assert.assertTrue(result.keys().contains(ALIASED));
            checkResource(includeAlias, result.get(ALIASED, MODEL), ro, rw, rt, childRw);
        } else {
            Assert.assertFalse(result.get(ALIASED, MODEL).isDefined());
        }


    }

    private void readResource(boolean includeRuntime, boolean includeAlias, String ro, String rw, String rt, String childRw, String...address) throws Exception {
        ModelNode op = createOperation(READ_RESOURCE_OPERATION, address);
        op.get(RECURSIVE).set(true);
        if (includeRuntime) {
            op.get(INCLUDE_RUNTIME).set(true);
        }
        if (includeAlias) {
            op.get(INCLUDE_ALIASES).set(true);
        }
        ModelNode result = executeForResult(op);
        checkResource(includeAlias, result, ro, rw, rt, childRw);
    }

    private void checkResource(boolean includeAlias, ModelNode model, String ro, String rw, String rt, String childRw) throws Exception {
        if (ro != null && rw != null) {
            Assert.assertEquals(ro, model.get("ro").asString());
            Assert.assertEquals(rw, model.get("rw").asString());
            if (includeAlias) {
                Assert.assertEquals(rw, model.get("rwa").asString());
                Assert.assertEquals(ro, model.get("roa").asString());
            } else {
                Assert.assertFalse(model.hasDefined("rwa"));
                Assert.assertFalse(model.hasDefined("roa"));
            }
            if (rt != null) {
                Assert.assertEquals(rt, model.get("rt").asString());
            }
            if (childRw != null) {
                Assert.assertTrue(model.get(CHILD, KID_MODEL).isDefined());
                Assert.assertEquals(childRw, model.get(CHILD, KID_MODEL, "rw").asString());
                if (includeAlias) {
                    Assert.assertTrue(model.get(CHILD, KID_ALIASED).isDefined());
                    Assert.assertEquals(childRw, model.get(CHILD, KID_ALIASED, "rw").asString());
                }
            } else {
                Assert.assertFalse(model.get(CHILD, KID_MODEL).isDefined());
            }

        } else {
            Assert.assertFalse(model.isDefined());
        }
    }

    private void checkReadResourceDescription(ModelNode resource, boolean aliasedChild) throws Exception {
        Assert.assertEquals("The test resource", resource.get(DESCRIPTION).asString());
        Assert.assertEquals(5, resource.get(ATTRIBUTES).keys().size());
        checkAttribute(resource.get(ATTRIBUTES, "ro"), "R-O attr", AttributeAccess.AccessType.READ_ONLY.toString(), AttributeAccess.Storage.CONFIGURATION.toString());
        checkAttribute(resource.get(ATTRIBUTES, "rw"), "R-W attr", AttributeAccess.AccessType.READ_WRITE.toString(), AttributeAccess.Storage.CONFIGURATION.toString());
        checkAttribute(resource.get(ATTRIBUTES, "rt"), "R-T attr", AttributeAccess.AccessType.READ_ONLY.toString(), AttributeAccess.Storage.RUNTIME.toString());
        checkAttribute(resource.get(ATTRIBUTES, "roa"), "R-O alias", AttributeAccess.AccessType.READ_ONLY.toString(), AttributeAccess.Storage.CONFIGURATION.toString());
        checkAttribute(resource.get(ATTRIBUTES, "rwa"), "R-W alias", AttributeAccess.AccessType.READ_WRITE.toString(), AttributeAccess.Storage.CONFIGURATION.toString());

        Assert.assertEquals(3, resource.get(OPERATIONS).keys().size());
        Assert.assertTrue(resource.get(OPERATIONS, ADD).isDefined());
        Assert.assertTrue(resource.get(OPERATIONS, REMOVE).isDefined());
        Assert.assertTrue(resource.get(OPERATIONS, "core-test").isDefined());

        Assert.assertEquals(1, resource.get(CHILDREN).keys().size());
        Assert.assertTrue(resource.get(CHILDREN, CHILD, MODEL_DESCRIPTION, KID_MODEL).isDefined());
        checkChildResourceDescription(resource.get(CHILDREN, CHILD, MODEL_DESCRIPTION, KID_MODEL));
        if (aliasedChild) {
            Assert.assertTrue(resource.get(CHILDREN, CHILD, MODEL_DESCRIPTION, KID_ALIASED).isDefined());
            checkChildResourceDescription(resource.get(CHILDREN, CHILD, MODEL_DESCRIPTION, KID_ALIASED));
        } else {
            Assert.assertFalse(resource.get(CHILDREN, CHILD, MODEL_DESCRIPTION, KID_ALIASED).isDefined());
        }
    }

    private void checkChildResourceDescription(ModelNode resource) {
        Assert.assertEquals(1, resource.get(ATTRIBUTES).keys().size());
        checkAttribute(resource.get(ATTRIBUTES, "rw"), "R-W attr", AttributeAccess.AccessType.READ_WRITE.toString(), AttributeAccess.Storage.CONFIGURATION.toString());

        Assert.assertEquals(3, resource.get(OPERATIONS).keys().size());
        Assert.assertTrue(resource.get(OPERATIONS, ADD).isDefined());
        Assert.assertTrue(resource.get(OPERATIONS, REMOVE).isDefined());
        Assert.assertTrue(resource.get(OPERATIONS, "child-test").isDefined());

    }

    private void readOperationNames(String core) throws Exception {
        ModelNode op = createOperation(READ_OPERATION_NAMES_OPERATION, core, MODEL);
        ModelNode result = executeForResult(op);
        checkOperationNames(result, "core-test");

        op = createOperation(READ_OPERATION_NAMES_OPERATION, core, MODEL, CHILD, KID_MODEL);
        result = executeForResult(op);
        checkOperationNames(result, "child-test");

        op = createOperation(READ_OPERATION_NAMES_OPERATION, core, MODEL, CHILD, KID_ALIASED);
        result = executeForResult(op);
        checkOperationNames(result, "child-test");
    }

    private void checkOperationNames(ModelNode result, String unique) {
        Assert.assertTrue(result.asList().contains(new ModelNode(ADD)));
        Assert.assertTrue(result.asList().contains(new ModelNode(REMOVE)));
        Assert.assertTrue(result.asList().contains(new ModelNode(unique)));
    }

    private void readOperationDescription(String core) throws Exception {
        ModelNode op = createOperation(READ_OPERATION_DESCRIPTION_OPERATION, core, MODEL);
        op.get(NAME).set(ADD);
        ModelNode result = executeForResult(op);
        Assert.assertEquals(ADD, result.get(OPERATION_NAME).asString());

        op = createOperation(READ_OPERATION_DESCRIPTION_OPERATION, core, MODEL);
        op.get(NAME).set(REMOVE);
        result = executeForResult(op);
        Assert.assertEquals(REMOVE, result.get(OPERATION_NAME).asString());

        op = createOperation(READ_OPERATION_DESCRIPTION_OPERATION, core, MODEL);
        op.get(NAME).set("core-test");
        result = executeForResult(op);
        Assert.assertEquals("core-test", result.get(OPERATION_NAME).asString());

        readOperationDescriptionForChild(core, KID_MODEL);
        readOperationDescriptionForChild(core, KID_ALIASED);
    }

    private void readOperationDescriptionForChild(String core, String child) throws Exception {
        ModelNode op = createOperation(READ_OPERATION_DESCRIPTION_OPERATION, core, MODEL, CHILD, child);
        op.get(NAME).set(ADD);
        ModelNode result = executeForResult(op);
        Assert.assertEquals(ADD, result.get(OPERATION_NAME).asString());

        op = createOperation(READ_OPERATION_DESCRIPTION_OPERATION, core, MODEL, CHILD, child);
        op.get(NAME).set(REMOVE);
        result = executeForResult(op);
        Assert.assertEquals(REMOVE, result.get(OPERATION_NAME).asString());

        op = createOperation(READ_OPERATION_DESCRIPTION_OPERATION, core, MODEL, CHILD, child);
        op.get(NAME).set("child-test");
        result = executeForResult(op);
        Assert.assertEquals("child-test", result.get(OPERATION_NAME).asString());
    }

    private void writeAttribute(String name, String value, String...address) throws Exception {
        ModelNode op = createOperation(WRITE_ATTRIBUTE_OPERATION, address);
        op.get(NAME).set(name);
        op.get(VALUE).set(value);
        executeForResult(op);
    }

    private String readAttribute(String name, String...address) throws Exception {
        ModelNode op = createOperation(READ_ATTRIBUTE_OPERATION, address);
        op.get(NAME).set(name);
        ModelNode result =  executeForResult(op);
        if (result.isDefined()) {
            return result.asString();
        }
        return null;
    }

    private void undefineAttribute(String name, String...address) throws Exception {
        ModelNode op = createOperation(UNDEFINE_ATTRIBUTE_OPERATION, address);
        op.get(NAME).set(name);
        executeForResult(op);
    }


    private void checkAttribute(ModelNode attr, String description, String accessType, String storage) {
        Assert.assertTrue(attr.isDefined());
        Assert.assertEquals(description, attr.get(DESCRIPTION).asString());
        Assert.assertEquals(accessType, attr.get(ACCESS_TYPE).asString());
        Assert.assertEquals(storage, attr.get(STORAGE).asString());
    }

    @Override
    protected DescriptionProvider getRootDescriptionProvider() {
        return new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                ModelNode node = new ModelNode();
                node.get(DESCRIPTION).set("The root node of the test management API");
                node.get(CHILDREN, CORE, DESCRIPTION).set("The core model");
                node.get(CHILDREN, CORE, MIN_OCCURS).set(0);
                node.get(CHILDREN, CORE, MODEL_DESCRIPTION);
                node.get(CHILDREN, ALIASED, DESCRIPTION).set("The aliased model");
                node.get(CHILDREN, ALIASED, MIN_OCCURS).set(0);
                node.get(CHILDREN, ALIASED, MODEL_DESCRIPTION);
                return node;
            }
        };
    }

    @Override
    protected void initModel(Resource rootResource, ManagementResourceRegistration registration) {
        GlobalOperationHandlers.registerGlobalOperations(registration, processType);

        registration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);

        ManagementResourceRegistration coreResourceRegistration = registration.registerSubModel(new CoreResourceDefinition());
        registration.registerAlias(getAliasedModelElement(),
                new TestAliasEntry(coreResourceRegistration));

        ManagementResourceRegistration childReg = coreResourceRegistration.registerSubModel(new ChildResourceDefinition());
        coreResourceRegistration.registerAlias(getAliasedChildModelElement(),
                new TestAliasEntry(childReg));
    }

    private PathElement getCoreModelElement() {
        return PathElement.pathElement(CORE, MODEL);
    }

    private PathElement getAliasedModelElement() {
        return PathElement.pathElement(ALIASED, MODEL);
    }

    private PathElement getChildModelElement() {
        return PathElement.pathElement(CHILD, KID_MODEL);
    }

    private PathElement getAliasedChildModelElement() {
        return PathElement.pathElement(CHILD, KID_ALIASED);
    }

    private static SimpleAttributeDefinition READ_WRITE = new SimpleAttributeDefinition("rw", ModelType.STRING, true);
    private static SimpleAttributeDefinition READ_ONLY = new SimpleAttributeDefinition("ro", ModelType.STRING, false);
    private static SimpleAttributeDefinition RUNTIME = SimpleAttributeDefinitionBuilder.create("rt", ModelType.STRING, false).setStorageRuntime().build();
    private static SimpleAttributeDefinition READ_WRITE_ALIAS = SimpleAttributeDefinitionBuilder.create("rwa", READ_WRITE)
            .setFlags(AttributeAccess.Flag.ALIAS)
            .build();
    private static SimpleAttributeDefinition READ_ONLY_ALIAS = SimpleAttributeDefinitionBuilder.create("roa", READ_ONLY)
            .setFlags(AttributeAccess.Flag.ALIAS)
            .build();

    private class CoreResourceDefinition extends SimpleResourceDefinition {

        public CoreResourceDefinition() {
            super(getCoreModelElement(), createResourceDescriptionResolver(), new CoreAddHandler(), new CoreRemoveHandler());
        }

        @Override
        public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
            resourceRegistration.registerReadWriteAttribute(READ_WRITE, null, new WriteAttributeHandlers.StringLengthValidatingHandler(1, true));
            resourceRegistration.registerReadOnlyAttribute(READ_ONLY, null);
            resourceRegistration.registerReadOnlyAttribute(RUNTIME, new CoreRuntimeHandler());
            resourceRegistration.registerReadWriteAttribute(READ_WRITE_ALIAS, TestAttributeAliasHandler.READ_WRITE_ALIAS, TestAttributeAliasHandler.READ_WRITE_ALIAS);
            resourceRegistration.registerReadOnlyAttribute(READ_ONLY_ALIAS, TestAttributeAliasHandler.READ_ONLY_ALIAS);
        }


        @Override
        public void registerOperations(ManagementResourceRegistration resourceRegistration) {
            super.registerOperations(resourceRegistration);
            resourceRegistration.registerOperationHandler("core-test", new TestOperationHandler("core"), new DescriptionProvider() {
                @Override
                public ModelNode getModelDescription(Locale locale) {
                    ModelNode node = new ModelNode();
                    node.get(OPERATION_NAME).set("core-test");
                    node.get(DESCRIPTION).set("An op");
                    return node;
                }
            });
        }
    }

    private class ChildResourceDefinition extends SimpleResourceDefinition {
        public ChildResourceDefinition() {
            super(getChildModelElement(), createResourceDescriptionResolver(), new ChildAddHandler(), new ChildRemoveHandler());
        }

        @Override
        public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
            resourceRegistration.registerReadWriteAttribute(READ_WRITE, null, new WriteAttributeHandlers.StringLengthValidatingHandler(1, false));
        }

        @Override
        public void registerOperations(ManagementResourceRegistration resourceRegistration) {
            super.registerOperations(resourceRegistration);
            resourceRegistration.registerOperationHandler("child-test", new TestOperationHandler("child"), new DescriptionProvider() {

                @Override
                public ModelNode getModelDescription(Locale locale) {
                    ModelNode node = new ModelNode();
                    node.get(OPERATION_NAME).set("child-test");
                    node.get(DESCRIPTION).set("An op");
                    return node;
                }
            });
        }
    }

    static ResourceDescriptionResolver createResourceDescriptionResolver() {
        final Map<String, String> strings = new HashMap<String, String>();
        strings.put("test", "The test resource");
        strings.put("test.ro", "R-O attr");
        strings.put("test.rw", "R-W attr");
        strings.put("test.rt", "R-T attr");
        strings.put("test.roa", "R-O alias");
        strings.put("test.rwa", "R-W alias");
        strings.put("test.add", "Add test resource");
        strings.put("test.remove", "Remove test resource");
        strings.put("test.child", "Remove test resource");


        return new StandardResourceDescriptionResolver("test", AliasResourceTestCase.class.getName() + ".properties", AliasResourceTestCase.class.getClassLoader(), true, false) {

            @Override
            public ResourceBundle getResourceBundle(Locale locale) {
                return new ResourceBundle() {

                    @Override
                    protected Object handleGetObject(String key) {
                        return strings.get(key);
                    }

                    @Override
                    public Enumeration<String> getKeys() {
                        return null;
                    }
                };
            }

        };
    }

    private static class CoreAddHandler extends AbstractAddStepHandler {
        @Override
        protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
            READ_WRITE.validateAndSet(operation, model);
            READ_ONLY.validateAndSet(operation, model);
        }
    }

    private static class CoreRemoveHandler extends AbstractRemoveStepHandler {
        protected void performRemove(OperationContext context, final ModelNode operation, final ModelNode model) throws OperationFailedException {
            super.performRemove(context, operation, model);
        }
    }

    private static class CoreRuntimeHandler implements OperationStepHandler {

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            context.getResult().set("runtime");
            context.stepCompleted();
        }
    }

    private static class ChildAddHandler extends AbstractAddStepHandler {
        @Override
        protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
            READ_WRITE.validateAndSet(operation, model);
        }
    }

    private static class ChildRemoveHandler extends AbstractRemoveStepHandler {
        protected void performRemove(OperationContext context, final ModelNode operation, final ModelNode model) throws OperationFailedException {
            super.performRemove(context, operation, model);
        }
    }

    private static class TestOperationHandler implements OperationStepHandler {
        final String value;

        public TestOperationHandler(String value) {
            this.value = value;
        }


        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            context.getResult().set(value);
            context.stepCompleted();
        }
    }

    public static class TestAliasEntry extends AliasEntry {

        public TestAliasEntry(final ManagementResourceRegistration target) {
            super(target);
        }

        @Override
        public PathAddress convertToTargetAddress(PathAddress addr) {
            if (addr.size() < getAliasAddress().size()) {
                throw new IllegalArgumentException("TODO i18n -  Expected an address under " + getAliasAddress() + ", was " + addr);
            }

            PathAddress mapped = getTargetAddress();
            PathAddress relative = addr.subAddress(getAliasAddress().size());
            return mapped.append(relative);
        }
    }

    private static class TestAttributeAliasHandler implements OperationStepHandler {
        private final String targetAttribute;

        static final TestAttributeAliasHandler READ_ONLY_ALIAS = new TestAttributeAliasHandler("ro");

        static final TestAttributeAliasHandler READ_WRITE_ALIAS = new TestAttributeAliasHandler("rw");

        public TestAttributeAliasHandler(String targetAttribute) {
            this.targetAttribute = targetAttribute;
        }

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            OperationStepHandler step =  operation.get(OP).asString().equals(WRITE_ATTRIBUTE_OPERATION) ? WriteAttributeHandler.INSTANCE : ReadAttributeHandler.INSTANCE;
            ModelNode aliasOp = operation.clone();
            aliasOp.get(NAME).set(targetAttribute);
            context.addStep(aliasOp, step, Stage.IMMEDIATE);
            context.stepCompleted();
        }


    }
}
