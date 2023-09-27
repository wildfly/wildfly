/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.web.multipart.defaultservlet;

import static org.junit.Assert.assertEquals;

import java.net.URL;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that config applied to the default servlet is merged into its configuration
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
@RunAsClient
public class DefaultServletMultipartConfigTestCase {

    public static final String MESSAGE = "A text part";
    @ArquillianResource
    protected URL url;

    @Deployment
    public static Archive<?> deploy2() throws Exception {
        WebArchive jar = ShrinkWrap.create(WebArchive.class, "multipathTest.war");
        jar.addClasses(MultipartFilter.class);
        jar.addAsWebInfResource(DefaultServletMultipartConfigTestCase.class.getPackage(), "web.xml", "web.xml");
        return jar;
    }

    @Test
    public void testMultipartRequestToDefaultServlet() throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(url.toExternalForm() + "/servlet");
            post.setEntity(MultipartEntityBuilder.create()
                    .addTextBody("file", MESSAGE)
                    .build());

            HttpResponse response = httpClient.execute(post);
            HttpEntity entity = response.getEntity();

            StatusLine statusLine = response.getStatusLine();
            assertEquals(200, statusLine.getStatusCode());

            String result = EntityUtils.toString(entity);
            Assert.assertEquals(MESSAGE, result);
        }
    }
}
