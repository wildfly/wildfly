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
import java.util.Set;

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
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.registry.Resource.ResourceEntry;
import org.jboss.as.controller.transform.ResourceTransformationContext;
import org.jboss.as.controller.transform.TransformationTarget;
import org.jboss.as.controller.transform.TransformationTargetImpl;
import org.jboss.as.controller.transform.TransformerRegistry;
import org.jboss.as.controller.transform.Transformers;
import org.jboss.as.controller.transform.TransformersSubRegistration;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ChildRedirectTestCase {

    private static PathElement PATH = PathElement.pathElement("toto", "testSubsystem");
    private static PathElement CHILD = PathElement.pathElement("child");
    private static PathElement CHILD_ONE = PathElement.pathElement("child", "one");
    private static PathElement CHILD_TWO = PathElement.pathElement("child", "two");

    private static PathElement NEW_CHILD = PathElement.pathElement("new-child");

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

        final Resource childOne = Resource.Factory.create();
        toto.registerChild(CHILD_ONE, childOne);
        toto.getModel().setEmptyObject();

        final Resource childTwo = Resource.Factory.create();
        toto.registerChild(CHILD_TWO, childTwo);
        toto.getModel().setEmptyObject();

        // Register the description
        transformersSubRegistration = registry.getServerRegistration(ModelVersion.create(1));
    }


    @Test
    public void testWildcardRedirect() throws Exception {
        final ResourceTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createInstance(PATH);
        builder.addChildRedirection(CHILD, NEW_CHILD);
        TransformationDescription.Tools.register(builder.build(), transformersSubRegistration);

        final Resource resource = transformResource();
        Assert.assertNotNull(resource);
        final Resource toto = resource.getChild(PATH);
        Assert.assertNotNull(toto);

        Set<String> types = toto.getChildTypes();
        Assert.assertEquals(1, types.size());
        Assert.assertTrue(types.contains(NEW_CHILD.getKey()));

        Set<ResourceEntry> entries = toto.getChildren(NEW_CHILD.getKey());
        Assert.assertEquals(2, entries.size());

        PathElement[] expectedChildren = new PathElement[] {PathElement.pathElement(NEW_CHILD.getKey(), CHILD_ONE.getValue()), PathElement.pathElement(NEW_CHILD.getKey(), CHILD_TWO.getValue())};
        for (PathElement expectedChild : expectedChildren) {
            boolean found = false;
            for (ResourceEntry entry : entries) {
                if (entry.getPathElement().equals(expectedChild)) {
                    found = true;
                    break;
                }
            }
            Assert.assertTrue(found);
        }
    }

    @Test
    public void testFixedRedirect() throws Exception {
        PathElement newChild = PathElement.pathElement("new-style", "lalala");
        final ResourceTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createInstance(PATH);
        builder.addChildRedirection(CHILD_ONE, newChild);
        TransformationDescription.Tools.register(builder.build(), transformersSubRegistration);

        final Resource resource = transformResource();
        Assert.assertNotNull(resource);
        final Resource toto = resource.getChild(PATH);
        Assert.assertNotNull(toto);

        Set<String> types = toto.getChildTypes();
        Assert.assertEquals(2, types.size());
        Assert.assertTrue(types.contains(CHILD_TWO.getKey()));
        Assert.assertTrue(types.contains(newChild.getKey()));

        Set<ResourceEntry> childEntries = toto.getChildren(CHILD_TWO.getKey());
        Assert.assertEquals(1, childEntries.size());
        Assert.assertEquals(CHILD_TWO, childEntries.iterator().next().getPathElement());

        childEntries = toto.getChildren(newChild.getKey());
        Assert.assertEquals(1, childEntries.size());
        Assert.assertEquals(newChild, childEntries.iterator().next().getPathElement());
    }


    private Resource transformResource() throws OperationFailedException {
        final TransformationTarget target = create(registry, ModelVersion.create(1));
        final ResourceTransformationContext context = createContext(target);
        return getTransfomers(target).transformResource(context, resourceRoot);
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
}
