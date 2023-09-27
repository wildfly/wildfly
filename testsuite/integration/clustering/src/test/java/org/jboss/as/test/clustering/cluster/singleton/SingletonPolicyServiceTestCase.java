/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.singleton;

import static org.jboss.as.test.clustering.ClusterTestUtil.execute;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.clustering.cluster.singleton.service.NodeServicePolicyActivator;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class SingletonPolicyServiceTestCase extends AbstractClusteringTestCase {

    private static final String MODULE_NAME = SingletonPolicyServiceTestCase.class.getSimpleName();

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(NODE_1)
    public static Archive<?> deployment1() {
        return createDeployment();
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(NODE_2)
    public static Archive<?> deployment2() {
        return createDeployment();
    }

    private static Archive<?> createDeployment() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        jar.addClass(NodeServicePolicyActivator.class);
        jar.addAsServiceProvider(ServiceActivator.class, NodeServicePolicyActivator.class);
        return jar;
    }

    @Test
    public void testSingletonService(
            @ArquillianResource @OperateOnDeployment(DEPLOYMENT_1) ManagementClient client1,
            @ArquillianResource @OperateOnDeployment(DEPLOYMENT_2) ManagementClient client2)
            throws Exception {

        // Needed to be able to inject ArquillianResource
        stop(NODE_2);

        String primaryProviderRequest = String.format("/subsystem=singleton/singleton-policy=default/service=%s:read-attribute(name=primary-provider)", NodeServicePolicyActivator.SERVICE_NAME.getCanonicalName());
        String isPrimaryRequest = String.format("/subsystem=singleton/singleton-policy=default/service=%s:read-attribute(name=is-primary)", NodeServicePolicyActivator.SERVICE_NAME.getCanonicalName());
        String getProvidersRequest = String.format("/subsystem=singleton/singleton-policy=default/service=%s:read-attribute(name=providers)", NodeServicePolicyActivator.SERVICE_NAME.getCanonicalName());

        Assert.assertEquals(NODE_1, execute(client1, primaryProviderRequest).asStringOrNull());
        Assert.assertTrue(execute(client1, isPrimaryRequest).asBoolean(false));
        Assert.assertEquals(Collections.singletonList(NODE_1), execute(client1, getProvidersRequest).asList().stream().map(ModelNode::asString).collect(Collectors.toList()));

        start(NODE_2);

        Assert.assertEquals(NODE_1, execute(client1, primaryProviderRequest).asStringOrNull());
        Assert.assertTrue(execute(client1, isPrimaryRequest).asBoolean(false));
        Assert.assertEquals(Arrays.asList(NODE_1, NODE_2), execute(client1, getProvidersRequest).asList().stream().map(ModelNode::asString).sorted().collect(Collectors.toList()));
        Assert.assertEquals(NODE_1, execute(client2, primaryProviderRequest).asStringOrNull());
        Assert.assertFalse(execute(client2, isPrimaryRequest).asBoolean(true));
        Assert.assertEquals(Arrays.asList(NODE_1, NODE_2), execute(client2, getProvidersRequest).asList().stream().map(ModelNode::asString).sorted().collect(Collectors.toList()));

        stop(NODE_2);

        Assert.assertEquals(NODE_1, execute(client1, primaryProviderRequest).asStringOrNull());
        Assert.assertTrue(execute(client1, isPrimaryRequest).asBoolean(false));
        Assert.assertEquals(Collections.singletonList(NODE_1), execute(client1, getProvidersRequest).asList().stream().map(ModelNode::asString).collect(Collectors.toList()));

        start(NODE_2);

        Assert.assertEquals(NODE_1, execute(client1, primaryProviderRequest).asStringOrNull());
        Assert.assertTrue(execute(client1, isPrimaryRequest).asBoolean(false));
        Assert.assertEquals(Arrays.asList(NODE_1, NODE_2), execute(client1, getProvidersRequest).asList().stream().map(ModelNode::asString).sorted().collect(Collectors.toList()));
        Assert.assertEquals(NODE_1, execute(client2, primaryProviderRequest).asStringOrNull());
        Assert.assertFalse(execute(client2, isPrimaryRequest).asBoolean(true));
        Assert.assertEquals(Arrays.asList(NODE_1, NODE_2), execute(client2, getProvidersRequest).asList().stream().map(ModelNode::asString).sorted().collect(Collectors.toList()));

        stop(NODE_1);

        Assert.assertEquals(NODE_2, execute(client2, primaryProviderRequest).asStringOrNull());
        Assert.assertTrue(execute(client2, isPrimaryRequest).asBoolean(false));
        Assert.assertEquals(Collections.singletonList(NODE_2), execute(client2, getProvidersRequest).asList().stream().map(ModelNode::asString).collect(Collectors.toList()));

        start(NODE_1);

        Assert.assertEquals(NODE_2, execute(client1, primaryProviderRequest).asStringOrNull());
        Assert.assertFalse(execute(client1, isPrimaryRequest).asBoolean(true));
        Assert.assertEquals(Arrays.asList(NODE_1, NODE_2), execute(client1, getProvidersRequest).asList().stream().map(ModelNode::asString).sorted().collect(Collectors.toList()));
        Assert.assertEquals(NODE_2, execute(client2, primaryProviderRequest).asStringOrNull());
        Assert.assertTrue(execute(client2, isPrimaryRequest).asBoolean(false));
        Assert.assertEquals(Arrays.asList(NODE_1, NODE_2), execute(client2, getProvidersRequest).asList().stream().map(ModelNode::asString).sorted().collect(Collectors.toList()));
    }
}
