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

package org.wildfly.test.integration.microprofile.health;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2018 Red Hat inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
public abstract class MicroProfileHealthTestBase {

    abstract void checkGlobalOutcome(ManagementClient managementClient, boolean mustBeUP, String probeName) throws IOException;

    @Deployment(name = "MicroProfileHealthTestCase", managed = false)
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "MicroProfileHealthTestCase.war")
                .addClasses(TestApplication.class, TestApplication.Resource.class, MyProbe.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        return war;
    }

    @ContainerResource
    ManagementClient managementClient;

    @ArquillianResource
    private Deployer deployer;

    @Test
    @InSequence(1)
    public void testHealthCheckBeforeDeployment() throws Exception {
        checkGlobalOutcome(managementClient, true, null);

        // deploy the archive
        deployer.deploy("MicroProfileHealthTestCase");
    }

    @Test
    @InSequence(2)
    @OperateOnDeployment("MicroProfileHealthTestCase")
    public void testHealthCheckAfterDeployment(@ArquillianResource URL url) throws Exception {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {

            checkGlobalOutcome(managementClient, true, "myProbe");

            HttpPost request = new HttpPost(url + "microprofile/myApp");
            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            nvps.add(new BasicNameValuePair("up", "false"));
            request.setEntity(new UrlEncodedFormEntity(nvps));

            CloseableHttpResponse response = client.execute(request);
            assertEquals(200, response.getStatusLine().getStatusCode());

            checkGlobalOutcome(managementClient, false, "myProbe");
        }
    }

    @Test
    @InSequence(3)
    public void testHealthCheckAfterUndeployment() throws Exception {

        deployer.undeploy("MicroProfileHealthTestCase");

        checkGlobalOutcome(managementClient, true, null);
    }


}
