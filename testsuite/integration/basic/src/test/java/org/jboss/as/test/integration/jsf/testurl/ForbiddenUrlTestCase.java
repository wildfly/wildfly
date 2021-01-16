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

/**
 * UnallowedUrlTestCase
 * For more information visit <a href="https://issues.redhat.com/browse/WFLY-13439">https://issues.redhat.com/browse/WFLY-13439</a>
 * @author Petr Adamec
 */
package org.jboss.as.test.integration.jsf.testurl;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
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

import java.net.URL;

/**
 * Test if 404 is returned for url address which isn't allowed
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ForbiddenUrlTestCase {
    private static final String ALLOWED_URL = "faces/javax.faces.resource/lala.js?con=lala";
    private static final String FORBIDDEN_URL = "faces/javax.faces.resource/lala.js?con=lala/../lala";
    private static final int ALLOWED_STATUS_CODE = 200;
    private static final int FORBIDDEN_STATUS_CODE = 404;

    @ArquillianResource
    private URL url;


    @Deployment
    public static Archive<?> deploy() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "jsf-resources.war");
        war.addAsWebInfResource(ForbiddenUrlTestCase.class.getPackage(), "web.xml", "web.xml");
        war.addAsWebResource(ForbiddenUrlTestCase.class.getPackage(), "index.xhtml", "index.xhtml");
        war.addAsWebResource(ForbiddenUrlTestCase.class.getPackage(), "lala.jsp", "lala.jsp");
        war.addAsWebResource(ForbiddenUrlTestCase.class.getPackage(), "lala.js", "contracts/lala/lala.js");
        return war;
    }

    /**
     * Test if 200 is returned for allowed url
     * <br /> For more information see https://issues.redhat.com/browse/WFLY-13439.
     * @throws Exception
     */
    @Test
    public void testAllowedUrl() throws Exception {
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();

        try(CloseableHttpClient client = httpClientBuilder.build()) {

            HttpUriRequest getVarRequest = new HttpGet(url.toExternalForm() + ALLOWED_URL);
            try (CloseableHttpResponse getVarResponse = client.execute(getVarRequest)) {
                int statusCode = getVarResponse.getStatusLine().getStatusCode();
                Assert.assertEquals("Status code should be " + ALLOWED_STATUS_CODE + ", but is " + statusCode + ". For more information see JBEAP-18842. ", ALLOWED_STATUS_CODE, statusCode);
            }
        }
    }

    /**
     * Test id 404 is returned fot forbidden url.
     * <br /> For more information see https://issues.redhat.com/browse/WFLY-13439
     * @throws Exception
     */
    @Test
    public void testForbiddenUrl() throws Exception {
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();

        try(CloseableHttpClient client = httpClientBuilder.build()) {

            HttpUriRequest getVarRequest = new HttpGet(url.toExternalForm() + FORBIDDEN_URL);
            try (CloseableHttpResponse getVarResponse = client.execute(getVarRequest)) {
                int statusCode = getVarResponse.getStatusLine().getStatusCode();
                Assert.assertEquals("Status code should be " + FORBIDDEN_STATUS_CODE + ", but is " + statusCode + ". For more information see JBEAP-18842. ", FORBIDDEN_STATUS_CODE, statusCode);
            }
        }
    }
}
