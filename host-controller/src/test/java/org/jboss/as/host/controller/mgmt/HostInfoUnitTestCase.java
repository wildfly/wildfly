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

package org.jboss.as.host.controller.mgmt;

import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.host.controller.RemoteDomainConnectionService;
import org.jboss.as.host.controller.ignored.IgnoreDomainResourceTypeResource;
import org.jboss.as.host.controller.ignored.IgnoredDomainResourceRegistry;
import org.jboss.as.host.controller.operations.LocalHostControllerInfoImpl;
import org.jboss.as.version.ProductConfig;
import org.jboss.as.version.Version;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests of the {@link HostInfo} class.
 *
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
public class HostInfoUnitTestCase {

    @Test
    public void testBasicInfo() {

        LocalHostControllerInfoImpl lch = new MockLocalHostControllerInfo(new ControlledProcessState(true), "test");
        ProductConfig productConfig = new ProductConfig("product", "version", "main");
        IgnoredDomainResourceRegistry ignoredRegistry = new IgnoredDomainResourceRegistry(lch);
        ModelNode model = HostInfo.createLocalHostHostInfo(lch, productConfig, ignoredRegistry);

        HostInfo testee = HostInfo.fromModelNode(model);
        Assert.assertEquals("test", testee.getHostName());
        Assert.assertEquals("product", testee.getProductName());
        Assert.assertEquals("version", testee.getProductVersion());
        Assert.assertEquals(Version.AS_VERSION, testee.getReleaseVersion());
        Assert.assertEquals(Version.AS_RELEASE_CODENAME, testee.getReleaseCodeName());
        Assert.assertEquals(Version.MANAGEMENT_MAJOR_VERSION, testee.getManagementMajorVersion());
        Assert.assertEquals(Version.MANAGEMENT_MINOR_VERSION, testee.getManagementMinorVersion());
        Assert.assertEquals(Version.MANAGEMENT_MICRO_VERSION, testee.getManagementMicroVersion());
        Assert.assertNull(testee.getRemoteConnectionId());

        productConfig = new ProductConfig(null, null, "main");
        model = HostInfo.createLocalHostHostInfo(lch, productConfig, ignoredRegistry);
        model.get(RemoteDomainConnectionService.DOMAIN_CONNECTION_ID).set(1L);
        testee = HostInfo.fromModelNode(model);
        Assert.assertNull(testee.getProductName());
        Assert.assertNull(testee.getProductVersion());
        Assert.assertNotNull(testee.getRemoteConnectionId());
        Assert.assertEquals(1L, testee.getRemoteConnectionId().longValue());
    }

