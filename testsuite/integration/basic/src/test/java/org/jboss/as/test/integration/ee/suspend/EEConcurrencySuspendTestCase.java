/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
 * 2110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.test.integration.ee.suspend;

import org.junit.Assert;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Tests for suspend/resume functionality with EE concurrency
 *
 * TODO: this test needs to be able to read the suspend state in order to really test anything,
 * at the moment it just tests that it does not blow up
 */
@RunWith(Arquillian.class)
public class EEConcurrencySuspendTestCase {

    protected static Logger log = Logger.getLogger(EEConcurrencySuspendTestCase.class);


    @ArquillianResource
    private ManagementClient managementClient;

    @Deployment
    public static WebArchive deployment() {

        WebArchive war = ShrinkWrap.create(WebArchive.class, "ee-suspend.war");
        war.addPackage(EEConcurrencySuspendTestCase.class.getPackage());
        war.addPackage(HttpRequest.class.getPackage());
        war.addClass(TestSuiteEnvironment.class);
        war.addAsResource(new StringAsset("Dependencies: org.jboss.dmr, org.jboss.as.controller\n"), "META-INF/MANIFEST.MF");
        return war;
    }

    @Test
    public void testRequestInShutdown() throws Exception {

        final String address = "http://" + TestSuiteEnvironment.getServerAddress() + ":8080/ee-suspend/ShutdownServlet";
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            Future<Object> result = executorService.submit(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    return HttpRequest.get(address, 60, TimeUnit.SECONDS);
                }
            });

            Thread.sleep(1000); //nasty, but we need to make sure the HTTP request has started

            ModelNode op = new ModelNode();
            op.get(ModelDescriptionConstants.OP).set("suspend");
            managementClient.getControllerClient().execute(op);

            ShutdownServlet.requestLatch.countDown();
            Assert.assertEquals(ShutdownServlet.TEXT, result.get());

            final HttpURLConnection conn = (HttpURLConnection) new URL(address).openConnection();
            try {
                conn.setDoInput(true);
                int responseCode = conn.getResponseCode();
                Assert.assertEquals(503, responseCode);
            } finally {
                conn.disconnect();
            }

        } finally {
            ShutdownServlet.requestLatch.countDown();
            executorService.shutdown();

            ModelNode op = new ModelNode();
            op.get(ModelDescriptionConstants.OP).set("resume");
            managementClient.getControllerClient().execute(op);
        }


    }

}
