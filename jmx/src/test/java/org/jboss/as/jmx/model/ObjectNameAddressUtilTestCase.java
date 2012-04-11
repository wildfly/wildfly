/*
* JBoss, Home of Professional Open Source.
* Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.jmx.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.management.ObjectName;

import junit.framework.Assert;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.junit.Test;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ObjectNameAddressUtilTestCase {

    static final String TOP = "top";
    static final String ONE = "one";
    static final String BOTTOM = "bottom";
    static final String TWO = "two";
    static final String COMPLEX_KEY = "*";
    static final String COMPLEX_VALUE = "\":=*?\n {}[] \":=*?\n";
    static final PathElement TOP_ONE = PathElement.pathElement(TOP, ONE);
    static final PathElement BOTTOM_TWO = PathElement.pathElement(BOTTOM, TWO);
    static final PathElement TOP_COMPLEX_VALUE = PathElement.pathElement(TOP, COMPLEX_VALUE);
    static final PathElement COMPLEX_KEY_ONE = PathElement.pathElement(COMPLEX_KEY, ONE);

    static final Resource rootResource;
    static{
        Resource root = new TestResource();
        Resource topOne = new TestResource();
        root.registerChild(TOP_ONE, topOne);
        topOne.registerChild(BOTTOM_TWO, new TestResource());

        root.registerChild(TOP_COMPLEX_VALUE, new TestResource());
        root.registerChild(COMPLEX_KEY_ONE, new TestResource());
        rootResource = root;
    }


    @Test
    public void testSimpleAddress() throws Exception {
        checkObjectName(TOP_ONE, BOTTOM_TWO);
    }

    @Test
    public void testComplexValueAddress() throws Exception {
        checkObjectName(TOP_COMPLEX_VALUE);
    }

    @Test
    public void testComplexKeyAddress() throws Exception {
        checkObjectName(COMPLEX_KEY_ONE);
    }

    private void checkObjectName(PathElement...elements) {
        PathAddress pathAddress = PathAddress.pathAddress(elements);
        ObjectName on = ObjectNameAddressUtil.createObjectName(pathAddress);
        Assert.assertNotNull(on);
        PathAddress resolved = ObjectNameAddressUtil.resolvePathAddress(rootResource, on);
        Assert.assertEquals(pathAddress, resolved);
    }

    private static class TestResource implements Resource {

        private Map<String, Map<String, Resource>> children = new HashMap<String, Map<String,Resource>>();

        @Override
        public Resource getChild(PathElement element) {
            Map<String, Resource> resources = children.get(element.getKey());
            if (resources != null) {
                return resources.get(element.getValue());
            }
            return null;
        }

        @Override
        public void registerChild(PathElement address, Resource resource) {
            Map<String, Resource> resources = children.get(address.getKey());
            if (resources == null) {
                resources = new HashMap<String, Resource>();
                children.put(address.getKey(), resources);
            }
            resources.put(address.getValue(), resource);
        }


        //THe rest of these don't currently get called
        @Override
        public void writeModel(ModelNode newModel) {
        }

        @Override
        public Resource requireChild(PathElement element) {
            return null;
        }

        @Override
        public Resource removeChild(PathElement address) {
            return null;
        }

        @Override
        public Resource navigate(PathAddress address) {
            return null;
        }

        @Override
        public boolean isRuntime() {
            return false;
        }

        @Override
        public boolean isProxy() {
            return false;
        }

        @Override
        public boolean isModelDefined() {
            return false;
        }

        @Override
        public boolean hasChildren(String childType) {
            return false;
        }

        @Override
        public boolean hasChild(PathElement element) {
            return false;
        }

        @Override
        public ModelNode getModel() {
            return null;
        }

        @Override
        public Set<String> getChildrenNames(String childType) {
            return null;
        }

        @Override
        public Set<ResourceEntry> getChildren(String childType) {
            return null;
        }

        @Override
        public Set<String> getChildTypes() {
            return null;
        }

        public Resource clone() {
            return this;
        }

    };
}
