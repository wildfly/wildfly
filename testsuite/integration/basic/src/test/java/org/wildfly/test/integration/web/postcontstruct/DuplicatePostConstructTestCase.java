/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.web.postcontstruct;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.shared.CdiUtils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.web.postcontstruct.beanjar.SimpleServlet;

@RunWith(Arquillian.class)
@RunAsClient
public class DuplicatePostConstructTestCase {

    @ArquillianResource
    URL url;

    @Deployment
    public static WebArchive createDeployment() {
        JavaArchive beanJar = ShrinkWrap.create(JavaArchive.class)
                .addPackage(SimpleServlet.class.getPackage())
                .addAsManifestResource(CdiUtils.createBeansXml(), "beans.xml")
                .addAsManifestResource(SimpleServlet.class.getPackage(), "web-fragment.xml",
                        "web-fragment.xml");
        return ShrinkWrap.create(WebArchive.class).addAsLibrary(beanJar);
    }

    @Test
    public void test() throws URISyntaxException, IOException, InterruptedException {
        String response = HttpClient.newHttpClient()
                .send(
                        HttpRequest.newBuilder().uri(url.toURI()).build(),
                        HttpResponse.BodyHandlers.ofString())
                .body();

        Assert.assertEquals("34", response);
    }
}
