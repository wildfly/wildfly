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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
import org.jboss.as.controller.registry.GlobalTransformerRegistry;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationTransformerRegistry;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.registry.Resource.ResourceEntry;
import org.jboss.as.controller.transform.AbstractOperationTransformer;
import org.jboss.as.controller.transform.OperationResultTransformer;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.controller.transform.ResourceTransformationContext;
import org.jboss.as.controller.transform.ResourceTransformer;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.TransformationTarget;
import org.jboss.as.controller.transform.TransformationTargetImpl;
import org.jboss.as.controller.transform.TransformerRegistry;
import org.jboss.as.controller.transform.Transformers;
import org.jboss.as.controller.transform.TransformersSubRegistration;
import org.jboss.as.controller.transform.chained.ChainedResourceTransformationContext;
import org.jboss.as.controller.transform.chained.ChainedResourceTransformer;
import org.jboss.as.controller.transform.chained.ChainedResourceTransformerEntry;
import org.jboss.dmr.ModelNode;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Emanuel Muckenhuber
 */
public class OperationTransformationTestCase {

    private static final PathAddress TEST_DISCARD = PathAddress.pathAddress(PathElement.pathElement("test", "discard"));
    private static final PathAddress TEST_NORMAL = PathAddress.pathAddress(PathElement.pathElement("test", "normal"));
    private static final ModelNode subsystems = new ModelNode();

    static {
        // Same subsystem response
        subsystems.add("test", "1.0.0");
        subsystems.add("modcluster", "1.1");
        subsystems.add("naming", "1.0");
        subsystems.add("osgi", "1.0");
        subsystems.add("pojo", "1.0");
        subsystems.add("remoting", "1.1");
        subsystems.add("resource-adapters", "1.1");
    }

    private final Resource resourceRoot = Resource.Factory.create();
    private final GlobalTransformerRegistry registry = new GlobalTransformerRegistry();
    private final ManagementResourceRegistration resourceRegistration = ManagementResourceRegistration.Factory.create(ROOT);
    private final OperationTransformer NOOP_TRANSFORMER = new OperationTransformer() {
        @Override
        public TransformedOperation transformOperation(TransformationContext context, PathAddress address, ModelNode operation) {
            return new TransformedOperation(new ModelNode(), OperationResultTransformer.ORIGINAL_RESULT);
        }
    };
    private final OperationTransformer OPERATION_TRANSFORMER = new OperationTransformer() {
        @Override
        public TransformedOperation transformOperation(TransformationContext context, PathAddress address, ModelNode operation) throws OperationFailedException {
            final ModelNode transformed = operation.clone();
            transformed.get("param1").set("value1");
            return new TransformedOperation(transformed, OperationResultTransformer.ORIGINAL_RESULT);
        }
    };

    @Before
    public void setUp() {
        registry.discardOperation(TEST_DISCARD, 1, 1, "discard");
        registry.registerTransformer(TEST_NORMAL, 1, 2, "normal", OPERATION_TRANSFORMER);
    }

    @Test
    public void testDiscardOperation() throws OperationFailedException  {
        final ModelNode operation = new ModelNode();
        operation.get(ModelDescriptionConstants.OP).set("discard");
        operation.get(ModelDescriptionConstants.OP_ADDR).set(TEST_DISCARD.toModelNode());
        Assert.assertNull(transform(operation, 1, 1));
    }

    @Test
    public void testBasicTransformation() throws OperationFailedException  {
        final ModelNode operation = new ModelNode();
        operation.get(ModelDescriptionConstants.OP).set("normal");
        operation.get(ModelDescriptionConstants.OP_ADDR).set(TEST_NORMAL.toModelNode());
        final ModelNode transformed = transform(operation, 1, 2);
        Assert.assertNotNull(transformed);
        Assert.assertTrue(transformed.has("param1"));
        Assert.assertEquals("value1", transformed.get("param1").asString());
    }

    @Test
    public void testDefaultPolicy() throws OperationFailedException {
        final ModelNode operation = new ModelNode();
        operation.get(ModelDescriptionConstants.OP).set("normal");
        operation.get(ModelDescriptionConstants.OP_ADDR).set(TEST_NORMAL.toModelNode());
        final ModelNode transformed = transform(operation, 1, 1);
        Assert.assertNotNull(transformed);
        Assert.assertFalse(transformed.has("param1"));
    }

