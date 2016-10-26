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

package org.jboss.as.test.integration.ws.anonymousPojos;

import java.net.URL;
import org.jboss.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * [JBWS-3276] Tests anonymous POJO in web archive that is missing web.xml.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class Usecase2TestCase {

    @ArquillianResource
    URL baseUrl;

    private static final Logger log = Logger.getLogger(Usecase2TestCase.class.getName());

    @Deployment
    public static WebArchive createDeployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "anonymous-pojo-usecase2.war");
        war.addPackage(AnonymousPOJO.class.getPackage());
        war.addClass(AnonymousPOJO.class);
        return war;
    }

    @Test
    public void testAnonymousEndpoint() throws Exception {
        final QName serviceName = new QName("org.jboss.as.test.integration.ws.anonymousPojos", "AnonymousPOJOService");
        final URL wsdlURL = new URL(baseUrl, "/anonymous-pojo-usecase2/AnonymousPOJOService?wsdl");
        final Service service = Service.create(wsdlURL, serviceName);
        final POJOIface port = service.getPort(POJOIface.class);
        final String result = port.echo("hello");
        Assert.assertEquals("hello from anonymous POJO", result);
    }

}
