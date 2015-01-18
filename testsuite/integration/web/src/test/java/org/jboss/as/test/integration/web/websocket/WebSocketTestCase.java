package org.jboss.as.test.integration.web.websocket;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

import java.io.FilePermission;
import java.net.SocketPermission;
import java.net.URI;
import java.util.PropertyPermission;

import javax.websocket.ContainerProvider;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class WebSocketTestCase {

    @Deployment
    public static WebArchive deploy() {
        final String jbossHome = System.getenv("JBOSS_HOME");
        return ShrinkWrap.create(WebArchive.class, "websocket.war")
                .addPackage(WebSocketTestCase.class.getPackage())
                .addClass(TestSuiteEnvironment.class)
                .addAsManifestResource(
                        createPermissionsXmlAsset(
                                // Needed for the TestSuiteEnvironment.getServerAddress()
                                new PropertyPermission("management.address", "read"),
                                new PropertyPermission("node0", "read"),
                                // Needed for the serverContainer.connectToServer()
                                new SocketPermission("*:8080", "connect,resolve")
                        ), "permissions.xml")
                .addAsManifestResource(new StringAsset("io.undertow.websockets.jsr.UndertowContainerProvider"),
                        "services/javax.websocket.ContainerProvider");
    }

    @Test
    public void testWebSocket() throws Exception {
        AnnotatedClient endpoint = new AnnotatedClient();
        WebSocketContainer serverContainer = ContainerProvider.getWebSocketContainer();
        Session session = serverContainer.connectToServer(endpoint, new URI("ws", "", TestSuiteEnvironment.getServerAddress(), 8080, "/websocket/websocket/Stuart", "", ""));
        Assert.assertEquals("Hello Stuart", endpoint.getMessage());
    }
}
