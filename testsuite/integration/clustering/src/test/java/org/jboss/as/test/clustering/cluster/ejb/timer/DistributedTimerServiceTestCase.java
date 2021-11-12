/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.test.clustering.cluster.ejb.timer;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.DateUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.clustering.cluster.ejb.timer.beans.ManualTimerBean;
import org.jboss.as.test.clustering.cluster.ejb.timer.beans.SingleActionTimerBean;
import org.jboss.as.test.clustering.cluster.ejb.timer.beans.TimerBean;
import org.jboss.as.test.clustering.cluster.ejb.timer.servlet.TimerServlet;
import org.jboss.as.test.clustering.ejb.EJBDirectory;
import org.jboss.as.test.http.util.TestHttpClientUtils;
import org.jboss.as.test.shared.CLIServerSetupTask;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Validates failover of distributed EJB timers.
 * @author Paul Ferraro
 */
@RunWith(Arquillian.class)
@ServerSetup(DistributedTimerServiceTestCase.ServerSetupTask.class)
public class DistributedTimerServiceTestCase extends AbstractClusteringTestCase {
    private static final String MODULE_NAME = DistributedTimerServiceTestCase.class.getSimpleName();

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(NODE_1)
    public static Archive<?> deployment0() {
        return createArchive();
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(NODE_2)
    public static Archive<?> deployment1() {
        return createArchive();
    }

    private static Archive<?> createArchive() {
        return ShrinkWrap.create(WebArchive.class, MODULE_NAME + ".war")
                .addPackage(TimerServlet.class.getPackage())
                .addPackage(EJBDirectory.class.getPackage())
                .addPackage(TimerBean.class.getPackage())
                ;
    }

    @Test
    public void test(@ArquillianResource(TimerServlet.class) @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1, @ArquillianResource(TimerServlet.class) @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2) throws IOException, URISyntaxException {

        Map<String, URI> uris = new TreeMap<>();
        uris.put(NODE_1, TimerServlet.createURI(baseURL1, MODULE_NAME));
        uris.put(NODE_2, TimerServlet.createURI(baseURL2, MODULE_NAME));

        try (CloseableHttpClient client = TestHttpClientUtils.promiscuousCookieHttpClient()) {

            TimeUnit.SECONDS.sleep(2);

            // Create manual timers
            try (CloseableHttpResponse response = client.execute(new HttpPut(uris.get(NODE_1)))) {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            }

            for (Map.Entry<String, URI> entry : uris.entrySet()) {
                try (CloseableHttpResponse response = client.execute(new HttpHead(entry.getValue()))) {
                    Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                    for (Class<? extends TimerBean> beanClass : TimerServlet.TIMER_CLASSES) {
                        int count = Integer.parseInt(response.getFirstHeader(beanClass.getName()).getValue());
                        Assert.assertEquals(entry.getKey() + ": " + beanClass.getName(), 1, count);
                    }
                }
            }

            TimeUnit.SECONDS.sleep(2);

            Map<Class<? extends TimerBean>, Map<String, List<Instant>>> timeouts = new IdentityHashMap<>();
            for (Class<? extends TimerBean> beanClass : TimerServlet.TIMER_CLASSES) {
                timeouts.put(beanClass, new TreeMap<>());
            }

            for (Map.Entry<String, URI> entry : uris.entrySet()) {
                try (CloseableHttpResponse response = client.execute(new HttpGet(entry.getValue()))) {
                    Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                    for (Map.Entry<Class<? extends TimerBean>, Map<String, List<Instant>>> beanEntry : timeouts.entrySet()) {
                        beanEntry.getValue().put(entry.getKey(), parseTimeouts(response.getHeaders(beanEntry.getKey().getName())));
                    }
                }
            }

            // Single action timer should have exactly 1 timeout on node 1
            Map<String, List<Instant>> singleActionTimeouts = timeouts.remove(SingleActionTimerBean.class);
            Assert.assertEquals(singleActionTimeouts.toString(), 1, singleActionTimeouts.get(NODE_1).size());
            Assert.assertEquals(singleActionTimeouts.toString(), 0, singleActionTimeouts.get(NODE_2).size());

            // Other timer timeouts should only have been received on node 1
            for (Map.Entry<Class<? extends TimerBean>, Map<String, List<Instant>>> beanEntry : timeouts.entrySet()) {
                if (ManualTimerBean.class.isAssignableFrom(beanEntry.getKey())) {
                    Assert.assertFalse(beanEntry.toString(), beanEntry.getValue().get(NODE_1).isEmpty());
                    Assert.assertTrue(beanEntry.toString(), beanEntry.getValue().get(NODE_2).isEmpty());
                } else {
                    // Auto-timers might have been rescheduled during startup
                    Assert.assertTrue(beanEntry.toString(), !beanEntry.getValue().get(NODE_1).isEmpty() || !beanEntry.getValue().get(NODE_2).isEmpty());
                }
            }

            for (Map.Entry<String, URI> entry : uris.entrySet()) {
                try (CloseableHttpResponse response = client.execute(new HttpHead(entry.getValue()))) {
                    Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                    for (Class<? extends TimerBean> beanClass : TimerServlet.TIMER_CLASSES) {
                        int count = Integer.parseInt(response.getFirstHeader(beanClass.getName()).getValue());
                        if (SingleActionTimerBean.class.equals(beanClass)) {
                            Assert.assertEquals(entry.getKey() + ": " + beanClass.getName(), 0, count);
                        } else {
                            Assert.assertEquals(entry.getKey() + ": " + beanClass.getName(), 1, count);
                        }
                    }
                }
            }

            TimeUnit.SECONDS.sleep(2);

            for (Map<String, List<Instant>> beanTimeouts : timeouts.values()) {
                beanTimeouts.clear();
            }
            timeouts.put(SingleActionTimerBean.class, new TreeMap<>());

            for (Map.Entry<String, URI> entry : uris.entrySet()) {
                try (CloseableHttpResponse response = client.execute(new HttpGet(entry.getValue()))) {
                    Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                    for (Map.Entry<Class<? extends TimerBean>, Map<String, List<Instant>>> beanEntry : timeouts.entrySet()) {
                        beanEntry.getValue().put(entry.getKey(), parseTimeouts(response.getHeaders(beanEntry.getKey().getName())));
                    }
                }
            }

            // Single action timer will already have expired
            singleActionTimeouts = timeouts.remove(SingleActionTimerBean.class);
            Assert.assertEquals(singleActionTimeouts.toString(), 0, singleActionTimeouts.get(NODE_1).size());
            Assert.assertEquals(singleActionTimeouts.toString(), 0, singleActionTimeouts.get(NODE_2).size());

            // Other timer timeouts should only have been received on one member or the other
            for (Map.Entry<Class<? extends TimerBean>, Map<String, List<Instant>>> beanEntry : timeouts.entrySet()) {
                if (ManualTimerBean.class.isAssignableFrom(beanEntry.getKey())) {
                    Assert.assertFalse(beanEntry.toString(), beanEntry.getValue().get(NODE_1).isEmpty());
                    Assert.assertTrue(beanEntry.toString(), beanEntry.getValue().get(NODE_2).isEmpty());
                } else {
                    // Auto-timers might have been rescheduled during startup
                    Assert.assertTrue(beanEntry.toString(), !beanEntry.getValue().get(NODE_1).isEmpty() || !beanEntry.getValue().get(NODE_2).isEmpty());
                }
            }

            this.stop(NODE_1);

            TimeUnit.SECONDS.sleep(2);

            try (CloseableHttpResponse response = client.execute(new HttpHead(uris.get(NODE_2)))) {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                for (Class<? extends TimerBean> beanClass : TimerServlet.TIMER_CLASSES) {
                    int count = Integer.parseInt(response.getFirstHeader(beanClass.getName()).getValue());
                    if (SingleActionTimerBean.class.equals(beanClass)) {
                        Assert.assertEquals(beanClass.getName(), 0, count);
                    } else {
                        Assert.assertEquals(beanClass.getName(), 1, count);
                    }
                }
            }

            for (Map<String, List<Instant>> beanTimeouts : timeouts.values()) {
                beanTimeouts.clear();
            }

            try (CloseableHttpResponse response = client.execute(new HttpGet(uris.get(NODE_2)))) {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                for (Map.Entry<Class<? extends TimerBean>, Map<String, List<Instant>>> beanEntry : timeouts.entrySet()) {
                    beanEntry.getValue().put(NODE_2, parseTimeouts(response.getHeaders(beanEntry.getKey().getName())));
                }
            }

            for (Map.Entry<Class<? extends TimerBean>, Map<String, List<Instant>>> entry : timeouts.entrySet()) {
                Assert.assertNotEquals(entry.toString(), 0, entry.getValue().get(NODE_2).size());
            }

            this.start(NODE_1);

            TimeUnit.SECONDS.sleep(2);

            for (Map<String, List<Instant>> beanTimeouts : timeouts.values()) {
                beanTimeouts.clear();
            }
            for (Map.Entry<String, URI> entry : uris.entrySet()) {
                try (CloseableHttpResponse response = client.execute(new HttpGet(entry.getValue()))) {
                    Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                    for (Map.Entry<Class<? extends TimerBean>, Map<String, List<Instant>>> beanEntry : timeouts.entrySet()) {
                        beanEntry.getValue().put(entry.getKey(), parseTimeouts(response.getHeaders(beanEntry.getKey().getName())));
                    }
                }
            }

            // Verify that at least 1 timeout was received on either member
            for (Map.Entry<Class<? extends TimerBean>, Map<String, List<Instant>>> entry : timeouts.entrySet()) {
                Assert.assertFalse(entry.toString(), entry.getValue().get(NODE_1).isEmpty() && entry.getValue().get(NODE_2).isEmpty());
            }

            TimeUnit.SECONDS.sleep(2);

            for (Map<String, List<Instant>> beanTimeouts : timeouts.values()) {
                beanTimeouts.clear();
            }
            for (Map.Entry<String, URI> entry : uris.entrySet()) {
                try (CloseableHttpResponse response = client.execute(new HttpGet(entry.getValue()))) {
                    Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                    for (Map.Entry<Class<? extends TimerBean>, Map<String, List<Instant>>> beanEntry : timeouts.entrySet()) {
                        beanEntry.getValue().put(entry.getKey(), parseTimeouts(response.getHeaders(beanEntry.getKey().getName())));
                    }
                }
            }

            // Verify that at least 1 timeout was received on a single member
            for (Map.Entry<Class<? extends TimerBean>, Map<String, List<Instant>>> entry : timeouts.entrySet()) {
                Assert.assertTrue(entry.toString(), entry.getValue().get(NODE_1).isEmpty() ^ entry.getValue().get(NODE_2).isEmpty());
            }

            this.stop(NODE_2);

            TimeUnit.SECONDS.sleep(2);

            for (Map<String, List<Instant>> beanTimeouts : timeouts.values()) {
                beanTimeouts.clear();
            }

            try (CloseableHttpResponse response = client.execute(new HttpGet(uris.get(NODE_1)))) {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                for (Map.Entry<Class<? extends TimerBean>, Map<String, List<Instant>>> beanEntry : timeouts.entrySet()) {
                    beanEntry.getValue().put(NODE_1, parseTimeouts(response.getHeaders(beanEntry.getKey().getName())));
                }
            }

            for (Map.Entry<Class<? extends TimerBean>, Map<String, List<Instant>>> entry : timeouts.entrySet()) {
                Assert.assertNotEquals(entry.toString(), 0, entry.getValue().get(NODE_1).size());
            }

            this.start(NODE_2);

            TimeUnit.SECONDS.sleep(2);

            for (Map.Entry<String, URI> entry : uris.entrySet()) {
                try (CloseableHttpResponse response = client.execute(new HttpDelete(entry.getValue()))) {
                    Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                }
            }

            Instant cancellation = Instant.now();

            TimeUnit.SECONDS.sleep(2);

            for (Map<String, List<Instant>> beanTimeouts : timeouts.values()) {
                beanTimeouts.clear();
            }

            for (Map.Entry<String, URI> entry : uris.entrySet()) {
                try (CloseableHttpResponse response = client.execute(new HttpGet(entry.getValue()))) {
                    Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                    for (Map.Entry<Class<? extends TimerBean>, Map<String, List<Instant>>> beanEntry : timeouts.entrySet()) {
                        beanEntry.getValue().put(entry.getKey(), parseTimeouts(response.getHeaders(beanEntry.getKey().getName())));
                    }
                }
            }

            // Ensure all timeouts were received before cancellation was initiated.
            for (Map.Entry<Class<? extends TimerBean>, Map<String, List<Instant>>> entry : timeouts.entrySet()) {
                for (String node : uris.keySet()) {
                    if (ManualTimerBean.class.isAssignableFrom(entry.getKey())) {
                        Assert.assertTrue(cancellation + " " + entry.toString(), entry.getValue().get(node).stream().allMatch(instant -> instant.isBefore(cancellation)));
                    } else {
                        Assert.assertTrue(entry.toString(), !entry.getValue().get(NODE_1).isEmpty() || !entry.getValue().get(NODE_2).isEmpty());
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static List<Instant> parseTimeouts(Header[] headers) {
        List<Instant> timeouts = new ArrayList<>(headers.length);
        for (Header header : headers) {
            timeouts.add(DateUtils.parseDate(header.getValue()).toInstant());
        }
        return timeouts;
    }

    static class ServerSetupTask extends CLIServerSetupTask {
        ServerSetupTask() {
            this.builder.node(TWO_NODES)
                    // TODO: Replace with distributable-ejb management operations, once available
                    .setup("/system-property=jboss.ejb.timer-service.distributed.enabled:add(value=true)")
                    .reloadOnSetup(false)
                    .teardown("/system-property=jboss.ejb.timer-service.distributed.enabled:remove")
                    ;
        }

        @Override
        public void setup(ManagementClient client, String containerId) throws Exception {
            super.setup(client, containerId);
            // system-property=*:add does not put server in reload required state, so let's force a reload
            ServerReload.executeReloadAndWaitForCompletion(client);
        }
    }
}
