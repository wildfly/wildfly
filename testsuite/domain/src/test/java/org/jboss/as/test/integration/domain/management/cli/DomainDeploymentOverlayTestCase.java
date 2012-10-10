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

package org.jboss.as.test.integration.domain.management.cli;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_RUNTIME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INPUT_STREAM_INDEX;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.test.integration.domain.DomainTestSupport.validateResponse;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.domain.DomainTestSupport;
import org.jboss.as.test.integration.domain.suites.DomainTestSuite;
import org.jboss.as.test.integration.management.util.CLITestUtil;
import org.jboss.as.test.integration.management.util.SimpleServlet;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.impl.base.exporter.zip.ZipExporterImpl;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Alexey Loubyansky
 */
public class DomainDeploymentOverlayTestCase {

    private static final String SOCKET_BINDING_GROUP_NAME = "standard-sockets";

    private static File war1;
    private static File war2;
    private static File war3;
    private static URL webXml;
    private static URL overrideXml;

    private static DomainTestSupport testSupport;

    private CommandContext ctx;

    private DomainClient client;

    @BeforeClass
    public static void before() throws Exception {
        String tempDir = System.getProperty("java.io.tmpdir");

        WebArchive war;

        // deployment1
        war = ShrinkWrap.create(WebArchive.class, "deployment0.war");
        war.addClass(SimpleServlet.class);
        war.addAsWebInfResource("cli/deployment-overlay/web.xml", "web.xml");
        war1 = new File(tempDir + File.separator + war.getName());
        new ZipExporterImpl(war).exportTo(war1, true);

        war = ShrinkWrap.create(WebArchive.class, "deployment1.war");
        war.addClass(SimpleServlet.class);
        war.addAsWebInfResource("cli/deployment-overlay/web.xml", "web.xml");
        war2 = new File(tempDir + File.separator + war.getName());
        new ZipExporterImpl(war).exportTo(war2, true);

        war = ShrinkWrap.create(WebArchive.class, "another.war");
        war.addClass(SimpleServlet.class);
        war.addAsWebInfResource("cli/deployment-overlay/web.xml", "web.xml");
        war3 = new File(tempDir + File.separator + war.getName());
        new ZipExporterImpl(war).exportTo(war3, true);

        overrideXml = DomainDeploymentOverlayTestCase.class.getClassLoader().getResource("cli/deployment-overlay/override.xml");
        if(overrideXml == null) {
            Assert.fail("Failed to locate cli/deployment-overlay/override.xml");
        }
        webXml = DomainDeploymentOverlayTestCase.class.getClassLoader().getResource("cli/deployment-overlay/web.xml");
        if(webXml == null) {
            Assert.fail("Failed to locate cli/deployment-overlay/web.xml");
        }

        // Launch the domain
        testSupport = DomainTestSuite.createSupport(DomainDeploymentOverlayTestCase.class.getSimpleName());
    }

    @AfterClass
    public static void after() throws Exception {
        try {
            testSupport = null;
            DomainTestSuite.stopSupport();
        } finally {
            war1.delete();
            war2.delete();
            war3.delete();
        }
    }

    @Before
    public void setUp() throws Exception {
        ctx = CLITestUtil.getCommandContext();
        ctx.connectController();

        client = testSupport.getDomainMasterLifecycleUtil().createDomainClient();
    }

    @After
    public void tearDown() throws Exception {
        if(ctx != null) {
            ctx.handleSafe("undeploy --all-relevant-server-groups " + war1.getName());
            ctx.handleSafe("undeploy --all-relevant-server-groups " + war2.getName());
            ctx.handleSafe("undeploy --all-relevant-server-groups " + war3.getName());
            ctx.handleSafe("deployment-overlay remove --name=overlay-test");
            ctx.terminateSession();
            ctx = null;
        }
        client = null;
    }

