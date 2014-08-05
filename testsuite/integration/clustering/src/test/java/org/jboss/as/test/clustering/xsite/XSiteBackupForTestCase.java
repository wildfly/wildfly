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
package org.jboss.as.test.clustering.xsite;

import static org.jboss.as.test.clustering.ClusterTestUtil.waitForReplication;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.clustering.cluster.ExtendedClusterAbstractTestCase;
import org.jboss.as.test.http.util.HttpClientUtils;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test xsite functionality on a 4-node, 3-site test deployment:
 *
 * sites:
 *   LON: {LON-0, LON-1}  // maps to CONTAINER_1/CONTAINER_2 (see arquillian.xml)
 *   NYC: {NYC-0}         // maps to CONTAINER_3 (see arquillian.xml)
 *   SFO: {SFO-0}         // maps to CONTAINER_4 (see arquillian.xml)
 *
 * routes:
 *   LON -> NYC,SFO
 *   NYC -> LON
 *   SFO -> LON
 *
 * backups: (<site>:<container>:<cache>)
 *   LON:web:repl backed up by NYC:web:repl, SFO:web:LONrepl
 *   NYC not backed up
 *   SFO not backed up
 *
 *
 * @author Richard Achmatowicz
 */
@RunWith(Arquillian.class)
@RunAsClient
public class XSiteBackupForTestCase extends ExtendedClusterAbstractTestCase {

    @Deployment(name = DEPLOYMENT_1, managed = false)
    @TargetsContainer(CONTAINER_1)
    public static Archive<?> deployment0() {
        return getDeployment();
    }

    @Deployment(name = DEPLOYMENT_2, managed = false)
    @TargetsContainer(CONTAINER_2)
    public static Archive<?> deployment1() {
        return getDeployment();
    }

    @Deployment(name = DEPLOYMENT_3, managed = false)
    @TargetsContainer(CONTAINER_3)
    public static Archive<?> deployment2() {
        return getDeployment();
    }

    @Deployment(name = DEPLOYMENT_4, managed = false)
    @TargetsContainer(CONTAINER_4)
    public static Archive<?> deployment3() {
        return getBackupForDeployment();
    }

    // a deployment which reads from the cache jboss:/infinispan/web/repl
    private static Archive<?> getDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "xsite.war");
        war.addClass(CacheAccessServlet.class);
        war.setWebXML(XSiteBackupForTestCase.class.getPackage(), "web.xml");
        war.setManifest(new StringAsset("Manifest-Version: 1.0\nDependencies: org.infinispan\n"));
        log.info(war.toString(true));
        return war;
    }

    // a deployment which reads from cache jboss:/infinispan/web/LONrepl
    private static Archive<?> getBackupForDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "xsite.war");
        war.addClass(CacheAccessServlet.class);
        war.setWebXML(XSiteBackupForTestCase.class.getPackage(), "web-backupfor.xml");
        war.setManifest(new StringAsset("Manifest-Version: 1.0\nDependencies: org.infinispan\n"));
        log.info(war.toString(true));
        return war;
    }

    /*
     * Tests that puts get relayed to their backup sites, even when using backup-for to rename
     * the backup cache name.
     *
     * Put the key-value (a,100) on LON-0 on site LON and check that the key-value pair:
     *   arrives at LON-1 on site LON
     *   arrives at NYC-0 on site NYC
     *   arrives at SFO-0 on site SFO
     */
    @Test
    public void testPutRelayedToBackups(
            @ArquillianResource(CacheAccessServlet.class) @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1,
            @ArquillianResource(CacheAccessServlet.class) @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2,
            @ArquillianResource(CacheAccessServlet.class) @OperateOnDeployment(DEPLOYMENT_3) URL baseURL3,
            @ArquillianResource(CacheAccessServlet.class) @OperateOnDeployment(DEPLOYMENT_4) URL baseURL4)

            throws IllegalStateException, IOException, URISyntaxException {

        DefaultHttpClient client = HttpClientUtils.relaxedCookieHttpClient();

        URI url1 = CacheAccessServlet.createPutURI(baseURL1, "a", "100");
        URI url2 = CacheAccessServlet.createGetURI(baseURL2, "a");
        URI url3 = CacheAccessServlet.createGetURI(baseURL3, "a");
        URI url4 = CacheAccessServlet.createGetURI(baseURL4, "a");

        try {
            // put a value to LON-0
            System.out.println("Executing HTTP request: " + url1);
            HttpResponse response = client.execute(new HttpGet(url1));
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            response.getEntity().getContent().close();
            System.out.println("Executed HTTP request");

            // Lets wait for the session to replicate
            waitForReplication(GRACE_TIME_TO_REPLICATE);

            // do a get on LON-1
            System.out.println("Executing HTTP request: " + url2);
            response = client.execute(new HttpGet(url2));
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            Assert.assertEquals(100, Integer.parseInt(response.getFirstHeader("value").getValue()));
            response.getEntity().getContent().close();
            System.out.println("Executed HTTP request");

            // do a get on NYC-0
            System.out.println("Executing HTTP request: " + url3);
            response = client.execute(new HttpGet(url3));
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            Assert.assertEquals(100, Integer.parseInt(response.getFirstHeader("value").getValue()));
            response.getEntity().getContent().close();
            System.out.println("Executed HTTP request");

            // do a get on SFO-0
            System.out.println("Executing HTTP request: " + url4);
            response = client.execute(new HttpGet(url4));
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            Assert.assertEquals(100, Integer.parseInt(response.getFirstHeader("value").getValue()));
            response.getEntity().getContent().close();
            System.out.println("Executed HTTP request");
        } finally {
            client.getConnectionManager().shutdown();
        }
    }

}
