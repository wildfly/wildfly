/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
