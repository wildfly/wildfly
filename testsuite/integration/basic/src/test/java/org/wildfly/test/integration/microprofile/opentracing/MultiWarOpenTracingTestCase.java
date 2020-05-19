/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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
package org.wildfly.test.integration.microprofile.opentracing;

import java.net.URL;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.wildfly.test.integration.microprofile.opentracing.application.TracerIdentityApplication;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test verifying the assumption that different services provided by multiple WARs have different tracers.
 *
 * @author <a href="mailto:mjurc@redhat.com">Michal Jurc</a> (c) 2018 Red Hat, Inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class MultiWarOpenTracingTestCase {

    @ContainerResource
    ManagementClient managementClient;

    @ArquillianResource
    @OperateOnDeployment("ServiceOne.war")
    private URL serviceOneUrl;

    @ArquillianResource
    @OperateOnDeployment("ServiceTwo.war")
    private URL serviceTwoUrl;

    @Deployment(name = "ServiceOne.war")
    public static Archive<?> deployServiceOne() {
        WebArchive serviceOne = ShrinkWrap.create(WebArchive.class, "ServiceOne.war")
                .addClass(TracerIdentityApplication.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        return serviceOne;
    }

    @Deployment(name = "ServiceTwo.war")
    public static Archive<?> deployServiceTwo() {
        WebArchive serviceTwo = ShrinkWrap.create(WebArchive.class, "ServiceTwo.war")
                .addClass(TracerIdentityApplication.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        return serviceTwo;
    }

    @Test
    public void testMultipleWarServicesUseDifferentTracers() throws Exception {
        testHttpInvokation();
    }

    @Test
    public void testMultipleWarServicesUseDifferentTracersAfterReload() throws Exception {
        testHttpInvokation();
        ServerReload.executeReloadAndWaitForCompletion(managementClient);
        testHttpInvokation();
    }

    private void testHttpInvokation() throws Exception {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpResponse svcOneResponse = client.execute(new HttpGet(serviceOneUrl.toString() + "service-endpoint/app"));
            Assert.assertEquals(200, svcOneResponse.getStatusLine().getStatusCode());
            String serviceOneTracer = EntityUtils.toString(svcOneResponse.getEntity());
            HttpResponse svcTwoResponse = client.execute(new HttpGet(serviceTwoUrl.toString() + "service-endpoint/app"));
            Assert.assertEquals(200, svcTwoResponse.getStatusLine().getStatusCode());
            String serviceTwoTracer = EntityUtils.toString(svcTwoResponse.getEntity());
            Assert.assertNotEquals("Service one and service two tracer instance hash is same - " + serviceTwoTracer,
                    serviceOneTracer, serviceTwoTracer);
        }
    }
}
