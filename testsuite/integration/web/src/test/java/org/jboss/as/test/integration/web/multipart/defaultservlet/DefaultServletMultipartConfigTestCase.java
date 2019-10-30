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
