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

import java.util.Collections;
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
 * @author Emanuel Muckenhuber
 */
public class BasicTestCase {

    private static PathElement PATH = PathElement.pathElement("toto", "testSubsystem");
    private static PathElement DISCARD = PathElement.pathElement("discard");

    private static PathElement CONFIGURATION_TEST = PathElement.pathElement("configuration", "test");
    private static PathElement TEST_CONFIGURATION = PathElement.pathElement("test", "configuration");

    private static PathElement SETTING_DIRECTORY = PathElement.pathElement("setting", "directory");
    private static PathElement DIRECTORY_SETTING = PathElement.pathElement("directory", "setting");

    private Resource resourceRoot = Resource.Factory.create();
    private TransformerRegistry registry = TransformerRegistry.Factory.create(null);
    private ManagementResourceRegistration resourceRegistration = ManagementResourceRegistration.Factory.create(ROOT);

    private static final TransformationDescription description;

    static {
        // Build
        final ResourceTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createInstance(PATH);

        builder.getStringAttributeBuilder().rejectExpressions("test").end()
                .addCustomTransformation(new ModelTransformer() {
                    @Override
                    public boolean transform(ModelNode node, PathAddress address, TransformationContext context) throws OperationFailedException {
                        node.get("othertest").set(true);
                        return true;
                    }
                });

        // Discard all
        builder.discardChildResource(DISCARD);

        // configuration=test/setting=directory > test=configuration/directory=setting
        builder.addChildRedirection(CONFIGURATION_TEST, TEST_CONFIGURATION)
                .getStringAttributeBuilder().rejectExpressions("test-config").end()
                .addChildRedirection(SETTING_DIRECTORY, DIRECTORY_SETTING);

        // Register at the server root
        description = builder.build();
    }

    @Before
    public void setUp() {
        // Cleanup
        resourceRoot = Resource.Factory.create();
        registry = TransformerRegistry.Factory.create(null);
        resourceRegistration = ManagementResourceRegistration.Factory.create(ROOT);
        // test
        final Resource toto = Resource.Factory.create();
        toto.getModel().get("test").set("onetwothree");

        // discard
        final Resource discard = Resource.Factory.create();
        discard.getModel().get("attribute").set("two");
        toto.registerChild(PathElement.pathElement("discard", "one"), discard);

        // configuration
        final Resource configuration = Resource.Factory.create();
        final Resource setting = Resource.Factory.create();
        configuration.registerChild(SETTING_DIRECTORY, setting);
        toto.registerChild(CONFIGURATION_TEST, configuration);

        //
        resourceRoot.registerChild(PATH, toto);

        // Register the description
        final TransformersSubRegistration reg = registry.getServerRegistration(ModelVersion.create(1));
        description.register(reg);

    }

    @Test
    public void testResourceTransformation() throws Exception {

        final ModelNode address = new ModelNode();
        address.add("toto", "testSubsystem");

        final ModelNode node = new ModelNode();
        node.get(ModelDescriptionConstants.OP).set("add");
        node.get(ModelDescriptionConstants.OP_ADDR).set(address);

        final OperationTransformer.TransformedOperation op = transformOperation(node);
        Assert.assertNotNull(op);

        final Resource resource = transformResource();
        System.out.println(Resource.Tools.readModel(resource));

        Assert.assertNotNull(resource);
        final Resource toto = resource.getChild(PATH);
        final ModelNode model = toto.getModel();
        Assert.assertTrue(model.hasDefined("othertest"));
        Assert.assertNotNull(toto);
        Assert.assertFalse(toto.hasChild(PathElement.pathElement("discard", "one")));
        Assert.assertFalse(toto.hasChild(CONFIGURATION_TEST));
    }

    @Test
    public void testAddOperation() throws Exception {

        final ModelNode address = new ModelNode();
        address.add("toto", "testSubsystem");

        final ModelNode node = new ModelNode();
        node.get(ModelDescriptionConstants.OP).set("add");
        node.get(ModelDescriptionConstants.OP_ADDR).set(address);
        node.get("test").set("${one:two}");

        OperationTransformer.TransformedOperation op = transformOperation(node);
        Assert.assertTrue(op.rejectOperation(success()));

        node.get("test").set("concrete");
        op = transformOperation(node);
        Assert.assertFalse(op.rejectOperation(success()));
    }

    @Test
    public void testWriteAttribute() throws Exception {

        final ModelNode address = new ModelNode();
        address.add("toto", "testSubsystem");

        final ModelNode node = new ModelNode();
        node.get(ModelDescriptionConstants.OP).set("write-attribute");
        node.get(ModelDescriptionConstants.OP_ADDR).set(address);
        node.get("name").set("test");
        node.get("value").set("${one:two}");

        OperationTransformer.TransformedOperation op = transformOperation(node);
        Assert.assertTrue(op.rejectOperation(success()));

        node.get("value").set("test");
        op = transformOperation(node);
        Assert.assertFalse(op.rejectOperation(success()));
    }

    @Test
    public void testAlias() throws Exception {

        final ModelNode address = new ModelNode();
        address.add("toto", "testSubsystem");
        address.add("configuration", "test");

        final ModelNode node = new ModelNode();
        node.get(ModelDescriptionConstants.OP).set("add");
        node.get(ModelDescriptionConstants.OP_ADDR).set(address);
        node.get("test-config").set("${one:two}");

        OperationTransformer.TransformedOperation op = transformOperation(node);
        System.out.println(op.getTransformedOperation());
        Assert.assertTrue(op.rejectOperation(success()));

        node.get("test-config").set("concrete");
        op = transformOperation(node);
        Assert.assertFalse(op.rejectOperation(success()));

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

}
