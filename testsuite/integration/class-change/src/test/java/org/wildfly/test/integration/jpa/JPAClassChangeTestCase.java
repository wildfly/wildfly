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

package org.wildfly.test.integration.jpa;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
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
public class JPAClassChangeTestCase {

    private static final String JPA = "/jpa/";

    @Deployment
    public static WebArchive deploy() {
        return ShrinkWrap.create(WebArchive.class, "jpa.war")
                .addClasses(Employee.class, JpaResource.class, Activator.class)
                .addAsResource(JPAClassChangeTestCase.class.getPackage(), "persistence.xml", "META-INF/persistence.xml")
                .addAsWebInfResource(new StringAsset("remote.password=test"), "class-change.properties")
                .addAsWebInfResource(new StringAsset(""), "beans.xml");

    }


    @Test
    public void testJpaResourceReplacement() throws Exception {

        String deploymentUrl = "http://" + TestSuiteEnvironment.getHttpAddress() + ":" + TestSuiteEnvironment.getHttpPort() + JPA + "rest/jpa";
        try (SimpleRemoteReplacement replacement = new SimpleRemoteReplacement(JPA, JPAClassChangeTestCase.class, Employee.class, JpaResource.class, Activator.class)) {

            try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
                try (CloseableHttpResponse response = client.execute(new HttpPost(deploymentUrl))) {
                    Assert.assertEquals(StatusCodes.NO_CONTENT, response.getStatusLine().getStatusCode());
                }
                try (CloseableHttpResponse response = client.execute(new HttpGet(deploymentUrl))) {
                    Assert.assertEquals(StatusCodes.OK, response.getStatusLine().getStatusCode());
                    Assert.assertEquals("Name1", EntityUtils.toString(response.getEntity()));
                }

                replacement.queueClassReplacement(Employee.class, Employee1.class);
                replacement.queueClassReplacement(JpaResource.class, JpaResource1.class);
                replacement.doReplacement();

                try (CloseableHttpResponse response = client.execute(new HttpPost(deploymentUrl))) {
                    Assert.assertEquals(StatusCodes.NO_CONTENT, response.getStatusLine().getStatusCode());
                }
                try (CloseableHttpResponse response = client.execute(new HttpGet(deploymentUrl))) {
                    Assert.assertEquals(StatusCodes.OK, response.getStatusLine().getStatusCode());
                    Assert.assertEquals("Name1:Address1", EntityUtils.toString(response.getEntity()));
                }
            }
        }
    }


}