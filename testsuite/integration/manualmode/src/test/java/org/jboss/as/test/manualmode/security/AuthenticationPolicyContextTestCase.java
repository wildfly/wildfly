/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.test.manualmode.security;

import org.apache.commons.io.IOUtils;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.network.NetworkUtils;
import org.jboss.as.test.integration.management.util.CLIOpResult;
import org.jboss.as.test.integration.management.util.CLITestUtil;
import org.jboss.as.test.integration.management.util.ServerReload;
import org.jboss.as.test.manualmode.security.resources.EchoService;
import org.jboss.as.test.manualmode.security.resources.EchoServiceRemote;
import org.jboss.as.test.manualmode.security.resources.PicketLinkSTSService;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.ws.api.configuration.ClientConfigUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.picketlink.common.exceptions.fed.WSTrustException;
import org.picketlink.identity.federation.api.wstrust.WSTrustClient;
import org.picketlink.identity.federation.core.saml.v2.util.DocumentUtil;
import org.picketlink.identity.federation.core.wstrust.plugins.saml.SAMLUtil;
import org.picketlink.trust.jbossws.SAML2Constants;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.fail;

/**
 * Before the fix web service with STS (Picketlink) returned wrong subject with PolicyContext("javax.security.auth.subject.container").
 * This test calls SimpleSecurityManager and checks if no exception is thrown and PolicyContext.getContext("javax.security.auth.Subject.container") should not be null.
 * Test for [ WFLY-8946 ].
 *
 * @author Daniel Cihak
 */
@RunWith(Arquillian.class)
public class AuthenticationPolicyContextTestCase {

    private static Logger LOGGER = Logger.getLogger(AuthenticationPolicyContextTestCase.class);

    private static final String PICKETLINK_STS = "picketlink-sts";
    private static final String PICKETLINK_STS_WS = "picketlink-sts-ws";
    private static final String RESOURCE_DIR = "org/jboss/as/test/manualmode/security/resources/";

    private static final String HOST = TestSuiteEnvironment.getServerAddress();
    private static final int PORT_OFFSET = 0;
    private static final String USERNAME = "UserA";
    private static final String PASSWORD = "PassA";
    private static final String CONTAINER = "default-jbossas";

    private volatile ModelControllerClient client;
    private volatile CommandContext commandCtx;
    private volatile ByteArrayOutputStream consoleOut = new ByteArrayOutputStream();

    @ArquillianResource
    private static ContainerController container;

    @ArquillianResource
    private static volatile Deployer deployer;

    @Deployment(name = PICKETLINK_STS, managed = false, testable = false)
    @TargetsContainer(CONTAINER)
    public static Archive<?> createPicketlinkStsDeployment() {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        WebArchive war = ShrinkWrap.create(WebArchive.class, PICKETLINK_STS + ".war");
        war.addClass(PicketLinkSTSService.class);
        war.addAsResource(tccl.getResource( RESOURCE_DIR + "sts-users.properties"), "sts-users.properties");
        war.addAsResource(tccl.getResource( RESOURCE_DIR + "sts-roles.properties"), "sts-roles.properties");
        war.addAsWebInfResource(tccl.getResource( RESOURCE_DIR + "WEB-INF/sts_keystore.jks"), "classes/sts_keystore.jks");
        war.addAsWebInfResource(createFilteredAsset("resources/WEB-INF/picketlink-sts.xml"), "classes/picketlink-sts.xml");
        war.addAsWebInfResource(createFilteredAsset("resources/WEB-INF/wsdl/PicketLinkSTS.wsdl"), "wsdl/PicketLinkSTS.wsdl");
        war.addAsWebInfResource(tccl.getResource( RESOURCE_DIR + "WEB-INF/beans.xml"), "beans.xml");
        war.addAsWebInfResource(tccl.getResource( RESOURCE_DIR + "WEB-INF/jboss-web.xml"), "jboss-web.xml");
        war.addAsWebInfResource(tccl.getResource( RESOURCE_DIR + "WEB-INF/jboss-wsse-server.xml"), "jboss-wsse-server.xml");
        war.addAsWebInfResource(tccl.getResource( RESOURCE_DIR + "WEB-INF/web.xml"), "web.xml");
        war.addAsManifestResource(tccl.getResource(RESOURCE_DIR + "META-INF/jboss-deployment-structure.xml"), "jboss-deployment-structure.xml");
        war.addAsManifestResource(tccl.getResource(RESOURCE_DIR + "META-INF/jboss-webservices.xml"), "jboss-webservices.xml");
        return war;
    }

