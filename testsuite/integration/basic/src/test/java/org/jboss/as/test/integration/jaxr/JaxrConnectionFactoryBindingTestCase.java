/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.jaxr;

import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests the JAXR connection factory bound to JNDI
 *
 * @author Thomas.Diesler@jboss.com
 * @since 26-Oct-2011
 */
@RunAsClient
@RunWith(Arquillian.class)
public class JaxrConnectionFactoryBindingTestCase {

    @ArquillianResource
    private URL url;

    @Deployment
    public static WebArchive deployment() {
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "jaxr-connection-test.war");
        archive.addClasses(JaxrServlet.class);
        archive.addAsWebInfResource("jaxr/webA.xml", "web.xml");
        archive.addAsWebInfResource("jaxr/jboss-webA.xml", "jboss-web.xml");
        return archive;
    }

    @Test
    public void testConnectionFactoryLookup() throws Exception {
        String reqpath = url.toExternalForm() + "?method=lookup";
        String response = HttpRequest.get(reqpath, 10, TimeUnit.SECONDS);
        Assert.assertEquals("org.apache.ws.scout.registry.ConnectionFactoryImpl", response.trim());
    }

    @Test
    public void testConnectionFactoryNewInstance() throws Exception {
        String reqpath = url.toExternalForm() + "?method=new";
        String response = HttpRequest.get(reqpath, 10, TimeUnit.SECONDS);
        Assert.assertEquals("org.apache.ws.scout.registry.ConnectionFactoryImpl", response.trim());
    }
}
