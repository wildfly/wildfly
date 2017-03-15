/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.manualmode.ejb.client.reconnect;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.manualmode.ejb.Util;
import org.jboss.ejb.client.EJBClient;
import org.jboss.ejb.client.StatelessEJBLocator;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Simple ejb client reconnection test case.
 * See AS7-3215.
 *
 * @author <a href="mailto:istudens@redhat.com">Ivo Studensky</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class EJBClientReconnectionTestCase {
    private static final Logger log = Logger.getLogger(EJBClientReconnectionTestCase.class);

    private static final String DEPLOYMENT = "ejbclientreconnection";
    private static final String CONTAINER = "jbossas-non-clustered";

    @ArquillianResource
    private ContainerController controller;

    @ArquillianResource
    private Deployer deployer;

    private Context context;


    @Deployment(name = DEPLOYMENT, managed = false, testable = false)
    @TargetsContainer(CONTAINER)
    public static Archive<?> deploy() {
        return ShrinkWrap.create(JavaArchive.class, DEPLOYMENT + ".jar")
                .addClasses(SimpleCrashBean.class, SimpleCrashBeanRemote.class);
    }

    @Before
    public void before() throws Exception {
        final Properties ejbClientProperties = setupEJBClientProperties();
        this.context = Util.createNamingContext(ejbClientProperties);

        controller.start(CONTAINER);
        log.trace("===appserver started===");
        deployer.deploy(DEPLOYMENT);
        log.trace("===deployment deployed===");
    }

    @After
    public void after() throws Exception {
        this.context.close();

        try {
            if (!controller.isStarted(CONTAINER)) {
                controller.start(CONTAINER);
            }
            deployer.undeploy(DEPLOYMENT);
            log.trace("===deployment undeployed===");
        } finally {
            controller.stop(CONTAINER);
            log.trace("===appserver stopped===");
        }
    }

    @Test
    public void testReconnection() throws Throwable {
        SimpleCrashBeanRemote bean = lookup(SimpleCrashBeanRemote.class, SimpleCrashBean.class, DEPLOYMENT);
        assertNotNull(bean);
        String echo = bean.echo("Hello!");
        assertEquals("Hello!", echo);

        controller.stop(CONTAINER);
        log.trace("===appserver stopped===");
        controller.start(CONTAINER);
        log.trace("===appserver started again===");

        SimpleCrashBeanRemote bean2 = lookup(SimpleCrashBeanRemote.class, SimpleCrashBean.class, DEPLOYMENT);
        assertNotNull(bean2);
        echo = bean2.echo("Bye!");
        assertEquals("Bye!", echo);
    }

    @Test
    public void testReconnectionWithClientAPI() throws Throwable {
        // TODO Elytron: Determine how this should be adapted once the transaction client changes are in
        //final EJBClientTransactionContext localUserTxContext = EJBClientTransactionContext.createLocal();
        //EJBClientTransactionContext.setGlobalContext(localUserTxContext);

        final StatelessEJBLocator<SimpleCrashBeanRemote> locator = new StatelessEJBLocator(SimpleCrashBeanRemote.class, "", DEPLOYMENT, SimpleCrashBean.class.getSimpleName(), "");
        final SimpleCrashBeanRemote proxy = EJBClient.createProxy(locator);

        assertNotNull(proxy);
        String echo = proxy.echo("Hello!");
        assertEquals("Hello!", echo);

        controller.stop(CONTAINER);
        log.trace("===appserver stopped===");
        controller.start(CONTAINER);
        log.trace("===appserver started again===");


        final StatelessEJBLocator<SimpleCrashBeanRemote> locator2 = new StatelessEJBLocator(SimpleCrashBeanRemote.class, "", DEPLOYMENT, SimpleCrashBean.class.getSimpleName(), "");
        final SimpleCrashBeanRemote proxy2 = EJBClient.createProxy(locator2);

        assertNotNull(proxy2);
        echo = proxy2.echo("Bye!");
        assertEquals("Bye!", echo);
    }

    private <T> T lookup(final Class<T> remoteClass, final Class<?> beanClass, final String archiveName) throws NamingException {
        String myContext = Util.createRemoteEjbJndiContext(
                "",
                archiveName,
                "",
                beanClass.getSimpleName(),
                remoteClass.getName(),
                false);

        return remoteClass.cast(context.lookup(myContext));
    }

    /**
     * Sets up the EJB client properties based on this testcase specific jboss-ejb-client.properties file
     *
     * @return
     * @throws java.io.IOException
     */
    private static Properties setupEJBClientProperties() throws IOException {
        // setup the properties
        final String clientPropertiesFile = "jboss-ejb-client.properties";
        final InputStream inputStream = EJBClientReconnectionTestCase.class.getResourceAsStream(clientPropertiesFile);
        if (inputStream == null) {
            throw new IllegalStateException("Could not find " + clientPropertiesFile + " in classpath");
        }
        final Properties properties = new Properties();
        properties.load(inputStream);
        return properties;
    }


}
