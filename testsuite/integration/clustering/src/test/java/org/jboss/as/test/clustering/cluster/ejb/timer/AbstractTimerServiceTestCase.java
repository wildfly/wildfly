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

import jakarta.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.DateUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.clustering.cluster.ejb.timer.beans.AutoTimerBean;
import org.jboss.as.test.clustering.cluster.ejb.timer.beans.AutoTransientTimerBean;
import org.jboss.as.test.clustering.cluster.ejb.timer.beans.ManualTimerBean;
import org.jboss.as.test.clustering.cluster.ejb.timer.beans.SingleActionPersistentTimerBean;
import org.jboss.as.test.clustering.cluster.ejb.timer.beans.SingleActionTransientTimerBean;
import org.jboss.as.test.clustering.cluster.ejb.timer.beans.TimerBean;
import org.jboss.as.test.clustering.cluster.ejb.timer.servlet.TimerServlet;
import org.jboss.as.test.clustering.ejb.EJBDirectory;
import org.jboss.as.test.http.util.TestHttpClientUtils;
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
public abstract class AbstractTimerServiceTestCase extends AbstractClusteringTestCase {

    protected static WebArchive createArchive(Class<? extends AbstractTimerServiceTestCase> testClass) {
        return ShrinkWrap.create(WebArchive.class, testClass.getSimpleName() + ".war")
                .addPackage(TimerServlet.class.getPackage())
                .addPackage(EJBDirectory.class.getPackage())
                .addPackage(TimerBean.class.getPackage())
                ;
    }

    private final String moduleName;

    protected AbstractTimerServiceTestCase() {
        this.moduleName = this.getClass().getSimpleName();
    }