    @Deployment(name = PICKETLINK_STS_WS, managed = false, testable = false)
    @TargetsContainer(CONTAINER)
    public static Archive<?> createPicketlinkStsWsDeployment() {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        WebArchive war = ShrinkWrap.create(WebArchive.class, PICKETLINK_STS_WS + ".war");
        war.addClasses(EchoServiceRemote.class);
        war.addClasses(EchoService.class);
        war.addAsResource(tccl.getResource( RESOURCE_DIR + "sp-users.properties"), "sp-users.properties");
        war.addAsResource(tccl.getResource( RESOURCE_DIR + "sp-roles.properties"), "sp-roles.properties");
        war.addAsResource(tccl.getResource( RESOURCE_DIR + "sts-config.properties"), "sts-config.properties");
        war.addAsWebInfResource(createFilteredAsset("resources/WEB-INF/wsdl/PicketLinkSTS.wsdl"), "wsdl/PicketLinkSTS.wsdl");
        war.addAsWebInfResource(tccl.getResource( RESOURCE_DIR + "WEB-INF/beans.xml"), "beans.xml");
        war.addAsWebInfResource(tccl.getResource( RESOURCE_DIR + "WEB-INF/jboss-web-ws.xml"), "jboss-web-ws.xml");
        war.addAsWebInfResource(tccl.getResource( RESOURCE_DIR + "WEB-INF/jboss-wsse-server.xml"), "jboss-wsse-server.xml");
        war.addAsManifestResource(tccl.getResource(RESOURCE_DIR + "META-INF/jboss-deployment-structure.xml"), "jboss-deployment-structure.xml");
        war.addAsManifestResource(tccl.getResource(RESOURCE_DIR + "META-INF/jboss-webservices.xml"), "jboss-webservices.xml");
        war.addAsResource(AuthenticationPolicyContextTestCase.class.getPackage(), "dummmy-ws-handler.xml", "org/jboss/as/test/manualmode/security/resources/dummmy-ws-handler.xml");
        return war;
    }

    private static StringAsset createFilteredAsset(String resourceName) {
        return new StringAsset(replaceNodeAddress(resourceName));
    }

    private static String replaceNodeAddress(String resourceName) {
        String content = null;
        try {
            content = IOUtils.toString(AuthenticationPolicyContextTestCase.class.getResourceAsStream(resourceName), "UTF-8");
        } catch (IOException e) {
            throw new RuntimeException("Exception during replacing node address in resource", e);
        }
        return content.replaceAll("@node0@", NetworkUtils.formatPossibleIpv6Address(System.getProperty("node0", "127.0.0.1")));
    }

    @Before
    public void before() {
        container.start(CONTAINER);
        try {
            initServerConfiguration(PICKETLINK_STS, PICKETLINK_STS_WS);
        } catch (Throwable t) {
            LOGGER.error("Server configuration initialization failed.", t.getMessage(), t);
            fail("Throwable:" + t.getMessage());
        }
    }

    @After
    public void after() throws IOException {
        deployer.undeploy(PICKETLINK_STS);
        deployer.undeploy(PICKETLINK_STS_WS);
        container.stop(CONTAINER);
    }

