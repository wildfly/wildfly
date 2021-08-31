package org.wildfly.test.integration.microprofile.reactive.messaging.ported.channels;

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


/**
 * Ported from Quarkus and adjusted
 *
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@ServerSetup(EnableReactiveExtensionsSetupTask.class)
@RunWith(Arquillian.class)
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
    public void testEmitter() {
        bean.run();
        List<String> list = bean.list();
        Assert.assertEquals(3, list.size());
        Assert.assertEquals("a", list.get(0));
        Assert.assertEquals("b", list.get(1));
        Assert.assertEquals("c", list.get(2));

        List<String> sink1 = bean.sink1();
        Assert.assertEquals(3, sink1.size());
        Assert.assertEquals("a1", sink1.get(0));
        Assert.assertEquals("b1", sink1.get(1));
        Assert.assertEquals("c1", sink1.get(2));

        List<String> sink2 = bean.sink2();
        Assert.assertEquals(3, sink2.size());
        Assert.assertEquals("a2", sink2.get(0));
        Assert.assertEquals("b2", sink2.get(1));
        Assert.assertEquals("c2", sink2.get(2));
    }

}
