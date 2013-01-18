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

package org.jboss.as.controller.transform.description;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import junit.framework.Assert;

import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.controller.transform.ResourceTransformationContext;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.TransformationTarget;
import org.jboss.as.controller.transform.TransformationTargetImpl;
import org.jboss.as.controller.transform.TransformerRegistry;
import org.jboss.as.controller.transform.Transformers;
import org.jboss.as.controller.transform.TransformersSubRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.junit.Before;
import org.junit.Test;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class AttributesTestCase {

    private static PathElement PATH = PathElement.pathElement("toto", "testSubsystem");

    private Resource resourceRoot = Resource.Factory.create();
    private TransformerRegistry registry = TransformerRegistry.Factory.create(null);
    private ManagementResourceRegistration resourceRegistration = ManagementResourceRegistration.Factory.create(ROOT);
    private TransformersSubRegistration transformersSubRegistration;
    private ModelNode resourceModel;

    @Before
    public void setUp() {
        // Cleanup
        resourceRoot = Resource.Factory.create();
        registry = TransformerRegistry.Factory.create(null);
        resourceRegistration = ManagementResourceRegistration.Factory.create(ROOT);
        // test
        final Resource toto = Resource.Factory.create();
        resourceRoot.registerChild(PATH, toto);
        resourceModel = toto.getModel();

        // Register the description
        transformersSubRegistration = registry.getServerRegistration(ModelVersion.create(1));

    }


    @Test
    public void testRejectExpressions() throws Exception {
        //Set up the model
        resourceModel.get("reject").setExpression("${expr}");

        final ResourceTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createInstance(PATH);
            builder.getStringAttributeBuilder().setRejectExpressions("reject").end()
            .build().register(transformersSubRegistration);

        final Resource resource = transformResource();
        Assert.assertNotNull(resource);
        final Resource toto = resource.getChild(PATH);
        Assert.assertNotNull(toto);
        final ModelNode model = toto.getModel();
        //The rejection does not trigger for resource transformation
        Assert.assertTrue(model.hasDefined("reject"));

        ModelNode add = Util.createAddOperation(PathAddress.pathAddress(PATH));
        add.get("reject").setExpression("${expr}");
        OperationTransformer.TransformedOperation transformedAdd = transformOperation(add);
        Assert.assertTrue(transformedAdd.rejectOperation(success()));

        ModelNode write = Util.getWriteAttributeOperation(PathAddress.pathAddress(PATH), "reject", new ModelNode().setExpression("${expr}"));
        OperationTransformer.TransformedOperation transformedWrite = transformOperation(write);
        Assert.assertTrue(transformedWrite.rejectOperation(success()));
    }

    @Test
    public void testCustomRejectChecker() throws Exception {
        //Set up the model
        resourceModel.get("reject").setExpression("${expr}");
        DontRejectChecker dontRejectChecker = new DontRejectChecker();
        CustomRejectExpressionsChecker rejectAttributeChecker = new CustomRejectExpressionsChecker();
        List<RejectAttributeChecker> rejectCheckers = new ArrayList<RejectAttributeChecker>();
        rejectCheckers.add(dontRejectChecker);
        rejectCheckers.add(rejectAttributeChecker);
        final ResourceTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createInstance(PATH);
            builder.getStringAttributeBuilder()
                .addRejectCheck(dontRejectChecker, "reject")
                .addRejectCheck(rejectAttributeChecker, "reject")
                .end()
            .build().register(transformersSubRegistration);

        dontRejectChecker.called = false;
        rejectAttributeChecker.rejected = false;
        final Resource resource = transformResource();
        Assert.assertNotNull(resource);
        final Resource toto = resource.getChild(PATH);
        Assert.assertNotNull(toto);
        final ModelNode model = toto.getModel();
        //The rejection does not trigger for resource transformation
        //TODO this could be done if 'slave' is >= 7.2.0
        Assert.assertTrue(model.hasDefined("reject"));
        Assert.assertTrue(dontRejectChecker.called);
        Assert.assertTrue(rejectAttributeChecker.rejected);

        dontRejectChecker.called = false;
        rejectAttributeChecker.rejected = false;
        ModelNode add = Util.createAddOperation(PathAddress.pathAddress(PATH));
        add.get("reject").setExpression("${expr}");
        OperationTransformer.TransformedOperation transformedAdd = transformOperation(add);
        Assert.assertTrue(transformedAdd.rejectOperation(success()));
        Assert.assertTrue(dontRejectChecker.called);
        Assert.assertTrue(rejectAttributeChecker.rejected);

        dontRejectChecker.called = false;
        rejectAttributeChecker.rejected = false;
        ModelNode write = Util.getWriteAttributeOperation(PathAddress.pathAddress(PATH), "reject", new ModelNode().setExpression("${expr}"));
        OperationTransformer.TransformedOperation transformedWrite = transformOperation(write);
        Assert.assertTrue(transformedWrite.rejectOperation(success()));
        Assert.assertTrue(dontRejectChecker.called);
        Assert.assertTrue(rejectAttributeChecker.rejected);
    }

    @Test
    public void testDiscardAlways() throws Exception {
        //Set up the model
        resourceModel.get("discard").set("nothing");
        resourceModel.get("keep").set("here");

        final ResourceTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createInstance(PATH);
            builder.getStringAttributeBuilder().setDiscard(DiscardAttributeChecker.ALWAYS, "discard").end()
            .build().register(transformersSubRegistration);

        final Resource resource = transformResource();
        Assert.assertNotNull(resource);
        final Resource toto = resource.getChild(PATH);
        Assert.assertNotNull(toto);
        final ModelNode model = toto.getModel();
        Assert.assertTrue(model.hasDefined("keep"));
        Assert.assertFalse(model.has("discard"));

        ModelNode add = Util.createAddOperation(PathAddress.pathAddress(PATH));
        add.get("discard").set("nothing");
        add.get("keep").set("here");
        OperationTransformer.TransformedOperation transformedAdd = transformOperation(add);
        Assert.assertFalse(transformedAdd.rejectOperation(success()));
        Assert.assertTrue(transformedAdd.getTransformedOperation().hasDefined("keep"));
        Assert.assertFalse(transformedAdd.getTransformedOperation().has("discard"));

        checkOpDiscarded(Util.getWriteAttributeOperation(PathAddress.pathAddress(PATH), "discard", new ModelNode("nothing")));
        checkOpDiscarded(Util.getUndefineAttributeOperation(PathAddress.pathAddress(PATH), "discard"));
    }


    @Test
    public void testDiscardUndefined() throws Exception {
        //Set up the model
        resourceModel.get("discard");
        resourceModel.get("keep").set("here");

        final ResourceTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createInstance(PATH);
            builder.getStringAttributeBuilder().setDiscard(DiscardAttributeChecker.UNDEFINED, "discard", "keep").end()
            .build().register(transformersSubRegistration);

        final Resource resource = transformResource();
        Assert.assertNotNull(resource);
        final Resource toto = resource.getChild(PATH);
        Assert.assertNotNull(toto);
        final ModelNode model = toto.getModel();
        Assert.assertTrue(model.hasDefined("keep"));
        Assert.assertFalse(model.has("discard"));

        ModelNode add = Util.createAddOperation(PathAddress.pathAddress(PATH));
        add.get("discard");
        add.get("keep").set("here");
        OperationTransformer.TransformedOperation transformedAdd = transformOperation(add);
        Assert.assertFalse(transformedAdd.rejectOperation(success()));
        Assert.assertTrue(transformedAdd.getTransformedOperation().hasDefined("keep"));
        Assert.assertFalse(transformedAdd.getTransformedOperation().has("discard"));

        checkOpDiscarded(Util.getWriteAttributeOperation(PathAddress.pathAddress(PATH), "discard", new ModelNode()));

        checkWriteOp(Util.getWriteAttributeOperation(PathAddress.pathAddress(PATH), "discard", new ModelNode("nothing")),
                "discard", new ModelNode("nothing"));
    }

    @Test
    public void testDiscardNotHappeningWithExpressions() throws Exception {
        //Set up the model
        resourceModel.get("discard").setExpression("${xxx}");

        final ResourceTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createInstance(PATH);
            builder.getStringAttributeBuilder().setDiscard(new DefaultAttributeChecker(false, false) {
                @Override
                public boolean isValueDiscardable(String attributeName, ModelNode attributeValue, TransformationContext context) {
                    return true;
                }
            }, "discard").end()
            .build().register(transformersSubRegistration);

        final Resource resource = transformResource();
        Assert.assertNotNull(resource);
        final Resource toto = resource.getChild(PATH);
        Assert.assertNotNull(toto);
        final ModelNode model = toto.getModel();
        Assert.assertEquals(new ModelNode().setExpression("${xxx}"), model.get("discard"));

        ModelNode add = Util.createAddOperation(PathAddress.pathAddress(PATH));
        add.get("discard").setExpression("${xxx}");
        OperationTransformer.TransformedOperation transformedAdd = transformOperation(add);
        Assert.assertFalse(transformedAdd.rejectOperation(success()));
        Assert.assertEquals(add, transformedAdd.getTransformedOperation());

        checkWriteOp(Util.getWriteAttributeOperation(PathAddress.pathAddress(PATH), "discard", new ModelNode().setExpression("${xxx}")),
                "discard", new ModelNode().setExpression("${xxx}"));
    }

    @Test
    public void testDiscardDefaultValue() throws Exception {
        //Set up the model
        resourceModel.get("discard").set("default");
        resourceModel.get("keep").set("non-default");

        final ResourceTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createInstance(PATH);
            builder.getStringAttributeBuilder().setDiscard(new DefaultAttributeChecker(false, true) {
                @Override
                public boolean isValueDiscardable(String attributeName, ModelNode attributeValue, TransformationContext context) {
                    if (attributeName.equals("discard") || attributeName.equals("keep")) {
                        if (attributeValue.asString().equals("default")) {
                            return true;
                        }
                    }
                    return false;
                }
            }, "discard", "keep").end()
            .build().register(transformersSubRegistration);

        final Resource resource = transformResource();
        Assert.assertNotNull(resource);
        final Resource toto = resource.getChild(PATH);
        Assert.assertNotNull(toto);
        final ModelNode model = toto.getModel();
        Assert.assertTrue(model.hasDefined("keep"));
        Assert.assertFalse(model.has("discard"));

        ModelNode add = Util.createAddOperation(PathAddress.pathAddress(PATH));
        add.get("discard");
        add.get("keep").set("here");
        OperationTransformer.TransformedOperation transformedAdd = transformOperation(add);
        Assert.assertFalse(transformedAdd.rejectOperation(success()));
        Assert.assertTrue(transformedAdd.getTransformedOperation().hasDefined("keep"));
        Assert.assertFalse(transformedAdd.getTransformedOperation().has("discard"));

        checkOpDiscarded(Util.getWriteAttributeOperation(PathAddress.pathAddress(PATH), "discard", new ModelNode("default")));
        checkWriteOp(Util.getWriteAttributeOperation(PathAddress.pathAddress(PATH), "discard", new ModelNode("something")),
                "discard", new ModelNode("something"));
    }

    @Test
    public void testRenameAttribute() throws Exception {
        resourceModel.get("old").set("value");

        final ResourceTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createInstance(PATH);
            builder.getStringAttributeBuilder().addRename("old", "new").end()
            .build().register(transformersSubRegistration);

        final Resource resource = transformResource();
        Assert.assertNotNull(resource);
        final Resource toto = resource.getChild(PATH);
        Assert.assertNotNull(toto);
        final ModelNode model = toto.getModel();
        Assert.assertEquals(1, model.keys().size());
        Assert.assertEquals("value", model.get("new").asString());

        ModelNode add = Util.createAddOperation(PathAddress.pathAddress(PATH));
        add.get("old").set("value");
        OperationTransformer.TransformedOperation transformedAdd = transformOperation(add);
        Assert.assertFalse(transformedAdd.rejectOperation(success()));
        Assert.assertFalse(transformedAdd.getTransformedOperation().hasDefined("old"));
        Assert.assertEquals("value", transformedAdd.getTransformedOperation().get("new").asString());


        checkWriteOp(Util.getWriteAttributeOperation(PathAddress.pathAddress(PATH), "old", new ModelNode("value")),
                "new", new ModelNode("value"));
    }

    @Test
    public void testConvertValue() throws Exception {
        resourceModel.get("value1").set("one");
        resourceModel.get("value2").set("two");

        final ResourceTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createInstance(PATH);
        builder.getStringAttributeBuilder().setValueConverter(new AttributeConverter() {
            @Override
            public void convertAttribute(String name, ModelNode attributeValue, TransformationContext context) {
                if (name.equals("value2") && attributeValue.asString().equals("two")) {
                    attributeValue.set(1);
                }
            }
        }, "value1", "value2").end()
        .build().register(transformersSubRegistration);

        final Resource resource = transformResource();
        Assert.assertNotNull(resource);
        final Resource toto = resource.getChild(PATH);
        Assert.assertNotNull(toto);
        final ModelNode model = toto.getModel();
        Assert.assertEquals(2, model.keys().size());
        Assert.assertEquals("one", model.get("value1").asString());
        Assert.assertEquals(ModelType.INT, model.get("value2").getType());
        Assert.assertEquals(1, model.get("value2").asInt());

        ModelNode add = Util.createAddOperation(PathAddress.pathAddress(PATH));
        add.get("value1").set("one");
        add.get("value2").set("two");
        OperationTransformer.TransformedOperation transformedAdd = transformOperation(add);
        Assert.assertFalse(transformedAdd.rejectOperation(success()));
        Assert.assertEquals("one", transformedAdd.getTransformedOperation().get("value1").asString());
        Assert.assertEquals(ModelType.INT, transformedAdd.getTransformedOperation().get("value2").getType());
        Assert.assertEquals(1, transformedAdd.getTransformedOperation().get("value2").asInt());

        checkWriteOp(Util.getWriteAttributeOperation(PathAddress.pathAddress(PATH), "value1", new ModelNode("value")),
                "value1", new ModelNode("value"));
        checkWriteOp(Util.getWriteAttributeOperation(PathAddress.pathAddress(PATH), "value2", new ModelNode("two")),
                "value2", new ModelNode(1));
    }

    @Test
    public void testAddValue() throws Exception {
        resourceModel.get("old").set("existing");

        final ResourceTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createInstance(PATH);
        builder.getStringAttributeBuilder().addAttribute("added", (new AttributeConverter() {
            @Override
            public void convertAttribute(String name, ModelNode attributeValue, TransformationContext context) {
                attributeValue.set("extra");
            }
        })).end()
        .build().register(transformersSubRegistration);

        final Resource resource = transformResource();
        Assert.assertNotNull(resource);
        final Resource toto = resource.getChild(PATH);
        Assert.assertNotNull(toto);
        final ModelNode model = toto.getModel();
        Assert.assertEquals(2, model.keys().size());
        Assert.assertEquals("existing", model.get("old").asString());
        Assert.assertEquals("extra", model.get("added").asString());

        ModelNode add = Util.createAddOperation(PathAddress.pathAddress(PATH));
        add.get("old").set("existing");
        OperationTransformer.TransformedOperation transformedAdd = transformOperation(add);
        Assert.assertFalse(transformedAdd.rejectOperation(success()));
        Assert.assertEquals("existing", transformedAdd.getTransformedOperation().get("old").asString());
        Assert.assertEquals("extra", transformedAdd.getTransformedOperation().get("added").asString());

        //Can't write to this added attribute
    }

    @Test
    public void testComplexRejectAndModify() throws Exception {
        resourceModel.get("one").set("a");
        resourceModel.get("two").set("b");
        resourceModel.get("three").set("TRES");
        resourceModel.get("four");
        resourceModel.get("five");
        resourceModel.get("six");

        Map<String,String> renames = new HashMap<String, String>();
        renames.put("one", "uno");
        renames.put("two", "dos");
        renames.put("three", "TRES");

        CustomRejectExpressionsChecker rejectAttributeChecker = new CustomRejectExpressionsChecker();
        final ResourceTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createInstance(PATH);
        builder.getStringAttributeBuilder()
                .addRejectCheck(rejectAttributeChecker, "one", "two")
                .addAttribute("one", new AttributeConverter() {
                    @Override
                    public void convertAttribute(String name, ModelNode attributeValue, TransformationContext context) {
                        attributeValue.set("ONE");
                    }
                })
                .setDiscard(DiscardAttributeChecker.UNDEFINED, "four", "five")
                .setDiscard(DiscardAttributeChecker.UNDEFINED, "six")
                .setValueConverter(new AttributeConverter() {
                    @Override
                    public void convertAttribute(String name, ModelNode attributeValue, TransformationContext context) {
                        if (name.equals("one")) {
                            attributeValue.set("UNO");
                        } else if (name.equals("two")) {
                            attributeValue.set("DOS");
                        }
                    }
                }, "one", "two")
                .addRenames(renames)
                .addRename("three", "tres")
                //.rename(Collections.singletonMap("four", "cuatro"))
                .end()
            .build().register(transformersSubRegistration);

        //Try first with no expressions
        rejectAttributeChecker.rejected = false;
        final Resource resource = transformResource();
        Assert.assertNotNull(resource);
        final Resource toto = resource.getChild(PATH);
        Assert.assertNotNull(toto);
        final ModelNode model = toto.getModel();
        Assert.assertEquals(4, model.keys().size());
        Assert.assertEquals("ONE", model.get("one").asString());
        Assert.assertEquals("UNO", model.get("uno").asString());
        Assert.assertEquals("DOS", model.get("dos").asString());
        Assert.assertEquals("TRES", model.get("tres").asString());
        Assert.assertFalse(rejectAttributeChecker.rejected);

        final ModelNode add = Util.createAddOperation(PathAddress.pathAddress(PATH));
        add.get("one").set("a");
        add.get("two").set("b");
        add.get("three").set("TRES");
        add.get("four");
        add.get("five");
        add.get("six");
        final OperationTransformer.TransformedOperation transformedAdd = transformOperation(add);
        Assert.assertFalse(transformedAdd.rejectOperation(success()));
        final ModelNode transAdd = transformedAdd.getTransformedOperation();
        transAdd.remove(OP);
        transAdd.remove(OP_ADDR);
        Assert.assertEquals(4, transAdd.keys().size());
        Assert.assertEquals("ONE", transAdd.get("one").asString());
        Assert.assertEquals("UNO", transAdd.get("uno").asString());
        Assert.assertEquals("DOS", transAdd.get("dos").asString());
        Assert.assertEquals("TRES", transAdd.get("tres").asString());
        Assert.assertFalse(rejectAttributeChecker.rejected);


        checkWriteOp(Util.getWriteAttributeOperation(PathAddress.pathAddress(PATH), "one", new ModelNode("a")),
                 "uno", new ModelNode("UNO"));
        checkWriteOp(Util.getWriteAttributeOperation(PathAddress.pathAddress(PATH), "two", new ModelNode("b")),
                "dos", new ModelNode("DOS"));
        checkWriteOp(Util.getWriteAttributeOperation(PathAddress.pathAddress(PATH), "three", new ModelNode("THREE")),
                "tres", new ModelNode("THREE"));
        checkOpDiscarded(Util.getWriteAttributeOperation(PathAddress.pathAddress(PATH), "four", new ModelNode()));
        checkOpDiscarded(Util.getWriteAttributeOperation(PathAddress.pathAddress(PATH), "five", new ModelNode()));
        checkOpDiscarded(Util.getWriteAttributeOperation(PathAddress.pathAddress(PATH), "six", new ModelNode()));
        Assert.assertFalse(rejectAttributeChecker.rejected);

        //Check that expressions get rejected
        resourceModel.clear();
        resourceModel.get("one").setExpression("${test}");
        resourceModel.get("two").set("b");
        resourceModel.get("three").set("TRES");
        resourceModel.get("four");
        resourceModel.get("five");
        resourceModel.get("six");

        rejectAttributeChecker.rejected = false;
        final Resource resource2 = transformResource();
        Assert.assertNotNull(resource2);
        final Resource toto2 = resource2.getChild(PATH);
        Assert.assertNotNull(toto2);
        final ModelNode model2 = toto2.getModel();
        Assert.assertEquals(4, model2.keys().size());
        Assert.assertEquals("ONE", model2.get("one").asString());
        Assert.assertEquals("UNO", model2.get("uno").asString());
        Assert.assertEquals("DOS", model2.get("dos").asString());
        Assert.assertEquals("TRES", model2.get("tres").asString());
        Assert.assertTrue(rejectAttributeChecker.rejected);

        rejectAttributeChecker.rejected = false;
        final ModelNode add2 = Util.createAddOperation(PathAddress.pathAddress(PATH));
        add2.get("one").setExpression("${test}");
        add2.get("two").set("b");
        add2.get("three").set("TRES");
        add2.get("four");
        add2.get("five");
        add2.get("six");
        final OperationTransformer.TransformedOperation transformedAdd2 = transformOperation(add2);
        Assert.assertTrue(transformedAdd2.rejectOperation(success()));
        Assert.assertTrue(rejectAttributeChecker.rejected);

        rejectAttributeChecker.rejected = false;
        OperationTransformer.TransformedOperation write = transformOperation(Util.getWriteAttributeOperation(PathAddress.pathAddress(PATH), "one", new ModelNode().setExpression("${test}")));
        Assert.assertTrue(write.rejectOperation(success()));
        Assert.assertTrue(rejectAttributeChecker.rejected);
    }

    private void checkWriteOp(ModelNode write, String name, ModelNode value) throws OperationFailedException{
        OperationTransformer.TransformedOperation transformedWrite = transformOperation(write);
        Assert.assertFalse(transformedWrite.rejectOperation(success()));
        ModelNode transWrite = transformedWrite.getTransformedOperation();
        Assert.assertEquals(name, transWrite.get(NAME).asString());
        Assert.assertEquals(value, transWrite.get(VALUE));
        Assert.assertEquals(value.getType(), transWrite.get(VALUE).getType());
    }

    private void checkOpDiscarded(ModelNode write) throws OperationFailedException {
        OperationTransformer.TransformedOperation transformedWrite = transformOperation(write);
        Assert.assertFalse(transformedWrite.rejectOperation(success()));
        Assert.assertNull(transformedWrite.getTransformedOperation());
    }


    private Resource transformResource() throws OperationFailedException {
        final TransformationTarget target = create(registry, ModelVersion.create(1));
        final ResourceTransformationContext context = createContext(target);
        return getTransfomers(target).transformResource(context, resourceRoot);
    }

    private OperationTransformer.TransformedOperation transformOperation(final ModelNode operation) throws OperationFailedException {
        final TransformationTarget target = create(registry, ModelVersion.create(1));
        final TransformationContext context = createContext(target);
        return getTransfomers(target).transformOperation(context, operation);
    }

    private ResourceTransformationContext createContext(final TransformationTarget target) {
        return Transformers.Factory.create(target, resourceRoot, resourceRegistration, ExpressionResolver.DEFAULT, RunningMode.NORMAL, ProcessType.STANDALONE_SERVER);
    }

    private Transformers getTransfomers(final TransformationTarget target) {
        return Transformers.Factory.create(target);
    }

    protected TransformationTarget create(final TransformerRegistry registry, ModelVersion version) {
        return create(registry, version, TransformationTarget.TransformationTargetType.SERVER);
    }

    protected TransformationTarget create(final TransformerRegistry registry, ModelVersion version, TransformationTarget.TransformationTargetType type) {
        return TransformationTargetImpl.create(registry, version, Collections.<PathAddress, ModelVersion>emptyMap(), null, type);
    }

    private static final DescriptionProvider NOOP_PROVIDER = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return new ModelNode();
        }
    };

    private static final ResourceDefinition ROOT = new SimpleResourceDefinition(PathElement.pathElement("test"), NOOP_PROVIDER);

    private static final ModelNode success() {
        final ModelNode result = new ModelNode();
        result.get(ModelDescriptionConstants.OUTCOME).set(ModelDescriptionConstants.SUCCESS);
        result.get(ModelDescriptionConstants.RESULT);
        return result;
    }

    private static final ModelNode failed() {
        final ModelNode result = new ModelNode();
        result.get(ModelDescriptionConstants.OUTCOME).set(ModelDescriptionConstants.FAILED);
        result.get(ModelDescriptionConstants.FAILURE_DESCRIPTION).set("failed");
        result.get(ModelDescriptionConstants.RESULT);
        return result;
    }

    private static class DontRejectChecker implements RejectAttributeChecker {
        boolean called;
        @Override
        public boolean rejectAttribute(String attributeName, ModelNode attributeValue, TransformationContext context) {
            called = true;
            return false;
        }
    }

    private static class CustomRejectExpressionsChecker implements RejectAttributeChecker {
        boolean rejected;

        @Override
        public boolean rejectAttribute(String attributeName, ModelNode attributeValue, TransformationContext context) {
            rejected = SIMPLE_EXPRESSIONS.rejectAttribute(attributeName, attributeValue, context);
            return rejected;
        }

    }

}