    private void initServerConfiguration(String deployment1, String deployment2) throws Exception {
        client = ModelControllerClient.Factory.create(HOST, getManagementPort());
        commandCtx = CLITestUtil.getCommandContext(HOST, getManagementPort(), null, consoleOut, -1);
        commandCtx.connectController();

        File cliFile = File.createTempFile("add-security-domain-", ".cli");
        try (FileOutputStream fos = new FileOutputStream(cliFile)) {
            IOUtils.copy(AuthenticationPolicyContextTestCase.class.getResourceAsStream("add-security-domain.cli"), fos);
        }
        runBatch(cliFile);
        cliFile.delete();
        reload();

        cliFile = File.createTempFile("add-jaxws-endpoint-", ".cli");
        try (FileOutputStream fos = new FileOutputStream(cliFile)) {
            IOUtils.copy(AuthenticationPolicyContextTestCase.class.getResourceAsStream("add-jaxws-endpoint.cli"), fos);
        }
        runBatch(cliFile);
        cliFile.delete();
        reload();

        deployer.deploy(deployment1);
        deployer.deploy(deployment2);
    }

    /**
     * Sends command line to CLI.
     *
     * @param line specifies the command line.
     * @param ignoreError if set to false, asserts that handling the line did not result in a
     *        {@link org.jboss.as.cli.CommandLineException}.
     *
     * @return true if the CLI is in a non-error state following handling the line
     */
    public boolean sendLine(String line, boolean ignoreError) {
        consoleOut.reset();
        if (ignoreError) {
            commandCtx.handleSafe(line);
            return commandCtx.getExitCode() == 0;
        } else {
            try {
                commandCtx.handle(line);
            } catch (CommandLineException e) {
                StringWriter stackTrace = new StringWriter();
                e.printStackTrace(new PrintWriter(stackTrace));
                fail(String.format("Failed to execute line '%s'%n%s", line, stackTrace.toString()));
            }
        }
        return true;
    }

    private boolean runBatch(File batchFile) throws Exception {
        sendLine("run-batch --file=\"" + batchFile.getAbsolutePath() + "\" --headers={allow-resource-service-restart=true} -v", false);
        return new CLIOpResult(ModelNode.fromStream(new ByteArrayInputStream(consoleOut.toByteArray())))
                .isIsOutcomeSuccess();
    }

    private void reload() {
        ModelNode operation = Util.createOperation("reload", null);
        ServerReload.executeReloadAndWaitForCompletion(client, operation, (int) SECONDS.toMillis(90), HOST,
                getManagementPort());
    }

    public int getManagementPort() {
        return 9990 + PORT_OFFSET;
    }

    /**
     * Test gets SAML assertion by token using the web service in deployment picketlink-sts.war.
     * Afterwards web service EchoService from the deployment picketlink-sts-ws.war is called using role testRole and
     * security domain sp created during test initialization.
     *
     * @throws Exception
     */
    @Test
    @RunAsClient
    public void test() throws Exception {
        WSTrustClient client = new WSTrustClient("PicketLinkSTS", "PicketLinkSTSPort",
                "http://127.0.0.1:8080/picketlink-sts/PicketLinkSTS", new WSTrustClient.SecurityInfo(USERNAME, PASSWORD));
        Element assertion = null;
        try {
            LOGGER.info("Invoking token service to get SAML assertion for " + USERNAME);
            assertion = client.issueToken(SAMLUtil.SAML2_TOKEN_TYPE);
            String domElementAsString = DocumentUtil.getDOMElementAsString(assertion);
            System.out.println("assertion: " + domElementAsString);
            LOGGER.info("SAML assertion for " + USERNAME + " successfully obtained!");
        } catch (WSTrustException wse) {
            LOGGER.error("Unable to issue assertion: " + wse.getMessage());
            wse.printStackTrace();
            System.exit(1);
        }

        URL wsdl = new URL("http://localhost:8080/picketlink-sts-ws/EchoService?wsdl");
        QName serviceName = new QName("http://ws.picketlink.sts.jboss.org/", "EchoServiceService");
        Service service = Service.create(wsdl, serviceName);
        EchoServiceRemote port = service.getPort(new QName("http://ws.picketlink.sts.jboss.org/", "EchoServicePort"),
                EchoServiceRemote.class);

        BindingProvider bp = (BindingProvider) port;
        ClientConfigUtil.setConfigHandlers(bp, "standard-jaxws-client-config.xml", "SAML WSSecurity Client");
        bp.getRequestContext().put(SAML2Constants.SAML2_ASSERTION_PROPERTY, assertion);

        try {
            port.echo("Test");
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            fail(e.getMessage());
        }
    }
}