    @Test
    public void testMergeSubTree() {
        final PathAddress address = PathAddress.pathAddress(PathElement.pathElement("subsystem", "test"));
        final OperationTransformerRegistry localRegistry = registry.create(ModelVersion.create(1, 0, 0), Collections.<PathAddress, ModelVersion>emptyMap());

        OperationTransformerRegistry.OperationTransformerEntry entry = localRegistry.resolveOperationTransformer(address, "testing");
        Assert.assertEquals(OperationTransformerRegistry.TransformationPolicy.FORWARD, entry.getPolicy());

        registry.registerTransformer(address, 1, 0, "testing", NOOP_TRANSFORMER);
        localRegistry.mergeSubsystem(registry, "test", ModelVersion.create(1, 0));

        entry = localRegistry.resolveOperationTransformer(address, "testing");
        Assert.assertEquals(OperationTransformerRegistry.TransformationPolicy.TRANSFORM, entry.getPolicy());

    }

    @Test
    public void testGetSubRegistry() {
        final PathAddress profile = PathAddress.pathAddress(PathElement.pathElement(ModelDescriptionConstants.PROFILE));
        final PathAddress address = PathAddress.pathAddress(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, "test"));

        final OperationTransformer transformer = new AbstractOperationTransformer() {
            @Override
            protected ModelNode transform(TransformationContext context, PathAddress address, ModelNode operation) {
                return operation;
            }
        };
        final TransformerRegistry transformers = TransformerRegistry.Factory.create(null);

        final TransformersSubRegistration subsystem = transformers.registerSubsystemTransformers("test", ModelVersion.create(1), ResourceTransformer.DEFAULT);
        subsystem.registerOperationTransformer("test", transformer);

