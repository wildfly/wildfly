/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.domain;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DIRECTORY_GROUPING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELOAD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESTART_SERVERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_CONFIG;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.test.integration.domain.management.util.DomainTestSupport.safeClose;
import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Properties;
import java.util.PropertyPermission;
import java.util.concurrent.TimeoutException;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.controller.client.helpers.domain.ServerStatus;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.domain.management.util.DomainLifecycleUtil;
import org.jboss.as.test.integration.domain.management.util.DomainTestSupport;
import org.jboss.as.test.integration.domain.management.util.DomainTestUtils;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.integration.management.util.PropertiesServlet;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Verifies the uses of jboss.server.[base,log,data,temp].dir properties as managed server JMV options. The test uses a
 * preconfigured domain with three server groups with a server on each server group. The above properties are used to
 * configure the host, server-group and server JVM settings. The test validates that these properties can be used and
 * contains the expected values.
 *
 * @author <a href="mailto:yborgess@redhat.com">Yeray Borges</a>
 */
public class JVMServerPropertiesTestCase {
    protected static final PathElement SERVER_GROUP_ONE = PathElement.pathElement(SERVER_GROUP, "server-group-one");
    protected static final PathElement SERVER_GROUP_TWO = PathElement.pathElement(SERVER_GROUP, "server-group-two");
    protected static final PathElement SERVER_GROUP_THREE = PathElement.pathElement(SERVER_GROUP, "server-group-three");

    protected static final PathElement HOST_PRIMARY = PathElement.pathElement(HOST, "primary");

    protected static final PathElement SERVER_CONFIG_ONE = PathElement.pathElement(SERVER_CONFIG, "server-one");
    protected static final PathElement SERVER_CONFIG_TWO = PathElement.pathElement(SERVER_CONFIG, "server-two");
    protected static final PathElement SERVER_CONFIG_THREE = PathElement.pathElement(SERVER_CONFIG, "server-three");
    public static final String BY_SERVER = "by-server";
    public static final String BY_TYPE = "by-type";

    private static DomainTestSupport testSupport;
    private static DomainLifecycleUtil primaryLifecycleUtil;

    private static final String PROP_SERVLET_APP = "propertiesServletTestApp";
    private static final String PROP_SERVLET_APP_WAR = PROP_SERVLET_APP + ".war";
    private static final String PROP_SERVLET_APP_URL = PROP_SERVLET_APP + PropertiesServlet.SERVLET_PATH;

    private static Path deploymentPath;

    @BeforeClass
    public static void setupDomain() throws Exception {
        final DomainTestSupport.Configuration configuration = DomainTestSupport.Configuration.create(JVMServerPropertiesTestCase.class.getSimpleName(),
                "domain-configs/domain-jvm-properties.xml",
                "host-configs/host-primary-jvm-properties.xml",
                null
        );

        testSupport = DomainTestSupport.create(configuration);

        primaryLifecycleUtil = testSupport.getDomainPrimaryLifecycleUtil();

        // Prepares the application deployment file
        WebArchive deployment = createDeployment();
        deploymentPath = DomainTestSupport.getBaseDir(JVMServerPropertiesTestCase.class.getSimpleName()).toPath().resolve("deployments");
        deploymentPath.toFile().mkdirs();
        deployment.as(ZipExporter.class).exportTo(deploymentPath.resolve(PROP_SERVLET_APP_WAR).toFile(), true);

        testSupport.start();

        primaryLifecycleUtil.awaitServers(TimeoutUtil.adjust(30 * 1000));
    }

    @AfterClass
    public static void tearDownDomain() {
        testSupport.close();

        testSupport = null;
        primaryLifecycleUtil = null;
    }

    @Test
    public void testServerProperties() throws IOException, MgmtOperationException, InterruptedException, TimeoutException {
        ModelNode op = createDeploymentOperation(deploymentPath.resolve(PROP_SERVLET_APP_WAR), SERVER_GROUP_ONE, SERVER_GROUP_TWO, SERVER_GROUP_THREE);
        DomainTestUtils.executeForResult(op, primaryLifecycleUtil.createDomainClient());

        validateProperties("server-one", 8080, BY_SERVER);
        validateProperties("server-two", 8180, BY_SERVER);
        validateProperties("server-three", 8280, BY_SERVER);

        op = Util.getWriteAttributeOperation(PathAddress.pathAddress(HOST_PRIMARY), DIRECTORY_GROUPING, BY_TYPE);
        DomainTestUtils.executeForResult(op, primaryLifecycleUtil.createDomainClient());

        op = Util.createEmptyOperation(RELOAD, PathAddress.pathAddress(HOST_PRIMARY));
        op.get(RESTART_SERVERS).set(true);
        primaryLifecycleUtil.executeAwaitConnectionClosed(op);
        primaryLifecycleUtil.connect();
        primaryLifecycleUtil.awaitHostController(System.currentTimeMillis());

        DomainClient primaryClient = primaryLifecycleUtil.createDomainClient();
        DomainTestUtils.waitUntilState(primaryClient, PathAddress.pathAddress(HOST_PRIMARY, SERVER_CONFIG_ONE), ServerStatus.STARTED.toString());
        DomainTestUtils.waitUntilState(primaryClient, PathAddress.pathAddress(HOST_PRIMARY, SERVER_CONFIG_TWO), ServerStatus.STARTED.toString());
        DomainTestUtils.waitUntilState(primaryClient, PathAddress.pathAddress(HOST_PRIMARY, SERVER_CONFIG_THREE), ServerStatus.STARTED.toString());

        validateProperties("server-one", 8080, BY_TYPE);
        validateProperties("server-two", 8180, BY_TYPE);
        validateProperties("server-three", 8280, BY_TYPE);
    }

