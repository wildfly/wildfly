/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.test.manualmode.ejb.client.authentication;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.manualmode.ejb.Util;
import org.jboss.ejb.client.EJBClientConnection;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.ejb.protocol.remote.RemoteTransportProvider;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.security.auth.client.AuthenticationConfiguration;

import org.wildfly.security.auth.client.AuthenticationContext;
import org.wildfly.security.auth.client.MatchRule;
import org.wildfly.security.sasl.SaslMechanismSelector;

import javax.ejb.NoSuchEJBException;
import javax.naming.Context;
import javax.naming.NamingException;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.util.Properties;

import static org.junit.Assert.assertNotNull;

/**
 * Tests if the excessive suppressed exceptions occur when EJB client authentication fails.
 * Test for [ WFLY-9514 ].
 *
 * @author Daniel Cihak
 */
@RunWith(Arquillian.class)
@RunAsClient
public class EJBClientAuthenticationFailsTestCase {

    private static final Logger LOGGER = Logger.getLogger(EJBClientAuthenticationFailsTestCase.class);

    private static final String DEPLOYMENT = "deployment";
    private static final String CONTAINER = "default-jbossas";

    @ArquillianResource
    private ContainerController controller;

    @ArquillianResource
    private Deployer deployer;

    private Context context;

    @Deployment(name = DEPLOYMENT, managed = false, testable = false)
    @TargetsContainer(CONTAINER)
    public static Archive<?> deploy() {
        return ShrinkWrap.create(JavaArchive.class, DEPLOYMENT + ".jar")
                .addClass(EJBClientAuthenticationFailsTestCase.class)
                .addClasses(HelloBean.class, HelloBeanRemote.class);
    }

    @Before
    public void before() throws Exception {
        this.context = Util.createNamingContext(new Properties());

        controller.start(CONTAINER);
        LOGGER.trace("===appserver started===");
        deployer.deploy(DEPLOYMENT);
        LOGGER.trace("===deployment deployed===");
    }

    @After
    public void after() throws Exception {
        this.context.close();

        try {
            if (!controller.isStarted(CONTAINER)) {
                controller.start(CONTAINER);
            }
            deployer.undeploy(DEPLOYMENT);
            LOGGER.trace("===deployment undeployed===");
        } finally {
            controller.stop(CONTAINER);
            LOGGER.trace("===appserver stopped===");
        }
    }

    /**
     * Tests number of occurrences of the "Server rejected authentication" error message in the NoSuchEJBException stack trace
     *
     * @throws Throwable
     */
    @Test
    public void testEJBClientAuthenticationFailsMessage() throws Throwable {
        AuthenticationConfiguration common = AuthenticationConfiguration.empty()
                .setSaslMechanismSelector(SaslMechanismSelector.fromString("DIGEST-MD5"));
        AuthenticationContext authCtxEmpty = AuthenticationContext.empty();
        AuthenticationConfiguration joe = common.useName("joe").usePassword("joeIsAwesome2013!");
        final AuthenticationContext authCtx = authCtxEmpty.with(MatchRule.ALL, joe);

        final EJBClientContext ejbCtx = new EJBClientContext.Builder()
                .addTransportProvider(new RemoteTransportProvider())
                .addClientConnection(new EJBClientConnection.Builder()
                        .setDestination(URI.create("remote+http://127.0.0.1:8080")).build())
                .build();
        EJBClientContext.getContextManager().setThreadDefault(ejbCtx);
        AuthenticationContext.getContextManager().setThreadDefault(authCtx);

        HelloBeanRemote bean = lookup(HelloBeanRemote.class, HelloBean.class, DEPLOYMENT);
        assertNotNull(bean);

        PrintStream oldOut = System.err;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            try {
                bean.hello();
            } catch (NoSuchEJBException nsee) {
                System.setErr(new PrintStream(baos));
                nsee.printStackTrace();
                Thread.sleep(1000);
                System.setErr(oldOut);
                String output = new String(baos.toByteArray());
                int count = count(output, "Server rejected authentication");
                Assert.assertEquals("Number of occurrences of the message must be 2", 2, count);
            }
        } finally {
            System.setErr(oldOut);
        }
    }

    private static int count(final String string, final String substring) {
        int count = 0;
        int idx = 0;

        while ((idx = string.indexOf(substring, idx)) != -1) {
            idx++;
            count++;
        }

        return count;
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
}
