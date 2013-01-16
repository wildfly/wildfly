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
import org.junit.Before;
import org.junit.Test;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class AttributesTestsCase {

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
    public void testDiscardAttributeTransformation() throws Exception {
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
