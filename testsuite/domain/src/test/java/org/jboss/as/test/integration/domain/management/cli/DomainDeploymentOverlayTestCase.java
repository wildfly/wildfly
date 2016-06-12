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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.test.integration.domain.management.util.DomainTestSupport.validateResponse;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpResponse;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.network.NetworkUtils;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.domain.management.util.DomainTestSupport;
import org.jboss.as.test.integration.domain.suites.CLITestSuite;
import org.jboss.as.test.integration.management.util.CLITestUtil;
import org.jboss.as.test.integration.management.util.SimpleServlet;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.impl.base.exporter.zip.ZipExporterImpl;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * @author Alexey Loubyansky
 */
public class DomainDeploymentOverlayTestCase {

    private static final String SOCKET_BINDING_GROUP_NAME = "standard-sockets";

    private static File war1;
    private static File war2;
    private static File war3;
    private static File webXml;
    private static File overrideXml;

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

        final URL overrideXmlUrl = DomainDeploymentOverlayTestCase.class.getClassLoader().getResource("cli/deployment-overlay/override.xml");
        if(overrideXmlUrl == null) {
            Assert.fail("Failed to locate cli/deployment-overlay/override.xml");
        }
        overrideXml = new File(overrideXmlUrl.toURI());
        if(!overrideXml.exists()) {
            Assert.fail("Failed to locate cli/deployment-overlay/override.xml");
        }

        final URL webXmlUrl = DomainDeploymentOverlayTestCase.class.getClassLoader().getResource("cli/deployment-overlay/web.xml");
        if(webXmlUrl == null) {
            Assert.fail("Failed to locate cli/deployment-overlay/web.xml");
        }
        webXml = new File(webXmlUrl.toURI());
        if(!webXml.exists()) {
            Assert.fail("Failed to locate cli/deployment-overlay/web.xml");
        }

