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
package org.jboss.as.test.integration.microprofile.opentracing;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.microprofile.opentracing.application.TracerIdentityApplication;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URL;

import static org.wildfly.test.integration.microprofile.config.smallrye.HttpUtils.getContent;

/**
 * Test verifying the assumption that different services inside single EAR have different tracers.
 *
 * @author <a href="mailto:mjurc@redhat.com">Michal Jurc</a> (c) 2018 Red Hat, Inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class EarOpenTracingTestCase {

    @ContainerResource
    ManagementClient managementClient;

    @ArquillianResource
    private URL url;

    @Deployment
    public static Archive<?> deploy() {
        WebArchive serviceOne = ShrinkWrap.create(WebArchive.class, "ServiceOne.war")
                .addClass(TracerIdentityApplication.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        WebArchive serviceTwo = ShrinkWrap.create(WebArchive.class, "ServiceTwo.war")
                .addClass(TracerIdentityApplication.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "EarOpenTracingTestCase.ear")
                .addAsModules(serviceOne, serviceTwo);
        return ear;
    }

    @Test
    public void testEarServicesUseDifferentTracers() throws Exception {
        testHttpInvokation();
    }

    @Test
    public void testEarServicesUseDifferentTracersAfterReload() throws Exception {
        //TODO the tracer instance is same after reload as before it - check whether this is correct or no
        testHttpInvokation();
        ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
        testHttpInvokation();
    }

    private void testHttpInvokation() throws Exception {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpResponse svcOneResponse = client.execute(new HttpGet(url.toString() + "/ServiceOne/service-endpoint/app"));
            Assert.assertEquals(200, svcOneResponse.getStatusLine().getStatusCode());
            String serviceOneTracer = getContent(svcOneResponse);
            HttpResponse svcTwoResponse = client.execute(new HttpGet(url.toString() + "/ServiceTwo/service-endpoint/app"));
            Assert.assertEquals(200, svcTwoResponse.getStatusLine().getStatusCode());
            String serviceTwoTracer = getContent(svcTwoResponse);
            Assert.assertNotEquals("Service one and service two tracer instance hash is same - " + serviceTwoTracer,
                    serviceOneTracer, serviceTwoTracer);
        }
    }

}
