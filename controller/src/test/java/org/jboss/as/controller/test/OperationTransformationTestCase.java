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

import junit.framework.Assert;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.GlobalOperationTransformerRegistry;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationTransformerRegistry;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.AbstractOperationTransformer;
import org.jboss.as.controller.transform.OperationResultTransformer;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.TransformationTarget;
import org.jboss.as.controller.transform.TransformerRegistry;
import org.jboss.dmr.ModelNode;
import org.junit.Before;
import org.junit.Test;

import java.util.Locale;

/**
 * @author Emanuel Muckenhuber
 */
public class OperationTransformationTestCase {

    private static final PathAddress TEST_DISCARD = PathAddress.pathAddress(PathElement.pathElement("test", "discard"));
    private static final PathAddress TEST_NORMAL = PathAddress.pathAddress(PathElement.pathElement("test", "normal"));
    private static final ModelNode AS_7_1_1_SUBSYSTEM_VERSIONS = TransformerRegistry.AS_7_1_1_SUBSYSTEM_VERSIONS;

    private final GlobalOperationTransformerRegistry registry = new GlobalOperationTransformerRegistry();
    private final ManagementResourceRegistration resourceRegistration = ManagementResourceRegistration.Factory.create(ROOT);
    private final OperationTransformer NOOP_TRANSFORMER = new OperationTransformer() {
        @Override
        public TransformedOperation transformOperation(TransformationContext context, PathAddress address, ModelNode operation) {
            return new TransformedOperation(new ModelNode(), OperationResultTransformer.ORIGINAL_RESULT);
        }
    };
    private final Resource resourceRoot = Resource.Factory.create();

    @Before
    public void setUp() {
        registry.discardOperation(TEST_DISCARD, 1, 1, "discard");
        final OperationTransformer transformer = new AbstractOperationTransformer() {

            @Override
            protected ModelNode transform(TransformationContext context, PathAddress address, ModelNode operation) {
                final ModelNode transformed = operation.clone();
                transformed.get("param1").set("value1");
                return transformed;
            }

        };
        registry.registerTransformer(TEST_NORMAL, 1, 2, "normal", transformer);
    }

    @Test
    public void testDiscardOperation() {
        final ModelNode operation = new ModelNode();
        operation.get(ModelDescriptionConstants.OP).set("discard");
        operation.get(ModelDescriptionConstants.OP_ADDR).set(TEST_DISCARD.toModelNode());
        Assert.assertNull(transform(operation, 1, 1));
    }

    @Test
    public void testBasicTransformation() {
        final ModelNode operation = new ModelNode();
        operation.get(ModelDescriptionConstants.OP).set("normal");
        operation.get(ModelDescriptionConstants.OP_ADDR).set(TEST_NORMAL.toModelNode());
        final ModelNode transformed = transform(operation, 1, 2);
        Assert.assertNotNull(transformed);
        Assert.assertTrue(transformed.has("param1"));
        Assert.assertEquals("value1", transformed.get("param1").asString());
    }

    @Test
    public void testDefaultPolicy() {
        final ModelNode operation = new ModelNode();
        operation.get(ModelDescriptionConstants.OP).set("normal");
        operation.get(ModelDescriptionConstants.OP_ADDR).set(TEST_NORMAL.toModelNode());
        final ModelNode transformed = transform(operation, 1, 1);
        Assert.assertNotNull(transformed);
        Assert.assertFalse(transformed.has("param1"));
    }

    @Test
    public void testMergeSubTree() {
        final ModelNode subsystems = new ModelNode();
        final PathAddress address = PathAddress.pathAddress(PathElement.pathElement("subsystem", "test"));
        final OperationTransformerRegistry localRegistry = registry.resolve(ModelVersion.create(1, 0, 0), subsystems.setEmptyList());

        OperationTransformerRegistry.TransformerEntry entry = localRegistry.resolveTransformer(address, "testing");
        Assert.assertEquals(OperationTransformerRegistry.TransformationPolicy.FORWARD, entry.getPolicy());

        registry.registerTransformer(address, 1, 0, "testing", NOOP_TRANSFORMER);
        localRegistry.mergeSubsystem(registry, "test", ModelVersion.create(1, 0));

        entry = localRegistry.resolveTransformer(address, "testing");
        Assert.assertEquals(OperationTransformerRegistry.TransformationPolicy.TRANSFORM, entry.getPolicy());

    }

    protected ModelNode transform(final ModelNode operation, int major, int minor) {
        return transform(PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR)), operation, major, minor);
    }

    protected ModelNode transform(final PathAddress address, final ModelNode operation, int major, int minor) {
        final String operationName = operation.require(ModelDescriptionConstants.OP).asString();
        final OperationTransformerRegistry transformerRegistry = registry.resolve(major, minor, AS_7_1_1_SUBSYSTEM_VERSIONS);
        final OperationTransformerRegistry.TransformerEntry entry = transformerRegistry.resolveTransformer(address, operationName);
        if(entry.getPolicy() == OperationTransformerRegistry.TransformationPolicy.DISCARD) {
            return null;
        } else {
            final OperationTransformer transformer = entry.getTransformer();
            return transformer.transformOperation(TRANSFORMATION_CONTEXT, address, operation).getTransformedOperation();
        }
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
    };

    // As usual
    private static final DescriptionProvider NOOP_PROVIDER = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return new ModelNode();
        }
    };

    private static final ResourceDefinition ROOT = new SimpleResourceDefinition(PathElement.pathElement("test"), NOOP_PROVIDER);

}
