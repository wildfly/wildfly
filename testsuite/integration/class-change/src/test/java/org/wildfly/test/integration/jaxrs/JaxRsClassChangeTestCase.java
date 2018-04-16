/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.test.integration.jaxrs;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.util.SimpleRemoteReplacement;

import io.undertow.util.StatusCodes;

@RunWith(Arquillian.class)
@RunAsClient
public class JaxRsClassChangeTestCase {

    private static final String JAXRS = "/jaxrs/";
    private static final String CDI = "/cdi/";

    @Deployment
    public static WebArchive deploy() {
        return ShrinkWrap.create(WebArchive.class, "jaxrs.war")
                .addClasses(Activator.class, FooResource.class, RedeployResource.class)
                .addAsWebInfResource(new StringAsset("remote.password=test"), "class-change.properties");

    }

    @Deployment(name = "cdi")
    public static WebArchive cdi() {
        return ShrinkWrap.create(WebArchive.class, "cdi.war")
                .addClasses(Activator.class, FooResource.class, RedeployResource.class)
                .addAsWebInfResource(new StringAsset("remote.password=test"), "class-change.properties")
                .addAsWebInfResource(new StringAsset(""), "beans.xml");

    }

    @Test
    public void testJaxRsResourceReplacement() throws Exception {
        testJaxRsResourceReplacement(JAXRS);
    }

    @Test
    public void testJaxRsResourceReplacementCDI() throws Exception {
        testJaxRsResourceReplacement(CDI);
    }

    public void testJaxRsResourceReplacement(String path) throws Exception {

        String deploymentUrl = "http://" + TestSuiteEnvironment.getHttpAddress() + ":" + TestSuiteEnvironment.getHttpPort() + path;
        try (SimpleRemoteReplacement replacement = new SimpleRemoteReplacement(path, JaxRsClassChangeTestCase.class, FooResource.class, Activator.class, RedeployResource.class)) {

            try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
                HttpGet get = new HttpGet(deploymentUrl + "rest/foo/p1");
                try (CloseableHttpResponse response = client.execute(get)) {
                    Assert.assertEquals(StatusCodes.OK, response.getStatusLine().getStatusCode());
                    String test = EntityUtils.toString(response.getEntity());
                    Assert.assertEquals("p1-result", test);
                }
                replacement.queueClassReplacement(FooResource.class, FooResource1.class);
                replacement.doReplacement();
                try (CloseableHttpResponse response = client.execute(get)) {
                    Assert.assertEquals(StatusCodes.OK, response.getStatusLine().getStatusCode());
                    String test = EntityUtils.toString(response.getEntity());
                    Assert.assertEquals("p1-changed", test);
                }
                try (CloseableHttpResponse response = client.execute(new HttpGet(deploymentUrl + "rest/foo/p2"))) {
                    Assert.assertEquals(StatusCodes.OK, response.getStatusLine().getStatusCode());
                    String test = EntityUtils.toString(response.getEntity());
                    Assert.assertEquals("p2-changed", test);
                }

            }
        }
    }

    @Test
    public void testJaxRsResourceReplacementWithRedeployment() throws Exception {
        testJaxRsResourceReplacementWithRedeployment(JAXRS);
    }

    @Test
    public void testJaxRsResourceReplacementWithRedeploymentCdi() throws Exception {
        testJaxRsResourceReplacementWithRedeployment(CDI);
    }

    public void testJaxRsResourceReplacementWithRedeployment(String path) throws Exception {

        String deploymentUrl = "http://" + TestSuiteEnvironment.getHttpAddress() + ":" + TestSuiteEnvironment.getHttpPort() + path;
        try (SimpleRemoteReplacement replacement = new SimpleRemoteReplacement(path, JaxRsClassChangeTestCase.class, FooResource.class, Activator.class)) {

            try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
                String v1;
                HttpGet get = new HttpGet(deploymentUrl + "rest/redeploy");
                try (CloseableHttpResponse response = client.execute(get)) {
                    Assert.assertEquals(StatusCodes.OK, response.getStatusLine().getStatusCode());
                    v1 = EntityUtils.toString(response.getEntity());
                }
                replacement.queueClassReplacement(RedeployResource.class, RedeployResource1.class);
                replacement.doReplacement();
                try (CloseableHttpResponse response = client.execute(get)) {
                    Assert.assertEquals(StatusCodes.OK, response.getStatusLine().getStatusCode());
                    String test = EntityUtils.toString(response.getEntity());
                    Assert.assertNotEquals(v1, test);
                }

            }
        }
    }

    @Test
    public void testJaxRsAddResource() throws Exception {
        testJaxRsAddResource(JAXRS);
    }

    @Test
    public void testJaxRsAddResourceCDI() throws Exception {
        testJaxRsAddResource(CDI);
    }

    public void testJaxRsAddResource(String path) throws Exception {

        String deploymentUrl = "http://" + TestSuiteEnvironment.getHttpAddress() + ":" + TestSuiteEnvironment.getHttpPort() + path;
        try (SimpleRemoteReplacement replacement = new SimpleRemoteReplacement(path, JaxRsClassChangeTestCase.class, FooResource.class, Activator.class)) {

            try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
                HttpGet get = new HttpGet(deploymentUrl + "rest/add");
                try (CloseableHttpResponse response = client.execute(get)) {
                    Assert.assertEquals(StatusCodes.NOT_FOUND, response.getStatusLine().getStatusCode());
                    EntityUtils.toString(response.getEntity());
                }
                replacement.queueAddClass(AddResource.class);
                replacement.doReplacement();
                try (CloseableHttpResponse response = client.execute(get)) {
                    Assert.assertEquals(StatusCodes.OK, response.getStatusLine().getStatusCode());
                    String test = EntityUtils.toString(response.getEntity());
                    Assert.assertNotEquals("added", test);
                }

            }
        }
    }
}