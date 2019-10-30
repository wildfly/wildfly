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
package org.jboss.as.test.integration.ws.ejb;

import static java.util.concurrent.TimeUnit.SECONDS;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URL;

/**
 * EJB 3.1 FR 3.2.4 Stateless session beans and Singleton session beans may have web service clients.
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class EJBWebServicesTestCase {

    @ArquillianResource
    URL baseUrl;

    @Deployment
    public static JavaArchive deployment() {
        return ShrinkWrap.create(JavaArchive.class, "ejbws-example.jar")
                .addClasses(HttpRequest.class, SingletonEndpoint.class);
    }

    @Test
    public void testSingleton() throws Exception {
        final String message = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:test=\"http://ejb.ws.integration.test.as.jboss.org/\">"
                + "  <soapenv:Header/>"
                + "  <soapenv:Body>"
                + "    <test:setState>"
                + "      <arg0>Foo</arg0>"
                + "    </test:setState>"
                + "  </soapenv:Body>"
                + "</soapenv:Envelope>";
        URL webRoot = new URL(baseUrl, "/");
        String result = HttpRequest.post(webRoot.toString() + "ejbws-example/SingletonEndpoint", message, 10, SECONDS);
        // TODO: check something
    }

    @Test
    public void testSingletonWSDL() throws Exception {
        URL webRoot = new URL(baseUrl, "/");
        final String wsdl = HttpRequest.get(webRoot.toString() + "ejbws-example/SingletonEndpoint?wsdl", 10, SECONDS);
        // TODO: check something
    }

}
