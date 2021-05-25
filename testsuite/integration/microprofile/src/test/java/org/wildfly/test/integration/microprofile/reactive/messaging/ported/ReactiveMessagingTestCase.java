/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.wildfly.test.integration.microprofile.reactive.messaging.ported;

import java.util.List;

import javax.inject.Inject;

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
