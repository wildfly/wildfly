/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ws.context.as1605;

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * [AS7-1605] jboss-web.xml ignored for web service root
 * <p>
 * This test case tests if only EJB3 endpoints are in war archive.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class OnlyEjbInWarTestCase {

    @ArquillianResource
    URL baseUrl;

    @Deployment
    public static WebArchive createDeployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "as1605-usecase2.war");
        war.addClass(EndpointIface.class);
        war.addClass(EJB3Endpoint.class);
        war.addAsWebInfResource(new StringAsset(
                "<?xml version='1.0' encoding='UTF-8'?>\n" +
                        "<!DOCTYPE jboss-web PUBLIC \"-//JBoss//DTD Web Application 2.4//EN\" \"http://www.jboss.org/j2ee/dtd/jboss-web_4_0.dtd\">\n" +
                        "<jboss-web>\n" +
                        "<context-root>/as1605-customized</context-root>\n" +
                        "</jboss-web>\n"), "jboss-web.xml");
        return war;
    }

    @Test
    public void testEJB3Endpoint() throws Exception {
        final QName serviceName = new QName("org.jboss.as.test.integration.ws.context.as1605", "EJB3EndpointService");
        final URL wsdlURL = new URL(baseUrl, "/as1605-customized/EJB3Endpoint?wsdl");
        final Service service = Service.create(wsdlURL, serviceName);
        final EndpointIface port = service.getPort(EndpointIface.class);
        final String result = port.echo("hello");
        Assert.assertEquals("EJB3 hello", result);
    }

}
