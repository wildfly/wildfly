/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

package org.wildfly.test.integration.microprofile.openapi;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.microprofile.openapi.service.TestApplication;
import org.wildfly.test.integration.microprofile.openapi.service.multimodule.TestEjb;
import org.wildfly.test.integration.microprofile.openapi.service.multimodule.TestRequest;
import org.wildfly.test.integration.microprofile.openapi.service.multimodule.TestResource;
import org.wildfly.test.integration.microprofile.openapi.service.multimodule.TestResponse;

/**
 * Validates OpenAPI endpoint for a multi-module deployment with classes shared of several libraries and accessible
 * submodules.
 *
 * @author Joachim Grimm
 */
@RunWith(Arquillian.class)
@RunAsClient
public class OpenAPIMultiModuleDeploymentIndexTestCase {

    private static final String PARENT_DEPLOYMENT_NAME =
            OpenAPIMultiModuleDeploymentIndexTestCase.class.getSimpleName() + ".ear";

    @Deployment
    public static Archive<?> deploy() throws Exception {
        WebArchive jaxrs = ShrinkWrap.create(WebArchive.class, "rest.war")
                                     .addAsResource(TestResource.class.getResource("beans.xml"), "WEB-INF/beans.xml")
                                     .addClasses(TestApplication.class, TestResource.class);
        JavaArchive core = ShrinkWrap.create(JavaArchive.class, "core.jar")
                                     .addClasses(TestEjb.class, TestRequest.class)
                                     .addAsResource(new StringAsset(
                                                     "<ejb-jar version=\"3.0\" "
                                                             + "metadata-complete=\"true\"></ejb-jar>"),
                                             "META-INF/ejb-jar.xml");
        JavaArchive common = ShrinkWrap.create(JavaArchive.class, "common.jar").addClass(TestResponse.class);
        return ShrinkWrap.create(EnterpriseArchive.class, PARENT_DEPLOYMENT_NAME)
                         .addAsModules(jaxrs, core)
                         .addAsManifestResource(
                                 TestEjb.class.getResource("application.xml"),
                                 "application.xml")
                         .addAsLibraries(common);
    }

    @Test
    public void test(@ArquillianResource URL baseURL) throws IOException, URISyntaxException {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (CloseableHttpResponse response = client.execute(new HttpGet(baseURL.toURI().resolve("/openapi")))) {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals("application/yaml", response.getEntity().getContentType().getValue());
                JsonNode node =
                        new ObjectMapper(new YAMLFactory()).reader().readTree(response.getEntity().getContent());
                JsonNode schemas = node.get("components").get("schemas");
                Assert.assertNotNull(schemas);
                Assert.assertNotNull(schemas.get("TestRequest"));
                Assert.assertNotNull(schemas.get("TestResponse"));
            }
        }
    }
}
