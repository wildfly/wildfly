/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.remotelookup;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.SocketPermission;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ondrej Chaloupka
 */
@ExtendWith(ArquillianExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LookupTestCase {

    @ContainerResource
    private Context remoteContext;

    @Deployment(name = "test")
    public static Archive<?> deployment() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "deploy.jar");
        jar.addClass(StatelessBean.class);
        jar.addAsManifestResource(createPermissionsXmlAsset(
                    new SocketPermission(TestSuiteEnvironment.getHttpAddress() + ":" + TestSuiteEnvironment.getHttpPort(), "resolve")
                ), "permissions.xml");
        return jar;
    }

    @Test
    @OperateOnDeployment("test")
    @Order(1)
    public void testServerLocalLookup() throws Exception {
        InitialContext context = new InitialContext();

        lookupConnectionFactory(context, "java:jboss/exported/jms/RemoteConnectionFactory");

        context.close();
    }

    @Test
    @RunAsClient
    @Order(2)
    public void testClientRemoteLookup() throws Exception {
        lookupConnectionFactory(remoteContext, "jms/RemoteConnectionFactory");
    }

    private void lookupConnectionFactory(Context context, String name) throws Exception {
        ConnectionFactory cf = (ConnectionFactory) context.lookup(name);
        assertNotNull(cf);
        //"java.net.SocketPermission" "localhost" "resolve"
        Connection conn = cf.createConnection("guest", "guest");
        conn.close();
    }
}
