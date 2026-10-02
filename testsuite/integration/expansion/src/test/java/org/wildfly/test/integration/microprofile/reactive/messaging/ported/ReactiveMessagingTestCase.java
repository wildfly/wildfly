/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.reactive.messaging.ported;

import java.util.List;

import jakarta.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.CLIServerSetupTask;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.wildfly.test.integration.microprofile.reactive.EnableReactiveExtensionsSetupTask;
import org.wildfly.test.integration.microprofile.reactive.messaging.ported.channels.ChannelConsumer;
import org.wildfly.test.integration.microprofile.reactive.messaging.ported.channels.EmitterExample;

/**
 * Ported from Quarkus and adjusted
 *
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@ServerSetup(EnableReactiveExtensionsSetupTask.class)
@ExtendWith(ArquillianExtension.class)
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
        Assertions.assertEquals(4, SimpleBean.RESULT.size());
        Assertions.assertTrue(SimpleBean.RESULT.contains("HELLO"));
        Assertions.assertTrue(SimpleBean.RESULT.contains("SMALLRYE"));
        Assertions.assertTrue(SimpleBean.RESULT.contains("REACTIVE"));
        Assertions.assertTrue(SimpleBean.RESULT.contains("MESSAGE"));
    }

    @Test
    public void testChannelInjection() throws Exception {
        List<String> consumed = channelConsumer.consume();
        Assertions.assertEquals(5, consumed.size());
        Assertions.assertEquals("hello", consumed.get(0));
        Assertions.assertEquals("with", consumed.get(1));
        Assertions.assertEquals("SmallRye", consumed.get(2));
        Assertions.assertEquals("reactive", consumed.get(3));
        Assertions.assertEquals("message", consumed.get(4));
    }

    @Test
    public void testEmitter() {
        emitterExample.run();
        List<String> list = emitterExample.list();
        Assertions.assertEquals(3, list.size());
        Assertions.assertEquals("a", list.get(0));
        Assertions.assertEquals("b", list.get(1));
        Assertions.assertEquals("c", list.get(2));
    }

}
