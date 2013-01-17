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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

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
            builder.getStringAttributeBuilder().rejectExpressions("reject").end()
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
            builder.getStringAttributeBuilder().reject(rejectCheckers, "reject").end()
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
            builder.getStringAttributeBuilder().discard(DiscardAttributeChecker.ALWAYS, "discard").end()
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

        ModelNode write = Util.getWriteAttributeOperation(PathAddress.pathAddress(PATH), "discard", new ModelNode("nothing"));
        OperationTransformer.TransformedOperation transformedWrite = transformOperation(write);
        Assert.assertFalse(transformedWrite.rejectOperation(success()));
        //TODO this should be null, i.e. the write-attribute operation should not be pushed to the slave
        //Assert.assertNull(transformedWrite.getTransformedOperation());
    }


    @Test
    public void testDiscardUndefined() throws Exception {
        //Set up the model
        resourceModel.get("discard");
        resourceModel.get("keep").set("here");

        final ResourceTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createInstance(PATH);
            builder.getStringAttributeBuilder().discard(DiscardAttributeChecker.UNDEFINED, "discard", "keep").end()
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

        ModelNode write = Util.getWriteAttributeOperation(PathAddress.pathAddress(PATH), "discard", new ModelNode());
        OperationTransformer.TransformedOperation transformedWrite = transformOperation(write);
        Assert.assertFalse(transformedWrite.rejectOperation(success()));
        //TODO this should be null, i.e. the write-attribute operation should not be pushed to the slave
        //Assert.assertNull(transformedWrite.getTransformedOperation());

        ModelNode write2 = Util.getWriteAttributeOperation(PathAddress.pathAddress(PATH), "discard", new ModelNode("nothing"));
        OperationTransformer.TransformedOperation transformedWrite2 = transformOperation(write2);
        Assert.assertFalse(transformedWrite2.rejectOperation(success()));
        Assert.assertEquals(write2, transformedWrite2.getTransformedOperation());
    }

    @Test
    public void testDiscardNotHappeningWithExpressions() throws Exception {
        //Set up the model
        resourceModel.get("discard").setExpression("${xxx}");

        final ResourceTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createInstance(PATH);
            builder.getStringAttributeBuilder().discard(new DefaultAttributeChecker(false, false) {
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


        ModelNode write = Util.getWriteAttributeOperation(PathAddress.pathAddress(PATH), "discard", new ModelNode().setExpression("${xxx}"));
        OperationTransformer.TransformedOperation transformedWrite = transformOperation(write);
        Assert.assertFalse(transformedWrite.rejectOperation(success()));
        Assert.assertEquals(write, transformedWrite.getTransformedOperation());
    }

    @Test
    public void testDiscardDefaultValue() throws Exception {
        //Set up the model
        resourceModel.get("discard").set("default");
        resourceModel.get("keep").set("non-default");

        final ResourceTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createInstance(PATH);
            builder.getStringAttributeBuilder().discard(new DefaultAttributeChecker(false, true) {
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

        ModelNode write = Util.getWriteAttributeOperation(PathAddress.pathAddress(PATH), "discard", new ModelNode("default"));
        OperationTransformer.TransformedOperation transformedWrite = transformOperation(write);
        Assert.assertFalse(transformedWrite.rejectOperation(success()));
        //TODO this should be null, i.e. the write-attribute operation should not be pushed to the slave
        //Assert.assertNull(transformedWrite.getTransformedOperation());

        ModelNode write2 = Util.getWriteAttributeOperation(PathAddress.pathAddress(PATH), "discard", new ModelNode("something"));
        OperationTransformer.TransformedOperation transformedWrite2 = transformOperation(write2);
        Assert.assertFalse(transformedWrite2.rejectOperation(success()));
        Assert.assertEquals(write2, transformedWrite2.getTransformedOperation());
    }

    @Test
    public void testRenameAttribute() throws Exception {
        resourceModel.get("old").set("value");

        final ResourceTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createInstance(PATH);
            builder.getStringAttributeBuilder().rename(Collections.singletonMap("old", "new")).end()
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


        ModelNode write = Util.getWriteAttributeOperation(PathAddress.pathAddress(PATH), "old", new ModelNode("value"));
        OperationTransformer.TransformedOperation transformedWrite = transformOperation(write);
        Assert.assertFalse(transformedWrite.rejectOperation(success()));
        Assert.assertEquals("new", transformedWrite.getTransformedOperation().get(NAME));
        Assert.assertEquals("value", transformedWrite.getTransformedOperation().get(VALUE));
    }

    @Test
    public void testConvertValue() throws Exception {
        resourceModel.get("value1").set("one");
        resourceModel.get("value2").set("two");

        final ResourceTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createInstance(PATH);
        builder.getStringAttributeBuilder().convertValue(new AttributeConverter() {
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

        ModelNode write = Util.getWriteAttributeOperation(PathAddress.pathAddress(PATH), "value1", new ModelNode("value"));
        OperationTransformer.TransformedOperation transformedWrite = transformOperation(write);
        Assert.assertFalse(transformedWrite.rejectOperation(success()));
        Assert.assertEquals(write, transformedWrite.getTransformedOperation());

        ModelNode write2 = Util.getWriteAttributeOperation(PathAddress.pathAddress(PATH), "value2", new ModelNode("two"));
        OperationTransformer.TransformedOperation transformedWrite2 = transformOperation(write2);
        Assert.assertFalse(transformedWrite2.rejectOperation(success()));
        Assert.assertEquals(ModelType.INT, transformedWrite2.getTransformedOperation().get("value").getType());
        Assert.assertEquals(1, transformedWrite2.getTransformedOperation().get("value").asInt());
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