    @Test
    public void test(@ArquillianResource(TimerServlet.class) @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1, @ArquillianResource(TimerServlet.class) @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2) throws IOException, URISyntaxException {

        Map<String, URI> uris = new TreeMap<>();
        uris.put(NODE_1, TimerServlet.createURI(baseURL1, this.moduleName));
        uris.put(NODE_2, TimerServlet.createURI(baseURL2, this.moduleName));

        List<Class<? extends TimerBean>> singleActionTimerBeanClasses = List.of(SingleActionPersistentTimerBean.class, SingleActionTransientTimerBean.class);

        try (CloseableHttpClient client = TestHttpClientUtils.promiscuousCookieHttpClient()) {

            TimeUnit.SECONDS.sleep(2);

            // Create manual timers on node 1 only
            try (CloseableHttpResponse response = client.execute(new HttpPut(uris.get(NODE_1)))) {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            }

            for (Map.Entry<String, URI> entry : uris.entrySet()) {
                try (CloseableHttpResponse response = client.execute(new HttpHead(entry.getValue()))) {
                    Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                    for (Class<? extends TimerBean> beanClass : TimerServlet.TIMER_CLASSES) {
                        int count = Integer.parseInt(response.getFirstHeader(beanClass.getName()).getValue());
                        if (TimerServlet.MANUAL_TRANSIENT_TIMER_CLASSES.contains(beanClass) && entry.getKey().equals(NODE_2)) {
                            Assert.assertEquals(entry.getKey() + ": " + beanClass.getName(), 0, count);
                        } else {
                            Assert.assertEquals(entry.getKey() + ": " + beanClass.getName(), 1, count);
                        }
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
            for (Class<? extends TimerBean> beanClass : singleActionTimerBeanClasses) {
                Map<String, List<Instant>> singleActionTimeouts = timeouts.remove(beanClass);
                Assert.assertEquals(singleActionTimeouts.toString(), 1, singleActionTimeouts.get(NODE_1).size());
                Assert.assertEquals(singleActionTimeouts.toString(), 0, singleActionTimeouts.get(NODE_2).size());
            }

            for (Map.Entry<Class<? extends TimerBean>, Map<String, List<Instant>>> beanEntry : timeouts.entrySet()) {
                if (ManualTimerBean.class.isAssignableFrom(beanEntry.getKey())) {
                    // Other timer timeouts should only have been received on node 1
                    Assert.assertFalse(beanEntry.toString(), beanEntry.getValue().get(NODE_1).isEmpty());
                    Assert.assertTrue(beanEntry.toString(), beanEntry.getValue().get(NODE_2).isEmpty());
                } else if (AutoTransientTimerBean.class.equals(beanEntry.getKey())) {
                    // Transient auto-timers will exist on both nodes
                    Assert.assertFalse(beanEntry.toString(), beanEntry.getValue().get(NODE_1).isEmpty());
                    Assert.assertFalse(beanEntry.toString(), beanEntry.getValue().get(NODE_2).isEmpty());
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
                        if (TimerServlet.SINGLE_ACTION_TIMER_CLASSES.contains(beanClass) || (TimerServlet.MANUAL_TRANSIENT_TIMER_CLASSES.contains(beanClass) && entry.getKey().equals(NODE_2))) {
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
            for (Class<? extends TimerBean> singleActionTimerBeanClass : singleActionTimerBeanClasses) {
                timeouts.put(singleActionTimerBeanClass, new TreeMap<>());
            }

            for (Map.Entry<String, URI> entry : uris.entrySet()) {
                try (CloseableHttpResponse response = client.execute(new HttpGet(entry.getValue()))) {
                    Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                    for (Map.Entry<Class<? extends TimerBean>, Map<String, List<Instant>>> beanEntry : timeouts.entrySet()) {
                        beanEntry.getValue().put(entry.getKey(), parseTimeouts(response.getHeaders(beanEntry.getKey().getName())));
                    }
                }
            }

            // Single action timers will already have expired
            for (Class<? extends TimerBean> singleActionTimerBeanClass : singleActionTimerBeanClasses) {
                Map<String, List<Instant>> singleActionTimers = timeouts.remove(singleActionTimerBeanClass);
                Assert.assertEquals(singleActionTimers.toString(), 0, singleActionTimers.get(NODE_1).size());
                Assert.assertEquals(singleActionTimers.toString(), 0, singleActionTimers.get(NODE_2).size());
            }

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
                    if (TimerServlet.SINGLE_ACTION_TIMER_CLASSES.contains(beanClass) || TimerServlet.MANUAL_TRANSIENT_TIMER_CLASSES.contains(beanClass)) {
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
                if (TimerServlet.PERSISTENT_TIMER_CLASSES.contains(entry.getKey()) || AutoTimerBean.class.isAssignableFrom(entry.getKey())) {
                    Assert.assertNotEquals(entry.toString(), 0, entry.getValue().get(NODE_2).size());
                } else {
                    // Manual transient timers were never created on node 2
                    Assert.assertEquals(entry.toString(), 0, entry.getValue().get(NODE_2).size());
                }
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

            for (Map.Entry<Class<? extends TimerBean>, Map<String, List<Instant>>> entry : timeouts.entrySet()) {
                if (AutoTransientTimerBean.class.equals(entry.getKey())) {
                    // Manual auto timers will be triggered on both nodes
                    Assert.assertFalse(entry.toString(), entry.getValue().get(NODE_1).isEmpty());
                    Assert.assertFalse(entry.toString(), entry.getValue().get(NODE_2).isEmpty());
                } else if (TimerServlet.TRANSIENT_TIMER_CLASSES.contains(entry.getKey())) {
                    // Manual transient timers will not exist on either node
                    Assert.assertTrue(entry.toString(), entry.getValue().get(NODE_1).isEmpty());
                    Assert.assertTrue(entry.toString(), entry.getValue().get(NODE_2).isEmpty());
                } else {
                    // Verify that at least 1 timeout was received on either member
                    Assert.assertFalse(entry.toString(), entry.getValue().get(NODE_1).isEmpty() && entry.getValue().get(NODE_2).isEmpty());
                }
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

            for (Map.Entry<Class<? extends TimerBean>, Map<String, List<Instant>>> entry : timeouts.entrySet()) {
                if (AutoTransientTimerBean.class.equals(entry.getKey())) {
                    // Manual auto timers will be triggered on both nodes
                    Assert.assertFalse(entry.toString(), entry.getValue().get(NODE_1).isEmpty());
                    Assert.assertFalse(entry.toString(), entry.getValue().get(NODE_2).isEmpty());
                } else if (TimerServlet.TRANSIENT_TIMER_CLASSES.contains(entry.getKey())) {
                    // Manual transient timers will not exist on either node
                    Assert.assertTrue(entry.toString(), entry.getValue().get(NODE_1).isEmpty());
                    Assert.assertTrue(entry.toString(), entry.getValue().get(NODE_2).isEmpty());
                } else {
                    // Verify that at least 1 timeout was received on a single member
                    Assert.assertTrue(entry.toString(), entry.getValue().get(NODE_1).isEmpty() ^ entry.getValue().get(NODE_2).isEmpty());
                }
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
                if (TimerServlet.PERSISTENT_TIMER_CLASSES.contains(entry.getKey()) || AutoTimerBean.class.isAssignableFrom(entry.getKey())) {
                    Assert.assertNotEquals(entry.toString(), 0, entry.getValue().get(NODE_1).size());
                } else {
                    // Manual transient timers were never created on node 2
                    Assert.assertEquals(entry.toString(), 0, entry.getValue().get(NODE_1).size());
                }
            }

            this.start(NODE_2);

            TimeUnit.SECONDS.sleep(2);

            try (CloseableHttpResponse response = client.execute(new HttpDelete(uris.get(NODE_1)))) {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
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
}
