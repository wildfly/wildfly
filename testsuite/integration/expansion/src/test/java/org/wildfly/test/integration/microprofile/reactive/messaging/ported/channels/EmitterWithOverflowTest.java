/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.reactive.messaging.ported.channels;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import jakarta.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.CLIServerSetupTask;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.wildfly.test.integration.microprofile.reactive.EnableReactiveExtensionsSetupTask;


/**
 * Ported from Quarkus and adjusted
 *
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@ServerSetup(EnableReactiveExtensionsSetupTask.class)
@ExtendWith(ArquillianExtension.class)
public class EmitterWithOverflowTest {

    @Deployment
    public static WebArchive createDeployment() {
        final WebArchive webArchive = ShrinkWrap.create(WebArchive.class, "rx-messaging-emitter-with-overflow.war")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addClasses(ChannelEmitterWithOverflow.class)
                .addClasses(EnableReactiveExtensionsSetupTask.class, CLIServerSetupTask.class);
        return webArchive;
    }

    @Inject
    ChannelEmitterWithOverflow bean;

    @Test
    void emitter() {
        bean.run();
        List<String> list = bean.list();
        assertEquals(3, list.size());
        assertEquals("a", list.get(0));
        assertEquals("b", list.get(1));
        assertEquals("c", list.get(2));

        List<String> sink1 = bean.sink1();
        assertEquals(3, sink1.size());
        assertEquals("a1", sink1.get(0));
        assertEquals("b1", sink1.get(1));
        assertEquals("c1", sink1.get(2));

        List<String> sink2 = bean.sink2();
        assertEquals(3, sink2.size());
        assertEquals("a2", sink2.get(0));
        assertEquals("b2", sink2.get(1));
        assertEquals("c2", sink2.get(2));
    }

}
