/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023, Red Hat, Inc., and individual contributors
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

package org.wildfly.test.integration.microprofile.restclient;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.stream.JsonParser;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.microprofile.restclient.client.InfoResource;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;

import static org.jboss.as.test.shared.TestSuiteEnvironment.*;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author <a href="mailto:dkafetzi@redhat.com">Dimitris Kafetzis</a>
 *
 * Test if the Microprofile Header Propagation feature works properly.
 * The server has 2 resources, a client and a server the client calls
 * a microprofile service and that in turn forwards the call to the
 * server, what this tests is if the specified headers from the
 * microprofile config file are propagated correctly to the server service.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class HeaderPropagationTestCase {
    private final HttpClient httpClient = HttpClientBuilder.create().build();

    private static final String HOST = "http://localhost:8080";

    @ArquillianResource
    private URL url;

    @Deployment
    public static Archive<?> deploy() throws MalformedURLException {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "headerpropagation.war")
                .addClass(ClientResource.class)
                .addClass(ServerResource.class)
                .addClass(InfoResource.class)
                .addClass(ServerServiceApplication.class)
                .addClass(HeaderPropagationTestCase.class)
                .addAsManifestResource(
                        new StringAsset("org.eclipse.microprofile.rest.client.propagateHeaders=Authorization,Accept-Language,Header2Propagate\n"
                        + "org.wildfly.test.integration.microprofile.restclient.client.InfoResource/mp-rest/uri="+getHttpUrl().toString()+"/mp-hp"),
                        "microprofile-config.properties")
                .addAsWebInfResource(new FileAsset(
                        new File("src/test/resources/restclient/web.xml")),"web.xml")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        return war;
    }

    @Test
    public void testPropagation() throws IOException {
        HttpGet req = new HttpGet(url+"api/client");
        req.addHeader("Accept", "application/json");
        req.addHeader("Header2Propagate","PropagatedValue");
        HttpResponse res = httpClient.execute(req);
        String sres = EntityUtils.toString(res.getEntity());
        System.out.println(sres);
        JsonParser par = Json.createParser(new StringReader(sres));
        par.next();
        JsonObject jres = par.getObject();
        try {
            System.out.println(jres.getJsonObject("IncomingRequestHeaders").getJsonString("Header2Propagate").toString());
        } catch(NullPointerException e) {
            fail("The request to the client does not contain the Header2Propagate header");
        }
        try {
            System.out.println(jres.getJsonObject("ServerResponse").getJsonObject("IncomingRequestHeaders").getJsonString("Header2Propagate").toString());
        } catch(NullPointerException e) {
            fail("The Response from the server did not contain the propagated Header");
        }
    }
}
