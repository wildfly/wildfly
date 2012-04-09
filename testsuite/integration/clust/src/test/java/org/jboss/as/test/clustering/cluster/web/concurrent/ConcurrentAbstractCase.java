/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.clustering.cluster.web.concurrent;

import java.net.URL;
import java.util.Random;
import java.util.concurrent.Semaphore;
import junit.framework.Assert;
import org.apache.http.client.HttpClient;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.clustering.ClusterHttpClientUtil;
import static org.jboss.as.test.clustering.ClusteringTestConstants.*;
import org.jboss.as.test.clustering.NodeUtil;
import org.jboss.as.test.http.util.HttpClientUtils;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Simple clustering test case of get/set. It is session-based granularity with concurrent access.
 *
 * @author Radoslav Husar
 * @version April 2012
 */
@RunWith(Arquillian.class)
@RunAsClient
public abstract class ConcurrentAbstractCase {

    private Throwable exceptionThrown = null;
    private final int PERMITS = 100;
    private Semaphore semaphore = new Semaphore(PERMITS);
    private final String SET_URL = "testsessionreplication.jsp";
    private final String GET_URL = "getattribute.jsp";
    @ArquillianResource
    private ContainerController controller;
    @ArquillianResource
    private Deployer deployer;

    @Test
    @InSequence(-1)
    public void testStartContainers() {
        NodeUtil.start(controller, deployer, CONTAINER_1, DEPLOYMENT_1);
        NodeUtil.start(controller, deployer, CONTAINER_2, DEPLOYMENT_2);
    }

    /**
     * Test different session set in different servers.
     */
    @Test
    @InSequence(1)
    public void testConcurrentPut(
            @ArquillianResource @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1,
            @ArquillianResource @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2)
            throws Exception {
        int TIMES = 10;
        String baseURL0_ = baseURL1.toString();
        String baseURL1_ = baseURL2.toString();
        String servers_[] = {CONTAINER_1, CONTAINER_2};

        for (int i = 0; i < 10; i++) {
            String threadName = "startWithServer_1_ " + i;
            Thread t1 = runThread(threadName, baseURL0_, baseURL1_, servers_[1], TIMES, i);
            threadName = "startWithServer_2_ " + i;
            Thread t2 = runThread(threadName, baseURL1_, baseURL0_, servers_[0], TIMES, i);
            t1.start();
            t2.start();
        }

        Thread.sleep(1000);

        // Man this is just nasty, ouch. --Rado
        while (true) {
            if (semaphore.availablePermits() == PERMITS) {
                Thread.sleep(1000);
                continue;
            } else {
                break;
            }
        }

        if (exceptionThrown != null) {
            Assert.fail("Test fail, exception occured." + exceptionThrown);
        }

        //Assert.fail("Show me the logs, please.");
    }

    /**
     * Thread to execute the HTTP request.
     */
    protected Thread runThread(final String threadName, final String baseURL0, final String baseURL1, final String server2, final int TIMES, final int SEED) {

        return new Thread(threadName) {

            Random rand = new Random(SEED);

            @Override
            public void run() {
                try {
                    semaphore.acquire();
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                    exceptionThrown = ie;
                    return;
                }

                try {
                    for (int i = 0; i < TIMES; i++) {
                        work();
                        // Random numbder between [0, 200].
                        long msecs = rand.nextInt(200);
                        try {
                            Thread.sleep(msecs);
                        } catch (InterruptedException ex) {
                        }
                    }
                } finally {
                    semaphore.release();
                }
            }

            protected void work() {
                System.out.println("Enter runThread");

                System.out.println("URLs to query: /" + SET_URL + ", /" + GET_URL);

                // Create an instance of HttpClient.
                HttpClient client = HttpClientUtils.relaxedCookieHttpClient();

                try {
                    // Set the session attribute first
                    ClusterHttpClientUtil.tryGetAndConsume(client, baseURL0 + SET_URL);

                    // Get the Attribute set by testsessionreplication.jsp
                    String attrs1 = ClusterHttpClientUtil.tryGetAndConsume(client, baseURL0 + GET_URL);
                    System.out.println(attrs1);

                    // Let's switch to server 2 to retrieve the session attribute.
                    System.out.println("Switching to server: " + server2);

                    Thread.sleep(GRACE_TIME_TO_REPLICATE);

                    String attrs2 = ClusterHttpClientUtil.tryGetAndConsume(client, baseURL1 + GET_URL);
                    System.out.println(attrs2);

                    // Check the result
                    Assert.assertEquals("HTTP session replication attributes retrieved from both servers", attrs1, attrs2);
                } catch (Throwable ex) {
                    exceptionThrown = ex;
                }

                System.out.println("HTTP Session replication has happened");
                System.out.println("Exit thread");
            }
        };
    }
}
