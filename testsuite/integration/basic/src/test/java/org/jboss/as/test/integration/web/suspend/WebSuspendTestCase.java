/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.web.suspend;

import java.io.FilePermission;
import java.net.HttpURLConnection;
import java.net.SocketPermission;
import java.net.URL;
import java.util.PropertyPermission;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.remoting3.security.RemotingPermission;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

/**
 * Tests for suspend/resume functionality in the web subsystem
 */
@RunWith(Arquillian.class)
public class WebSuspendTestCase {

    protected static Logger log = Logger.getLogger(WebSuspendTestCase.class);


    @ArquillianResource
    private ManagementClient managementClient;

    @Deployment
    public static WebArchive deployment() {

        WebArchive war = ShrinkWrap.create(WebArchive.class, "web-suspend.war");
        war.addPackage(WebSuspendTestCase.class.getPackage());
        war.addPackage(HttpRequest.class.getPackage());
        war.addClass(TestSuiteEnvironment.class);
        war.addAsResource(new StringAsset("Dependencies: org.jboss.dmr, org.jboss.as.controller, org.jboss.remoting3\n"), "META-INF/MANIFEST.MF");
        war.addAsManifestResource(createPermissionsXmlAsset(
                new PropertyPermission("management.address", "read"),
                new PropertyPermission("jboss.http.port", "read"),
                new PropertyPermission("node0", "read"),
                // executorService.shutdown() needs the following permission
                new RuntimePermission("modifyThread"),
                // ManagementClient needs the following permissions and a dependency on 'org.jboss.remoting3' module
                new RemotingPermission("createEndpoint"),
                new RemotingPermission("connect"),
                // HttpClient needs the following permission
                new SocketPermission(TestSuiteEnvironment.getServerAddress() + ":" + TestSuiteEnvironment.getHttpPort(), "connect,resolve"),
                new FilePermission(System.getProperty("jboss.inst") + "/standalone/tmp/auth/*", "read")
        ), "permissions.xml");
        return war;
    }

    @Test
    public void testRequestInShutdown() throws Exception {

        final String address = "http://" + TestSuiteEnvironment.getServerAddress() + ":" + TestSuiteEnvironment.getHttpPort() + "/web-suspend/ShutdownServlet";
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
