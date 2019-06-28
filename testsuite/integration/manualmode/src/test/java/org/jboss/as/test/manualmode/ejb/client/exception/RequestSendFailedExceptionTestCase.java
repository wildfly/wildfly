/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.manualmode.ejb.client.exception;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.manualmode.ejb.Util;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.Context;
import javax.naming.NamingException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.regex.Pattern;

import static org.junit.Assert.fail;

/**
 * Tests that RequestSendFailedException thrown by EJB client contains host:port in the message.
 * Test for [ WFLY-12211 ].
 *
 * @author Daniel Cihak
 */
@RunWith(Arquillian.class)
@RunAsClient
public class RequestSendFailedExceptionTestCase {

    private static final String DEPLOYMENT = "RequestSendFailedException";
    private static final String CONTAINER = "jbossas-non-clustered";

    private Context context;

    @Deployment(name = DEPLOYMENT, managed = false, testable = false)
    @TargetsContainer(CONTAINER)
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, DEPLOYMENT + ".jar");
        jar.addPackage(RequestSendFailedExceptionTestCase.class.getPackage());
        return jar;
    }

    @Before
    public void before() throws Exception {
        final Properties ejbClientProperties = setupEJBClientProperties();
        this.context = Util.createNamingContext(ejbClientProperties);
    }

    @After
    public void after() throws Exception {
        this.context.close();
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

    private static Properties setupEJBClientProperties() throws IOException {
        final String clientPropertiesFile = "jboss-ejb-client.properties";
        final InputStream inputStream = RequestSendFailedExceptionTestCase.class.getResourceAsStream(clientPropertiesFile);
        if (inputStream == null) {
            throw new IllegalStateException("Could not find " + clientPropertiesFile + " in classpath");
        }
        final Properties properties = new Properties();
        properties.load(inputStream);
        return properties;
    }

    @Test
    public void testRequestSendFailedException() {
        Pattern hostPortPattern = Pattern.compile("^"
                + "http://"
                + "(((?!-)[A-Za-z0-9-]{1,63}(?<!-)\\.)+[A-Za-z]{2,6}" // Domain name
                + "|"
                + "localhost"
                + "|"
                + "(([0-9]{1,3}\\.){3})[0-9]{1,3})" // IP
                + ":"
                + "[0-9]{1,5}$"); // port

        try {
            SimpleBeanRemote bean = lookup(SimpleBeanRemote.class, SimpleBean.class, DEPLOYMENT);
            bean.doSomething();
            fail("It was expected a RequestSendFailedException being thrown");
        } catch (Exception e) {
            String exceptionMessage = e.getSuppressed()[0].getMessage();
            if (exceptionMessage == null) {
                fail("RequestSendFailedException must contain message with host:port.");
            }
            String[] messageParts = exceptionMessage.split("Destination @ remote\\+");
            Assert.assertTrue(hostPortPattern.matcher(messageParts[1]).matches());
        }
    }
}