    @Test
    public void testSimpleOverride() throws Exception {

        ctx.handle("deployment-overlay add --name=overlay-test --content=WEB-INF/web.xml=" + overrideXml.getPath()
                + " --deployments=" + war1.getName() + " --server-groups=main-server-group");

/*        ModelNode op = new ModelNode();
        op.get(ModelDescriptionConstants.OP_ADDR).set(ModelDescriptionConstants.DEPLOYMENT_OVERLAY, "overlay-test");
        op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
        executeOnMaster(op);

        op = new ModelNode();
        OperationBuilder builder = new OperationBuilder(op, true);
        ModelNode addr = new ModelNode();
        addr.add(ModelDescriptionConstants.DEPLOYMENT_OVERLAY, "overlay-test");
        addr.add(ModelDescriptionConstants.CONTENT, "WEB-INF/web.xml");
        op.get(ModelDescriptionConstants.OP_ADDR).set(addr);
        op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
        op.get(ModelDescriptionConstants.CONTENT).get(INPUT_STREAM_INDEX).set(0);
        builder.addInputStream(overrideXml.openStream());
        executeOnMaster(builder.build());

        op = new ModelNode();
        addr = new ModelNode();
        addr.add(ModelDescriptionConstants.SERVER_GROUP, "main-server-group");
        addr.add(ModelDescriptionConstants.DEPLOYMENT_OVERLAY, "overlay-test");
        op.get(ModelDescriptionConstants.OP_ADDR).set(addr);
        op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
        executeOnMaster(op);

        op = new ModelNode();
        addr = new ModelNode();
        addr.add(ModelDescriptionConstants.SERVER_GROUP, "main-server-group");
        addr.add(ModelDescriptionConstants.DEPLOYMENT_OVERLAY, "overlay-test");
        addr.add(ModelDescriptionConstants.DEPLOYMENT, war1.getName());
        op.get(ModelDescriptionConstants.OP_ADDR).set(addr);
        op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
        executeOnMaster(op);
*/
        ctx.handle("deploy --server-groups=main-server-group " + war1.getAbsolutePath());
        ctx.handle("deploy --server-groups=main-server-group " + war2.getAbsolutePath());


        assertEquals("NON OVERRIDDEN", performHttpCall("master", "main-one", "deployment0"));
        assertEquals("NON OVERRIDDEN", performHttpCall("slave", "main-three", "deployment0"));
        assertEquals("NON OVERRIDDEN", performHttpCall("master", "main-one", "deployment1"));
        assertEquals("NON OVERRIDDEN", performHttpCall("slave", "main-three", "deployment1"));

//        ctx.handle("deployment-overlay redeploy-affected --name=overlay-test");
/*
        assertEquals("OVERRIDDEN", performHttpCall("master", "main-one", "deployment0"));
        assertEquals("OVERRIDDEN", performHttpCall("slave", "main-three", "deployment0"));
        assertEquals("NON OVERRIDDEN", performHttpCall("master", "main-one", "deployment1"));
        assertEquals("NON OVERRIDDEN", performHttpCall("slave", "main-three", "deployment1"));
*/    }

    private static ModelNode executeOnMaster(ModelNode op) throws IOException {
        return validateResponse(testSupport.getDomainMasterLifecycleUtil().getDomainClient().execute(op));
    }

