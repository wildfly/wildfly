/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

package org.wildfly.test.integration.microprofile.health;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.eclipse.microprofile.health.HealthCheckResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.wildfly.test.integration.microprofile.health.deployment.LivenessHealthCheck;

/**
 * Validates HealthCheckResponse concrete class which was introduced in MicroProfile Health v. 2.2
 * @author Fabio Burzigotti
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ConcreteHealthCheckResponseTestCase {
    private static final String DEPLOYMENT_NAME = ConcreteHealthCheckResponseTestCase.class.getSimpleName() + ".war";

    @ContainerResource
    private ManagementClient managementClient;

    @Deployment
    public static Archive<?> deploy() {
        return ShrinkWrap.create(WebArchive.class, DEPLOYMENT_NAME)
                .addClass(LivenessHealthCheck.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                ;
    }

    @Test
    @Ignore("https://github.com/eclipse/microprofile-health/issues/243")
    public void test(@ArquillianResource URL baseURL) throws IOException, URISyntaxException {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpUriRequest request = new HttpGet(
                    String.format("http://%s:%d/health", managementClient.getMgmtAddress(), managementClient.getMgmtPort()));

            try (CloseableHttpResponse response = client.execute(request)) {
                Assert.assertEquals(
                        "Health check endpoint call failed",
                        HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());

                final String responseContent = EntityUtils.toString(response.getEntity());
                HealthCheckResponse typedResponse = new ObjectMapper().readValue(
                        responseContent, HealthCheckResponse.class);

                final String healthCheckName = typedResponse.getName();
                Assert.assertEquals(
                        String.format("Typed response instance \"name\" property has unexpected value: %s", healthCheckName),
                        healthCheckName, LivenessHealthCheck.HEALTH_CHECK_NAME);
            }
        }
    }
}
