/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

package org.jboss.as.test.integration.ejb.mapbased;

import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for EJBCLIENT-34: properties-based JNDI InitialContext for EJB clients.
 *
 * @author Jan Martiska / jmartisk@redhat.com
 */
@RunWith(Arquillian.class)
@RunAsClient
public class MapBasedInitialContextEjbClientTestCase {

    private static final String ARCHIVE_NAME = "map-based-client-1";

    @Deployment
    public static Archive<?> getDeployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME);
        archive.addPackage(StatelessBean.class.getPackage());
        return archive;
    }


    /**
     * Tests that invocations on a scoped EJB client context use the correct receiver(s)
     *
     * @throws Exception
     */
    @Test
    public void testScopedEJBClientContexts() throws Exception {
        InitialContext ctx = new InitialContext(getEjbClientProperties(System.getProperty("node0", "127.0.0.1"), 8080));
        try {
            String lookupName = "ejb:/" + ARCHIVE_NAME + "/" + StatelessBean.class.getSimpleName() + "!" + StatelessIface.class.getCanonicalName();
            StatelessIface beanStateless = (StatelessIface) ctx.lookup(lookupName);
            Assert.assertEquals("Unexpected EJB client context used for invoking stateless bean", CustomCallbackHandler.USER_NAME, beanStateless.getCallerPrincipalName());
            lookupName = "ejb:/" + ARCHIVE_NAME + "/" + StatefulBean.class.getSimpleName() + "!" + StatefulIface.class.getCanonicalName() + "?stateful";
            StatefulIface beanStateful = (StatefulIface) ctx.lookup(lookupName);
            Assert.assertEquals("Unexpected EJB client context used for invoking stateful bean", CustomCallbackHandler.USER_NAME, beanStateful.getCallerPrincipalName());
            ctx.close();
        } finally {
            ctx.close();
        }
    }

    private Properties getEjbClientProperties(String node, int port) {
        Properties props = new Properties();
        props.put("org.jboss.ejb.client.scoped.context", true);
        props.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        props.put("endpoint.name", "client");
        props.put("remote.connections", "main");
        props.put("remote.connection.main.host", node);
        props.put("remote.connection.main.port", Integer.toString(port));
        props.put("remote.connection.main.callback.handler.class", CustomCallbackHandler.class.getName());
        props.put("remote.connection.main.connect.options.org.xnio.Options.SASL_POLICY_NOANONYMOUS", "false");
        props.put("remote.connection.main.connect.options.org.xnio.Options.SASL_POLICY_NOPLAINTEXT", "true");
        props.put("remote.connectionprovider.create.options.org.xnio.Options.SSL_ENABLED", "false");
        return props;
    }

}