    private static ModelNode executeOnMaster(Operation op) throws IOException {
        return validateResponse(testSupport.getDomainMasterLifecycleUtil().getDomainClient().execute(op));
    }

/*
    @Test
    public void testSimpleOverrideWithRedeployAffected() throws Exception {

        ctx.handle("deploy " + war1.getAbsolutePath());
        ctx.handle("deploy " + war2.getAbsolutePath());

        ctx.handle("deployment-overlay add --name=overlay-test --content=WEB-INF/web.xml=" + overrideXml.getPath()
                + " --deployments=" + war1.getName() + " --redeploy-affected");

        String response = readResponse("deployment0");
        assertEquals("OVERRIDDEN", response);
        response = readResponse("deployment1");
        assertEquals("NON OVERRIDDEN", response);
    }

    @Test
    public void testWildcardOverride() throws Exception {

        ctx.handle("deployment-overlay add --name=overlay-test --content=WEB-INF/web.xml=" + overrideXml.getPath()
                + " --wildcards=deployment.*\\.war");

        ctx.handle("deploy " + war1.getAbsolutePath());
        ctx.handle("deploy " + war2.getAbsolutePath());
        ctx.handle("deploy " + war3.getAbsolutePath());

        String response = readResponse("deployment0");
        assertEquals("OVERRIDDEN", response);
        response = readResponse("deployment1");
        assertEquals("OVERRIDDEN", response);
        response = readResponse("another");
        assertEquals("NON OVERRIDDEN", response);
    }

    @Test
    @Ignore
    // TODO this because the cli is using wildcards instead of regexp
    // to determine the matching deployments
    public void testWildcardOverrideWithRedeployAffected() throws Exception {

        ctx.handle("deploy " + war1.getAbsolutePath());
        ctx.handle("deploy " + war2.getAbsolutePath());
        ctx.handle("deploy " + war3.getAbsolutePath());

        ctx.handle("deployment-overlay add --name=overlay-test --content=WEB-INF/web.xml=" + overrideXml.getPath()
                + " --wildcards=deployment.*\\.war --redeploy-affected");

        Thread.sleep(2000);
        String response = readResponse("deployment0");
        assertEquals("OVERRIDDEN", response);
        response = readResponse("deployment1");
        assertEquals("OVERRIDDEN", response);
        response = readResponse("another");
        assertEquals("NON OVERRIDDEN", response);
    }

    @Test
    public void testMultipleLinks() throws Exception {

        ctx.handle("deployment-overlay add --name=overlay-test --content=WEB-INF/web.xml=" + overrideXml.getPath()
                + " --deployments=" + war1.getName());

        ctx.handle("deploy " + war1.getAbsolutePath());
        ctx.handle("deploy " + war2.getAbsolutePath());
        ctx.handle("deploy " + war3.getAbsolutePath());

        String response = readResponse("deployment0");
        assertEquals("OVERRIDDEN", response);
        response = readResponse("deployment1");
        assertEquals("NON OVERRIDDEN", response);
        response = readResponse("another");
        assertEquals("NON OVERRIDDEN", response);

        ctx.handle("deployment-overlay link --name=overlay-test --wildcards=a.*\\.war");

        response = readResponse("deployment0");
        assertEquals("OVERRIDDEN", response);
        response = readResponse("deployment1");
        assertEquals("NON OVERRIDDEN", response);
        response = readResponse("another");
        assertEquals("NON OVERRIDDEN", response);

        ctx.handle("/deployment=" + war1.getName() + ":redeploy");
        ctx.handle("/deployment=" + war2.getName() + ":redeploy");
        ctx.handle("/deployment=" + war3.getName() + ":redeploy");

        response = readResponse("deployment0");
        assertEquals("OVERRIDDEN", response);
        response = readResponse("deployment1");
        assertEquals("NON OVERRIDDEN", response);
        response = readResponse("another");
        assertEquals("OVERRIDDEN", response);

        ctx.handle("deployment-overlay link --name=overlay-test --deployments=" + war2.getName() + " --redeploy-affected");

        response = readResponse("deployment0");
        assertEquals("OVERRIDDEN", response);
        response = readResponse("deployment1");
        assertEquals("OVERRIDDEN", response);
        response = readResponse("another");
        assertEquals("OVERRIDDEN", response);

        ctx.handle("deployment-overlay remove --name=overlay-test --deployments=" + war2.getName() + " --redeploy-affected");

        response = readResponse("deployment0");
        assertEquals("OVERRIDDEN", response);
        response = readResponse("deployment1");
        assertEquals("NON OVERRIDDEN", response);
        response = readResponse("another");
        assertEquals("OVERRIDDEN", response);

        ctx.handle("deployment-overlay remove --name=overlay-test --wildcards=a.*\\.war");

        response = readResponse("deployment0");
        assertEquals("OVERRIDDEN", response);
        response = readResponse("deployment1");
        assertEquals("NON OVERRIDDEN", response);
        response = readResponse("another");
        assertEquals("OVERRIDDEN", response);

        ctx.handle("/deployment=" + war1.getName() + ":redeploy");
        ctx.handle("/deployment=" + war2.getName() + ":redeploy");
        ctx.handle("/deployment=" + war3.getName() + ":redeploy");

        response = readResponse("deployment0");
        assertEquals("OVERRIDDEN", response);
        response = readResponse("deployment1");
        assertEquals("NON OVERRIDDEN", response);
        response = readResponse("another");
        assertEquals("NON OVERRIDDEN", response);

        ctx.handle("deployment-overlay remove --name=overlay-test --content=WEB-INF/web.xml --redeploy-affected");

        response = readResponse("deployment0");
        assertEquals("NON OVERRIDDEN", response);
        response = readResponse("deployment1");
        assertEquals("NON OVERRIDDEN", response);
        response = readResponse("another");
        assertEquals("NON OVERRIDDEN", response);

        ctx.handle("deployment-overlay upload --name=overlay-test --content=WEB-INF/web.xml=" + overrideXml.getPath() + " --redeploy-affected");

        response = readResponse("deployment0");
        assertEquals("OVERRIDDEN", response);
        response = readResponse("deployment1");
        assertEquals("NON OVERRIDDEN", response);
        response = readResponse("another");
        assertEquals("NON OVERRIDDEN", response);
    }

    @Test
    public void testRedeployAffected() throws Exception {

        ctx.handle("deploy " + war1.getAbsolutePath());
        ctx.handle("deploy " + war2.getAbsolutePath());
        ctx.handle("deploy " + war3.getAbsolutePath());

        ctx.handle("deployment-overlay add --name=overlay-test --content=WEB-INF/web.xml=" + overrideXml.getPath());
        ctx.handle("deployment-overlay link --name=overlay-test --deployments=deployment0.war --wildcards=a.*\\.war");

        String response = readResponse("deployment0");
        assertEquals("NON OVERRIDDEN", response);
        response = readResponse("deployment1");
        assertEquals("NON OVERRIDDEN", response);
        response = readResponse("another");
        assertEquals("NON OVERRIDDEN", response);

        ctx.handle("deployment-overlay redeploy-affected --name=overlay-test");

        response = readResponse("deployment0");
        assertEquals("OVERRIDDEN", response);
        response = readResponse("deployment1");
        assertEquals("NON OVERRIDDEN", response);
        // TODO the below will fail because CLI uses simple wildcards to determine the target deployments
        // instead of true regexp
//        response = readResponse("another");
//        assertEquals("OVERRIDDEN", response);
    }
*/
    private String performHttpCall(String host, String server, String deployment) throws Exception {
        ModelNode op = new ModelNode();
        op.get(OP).set(READ_RESOURCE_OPERATION);
        op.get(OP_ADDR).add(HOST, host).add(SERVER, server).add(SOCKET_BINDING_GROUP, SOCKET_BINDING_GROUP_NAME).add(SOCKET_BINDING, "http");
        op.get(INCLUDE_RUNTIME).set(true);
        ModelNode socketBinding = validateResponse(client.execute(op));

        URL url = new URL("http",
                org.jboss.as.arquillian.container.NetworkUtils.formatPossibleIpv6Address(socketBinding.get("bound-address").asString()),
                socketBinding.get("bound-port").asInt(),
                "/" + deployment + "/SimpleServlet?env-entry=overlay-test");
        System.err.println("url: " + url);
        return HttpRequest.get(url.toExternalForm(), 10, TimeUnit.SECONDS).trim();
//        HttpGet get = new HttpGet(url.toURI());
//        HttpClient httpClient = new DefaultHttpClient();
//        HttpResponse response = httpClient.execute(get);
//        return getContent(response);
    }

    public static String getContent(HttpResponse response) throws IOException {
        InputStreamReader reader = new InputStreamReader(response.getEntity().getContent());
        StringBuilder content = new StringBuilder();
        int c;
        while (-1 != (c = reader.read())) {
            content.append((char) c);
        }
        reader.close();
        return content.toString();
    }
}