    private void validateProperties(String server, int port, String directoryGrouping) throws IOException {
        final Path serverHome = DomainTestSupport.getHostDir(JVMServerPropertiesTestCase.class.getSimpleName(), "primary").toPath();

        final Path serverBaseDir = serverHome.resolve("servers").resolve(server);
        final Path serverLogDir = BY_SERVER.equals(directoryGrouping) ? serverBaseDir.resolve("log") : serverHome.resolve("log").resolve("servers").resolve(server);
        final Path serverDataDir = BY_SERVER.equals(directoryGrouping) ? serverBaseDir.resolve("data") : serverHome.resolve("data").resolve("servers").resolve(server);
        final Path serverTmpDir = BY_SERVER.equals(directoryGrouping) ? serverBaseDir.resolve("tmp") : serverHome.resolve("tmp").resolve("servers").resolve(server);

        String response = performHttpCall(DomainTestSupport.primaryAddress, port, PROP_SERVLET_APP_URL);
        Properties p = new Properties();
        try (StringReader isr = new StringReader(response.replace("\\", "\\\\"))) {
            p.load(isr);
        }

        Assert.assertEquals(serverBaseDir.toAbsolutePath().toString(), p.getProperty("test.jboss.server.base.dir"));
        Assert.assertEquals(serverLogDir.toAbsolutePath().toString(), p.getProperty("test.jboss.server.log.dir"));
        Assert.assertEquals(serverDataDir.toAbsolutePath().toString(), p.getProperty("test.jboss.server.data.dir"));
        Assert.assertEquals(serverTmpDir.toAbsolutePath().toString(), p.getProperty("test.jboss.server.temp.dir"));
    }

    private static String performHttpCall(String host, int port, String context) throws IOException {
        URLConnection conn;
        InputStream in = null;
        BufferedReader input = null;
        InputStreamReader isr = null;
        try {
            URL url = new URL("http://" + TestSuiteEnvironment.formatPossibleIpv6Address(host) + ":" + port + "/" + context);
            conn = url.openConnection();
            conn.setDoInput(true);

            in = conn.getInputStream();
            isr = new InputStreamReader(in, StandardCharsets.UTF_8);
            input = new BufferedReader(isr);
            StringBuilder strBuilder = new StringBuilder();
            String str;
            while (null != (str = input.readLine())) {
                strBuilder.append(str).append("\r\n");
            }

            return strBuilder.toString();
        } finally {
            safeClose(input);
            safeClose(isr);
            safeClose(in);
        }
    }


    private ModelNode createDeploymentOperation(Path deployment, PathElement... serverGroups) throws MalformedURLException {
        ModelNode content = new ModelNode();
        content.get("url").set(deployment.toUri().toURL().toString());

        ModelNode op = Util.createEmptyOperation(COMPOSITE, PathAddress.EMPTY_ADDRESS);
        ModelNode steps = op.get(STEPS);
        ModelNode step1 = steps.add();
        step1.set(Util.createEmptyOperation(ADD, PathAddress.pathAddress(PathElement.pathElement(DEPLOYMENT, deployment.getFileName().toString()))));
        step1.get(CONTENT).add(content);
        for (PathElement serverGroup : serverGroups) {
            ModelNode sg = steps.add();
            sg.set(Util.createEmptyOperation(ADD, PathAddress.pathAddress(serverGroup, PathElement.pathElement(DEPLOYMENT, deployment.getFileName().toString()))));
            sg.get(ENABLED).set(true);
        }

        return op;
    }

    public static WebArchive createDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, PROP_SERVLET_APP_WAR);
        war.addClass(PropertiesServlet.class);
        war.addAsManifestResource(createPermissionsXmlAsset(
                new PropertyPermission("*", "read, write")
        ), "permissions.xml");

        return war;
    }
}
