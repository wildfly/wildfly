/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.threads;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.junit.Assert.assertEquals;

/**
 * Tests the use of a custom thread pool with servlet deployments.
 * <p/>
 * This creates an executor with a single thread, and then invokes RaceyServlet
 * multiple times from several threads. If it is not using the correct
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(ServletThreadPoolSelectionTestCase.ServletThreadPoolSelectionTestCaseSetupAction.class)
public class ServletThreadPoolSelectionTestCase {

    public static class ServletThreadPoolSelectionTestCaseSetupAction implements ServerSetupTask {

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            ModelNode op;
            op = new ModelNode();
            op.get(OP).set(ADD);
            op.get(OP_ADDR).add(SUBSYSTEM, "io");
            op.get(OP_ADDR).add("worker", "test-worker");
            op.get("task-max-threads").set(1);
            managementClient.getControllerClient().execute(op);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            ModelNode op;
            op = new ModelNode();
            op.get(OP).set(REMOVE);
            op.get(OP_ADDR).add(SUBSYSTEM, "io");
            op.get(OP_ADDR).add("worker", "test-worker");
            managementClient.getControllerClient().execute(op);
        }
    }


    @ArquillianResource
    private URL url;


    @Deployment
    public static WebArchive deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "war-example.war");
        war.addClasses(HttpRequest.class, RaceyServlet.class);
        war.addAsWebInfResource(ServletThreadPoolSelectionTestCase.class.getPackage(), "jboss-web.xml", "jboss-web.xml");
        return war;
    }

    @Test
    public void testExecutor() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        try {
            final List<Future<?>> results = new ArrayList<Future<?>>();
            for (int i = 0; i < 100; ++i) {
                results.add(executor.submit(new Callable<Object>() {

                    @Override
                    public Object call() throws Exception {
                        HttpRequest.get(url.toExternalForm() + "/race", 10, SECONDS);
                        return null;
                    }
                }));
            }
            for (Future<?> res : results) {
                res.get();
            }
            String result = HttpRequest.get(url.toExternalForm() + "/race", 10, SECONDS);
            assertEquals("100", result);
        } finally {
            executor.shutdown();
        }
    }
}
