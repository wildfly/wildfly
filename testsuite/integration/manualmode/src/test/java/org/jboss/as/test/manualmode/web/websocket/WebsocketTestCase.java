package org.jboss.as.test.manualmode.web.websocket;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author rhatlapa (rhatlapa@redhat.com)
 */
@RunWith(Arquillian.class)
@RunAsClient
public class WebsocketTestCase {

    private static Logger log = Logger.getLogger(WebsocketTestCase.class);
    private static final String DEPLOYMENT = "websocket";

    public static final String CONTAINER = "default-jbossas";
    private final ConnectorsServerSetup connectorsServerSetup = new ConnectorsServerSetup();

    @ArquillianResource
    private static ContainerController container;
    @ArquillianResource
    private Deployer deployer;

    @BeforeClass
    public static void beforeClass() {
        log.info("Trying to run websockets test on JDK " + getJavaVersion());
        Assume.assumeTrue("Websockets require JDK 1.7 and higher", getJavaVersion() > 1.6);
    }

    @Deployment(name = DEPLOYMENT, testable = false, managed = false)
    @TargetsContainer(CONTAINER)
    public static WebArchive deployment() {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        final String resourcesLocation = "org/jboss/as/test/manualmode/web/websocket/resources/";

        WebArchive war = ShrinkWrap.create(WebArchive.class, "websocket.war");
        war.addClasses(WebSocketServerEndpoint.class, WebSocketClient.class);
        war.addAsWebInfResource(tccl.getResource(resourcesLocation + "jboss-web.xml"), "jboss-web.xml");
        log.info(war.toString(true));
        return war;
    }

    @Test
    @InSequence(-1)
    public void prepareServer() throws Exception {
        container.start(CONTAINER);
        log.debug("Server started");
        final ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient();
        ManagementClient managementClient = new ManagementClient(client, TestSuiteEnvironment.getServerAddress(),
                TestSuiteEnvironment.getServerPort());
        connectorsServerSetup.setup(managementClient, CONTAINER);
        CliUtilsForConnectors.reload(managementClient.getControllerClient());
        deployer.deploy(DEPLOYMENT);
        log.debug("Deployed " + DEPLOYMENT);
    }


    /**
     * Testing that it is possible to establish websocket connection
     *
     * @param url
     * @throws Exception
     */
    @Test
    @InSequence(1)
    @OperateOnDeployment(DEPLOYMENT)
    public void websocketTest(@ArquillianResource URL url) throws Exception {
        WebSocketClient.latch = new CountDownLatch(1);
        final Session session = connectToServer(WebSocketClient.class, url);
        assertNotNull(session);
        final String testMessage = "some websocket test message";
        session.getBasicRemote().sendText(testMessage);
        assertTrue("Send message not received in 5 s", WebSocketClient.latch.await(5, TimeUnit.SECONDS));
        assertTrue("Clients didn't received specified message", WebSocketClient.history.contains("Reply to " + testMessage));
        session.close();
    }

    /**
     * Creates websocket connection using given endpoint and url
     */
    public Session connectToServer(Class<?> endpoint, URL base) throws DeploymentException, IOException, URISyntaxException {
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        URI uri = new URI("ws://"
                + base.getHost()
                + ":"
                + base.getPort()
                + base.getPath()
                + "websocket");
        return container.connectToServer(endpoint, uri);
    }


    @Test
    @InSequence(100)
    public void tearDownTheServer() {
        try {
            deployer.undeploy(DEPLOYMENT);
            log.info("Undeployed " + DEPLOYMENT);
        } finally {
            log.info("Restoring connector configuration");
            final ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient();
            ManagementClient managementClient = new ManagementClient(client, TestSuiteEnvironment.getServerAddress(),
                    TestSuiteEnvironment.getServerPort());
            container.stop(CONTAINER);
            log.info("Server stopped");
        }
    }


    static class ConnectorsServerSetup implements ServerSetupTask {
        private static final String HTTP_CONNECTOR_NAME = "http";
        private static final String HTTP_NIO_PROTOCOL = "org.apache.coyote.http11.Http11NioProtocol";

        private String originalProtocol = null;

        @Override
        public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
            originalProtocol = CliUtilsForConnectors.getConnectorProtocol(managementClient.getControllerClient(), HTTP_CONNECTOR_NAME);
            CliUtilsForConnectors.defineConnectorProtocol(managementClient.getControllerClient(), HTTP_CONNECTOR_NAME, HTTP_NIO_PROTOCOL);
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            if (originalProtocol != null) {
                CliUtilsForConnectors.defineConnectorProtocol(managementClient.getControllerClient(), HTTP_CONNECTOR_NAME, originalProtocol);
            }
        }
    }


    /**
     * @return as double java version (1.6 vs 1.7, ...)
     */
    public static double getJavaVersion() {
        String version = System.getProperty("java.version");
        int pos = 0, count = 0;
        for (; pos < version.length() && count < 2; pos++) {
            if (version.charAt(pos) == '.') count++;
        }
        return Double.parseDouble(version.substring(0, pos - 1));
    }


}
