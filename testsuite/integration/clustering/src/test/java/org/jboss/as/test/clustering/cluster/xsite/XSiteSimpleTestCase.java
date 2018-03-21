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
package org.jboss.as.test.clustering.cluster.xsite;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.clustering.CLIServerSetupTask;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.http.util.TestHttpClientUtils;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jgroups.util.StackType;
import org.jgroups.util.Util;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test xsite functionality on a 4-node, 3-site test deployment:
 *
 * sites:
 *   LON: {LON-0, LON-1}  // maps to NODE_1/NODE_2
 *   NYC: {NYC-0}         // maps to NODE_3
 *   SFO: {SFO-0}         // maps to NODE_4
 *
 * routes:
 *   LON -> NYC,SFO
 *   NYC -> LON
 *   SFO -> LON
 *
 * backups: (<site>:<container>:<cache>)
 *   LON:web:dist backed up by NYC:web:dist, SFO:web:dist
 *   NYC not backed up
 *   SFO not backed up
 *
 * @author Richard Achmatowicz
 */
@RunWith(Arquillian.class)
@ServerSetup({XSiteSimpleTestCase.ServerSetupTask.class})
public class XSiteSimpleTestCase extends AbstractClusteringTestCase {

    private static final String MODULE_NAME = XSiteSimpleTestCase.class.getSimpleName();

