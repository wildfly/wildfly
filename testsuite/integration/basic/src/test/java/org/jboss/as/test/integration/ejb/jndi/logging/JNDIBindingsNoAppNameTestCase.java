/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.jndi.logging;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.shared.TestLogHandlerSetupTask;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.as.test.shared.logging.LoggingUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;

/**
 * Automated test for [ WFLY-11848 ] - Tests if JNDI bindings is correctly built in case there is no appName.
 *
 * @author Daniel Cihak
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(JNDIBindingsNoAppNameTestCase.TestLogHandlerSetup.class)
public class JNDIBindingsNoAppNameTestCase {

    private static final String JAR_NAME = "ejb-jndi.jar";
    private static String HOST = TestSuiteEnvironment.getServerAddress();
    private static int PORT = TestSuiteEnvironment.getHttpPort();
    private static final String TEST_HANDLER_NAME = "test-" + JNDIBindingsNoAppNameTestCase.class.getSimpleName();
    private static final String TEST_LOG_FILE_NAME = TEST_HANDLER_NAME + ".log";

    public static class TestLogHandlerSetup extends TestLogHandlerSetupTask {

        @Override
        public Collection<String> getCategories() {
            return Collections.singletonList("org.jboss");
        }

        @Override
        public String getLevel() {
            return "INFO";
        }
        @Override
        public String getHandlerName() {
            return TEST_HANDLER_NAME;
        }
        @Override
        public String getLogFileName() {
            return TEST_LOG_FILE_NAME;
        }
    }

    @Deployment
    public static JavaArchive createJar() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, JAR_NAME);
        jar.addClasses(JNDIBindingsNoAppNameTestCase.class, Hello.class, HelloBean.class);
        return jar;
    }

    @ContainerResource
    private ManagementClient managementClient;

    @Test
    public void testJNDIBindingsNoAppName() throws Exception {
        Context ctx = getInitialContext(HOST, PORT);
        Hello ejb = (Hello) ctx.lookup("ejb:/ejb-jndi/Hello!org.jboss.as.test.integration.ejb.jndi.logging.Hello");
        Assert.assertNotNull("Null object returned for local business interface lookup in the ejb namespace", ejb);
        Assert.assertTrue("Expected JNDI binding message not found", LoggingUtil.hasLogMessage(managementClient.getControllerClient(), TEST_HANDLER_NAME,
                "ejb:/ejb-jndi/Hello!org.jboss.as.test.integration.ejb.jndi.logging.Hello"));
    }

    private static Context getInitialContext(String host, Integer port)  throws NamingException {
        Properties props = new Properties();
        props.put(Context.INITIAL_CONTEXT_FACTORY, "org.wildfly.naming.client.WildFlyInitialContextFactory");
        props.put(Context.PROVIDER_URL, String.format("%s://%s:%d", "remote+http", host, port));
        return new InitialContext(props);
    }
}
