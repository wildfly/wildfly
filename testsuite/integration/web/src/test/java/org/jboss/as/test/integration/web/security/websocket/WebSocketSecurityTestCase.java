/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.web.security.websocket;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

import java.net.SocketPermission;
import java.net.URI;
import java.util.PropertyPermission;

import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.WebSocketContainer;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.security.WebSecurityPasswordBasedBase;
import org.jboss.as.test.integration.web.security.WebTestsSecurityDomainSetup;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.runner.RunWith;

/**
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
@ServerSetup(WebTestsSecurityDomainSetup.class)
@RunAsClient
public class WebSocketSecurityTestCase extends WebSecurityPasswordBasedBase {

    @Deployment
    public static WebArchive deploy() {
        return ShrinkWrap.create(WebArchive.class, "websocket.war")
                .addPackage(WebSocketSecurityTestCase.class.getPackage())
                .addAsWebInfResource(WebSocketSecurityTestCase.class.getPackage(), "jboss-web.xml", "jboss-web.xml")
                .addAsWebInfResource(WebSocketSecurityTestCase.class.getPackage(), "web.xml", "web.xml")
                .addClass(TestSuiteEnvironment.class)
                .addAsManifestResource(
                        createPermissionsXmlAsset(
                                // Needed for the TestSuiteEnvironment.getServerAddress()
                                new PropertyPermission("management.address", "read"),
                                new PropertyPermission("node0", "read"),
                                new PropertyPermission("jboss.http.port", "read"),
                                // Needed for the serverContainer.connectToServer()
                                new SocketPermission("*:" + TestSuiteEnvironment.getHttpPort(), "connect,resolve")
                        ), "permissions.xml")
                .addAsManifestResource(new StringAsset("io.undertow.websockets.jsr.UndertowContainerProvider"),
                        "services/javax.websocket.ContainerProvider");
    }

    @Override
    protected void makeCall(final String user, final String pass, final int expectedCode) throws Exception {
        AnnotatedClient endpoint = new AnnotatedClient();
        endpoint.setCredentials(user, pass);
        WebSocketContainer serverContainer = ContainerProvider.getWebSocketContainer();

        if (expectedCode == 200) {
            connectToServer(serverContainer, endpoint);
            Assert.assertEquals("Hello anil", endpoint.getMessage());
        } else {
            boolean exceptionThrown = false;
            try {
                connectToServer(serverContainer, endpoint);
            } catch (DeploymentException e) {
                exceptionThrown = true;
            } finally {
                Assert.assertTrue("We expected that 'DeploymentException' is thrown as we provided incorrect " +
                        "credentials to ws endpoint.", exceptionThrown);
            }
        }
    }

    private void connectToServer(WebSocketContainer serverContainer, AnnotatedClient endpoint) throws Exception {
        serverContainer.connectToServer(endpoint, new URI("ws", "", TestSuiteEnvironment.getServerAddress(),
                TestSuiteEnvironment.getHttpPort(), "/websocket/websocket", "", ""));
    }
}
