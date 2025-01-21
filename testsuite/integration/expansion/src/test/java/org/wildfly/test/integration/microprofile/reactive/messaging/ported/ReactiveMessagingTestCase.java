/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.reactive.messaging.ported;

import java.util.List;

import jakarta.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.CLIServerSetupTask;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.microprofile.reactive.EnableReactiveExtensionsSetupTask;
import org.wildfly.test.integration.microprofile.reactive.messaging.ported.channels.ChannelConsumer;
import org.wildfly.test.integration.microprofile.reactive.messaging.ported.channels.EmitterExample;

/**
 * Ported from Quarkus and adjusted
 *
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@ServerSetup(EnableReactiveExtensionsSetupTask.class)
@RunWith(Arquillian.class)
public class ReactiveMessagingTestCase {
    @Inject
    ChannelConsumer channelConsumer;

    @Inject
    EmitterExample emitterExample;

    @Deployment
    public static WebArchive createDeployment() {
        final WebArchive webArchive = ShrinkWrap.create(WebArchive.class, "rx-messaging-ported.war")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addClasses(ReactiveMessagingTestCase.class, SimpleBean.class, ChannelConsumer.class, EmitterExample.class)
                .addClasses(EnableReactiveExtensionsSetupTask.class, CLIServerSetupTask.class);
        return webArchive;
    }

    @Test
    public void testSimpleBean() {
        Assert.assertEquals(4, SimpleBean.RESULT.size());
        Assert.assertTrue(SimpleBean.RESULT.contains("HELLO"));
        Assert.assertTrue(SimpleBean.RESULT.contains("SMALLRYE"));
        Assert.assertTrue(SimpleBean.RESULT.contains("REACTIVE"));
        Assert.assertTrue(SimpleBean.RESULT.contains("MESSAGE"));
    }

    @Test
    public void testChannelInjection() throws Exception {
        List<String> consumed = channelConsumer.consume();
        Assert.assertEquals(5, consumed.size());
        Assert.assertEquals("hello", consumed.get(0));
        Assert.assertEquals("with", consumed.get(1));
        Assert.assertEquals("SmallRye", consumed.get(2));
        Assert.assertEquals("reactive", consumed.get(3));
        Assert.assertEquals("message", consumed.get(4));
    }

    @Test
    public void testEmitter() {
        emitterExample.run();
        List<String> list = emitterExample.list();
        Assert.assertEquals(3, list.size());
        Assert.assertEquals("a", list.get(0));
        Assert.assertEquals("b", list.get(1));
        Assert.assertEquals("c", list.get(2));
    }

}
