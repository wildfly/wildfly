package org.jboss.as.test.integration.ejb.remote.client.api.moreconnection;

import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.naming.client.WildFlyInitialContextFactory;

/**
 * Parse wildfly-config.xml with two connection
 * @author Petr Adamec
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ParseConfigWithMoreConnectionTestCase {

    private static final String MODULE_NAME = "ejb-more-connection-parse";

    @BeforeClass
    public static void beforeClass(){
        System.setProperty("wildfly.config.url", "src/test/resources/META-INF/ParseConfigWithMoreConnectionTestCase/wildfly-config.xml");
    }

    @Deployment(testable = false)
    public static Archive createDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        jar.addPackage(ParseConfigWithMoreConnectionTestCase.class.getPackage());
        return jar;
    }

    @Test
    public void check() throws NamingException {
        final Properties properties = new Properties();
        properties.put(Context.INITIAL_CONTEXT_FACTORY, WildFlyInitialContextFactory.class.getName());
        final InitialContext ejbCtx = new InitialContext(properties);
        try {
            HelloBeanRemote bean = (HelloBeanRemote)ejbCtx.lookup(
                    "ejb:/" + MODULE_NAME + "/" + HelloBean.class.getSimpleName() + "!"
                            + HelloBeanRemote.class.getName());

            final String valueSeenOnServer = bean.hello();
            Assert.assertEquals(HelloBeanRemote.VALUE, valueSeenOnServer);
        } catch (ExceptionInInitializerError e){
            Assert.fail("Test fails probably due to https://issues.jboss.org/browse/REM3-329 .");
        }
        finally {
            ejbCtx.close();
        }
    }

}