    @Test
    public void testIgnoredResources() {

        LocalHostControllerInfoImpl lch = new MockLocalHostControllerInfo(new ControlledProcessState(true), "test");
        ProductConfig productConfig = new ProductConfig(null, null, "main");
        IgnoredDomainResourceRegistry ignoredRegistry = new IgnoredDomainResourceRegistry(lch);

        Resource parent = ignoredRegistry.getRootResource();
        PathElement wildcardPE = PathElement.pathElement(ModelDescriptionConstants.IGNORED_RESOURCE_TYPE, "wildcard");
        parent.registerChild(wildcardPE, new IgnoreDomainResourceTypeResource("wildcard", new ModelNode(), Boolean.TRUE));
        PathElement listPE = PathElement.pathElement(ModelDescriptionConstants.IGNORED_RESOURCE_TYPE, "list");
        parent.registerChild(listPE, new IgnoreDomainResourceTypeResource("list", new ModelNode().add("ignored"), Boolean.FALSE));
        PathElement nullWildcardPE = PathElement.pathElement(ModelDescriptionConstants.IGNORED_RESOURCE_TYPE, "nullWildcard");
        parent.registerChild(nullWildcardPE, new IgnoreDomainResourceTypeResource("nullWildcard", new ModelNode().add("ignored"), null));
        PathElement emptyPE = PathElement.pathElement(ModelDescriptionConstants.IGNORED_RESOURCE_TYPE, "empty");
        parent.registerChild(emptyPE, new IgnoreDomainResourceTypeResource("empty", new ModelNode(), null));

        // Do some sanity checking on IgnoredDomainResourceRegistry
        Assert.assertTrue(ignoredRegistry.isResourceExcluded(PathAddress.pathAddress(PathElement.pathElement("wildcard", "ignored"))));
        Assert.assertTrue(ignoredRegistry.isResourceExcluded(PathAddress.pathAddress(PathElement.pathElement("list", "ignored"))));
        Assert.assertFalse(ignoredRegistry.isResourceExcluded(PathAddress.pathAddress(PathElement.pathElement("list", "used"))));
        Assert.assertTrue(ignoredRegistry.isResourceExcluded(PathAddress.pathAddress(PathElement.pathElement("nullWildcard", "ignored"))));
        Assert.assertFalse(ignoredRegistry.isResourceExcluded(PathAddress.pathAddress(PathElement.pathElement("nullWildcard", "used"))));
        Assert.assertFalse(ignoredRegistry.isResourceExcluded(PathAddress.pathAddress(PathElement.pathElement("empty", "ignored"))));
        Assert.assertFalse(ignoredRegistry.isResourceExcluded(PathAddress.pathAddress(PathElement.pathElement("empty", "used"))));
        Assert.assertFalse(ignoredRegistry.isResourceExcluded(PathAddress.pathAddress(PathElement.pathElement("random", "ignored"))));

        ModelNode model = HostInfo.createLocalHostHostInfo(lch, productConfig, ignoredRegistry);

        HostInfo testee = HostInfo.fromModelNode(model);

        Assert.assertTrue(testee.isResourceTransformationIgnored(PathAddress.pathAddress(PathElement.pathElement("wildcard", "ignored"))));
        Assert.assertTrue(testee.isResourceTransformationIgnored(PathAddress.pathAddress(PathElement.pathElement("list", "ignored"))));
        Assert.assertFalse(testee.isResourceTransformationIgnored(PathAddress.pathAddress(PathElement.pathElement("list", "used"))));
        Assert.assertTrue(testee.isResourceTransformationIgnored(PathAddress.pathAddress(PathElement.pathElement("nullWildcard", "ignored"))));
        Assert.assertFalse(testee.isResourceTransformationIgnored(PathAddress.pathAddress(PathElement.pathElement("nullWildcard", "used"))));
        Assert.assertFalse(testee.isResourceTransformationIgnored(PathAddress.pathAddress(PathElement.pathElement("empty", "ignored"))));
        Assert.assertFalse(testee.isResourceTransformationIgnored(PathAddress.pathAddress(PathElement.pathElement("empty", "used"))));
        Assert.assertFalse(testee.isResourceTransformationIgnored(PathAddress.pathAddress(PathElement.pathElement("random", "ignored"))));

        // No operation transformation is ignored
        Assert.assertFalse(testee.isOperationTransformationIgnored(PathAddress.pathAddress(PathElement.pathElement("wildcard", "ignored"))));
        Assert.assertFalse(testee.isOperationTransformationIgnored(PathAddress.pathAddress(PathElement.pathElement("list", "ignored"))));
        Assert.assertFalse(testee.isOperationTransformationIgnored(PathAddress.pathAddress(PathElement.pathElement("list", "used"))));
        Assert.assertFalse(testee.isOperationTransformationIgnored(PathAddress.pathAddress(PathElement.pathElement("nullWildcard", "ignored"))));
        Assert.assertFalse(testee.isOperationTransformationIgnored(PathAddress.pathAddress(PathElement.pathElement("nullWildcard", "used"))));
        Assert.assertFalse(testee.isOperationTransformationIgnored(PathAddress.pathAddress(PathElement.pathElement("empty", "ignored"))));
        Assert.assertFalse(testee.isOperationTransformationIgnored(PathAddress.pathAddress(PathElement.pathElement("empty", "used"))));
        Assert.assertFalse(testee.isOperationTransformationIgnored(PathAddress.pathAddress(PathElement.pathElement("random", "ignored"))));

    }

    private static class MockLocalHostControllerInfo extends LocalHostControllerInfoImpl {

        public MockLocalHostControllerInfo(final ControlledProcessState processState, final String localHostName) {
            super(processState, localHostName);
        }

        @Override
        public boolean isMasterDomainController() {
            return false;
        }
    }
}
