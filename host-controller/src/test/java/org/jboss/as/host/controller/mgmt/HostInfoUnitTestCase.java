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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.IGNORE_UNUSED_CONFIG;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INITIAL_SERVER_GROUPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_CONFIG;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;

import java.util.Set;

import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.RunningModeControl;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.RuntimeIgnoreTransformation;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.TransformationTarget;
import org.jboss.as.controller.transform.TransformersLogger;
import org.jboss.as.host.controller.IgnoredNonAffectedServerGroupsUtil;
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
        ModelNode model = HostInfo.createLocalHostHostInfo(lch, productConfig, ignoredRegistry, Resource.Factory.create());
        Assert.assertTrue(model.get(IGNORE_UNUSED_CONFIG).asBoolean());//TODO this will change
        Assert.assertEquals(new ModelNode().setEmptyObject(), model.get(INITIAL_SERVER_GROUPS));


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
        model = HostInfo.createLocalHostHostInfo(lch, productConfig, ignoredRegistry, Resource.Factory.create());
        model.get(RemoteDomainConnectionService.DOMAIN_CONNECTION_ID).set(1L);
        testee = HostInfo.fromModelNode(model);
        Assert.assertNull(testee.getProductName());
        Assert.assertNull(testee.getProductVersion());
        Assert.assertNotNull(testee.getRemoteConnectionId());
        Assert.assertEquals(1L, testee.getRemoteConnectionId().longValue());
    }

    @Test
    public void testRemoteDomainControllerIgnoreUnaffectedConfiguration() {
        LocalHostControllerInfoImpl lch = new MockLocalHostControllerInfo(new ControlledProcessState(true), "test");
        ProductConfig productConfig = new ProductConfig("product", "version", "main");
        Resource hostResource = Resource.Factory.create();
        Resource server1 = Resource.Factory.create();
        server1.getModel().get(GROUP).set("server-group1");
        server1.getModel().get(SOCKET_BINDING_GROUP).set("socket-binding-group1");
        hostResource.registerChild(PathElement.pathElement(SERVER_CONFIG, "server-1"), server1);
        Resource server2 = Resource.Factory.create();
        server2.getModel().get(GROUP).set("server-group1");
        hostResource.registerChild(PathElement.pathElement(SERVER_CONFIG, "server-2"), server2);

        Resource domainResource = Resource.Factory.create();
        domainResource.registerChild(PathElement.pathElement(HOST, "test"), hostResource);
        Resource serverGroup1 = Resource.Factory.create();
        serverGroup1.getModel().get(PROFILE).set("profile1");
        serverGroup1.getModel().get(SOCKET_BINDING_GROUP).set("socket-binding-group1a");
        domainResource.registerChild(PathElement.pathElement(SERVER_GROUP, "server-group1"), serverGroup1);
        Resource serverGroup2 = Resource.Factory.create();
        serverGroup2.getModel().get(PROFILE).set("profile1");
        domainResource.registerChild(PathElement.pathElement(SERVER_GROUP, "server-group2"), serverGroup2);

        IgnoredDomainResourceRegistry ignoredRegistry = new IgnoredDomainResourceRegistry(lch);

        ModelNode model = HostInfo.createLocalHostHostInfo(lch, productConfig, ignoredRegistry, hostResource);

        Assert.assertTrue(model.get(IGNORE_UNUSED_CONFIG).asBoolean());
        Assert.assertEquals(2, model.get(INITIAL_SERVER_GROUPS).keys().size());
        Assert.assertEquals("server-group1", model.get(INITIAL_SERVER_GROUPS, "server-1", GROUP).asString());
        Assert.assertEquals("socket-binding-group1", model.get(INITIAL_SERVER_GROUPS, "server-1", SOCKET_BINDING_GROUP).asString());
        Assert.assertEquals("server-group1", model.get(INITIAL_SERVER_GROUPS, "server-2", GROUP).asString());
        Assert.assertFalse(model.get(INITIAL_SERVER_GROUPS, "server-2", SOCKET_BINDING_GROUP).isDefined());


        final ExtensionRegistry extensionRegistry = new ExtensionRegistry(ProcessType.HOST_CONTROLLER, new RunningModeControl(RunningMode.NORMAL));
        //Test the master proxy side

        HostInfo testee = HostInfo.fromModelNode(model);
        RuntimeIgnoreTransformation masterIgnorer = DomainControllerRuntimeIgnoreTransformationEntry.create(testee, extensionRegistry);
        Assert.assertFalse(masterIgnorer.ignoreResource(domainResource, PathAddress.pathAddress(PathElement.pathElement(SERVER_GROUP, "server-group1"))));
        Assert.assertTrue(masterIgnorer.ignoreResource(domainResource, PathAddress.pathAddress(PathElement.pathElement(SERVER_GROUP, "server-group2"))));
        Assert.assertFalse(masterIgnorer.ignoreResource(domainResource, PathAddress.pathAddress(PathElement.pathElement(SOCKET_BINDING_GROUP, "socket-binding-group1"))));
        Assert.assertFalse(masterIgnorer.ignoreResource(domainResource, PathAddress.pathAddress(PathElement.pathElement(SOCKET_BINDING_GROUP, "socket-binding-group1a"))));
        Assert.assertTrue(masterIgnorer.ignoreResource(domainResource, PathAddress.pathAddress(PathElement.pathElement(SOCKET_BINDING_GROUP, "socket-binding-group2"))));
        Assert.assertFalse(masterIgnorer.ignoreResource(domainResource, PathAddress.pathAddress(PathElement.pathElement(PROFILE, "profile1"))));
        Assert.assertTrue(masterIgnorer.ignoreResource(domainResource, PathAddress.pathAddress(PathElement.pathElement(PROFILE, "profile2"))));

        //Test the slave side
        IgnoredNonAffectedServerGroupsUtil slaveUtil = IgnoredNonAffectedServerGroupsUtil.create(extensionRegistry);
        Set<IgnoredNonAffectedServerGroupsUtil.ServerConfigInfo> slaveServerConfigs = slaveUtil.getServerConfigsOnSlave(hostResource);
        Assert.assertFalse(slaveUtil.ignoreResource(domainResource, slaveServerConfigs, PathAddress.pathAddress(PathElement.pathElement(SERVER_GROUP, "server-group1"))));
        Assert.assertTrue(slaveUtil.ignoreResource(domainResource, slaveServerConfigs, PathAddress.pathAddress(PathElement.pathElement(SERVER_GROUP, "server-group2"))));
        Assert.assertFalse(slaveUtil.ignoreResource(domainResource, slaveServerConfigs, PathAddress.pathAddress(PathElement.pathElement(SOCKET_BINDING_GROUP, "socket-binding-group1"))));
        Assert.assertFalse(slaveUtil.ignoreResource(domainResource, slaveServerConfigs, PathAddress.pathAddress(PathElement.pathElement(SOCKET_BINDING_GROUP, "socket-binding-group1a"))));
        Assert.assertTrue(slaveUtil.ignoreResource(domainResource, slaveServerConfigs, PathAddress.pathAddress(PathElement.pathElement(SOCKET_BINDING_GROUP, "socket-binding-group2"))));
        Assert.assertFalse(slaveUtil.ignoreResource(domainResource, slaveServerConfigs, PathAddress.pathAddress(PathElement.pathElement(PROFILE, "profile1"))));
        Assert.assertTrue(slaveUtil.ignoreResource(domainResource, slaveServerConfigs, PathAddress.pathAddress(PathElement.pathElement(PROFILE, "profile2"))));

        //Change affected resources and do more tests
        server1.getModel().remove(SOCKET_BINDING_GROUP);
        slaveServerConfigs = slaveUtil.getServerConfigsOnSlave(hostResource);
        Assert.assertTrue(slaveUtil.ignoreResource(domainResource, slaveServerConfigs, PathAddress.pathAddress(PathElement.pathElement(SOCKET_BINDING_GROUP, "socket-binding-group1"))));
        Assert.assertFalse(slaveUtil.ignoreResource(domainResource, slaveServerConfigs, PathAddress.pathAddress(PathElement.pathElement(SOCKET_BINDING_GROUP, "socket-binding-group1a"))));
        Assert.assertTrue(slaveUtil.ignoreResource(domainResource, slaveServerConfigs, PathAddress.pathAddress(PathElement.pathElement(SOCKET_BINDING_GROUP, "socket-binding-group2"))));

        server1.getModel().get(GROUP).set("server-group2");
        server2.getModel().get(GROUP).set("server-group2");
        serverGroup2.getModel().get(SOCKET_BINDING_GROUP).set("socket-binding-group1");
        slaveServerConfigs = slaveUtil.getServerConfigsOnSlave(hostResource);
        Assert.assertTrue(slaveUtil.ignoreResource(domainResource, slaveServerConfigs, PathAddress.pathAddress(PathElement.pathElement(SERVER_GROUP, "server-group1"))));
        Assert.assertFalse(slaveUtil.ignoreResource(domainResource, slaveServerConfigs, PathAddress.pathAddress(PathElement.pathElement(SERVER_GROUP, "server-group2"))));
        Assert.assertFalse(slaveUtil.ignoreResource(domainResource, slaveServerConfigs, PathAddress.pathAddress(PathElement.pathElement(SOCKET_BINDING_GROUP, "socket-binding-group1"))));
        Assert.assertTrue(slaveUtil.ignoreResource(domainResource, slaveServerConfigs, PathAddress.pathAddress(PathElement.pathElement(SOCKET_BINDING_GROUP, "socket-binding-group1a"))));
        Assert.assertTrue(slaveUtil.ignoreResource(domainResource, slaveServerConfigs, PathAddress.pathAddress(PathElement.pathElement(SOCKET_BINDING_GROUP, "socket-binding-group2"))));
        Assert.assertFalse(slaveUtil.ignoreResource(domainResource, slaveServerConfigs, PathAddress.pathAddress(PathElement.pathElement(PROFILE, "profile1"))));
        Assert.assertTrue(slaveUtil.ignoreResource(domainResource, slaveServerConfigs, PathAddress.pathAddress(PathElement.pathElement(PROFILE, "profile2"))));

        serverGroup2.getModel().get(PROFILE).set("profile2");
        Assert.assertTrue(slaveUtil.ignoreResource(domainResource, slaveServerConfigs, PathAddress.pathAddress(PathElement.pathElement(SERVER_GROUP, "server-group1"))));
        Assert.assertFalse(slaveUtil.ignoreResource(domainResource, slaveServerConfigs, PathAddress.pathAddress(PathElement.pathElement(SERVER_GROUP, "server-group2"))));
        Assert.assertFalse(slaveUtil.ignoreResource(domainResource, slaveServerConfigs, PathAddress.pathAddress(PathElement.pathElement(SOCKET_BINDING_GROUP, "socket-binding-group1"))));
        Assert.assertTrue(slaveUtil.ignoreResource(domainResource, slaveServerConfigs, PathAddress.pathAddress(PathElement.pathElement(SOCKET_BINDING_GROUP, "socket-binding-group1a"))));
        Assert.assertTrue(slaveUtil.ignoreResource(domainResource, slaveServerConfigs, PathAddress.pathAddress(PathElement.pathElement(SOCKET_BINDING_GROUP, "socket-binding-group2"))));
        Assert.assertTrue(slaveUtil.ignoreResource(domainResource, slaveServerConfigs, PathAddress.pathAddress(PathElement.pathElement(PROFILE, "profile1"))));
        Assert.assertFalse(slaveUtil.ignoreResource(domainResource, slaveServerConfigs, PathAddress.pathAddress(PathElement.pathElement(PROFILE, "profile2"))));
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

        ModelNode model = HostInfo.createLocalHostHostInfo(lch, productConfig, ignoredRegistry, Resource.Factory.create());

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

        @Override
        public boolean isRemoteDomainControllerIgnoreUnaffectedConfiguration() {
            return true;
        }
    }

    private static class MockTransformationContext implements TransformationContext {
        private final Resource rootResource;
        public MockTransformationContext(final Resource rootResource) {
            this.rootResource = rootResource;
        }
        @Override
        public TransformationTarget getTarget() {
            return null;
        }
        @Override
        public ProcessType getProcessType() {
            return null;
        }
        @Override
        public RunningMode getRunningMode() {
            return null;
        }
        @Override
        public ImmutableManagementResourceRegistration getResourceRegistration(PathAddress address) {
            return null;
        }
        @Override
        public ImmutableManagementResourceRegistration getResourceRegistrationFromRoot(PathAddress address) {
            return null;
        }
        @Override
        public Resource readResource(PathAddress address) {
            return null;
        }
        @Override
        public Resource readResourceFromRoot(PathAddress address) {
            return rootResource;
        }

        @Override
        @Deprecated
        public ModelNode resolveExpressions(ModelNode node) throws OperationFailedException {
            return null;
        }

        @Override
        public TransformersLogger getLogger() {
            return null;
        }

        @Override
        public boolean isSkipRuntimeIgnoreCheck() {
            return false;
        }
    }
}