        final OperationTransformerRegistry server = transformers.resolveServer(ModelVersion.create(1, 2, 3), subsystems);
        Assert.assertNotNull(server);
        Assert.assertEquals(transformer, server.resolveOperationTransformer(address, "test").getTransformer());
        final OperationTransformerRegistry host = transformers.resolveHost(ModelVersion.create(1, 2, 3), subsystems);
        Assert.assertNotNull(host);
        Assert.assertNotSame(transformer, host.resolveOperationTransformer(address, "test").getTransformer());
        Assert.assertEquals(transformer, host.resolveOperationTransformer(profile.append(address), "test").getTransformer());
    }

    @Test
    public void testConcreteRegistry() throws Exception {

        final PathElement one = PathElement.pathElement(ModelDescriptionConstants.EXTENSION, "org.test.one");
        final PathElement two = PathElement.pathElement(ModelDescriptionConstants.EXTENSION, "org.test.two");

        final Resource resource = Resource.Factory.create();
        resource.registerChild(one, Resource.Factory.create());
        resource.registerChild(two, Resource.Factory.create());

        final TransformerRegistry transformers = TransformerRegistry.Factory.create(null);

        transformers.getDomainRegistration(ModelVersion.create(1, 2)).registerSubResource(one, true);
        transformers.getDomainRegistration(ModelVersion.create(1, 3)).registerSubResource(two, true);

        final TransformationTarget target10 = create(transformers, ModelVersion.create(1, 0));
        final Resource transformed10 = transform(target10, resource);
        Assert.assertEquals(2, transformed10.getChildren(ModelDescriptionConstants.EXTENSION).size());
        Assert.assertNotNull(transformed10.getChild(one));
        Assert.assertNotNull(transformed10.getChild(one));

        final TransformationTarget target12 = create(transformers, ModelVersion.create(1, 2));
        final Resource transformed12 = transform(target12, resource);
        Assert.assertEquals(1, transformed12.getChildren(ModelDescriptionConstants.EXTENSION).size());
        Assert.assertNull(transformed12.getChild(one));
        Assert.assertNotNull(transformed12.getChild(two));

        final TransformationTarget target13 = create(transformers, ModelVersion.create(1, 3));
        final Resource transformed13 = transform(target13, resource);
        Assert.assertEquals(1, transformed13.getChildren(ModelDescriptionConstants.EXTENSION).size());
        Assert.assertNull(transformed13.getChild(two));
        Assert.assertNotNull(transformed13.getChild(one));
    }

    @Test
    public void testChainedTransformer() throws Exception {
        final PathElement top = PathElement.pathElement("top", "test");
        final PathElement one = PathElement.pathElement("type", "one");
        final PathElement two = PathElement.pathElement("type", "two");

        final Resource rootResource = Resource.Factory.create();
        final Resource topResource = Resource.Factory.create();
        topResource.getModel().get("A").set("a");
        final Resource oneResource = Resource.Factory.create();
        oneResource.getModel().get("A").set("a");
        final Resource twoResource = Resource.Factory.create();
        twoResource.getModel().get("A").set("a");
        twoResource.getModel().get("B").set("b");
        rootResource.registerChild(top, topResource);
        topResource.registerChild(one, oneResource);
        topResource.registerChild(two, twoResource);


        final TransformerRegistry transformers = TransformerRegistry.Factory.create(null);
        TransformersSubRegistration rootTransformers = transformers.getDomainRegistration(ModelVersion.create(1, 0));

        ChainedResourceTransformerEntry shiftKey = new ChainedResourceTransformerEntry() {
            @Override
            public void transformResource(ChainedResourceTransformationContext context, PathAddress address, Resource resource)
                    throws OperationFailedException {
                ModelNode model = resource.getModel();
                if (model.isDefined()) {
                    Map<String, ModelNode> replacements = new HashMap<String, ModelNode>();
                    Set<String> keys = new HashSet<String>(model.keys());
                    for (String key : keys) {
                        replacements.put(key, model.remove(key));
                    }
                    for (String key : keys) {
                        StringBuffer sb = new StringBuffer(key);
                        for (int i = 0 ; i < sb.length() ; i++) {
                            char c = sb.charAt(0);
                            c++;
                            sb.setCharAt(i, c);
                        }
                        model.get(sb.toString()).set(replacements.get(key));
                    }
                }
            }
        };

        ChainedResourceTransformerEntry upperCaseValue = new ChainedResourceTransformerEntry() {
            @Override
            public void transformResource(ChainedResourceTransformationContext context, PathAddress address, Resource resource)
                    throws OperationFailedException {
                ModelNode model = resource.getModel();
                if (model.isDefined()) {
                    for (String key : model.keys()) {
                        String s = model.get(key).asString();
                        s = s.toUpperCase();
                        model.get(key).set(s);
                    }
                }
            }
        };

        TransformersSubRegistration topTransformers = rootTransformers.registerSubResource(top, new ChainedResourceTransformer(shiftKey, upperCaseValue));
        topTransformers.registerSubResource(two, new ChainedResourceTransformer(shiftKey, upperCaseValue));


        final TransformationTarget target = create(transformers, ModelVersion.create(1, 0));
        final Resource transformed = transform(target, rootResource);

        ModelNode expected = new ModelNode();
        expected.get("top", "test", "B").set("A");
        expected.get("top", "test", "type", "one", "A").set("a");
        expected.get("top", "test", "type", "two", "B").set("A");
        expected.get("top", "test", "type", "two", "C").set("B");
        Assert.assertEquals(expected, readModelRecursively(transformed));
    }


    @Test
    public void testAddSubsystem() throws Exception {

        final ModelVersion subsystem = ModelVersion.create(1, 2);
        final TransformerRegistry registry = TransformerRegistry.Factory.create(null);
        TransformersSubRegistration sub = registry.registerSubsystemTransformers("test", subsystem, ResourceTransformer.DISCARD);
        sub.registerOperationTransformer("test", OPERATION_TRANSFORMER);

        final TransformationTarget host = create(registry, ModelVersion.create(1, 2, 3));
        host.addSubsystemVersion("test", subsystem);

        final PathAddress profile = PathAddress.pathAddress(PathElement.pathElement(ModelDescriptionConstants.PROFILE, "test"));
        final PathAddress serverAddress = PathAddress.pathAddress(PathElement.pathElement(ModelDescriptionConstants.HOST, "test"), PathElement.pathElement(ModelDescriptionConstants.RUNNING_SERVER, "test"));
        final PathAddress subsytemAddress = PathAddress.pathAddress(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, "test"));

        final OperationTransformer profileTransformer = host.resolveTransformer(profile.append(subsytemAddress), "test");
        Assert.assertEquals(profileTransformer, OPERATION_TRANSFORMER);

        final OperationTransformer serverTransformer = host.resolveTransformer(serverAddress.append(subsytemAddress), "test");
        Assert.assertEquals(serverTransformer, OPERATION_TRANSFORMER);

    }

    protected TransformationTarget create(final TransformerRegistry registry, ModelVersion version) {
        return create(registry, version, TransformationTarget.TransformationTargetType.HOST);
    }

    protected TransformationTarget create(final TransformerRegistry registry, ModelVersion version, TransformationTarget.TransformationTargetType type) {
        return TransformationTargetImpl.create(registry, version, Collections.<PathAddress, ModelVersion>emptyMap(), null, type);
    }

    protected Resource transform(final TransformationTarget target, final Resource root) throws OperationFailedException {
        final Transformers transformers = Transformers.Factory.create(target);
        final ResourceTransformationContext ctx = Transformers.Factory.create(target, root, resourceRegistration, resolver, RunningMode.NORMAL, ProcessType.HOST_CONTROLLER);
        return transformers.transformResource(ctx, root);
    }

    protected ModelNode transform(final ModelNode operation, int major, int minor) throws OperationFailedException {
        return transform(PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR)), operation, major, minor);
    }

    protected ModelNode transform(final PathAddress address, final ModelNode operation, int major, int minor) throws OperationFailedException {
        final String operationName = operation.require(ModelDescriptionConstants.OP).asString();
        final OperationTransformerRegistry transformerRegistry = registry.create(ModelVersion.create(major, minor), Collections.<PathAddress, ModelVersion>emptyMap());
        final OperationTransformerRegistry.OperationTransformerEntry entry = transformerRegistry.resolveOperationTransformer(address, operationName);
        if(entry.getPolicy() == OperationTransformerRegistry.TransformationPolicy.DISCARD) {
            return null;
        } else {
            final OperationTransformer transformer = entry.getTransformer();
            return transformer.transformOperation(TRANSFORMATION_CONTEXT, address, operation).getTransformedOperation();
        }
    }

    private ModelNode readModelRecursively(Resource resource) {
        ModelNode model = new ModelNode();
        model.set(resource.getModel().clone());

        for (String type : resource.getChildTypes()) {
            for (ResourceEntry entry : resource.getChildren(type)) {
                model.get(type, entry.getName()).set(readModelRecursively(entry));
            }
        }
        return model;
    }

    TransformationContext TRANSFORMATION_CONTEXT = new TransformationContext() {

        @Override
        public TransformationTarget getTarget() {
            return null;
        }

        @Override
        public ProcessType getProcessType() {
            return ProcessType.DOMAIN_SERVER;
        }

        @Override
        public RunningMode getRunningMode() {
            return RunningMode.NORMAL;
        }

        @Override
        public ImmutableManagementResourceRegistration getResourceRegistration(final PathAddress address) {
            return resourceRegistration.getSubModel(address);
        }

        @Override
        public ImmutableManagementResourceRegistration getResourceRegistrationFromRoot(PathAddress address) {
            return resourceRegistration.getSubModel(address);
        }

        @Override
        public Resource readResource(final PathAddress address) {
            return Resource.Tools.navigate(resourceRoot, address);
        }

        @Override
        public Resource readResourceFromRoot(final PathAddress address) {
            return Resource.Tools.navigate(resourceRoot, address);
        }

        @Override
        public ModelNode resolveExpressions(ModelNode node) throws OperationFailedException {
            return node;
        }
    };

    // As usual
    private static final DescriptionProvider NOOP_PROVIDER = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return new ModelNode();
        }
    };

    private static final ResourceDefinition ROOT = new SimpleResourceDefinition(PathElement.pathElement("test"), NOOP_PROVIDER);

    private static final ExpressionResolver resolver = new ExpressionResolver() {
        @Override
        public ModelNode resolveExpressions(ModelNode node) throws OperationFailedException {
            return node;
        }
    };

}
