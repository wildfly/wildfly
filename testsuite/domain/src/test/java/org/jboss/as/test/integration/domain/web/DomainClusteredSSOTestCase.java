/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.domain.web;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADMIN_ONLY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RUNTIME_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.test.http.util.HttpClientUtils;
import org.jboss.as.test.integration.domain.BuildConfigurationTestBase;
import org.jboss.as.test.integration.domain.management.util.DomainLifecycleUtil;
import org.jboss.as.test.integration.domain.management.util.JBossAsManagedConfiguration;
import org.jboss.as.test.integration.management.util.ModelUtil;
import org.jboss.as.test.integration.web.sso.SSOTestBase;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.impl.base.exporter.zip.ZipExporterImpl;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author wangchao
 *
 */
public class DomainClusteredSSOTestCase extends BuildConfigurationTestBase {

    public static final String slaveAddress = System.getProperty("jboss.test.host.slave.address", "127.0.0.1");
    private static final Logger log = Logger.getLogger(DomainClusteredSSOTestCase.class);
    private static final String DEPLOYMENT_NAME = "clusteredsso.war";
    private static WebArchive war;
    private static File warFile;

    @BeforeClass
    public static void before() throws Exception {
        war = ShrinkWrap.create(WebArchive.class, DEPLOYMENT_NAME);
        war.addClasses(SimpleServlet.class, SimpleSecuredServlet.class);
        war.setWebXML(DomainClusteredSSOTestCase.class.getPackage(), "web.xml");
        log.info(war.toString(true));

        String tempDir = System.getProperty("java.io.tmpdir");
        warFile = new File(tempDir, "clusteredsso.war");
        new ZipExporterImpl(war).exportTo(warFile, true);
    }

    @Test
    public void testSso() throws Exception {
        final JBossAsManagedConfiguration config = createConfiguration("domain.xml", "host.xml", getClass().getSimpleName());
        final DomainLifecycleUtil utils = new DomainLifecycleUtil(config);
        try {
            utils.start();
            DomainClient domainClient = utils.getDomainClient();

            // Add sso configuration
            SSOTestBase.addClusteredSso(domainClient, "domain");

            // Set profile=ha
            List<ModelNode> updates = new ArrayList<ModelNode>();
            ModelNode writeOp;
            writeOp = createOpNode("server-group=main-server-group", WRITE_ATTRIBUTE_OPERATION);
            writeOp.get(NAME).set(PROFILE);
            writeOp.get(VALUE).set("ha");
            updates.add(writeOp);
            SSOTestBase.applyUpdates(updates, domainClient);

            // Set socket-bindind-group=ha-socket
            updates = new ArrayList<ModelNode>();
            writeOp = createOpNode("server-group=main-server-group", WRITE_ATTRIBUTE_OPERATION);
            writeOp.get(NAME).set(SOCKET_BINDING_GROUP);
            writeOp.get(VALUE).set("ha-sockets");
            updates.add(writeOp);
            SSOTestBase.applyUpdates(updates, domainClient);

            // Reload to update sso configuration
            ModelNode reloadOp;
            reloadOp = createOpNode("host=master", "reload");
            reloadOp.get(ADMIN_ONLY).set(false);
            // opReload.get(OP).set("restart-servers");

            utils.executeAwaitConnectionClosed(reloadOp);

            // Try to reconnect to the hc
            utils.connect();

            // Check that the servers are up
            utils.awaitServers(System.currentTimeMillis());

            domainClient = utils.getDomainClient();

            Operation deployOp = buildDeployOperation();
            domainClient.execute(deployOp);

            // Test propagation
            DefaultHttpClient client = HttpClientUtils.relaxedCookieHttpClient();
            URL baseURL1 = new URL("http://" + TestSuiteEnvironment.formatPossibleIpv6Address(slaveAddress) + ":8080/");
            URL baseURL2 = new URL("http://" + TestSuiteEnvironment.formatPossibleIpv6Address(slaveAddress) + ":8230/");

            final String servletPath = "clusteredsso" + SimpleSecuredServlet.SERVLET_PATH;

            try {
                // make request to a protected resource without authentication
                final HttpGet httpGet1 = new HttpGet(baseURL1.toString() + servletPath);
                HttpResponse response = client.execute(httpGet1);
                int statusCode = response.getStatusLine().getStatusCode();
                assertEquals("Server-One Unexpected HTTP response status code.", HttpServletResponse.SC_UNAUTHORIZED, statusCode);
                EntityUtils.consume(response.getEntity());

                // set credentials and retry the request
                final UsernamePasswordCredentials credentials = new UsernamePasswordCredentials("user1", "password1");
                client.getCredentialsProvider().setCredentials(new AuthScope(baseURL1.getHost(), baseURL1.getPort()),
                        credentials);
                response = client.execute(httpGet1);
                statusCode = response.getStatusLine().getStatusCode();
                assertEquals("Server-One Unexpected HTTP response status code.", HttpServletResponse.SC_OK, statusCode);
                EntityUtils.consume(response.getEntity());

                // check on the 2nd server
                final HttpGet httpGet2 = new HttpGet(baseURL2.toString() + servletPath);
                response = client.execute(httpGet2);
                Assert.assertEquals("Server-Two Unexpected HTTP response status code.", HttpServletResponse.SC_OK, response
                        .getStatusLine().getStatusCode());
                EntityUtils.consume(response.getEntity());
            } finally {
                client.getConnectionManager().shutdown();
            }
        } finally {
            // Delete war file and stop servers
            warFile.delete();
            utils.stop();
        }
    }

    private Operation buildDeployOperation() throws IOException, OperationFormatException {
        ModelNode addDeploymentOp = new ModelNode();
        addDeploymentOp.get(ADDRESS).add("deployment", DEPLOYMENT_NAME);
        addDeploymentOp.get(OP).set(ADD);
        addDeploymentOp.get("content").get(0).get("input-stream-index").set(0);
        addDeploymentOp.get(RUNTIME_NAME).set(DEPLOYMENT_NAME);

        ModelNode deployOp = new ModelNode();
        deployOp.get(OP).set(ADD);
        deployOp.get(ADDRESS).add(SERVER_GROUP, "main-server-group");
        deployOp.get(ADDRESS).add(DEPLOYMENT, DEPLOYMENT_NAME);
        deployOp.get(ENABLED).set(true);
        ModelNode[] steps = new ModelNode[2];
        steps[0] = addDeploymentOp;
        steps[1] = deployOp;
        ModelNode compositeOp = ModelUtil.createCompositeNode(steps);

        OperationBuilder ob = new OperationBuilder(compositeOp, true);
        ob.addInputStream(new FileInputStream(warFile));

        return ob.build();
    }
}