        // Launch the domain
        testSupport = CLITestSuite.createSupport(DomainDeploymentOverlayTestCase.class.getSimpleName());
    }

    @AfterClass
    public static void after() throws Exception {
        try {
            CLITestSuite.stopSupport();
            testSupport = null;
        } finally {
            war1.delete();
            war2.delete();
            war3.delete();
        }
    }

    @Before
    public void setUp() throws Exception {
        client = testSupport.getDomainMasterLifecycleUtil().createDomainClient();
        ctx = CLITestUtil.getCommandContext();
        //todo replace with proper fix coming with newer wildfly core.
        ctx.connectController(testSupport.getDomainMasterConfiguration().getHostControllerManagementAddress(),testSupport.getDomainMasterConfiguration().getHostControllerManagementPort());
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

        ctx.handle("deploy --server-groups=main-server-group,other-server-group " + war1.getAbsolutePath());
        ctx.handle("deploy --server-groups=main-server-group,other-server-group " + war2.getAbsolutePath());

        ctx.handle("deployment-overlay add --name=overlay-test --content=WEB-INF/web.xml=" + overrideXml.getAbsolutePath()
                + " --deployments=" + war1.getName() + " --server-groups=main-server-group,other-server-group");

        assertEquals("NON OVERRIDDEN", performHttpCall("master", "main-one", "deployment0"));
        assertEquals("NON OVERRIDDEN", performHttpCall("master", "main-one", "deployment1"));
//        assertEquals("NON OVERRIDDEN", performHttpCall("master", "other-one", "deployment0"));
//        assertEquals("NON OVERRIDDEN", performHttpCall("master", "other-one", "deployment1"));
        assertEquals("NON OVERRIDDEN", performHttpCall("slave", "main-three", "deployment0"));
        assertEquals("NON OVERRIDDEN", performHttpCall("slave", "main-three", "deployment1"));
//        assertEquals("NON OVERRIDDEN", performHttpCall("slave", "other-two", "deployment0"));
//        assertEquals("NON OVERRIDDEN", performHttpCall("slave", "other-two", "deployment1"));

        ctx.handle("deployment-overlay redeploy-affected --name=overlay-test");

        assertEquals("OVERRIDDEN", performHttpCall("master", "main-one", "deployment0"));
        assertEquals("NON OVERRIDDEN", performHttpCall("master", "main-one", "deployment1"));
//        assertEquals("OVERRIDDEN", performHttpCall("master", "other-one", "deployment0"));
//        assertEquals("NON OVERRIDDEN", performHttpCall("master", "other-one", "deployment1"));
        assertEquals("OVERRIDDEN", performHttpCall("slave", "main-three", "deployment0"));
        assertEquals("NON OVERRIDDEN", performHttpCall("slave", "main-three", "deployment1"));
//        assertEquals("OVERRIDDEN", performHttpCall("slave", "other-two", "deployment0"));
//        assertEquals("NON OVERRIDDEN", performHttpCall("slave", "other-two", "deployment1"));
    }

    @Test
    public void testSimpleOverrideWithRedeployAffected() throws Exception {

        ctx.handle("deploy --server-groups=main-server-group " + war1.getAbsolutePath());
        ctx.handle("deploy --server-groups=main-server-group " + war2.getAbsolutePath());

        ctx.handle("deployment-overlay add --name=overlay-test --content=WEB-INF/web.xml=" + overrideXml.getAbsolutePath()
                + " --deployments=" + war1.getName() + " --server-groups=main-server-group --redeploy-affected");

        assertEquals("OVERRIDDEN", performHttpCall("master", "main-one", "deployment0"));
        assertEquals("NON OVERRIDDEN", performHttpCall("master", "main-one", "deployment1"));
        assertEquals("OVERRIDDEN", performHttpCall("slave", "main-three", "deployment0"));
        assertEquals("NON OVERRIDDEN", performHttpCall("slave", "main-three", "deployment1"));
    }

    @Test
    public void testWildcardOverride() throws Exception {

        ctx.handle("deployment-overlay add --name=overlay-test --content=WEB-INF/web.xml=" + overrideXml.getAbsolutePath()
                + " --deployments=deployment*.war --server-groups=main-server-group --redeploy-affected");

        ctx.handle("deploy --server-groups=main-server-group " + war1.getAbsolutePath());
        ctx.handle("deploy --server-groups=main-server-group " + war2.getAbsolutePath());
        ctx.handle("deploy --server-groups=main-server-group " + war3.getAbsolutePath());

        assertEquals("OVERRIDDEN", performHttpCall("master", "main-one", "deployment0"));
        assertEquals("OVERRIDDEN", performHttpCall("master", "main-one", "deployment1"));
        assertEquals("NON OVERRIDDEN", performHttpCall("master", "main-one", "another"));
        assertEquals("OVERRIDDEN", performHttpCall("slave", "main-three", "deployment0"));
        assertEquals("OVERRIDDEN", performHttpCall("slave", "main-three", "deployment1"));
        assertEquals("NON OVERRIDDEN", performHttpCall("slave", "main-three", "another"));
    }

    @Test
    public void testWildcardOverrideWithRedeployAffected() throws Exception {

        ctx.handle("deploy --server-groups=main-server-group " + war1.getAbsolutePath());
        ctx.handle("deploy --server-groups=main-server-group " + war2.getAbsolutePath());
        ctx.handle("deploy --server-groups=main-server-group " + war3.getAbsolutePath());

        ctx.handle("deployment-overlay add --name=overlay-test --content=WEB-INF/web.xml=" + overrideXml.getAbsolutePath()
                + " --deployments=deployment*.war --server-groups=main-server-group --redeploy-affected");

        assertEquals("OVERRIDDEN", performHttpCall("master", "main-one", "deployment0"));
        assertEquals("OVERRIDDEN", performHttpCall("master", "main-one", "deployment1"));
        assertEquals("NON OVERRIDDEN", performHttpCall("master", "main-one", "another"));
        assertEquals("OVERRIDDEN", performHttpCall("slave", "main-three", "deployment0"));
        assertEquals("OVERRIDDEN", performHttpCall("slave", "main-three", "deployment1"));
        assertEquals("NON OVERRIDDEN", performHttpCall("slave", "main-three", "another"));
    }

    @Test
    public void testMultipleLinks() throws Exception {

        ctx.handle("deployment-overlay add --name=overlay-test --content=WEB-INF/web.xml=" + overrideXml.getAbsolutePath()
                + " --deployments=" + war1.getName() + " --server-groups=main-server-group");

        ctx.handle("deploy --server-groups=main-server-group " + war1.getAbsolutePath());
        ctx.handle("deploy --server-groups=main-server-group " + war2.getAbsolutePath());
        ctx.handle("deploy --server-groups=main-server-group " + war3.getAbsolutePath());

        assertEquals("OVERRIDDEN", performHttpCall("master", "main-one", "deployment0"));
        assertEquals("NON OVERRIDDEN", performHttpCall("master", "main-one", "deployment1"));
        assertEquals("NON OVERRIDDEN", performHttpCall("master", "main-one", "another"));
        assertEquals("OVERRIDDEN", performHttpCall("slave", "main-three", "deployment0"));
        assertEquals("NON OVERRIDDEN", performHttpCall("slave", "main-three", "deployment1"));
        assertEquals("NON OVERRIDDEN", performHttpCall("slave", "main-three", "another"));

        ctx.handle("deployment-overlay link --name=overlay-test --deployments=a*.war --server-groups=main-server-group");

        assertEquals("OVERRIDDEN", performHttpCall("master", "main-one", "deployment0"));
        assertEquals("NON OVERRIDDEN", performHttpCall("master", "main-one", "deployment1"));
        assertEquals("NON OVERRIDDEN", performHttpCall("master", "main-one", "another"));
        assertEquals("OVERRIDDEN", performHttpCall("slave", "main-three", "deployment0"));
        assertEquals("NON OVERRIDDEN", performHttpCall("slave", "main-three", "deployment1"));
        assertEquals("NON OVERRIDDEN", performHttpCall("slave", "main-three", "another"));

        ctx.handle("/server-group=main-server-group/deployment=" + war1.getName() + ":redeploy");
        ctx.handle("/server-group=main-server-group/deployment=" + war2.getName() + ":redeploy");
        ctx.handle("/server-group=main-server-group/deployment=" + war3.getName() + ":redeploy");

        assertEquals("OVERRIDDEN", performHttpCall("master", "main-one", "deployment0"));
        assertEquals("NON OVERRIDDEN", performHttpCall("master", "main-one", "deployment1"));
        assertEquals("OVERRIDDEN", performHttpCall("master", "main-one", "another"));
        assertEquals("OVERRIDDEN", performHttpCall("slave", "main-three", "deployment0"));
        assertEquals("NON OVERRIDDEN", performHttpCall("slave", "main-three", "deployment1"));
        assertEquals("OVERRIDDEN", performHttpCall("slave", "main-three", "another"));

        ctx.handle("deployment-overlay link --name=overlay-test --deployments=" + war2.getName() + " --redeploy-affected --server-groups=main-server-group");

        assertEquals("OVERRIDDEN", performHttpCall("master", "main-one", "deployment0"));
        assertEquals("OVERRIDDEN", performHttpCall("master", "main-one", "deployment1"));
        assertEquals("OVERRIDDEN", performHttpCall("master", "main-one", "another"));
        assertEquals("OVERRIDDEN", performHttpCall("slave", "main-three", "deployment0"));
        assertEquals("OVERRIDDEN", performHttpCall("slave", "main-three", "deployment1"));
        assertEquals("OVERRIDDEN", performHttpCall("slave", "main-three", "another"));

        ctx.handle("deployment-overlay remove --name=overlay-test --deployments=" + war2.getName() + " --redeploy-affected --server-groups=main-server-group");

        assertEquals("OVERRIDDEN", performHttpCall("master", "main-one", "deployment0"));
        assertEquals("NON OVERRIDDEN", performHttpCall("master", "main-one", "deployment1"));
        assertEquals("OVERRIDDEN", performHttpCall("master", "main-one", "another"));
        assertEquals("OVERRIDDEN", performHttpCall("slave", "main-three", "deployment0"));
        assertEquals("NON OVERRIDDEN", performHttpCall("slave", "main-three", "deployment1"));
        assertEquals("OVERRIDDEN", performHttpCall("slave", "main-three", "another"));

        ctx.handle("deployment-overlay remove --name=overlay-test --deployments=a*.war --server-groups=main-server-group");

        assertEquals("OVERRIDDEN", performHttpCall("master", "main-one", "deployment0"));
        assertEquals("NON OVERRIDDEN", performHttpCall("master", "main-one", "deployment1"));
        assertEquals("OVERRIDDEN", performHttpCall("master", "main-one", "another"));
        assertEquals("OVERRIDDEN", performHttpCall("slave", "main-three", "deployment0"));
        assertEquals("NON OVERRIDDEN", performHttpCall("slave", "main-three", "deployment1"));
        assertEquals("OVERRIDDEN", performHttpCall("slave", "main-three", "another"));

        ctx.handle("/server-group=main-server-group/deployment=" + war1.getName() + ":redeploy");
        ctx.handle("/server-group=main-server-group/deployment=" + war2.getName() + ":redeploy");
        ctx.handle("/server-group=main-server-group/deployment=" + war3.getName() + ":redeploy");
        ctx.handle("deployment-overlay remove --name=overlay-test --content=WEB-INF/web.xml --redeploy-affected");

        assertEquals("NON OVERRIDDEN", performHttpCall("master", "main-one", "deployment0"));
        assertEquals("NON OVERRIDDEN", performHttpCall("master", "main-one", "deployment1"));
        assertEquals("NON OVERRIDDEN", performHttpCall("master", "main-one", "another"));
        assertEquals("NON OVERRIDDEN", performHttpCall("slave", "main-three", "deployment0"));
        assertEquals("NON OVERRIDDEN", performHttpCall("slave", "main-three", "deployment1"));
        assertEquals("NON OVERRIDDEN", performHttpCall("slave", "main-three", "another"));

        ctx.handle("deployment-overlay upload --name=overlay-test --content=WEB-INF/web.xml=" + overrideXml.getAbsolutePath() + " --redeploy-affected");

        assertEquals("OVERRIDDEN", performHttpCall("master", "main-one", "deployment0"));
        assertEquals("NON OVERRIDDEN", performHttpCall("master", "main-one", "deployment1"));
        assertEquals("NON OVERRIDDEN", performHttpCall("master", "main-one", "another"));
        assertEquals("OVERRIDDEN", performHttpCall("slave", "main-three", "deployment0"));
        assertEquals("NON OVERRIDDEN", performHttpCall("slave", "main-three", "deployment1"));
        assertEquals("NON OVERRIDDEN", performHttpCall("slave", "main-three", "another"));
    }

    @Test
    public void testRedeployAffected() throws Exception {

        ctx.handle("deploy --server-groups=main-server-group " + war1.getAbsolutePath());
        ctx.handle("deploy --server-groups=main-server-group " + war2.getAbsolutePath());
        ctx.handle("deploy --server-groups=main-server-group " + war3.getAbsolutePath());

        ctx.handle("deployment-overlay add --name=overlay-test --content=WEB-INF/web.xml=" + overrideXml.getAbsolutePath());
        ctx.handle("deployment-overlay link --name=overlay-test --deployments=deployment0.war,a*.war --server-groups=main-server-group");

        assertEquals("NON OVERRIDDEN", performHttpCall("master", "main-one", "deployment0"));
        assertEquals("NON OVERRIDDEN", performHttpCall("master", "main-one", "deployment1"));
        assertEquals("NON OVERRIDDEN", performHttpCall("master", "main-one", "another"));
        assertEquals("NON OVERRIDDEN", performHttpCall("slave", "main-three", "deployment0"));
        assertEquals("NON OVERRIDDEN", performHttpCall("slave", "main-three", "deployment1"));
        assertEquals("NON OVERRIDDEN", performHttpCall("slave", "main-three", "another"));

        ctx.handle("deployment-overlay redeploy-affected --name=overlay-test");

        assertEquals("OVERRIDDEN", performHttpCall("master", "main-one", "deployment0"));
        assertEquals("NON OVERRIDDEN", performHttpCall("master", "main-one", "deployment1"));
        assertEquals("OVERRIDDEN", performHttpCall("master", "main-one", "another"));
        assertEquals("OVERRIDDEN", performHttpCall("slave", "main-three", "deployment0"));
        assertEquals("NON OVERRIDDEN", performHttpCall("slave", "main-three", "deployment1"));
        assertEquals("OVERRIDDEN", performHttpCall("slave", "main-three", "another"));
    }

    private String performHttpCall(String host, String server, String deployment) throws Exception {
        ModelNode op = new ModelNode();
        op.get(OP).set(READ_RESOURCE_OPERATION);
        op.get(OP_ADDR).add(HOST, host).add(SERVER, server).add(SOCKET_BINDING_GROUP, SOCKET_BINDING_GROUP_NAME).add(SOCKET_BINDING, "http");
        op.get(INCLUDE_RUNTIME).set(true);
        ModelNode socketBinding = validateResponse(client.execute(op));

        URL url = new URL("http",
                NetworkUtils.formatAddress(InetAddress.getByName(socketBinding.get("bound-address").asString())),
                socketBinding.get("bound-port").asInt(),
                "/" + deployment + "/SimpleServlet?env-entry=overlay-test");
        return HttpRequest.get(url.toExternalForm(), 10, TimeUnit.SECONDS).trim();
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