    public XSiteSimpleTestCase() {
        super(FOUR_NODES, FOUR_DEPLOYMENTS);
    }

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(NODE_1)
    public static Archive<?> deployment1() {
        return getDeployment();
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(NODE_2)
    public static Archive<?> deployment2() {
        return getDeployment();
    }

    @Deployment(name = DEPLOYMENT_3, managed = false, testable = false)
    @TargetsContainer(NODE_3)
    public static Archive<?> deployment3() {
        return getDeployment();
    }

    @Deployment(name = DEPLOYMENT_4, managed = false, testable = false)
    @TargetsContainer(NODE_4)
    public static Archive<?> deployment4() {
        return getDeployment();
    }

    private static Archive<?> getDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, MODULE_NAME + ".war");
        war.addClass(CacheAccessServlet.class);
        war.setWebXML(XSiteSimpleTestCase.class.getPackage(), "web.xml");
        war.setManifest(new StringAsset("Manifest-Version: 1.0\nDependencies: org.infinispan\n"));
        return war;
    }

    @BeforeClass
    public static void beforeClass() {
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            Assume.assumeFalse("Disable on Windows+IPv6 until CI environment is fixed", Util.checkForWindows() && (Util.getIpStackType() == StackType.IPv6));
            return null;
        });
    }

    @Test
    public void test(@ArquillianResource(CacheAccessServlet.class) @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1,
                     @ArquillianResource(CacheAccessServlet.class) @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2,
                     @ArquillianResource(CacheAccessServlet.class) @OperateOnDeployment(DEPLOYMENT_3) URL baseURL3,
                     @ArquillianResource(CacheAccessServlet.class) @OperateOnDeployment(DEPLOYMENT_4) URL baseURL4
    ) throws IllegalStateException, IOException, URISyntaxException, InterruptedException {
        /*
         * Tests that puts get relayed to their backup sites
         *
         * Put the key-value (a,100) on LON-0 on site LON and check that the key-value pair:
         *   arrives at LON-1 on site LON
         *   arrives at NYC-0 on site NYC
         *   arrives at SFO-0 on site SFO
         */

        String value = "100";
        URI url1 = CacheAccessServlet.createPutURI(baseURL1, "a", value);
        URI url2 = CacheAccessServlet.createGetURI(baseURL2, "a");
        URI url3 = CacheAccessServlet.createGetURI(baseURL3, "a");
        URI url4 = CacheAccessServlet.createGetURI(baseURL4, "a");

        try (CloseableHttpClient client = TestHttpClientUtils.promiscuousCookieHttpClient()) {
            // put a value to LON-0
            HttpResponse response = client.execute(new HttpGet(url1));
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            response.getEntity().getContent().close();

            // Lets wait for the session to replicate
            Thread.sleep(GRACE_TIME_TO_REPLICATE);

            // do a get on LON-1
            response = client.execute(new HttpGet(url2));
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            Assert.assertEquals(value, response.getFirstHeader("value").getValue());
            response.getEntity().getContent().close();

            // do a get on NYC-0
            response = client.execute(new HttpGet(url3));
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            Assert.assertEquals(value, response.getFirstHeader("value").getValue());
            response.getEntity().getContent().close();

            // do a get on SFO-0
            response = client.execute(new HttpGet(url4));
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            Assert.assertEquals(value, response.getFirstHeader("value").getValue());
            response.getEntity().getContent().close();
        }

        /*
         * Tests that puts at the backup caches do not get relayed back to the origin cache.
         *
         * Put the key-value (b,200) on NYC-0 on site NYC and check that the key-value pair:
         *   does not arrive at LON-0 on site LON
         */

        url1 = CacheAccessServlet.createGetURI(baseURL1, "b");
        url3 = CacheAccessServlet.createPutURI(baseURL3, "b", "200");

        try (CloseableHttpClient client = TestHttpClientUtils.promiscuousCookieHttpClient()) {
            // put a value to NYC-0
            HttpResponse response = client.execute(new HttpGet(url3));
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            response.getEntity().getContent().close();

            // Lets wait for the session to replicate
            Thread.sleep(GRACE_TIME_TO_REPLICATE);

            // do a get on LON-1 - this should fail
            response = client.execute(new HttpGet(url1));
            Assert.assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, response.getStatusLine().getStatusCode());
            response.getEntity().getContent().close();
        }
    }

    public static class ServerSetupTask extends CLIServerSetupTask {
        public ServerSetupTask() {
            this.builder
                    // LON
                    .node(NODE_1, NODE_2)
                    .setup("/subsystem=infinispan/cache-container=web/distributed-cache=dist/component=backups/backup=NYC:add(failure-policy=WARN,strategy=SYNC,timeout=10000,enabled=true)")
                    .setup("/subsystem=infinispan/cache-container=web/distributed-cache=dist/component=backups/backup=SFO:add(failure-policy=WARN,strategy=SYNC,timeout=10000,enabled=true)")
                    .setup("/subsystem=jgroups/channel=bridge:add(stack=tcp)")
                    .setup("/subsystem=jgroups/channel=ee:write-attribute(name=stack,value=udp)")
                    .setup("/subsystem=jgroups/stack=udp/relay=RELAY:add(site=LON)")
                    .setup("/subsystem=jgroups/stack=udp/relay=RELAY/remote-site=NYC:add(channel=bridge)")
                    .setup("/subsystem=jgroups/stack=udp/relay=RELAY/remote-site=SFO:add(channel=bridge)")
                    .setup("/socket-binding-group=standard-sockets/socket-binding=jgroups-mping:write-attribute(name=multicast-address,value=%s)", TESTSUITE_MCAST3)
                    .teardown("/socket-binding-group=standard-sockets/socket-binding=jgroups-mping:write-attribute(name=multicast-address,value=\"${jboss.default.multicast.address:230.0.0.4}\"")
                    .teardown("/subsystem=jgroups/stack=udp/relay=RELAY:remove")
                    .teardown("/subsystem=jgroups/channel=ee:write-attribute(name=stack,value=tcp)")
                    .teardown("/subsystem=jgroups/channel=bridge:remove")
                    .teardown("/subsystem=infinispan/cache-container=web/distributed-cache=dist/component=backups/backup=SFO:remove")
                    .teardown("/subsystem=infinispan/cache-container=web/distributed-cache=dist/component=backups/backup=NYC:remove")
                    .parent()
                    // NYC
                    .node(NODE_3)
                    .setup("/subsystem=jgroups/channel=bridge:add(stack=tcp)")
                    .setup("/subsystem=jgroups/channel=ee:write-attribute(name=stack,value=udp)")
                    .setup("/subsystem=jgroups/stack=udp/relay=RELAY:add(site=NYC)")
                    .setup("/subsystem=jgroups/stack=udp/relay=RELAY/remote-site=LON:add(channel=bridge)")
                    .setup("/subsystem=jgroups/stack=udp/relay=RELAY/remote-site=SFO:add(channel=bridge)")
                    .setup("/socket-binding-group=standard-sockets/socket-binding=jgroups-udp:write-attribute(name=multicast-address,value=%s)", TESTSUITE_MCAST1)
                    .setup("/socket-binding-group=standard-sockets/socket-binding=jgroups-mping:write-attribute(name=multicast-address,value=%s)", TESTSUITE_MCAST3)
                    .teardown("/socket-binding-group=standard-sockets/socket-binding=jgroups-mping:write-attribute(name=multicast-address,value=\"${jboss.default.multicast.address:230.0.0.4}\"")
                    .teardown("/socket-binding-group=standard-sockets/socket-binding=jgroups-udp:write-attribute(name=multicast-address,value=\"${jboss.default.multicast.address:230.0.0.4}\"")
                    .teardown("/subsystem=jgroups/stack=udp/relay=RELAY:remove")
                    .teardown("/subsystem=jgroups/channel=ee:write-attribute(name=stack,value=tcp)")
                    .teardown("/subsystem=jgroups/channel=bridge:remove")
                    .parent()
                    // SFO
                    .node(NODE_4)
                    .setup("/subsystem=jgroups/channel=bridge:add(stack=tcp)")
                    .setup("/subsystem=jgroups/channel=ee:write-attribute(name=stack,value=udp)")
                    .setup("/subsystem=jgroups/stack=udp/relay=RELAY:add(site=SFO)")
                    .setup("/subsystem=jgroups/stack=udp/relay=RELAY/remote-site=LON:add(channel=bridge)")
                    .setup("/subsystem=jgroups/stack=udp/relay=RELAY/remote-site=NYC:add(channel=bridge)")
                    .setup("/socket-binding-group=standard-sockets/socket-binding=jgroups-udp:write-attribute(name=multicast-address,value=%s)", TESTSUITE_MCAST2)
                    .setup("/socket-binding-group=standard-sockets/socket-binding=jgroups-mping:write-attribute(name=multicast-address,value=%s)", TESTSUITE_MCAST3)
                    .teardown("/socket-binding-group=standard-sockets/socket-binding=jgroups-mping:write-attribute(name=multicast-address,value=\"${jboss.default.multicast.address:230.0.0.4}\"")
                    .teardown("/socket-binding-group=standard-sockets/socket-binding=jgroups-udp:write-attribute(name=multicast-address,value=\"${jboss.default.multicast.address:230.0.0.4}\"")
                    .teardown("/subsystem=jgroups/stack=udp/relay=RELAY:remove")
                    .teardown("/subsystem=jgroups/channel=ee:write-attribute(name=stack,value=tcp)")
                    .teardown("/subsystem=jgroups/channel=bridge:remove")
            ;
        }
    }
}