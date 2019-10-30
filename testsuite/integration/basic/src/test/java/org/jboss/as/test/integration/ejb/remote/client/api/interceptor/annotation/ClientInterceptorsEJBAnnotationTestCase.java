/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.remote.client.api.interceptor.annotation;

import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.network.NetworkUtils;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.naming.client.WildFlyInitialContextFactory;

/**
 * Test that the @org.jboss.ejb.client.annotation.ClientInterceptors annotation works for adding EJB client interceptors.
 * @author Jan Martiska
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ClientInterceptorsEJBAnnotationTestCase {

    private static final String MODULE_NAME = "ejb-client-interceptor-annotated";

    @Deployment(testable = false)
    public static Archive createDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        jar.addPackage(ClientInterceptorsEJBAnnotationTestCase.class.getPackage());
        return jar;
    }

    @Test
    public void check() throws NamingException {
        final Properties properties = new Properties();
        properties.put(Context.INITIAL_CONTEXT_FACTORY, WildFlyInitialContextFactory.class.getName());
        properties.put(Context.PROVIDER_URL, "remote+http://" +
                NetworkUtils.formatPossibleIpv6Address(System.getProperty("node0", "localhost"))
                + ":8080");
        final InitialContext ejbCtx = new InitialContext(properties);
        try {
            SLSBReturningContextDataRemote bean = (SLSBReturningContextDataRemote)ejbCtx.lookup(
                    "ejb:/" + MODULE_NAME + "/" + SLSBReturningContextData.class.getSimpleName() + "!"
                            + SLSBReturningContextDataRemote.class.getName());

            final Object valueSeenOnServer = bean.getContextData("foo");
            Assert.assertEquals("bar", valueSeenOnServer);
        } finally {
            ejbCtx.close();
        }
    }

}
