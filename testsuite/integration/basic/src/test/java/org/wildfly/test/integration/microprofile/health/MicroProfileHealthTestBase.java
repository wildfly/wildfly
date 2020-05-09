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
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.arquillian.container.test.api.*;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2018 Red Hat inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
public abstract class MicroProfileHealthTestBase {

    public static final String JAR_ARCHIVE_NAME = "MicroProfileHealthTestCaseJar.jar";
    public static final String WAR_ARCHIVE_NAME = "MicroProfileHealthTestCase.war";
    public static final String EAR_ARCHIVE_NAME = "MicroProfileHealthTestCase.ear";

    abstract void checkGlobalOutcome(ManagementClient managementClient, String operation, boolean mustBeUP, String probeName) throws IOException;

    @Deployment(name = EAR_ARCHIVE_NAME, managed = false)
    public static Archive<?> deployEar() {
        JavaArchive jarArchive = ShrinkWrap.create(JavaArchive.class, JAR_ARCHIVE_NAME)
                .addClass(HealthConfigSource.class)
                .addAsServiceProvider(ConfigSource.class, HealthConfigSource.class)
                .addAsResource(EmptyAsset.INSTANCE, "beans.xml");


        WebArchive war = ShrinkWrap.create(WebArchive.class, WAR_ARCHIVE_NAME)
                .addClasses(TestApplication.class, TestApplication.Resource.class,
                        MyProbe.class, MyLiveProbe.class, HealthSubDeploymentConfigSource.class)
                .addAsServiceProvider(ConfigSource.class, HealthSubDeploymentConfigSource.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        WebArchive testableArchive = Testable.archiveToTest(war);

        EnterpriseArchive ear = ShrinkWrap
                .create(EnterpriseArchive.class, EAR_ARCHIVE_NAME)
                .addAsModule(testableArchive)
                .addAsLibraries(jarArchive)
                .addAsResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsResource(MicroProfileHealthTestBase.class.getPackage(), "application.xml", "application.xml")
                .addManifest();

        System.out.println(ear.toString(true));
        return ear;
    }

    @ContainerResource
    ManagementClient managementClient;

    @ArquillianResource
    private Deployer deployer;

    @Test
    @InSequence(1)
    public void testHealthCheckBeforeDeployment() throws Exception {
        checkGlobalOutcome(managementClient, "check", true, null);
        checkGlobalOutcome(managementClient, "check-live", true, null);

        // deploy the archive
        deployer.deploy(EAR_ARCHIVE_NAME);
    }

    @Test
    @InSequence(2)
    @OperateOnDeployment(EAR_ARCHIVE_NAME)
    public void testHealthCheckAfterDeployment(@ArquillianResource URL url) throws Exception {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {

            probeHealth(url, client);
        }
    }

    @Test
    @InSequence(3)
    public void testHealthCheckBetweenDeployments() throws Exception {

        deployer.undeploy(EAR_ARCHIVE_NAME);

        checkGlobalOutcome(managementClient, "check", true, null);
        checkGlobalOutcome(managementClient, "check-live", true, null);

    }

    private void probeHealth(URL url, CloseableHttpClient client) throws IOException {
        checkGlobalOutcome(managementClient, "check", true, "myProbe");
        checkGlobalOutcome(managementClient, "check-live", true, "myLiveProbe");

        HttpPost request = new HttpPost(url + "/microprofile/myApp");
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("up", "false"));
        request.setEntity(new UrlEncodedFormEntity(nvps));

        CloseableHttpResponse response = client.execute(request);
        assertEquals(200, response.getStatusLine().getStatusCode());

        checkGlobalOutcome(managementClient, "check", false, "myProbe");
        checkGlobalOutcome(managementClient, "check-live", false, "myLiveProbe");
    }

}
