/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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
package org.wildfly.test.integration.microprofile.opentracing;

import java.net.URI;
import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import org.wildfly.test.integration.microprofile.opentracing.application.SimpleJaxRs;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests integration with eclipse-microprofile-opentracing specification, see https://github.com/eclipse/microprofile-opentracing.
 *
 * @author Jan Stourac <jstourac@redhat.com>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class OnOffOpenTracingTestCase {

    @ArquillianResource
    private URL url;

    private static final String DEPLOYMENT = "deployment";

    private static final String WEB_XML =
              "<web-app version=\"3.1\" xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"\n"
            + "        xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
            + "        xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-app_3_1.xsd\">\n"
            + "    <servlet-mapping>\n"
            + "        <servlet-name>javax.ws.rs.core.Application</servlet-name>\n"
            + "        <url-pattern>/rest/*</url-pattern>\n"
            + "    </servlet-mapping>\n"
            + "</web-app>";

    @Deployment(name = DEPLOYMENT)
    public static Archive<?> createContainer1Deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, DEPLOYMENT + ".war");
        war.addClass(SimpleJaxRs.class);
        war.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        war.addAsWebInfResource(new StringAsset(WEB_XML), "web.xml");

        return war;
    }

    @Test
    public void testOpenTracingMultinodeServer() throws Exception {
        URI requestUri = new URI(url.toString() + "rest" + SimpleJaxRs.GET_TRACER);

        // Perform request to the deployment on server where OpenTracing is enabled. Deployment has to have a JaegerTracer instance available.
        String response = Utils.makeCall(requestUri, 200);
        Assert.assertNotEquals(SimpleJaxRs.TRACER_NOT_INITIALIZED, response);
        Assert.assertTrue(response.contains("Tracer instance is: JaegerTracer"));
    }

}
