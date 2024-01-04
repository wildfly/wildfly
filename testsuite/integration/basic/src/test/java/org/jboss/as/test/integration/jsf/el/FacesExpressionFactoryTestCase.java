/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jsf.el;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;

import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@RunAsClient
public class FacesExpressionFactoryTestCase {

    private static final String TEST_NAME = "FacesExpressionFactoryTestCase";

    @Deployment(name = TEST_NAME)
    public static Archive<?> deploy() {
        final Package resourcePackage = FacesExpressionFactoryTestCase.class.getPackage();
        return ShrinkWrap.create(WebArchive.class, TEST_NAME + ".war")
                .addClasses(ElServlet.class)
                .addAsWebInfResource(resourcePackage, "beans.xml", "beans.xml")
                .addAsWebInfResource(resourcePackage, "faces-config.xml", "faces-config.xml")
                .addAsManifestResource(createPermissionsXmlAsset(
                        new RuntimePermission("getClassLoader")),
                    "permissions.xml");
    }

    @ArquillianResource
    @OperateOnDeployment(TEST_NAME)
    private URL url;

    @Test
    public void testApplicationExpressionFactory() throws Exception {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpUriRequest getRequest = new HttpGet(url.toExternalForm() + "ElServlet");
            try (CloseableHttpResponse getResponse = client.execute(getRequest)) {
                org.junit.Assert.assertEquals(EntityUtils.toString(getResponse.getEntity()), HttpURLConnection.HTTP_OK, getResponse.getStatusLine().getStatusCode());
            }
        }
    }
}
