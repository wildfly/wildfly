/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.lifecycle.servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Matus Abaffy
 */
@RunAsClient
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ExtendWith(ArquillianExtension.class)
public class ListenerLifecycleCallbackInterceptionTestCase extends LifecycleInterceptionTestCase {

    @Deployment(name = REMOTE, managed = false, testable = false)
    public static WebArchive createRemoteTestArchive() {
        return createRemoteTestArchiveBase().addClasses(RemoteListener.class, RemoteListenerServlet.class);
    }

    @Deployment(testable = false)
    public static WebArchive createMainTestArchive() {
        return createMainTestArchiveBase();
    }

    /**
     * This is not a real test method.
     */
    @Test
    @Order(1)
    public void deployRemoteArchive() {
        // In order to use @ArquillianResource URL from the unmanaged deployment we need to deploy the test archive first
        deployer.deploy(REMOTE);
    }

    @Test
    @Order(2)
    public void testListenerPostConstructInterception(
            @ArquillianResource @OperateOnDeployment(REMOTE) URL remoteContextPath) throws IOException,
            ExecutionException, TimeoutException {

        assertEquals("1", doGetRequest(remoteContextPath + "/RemoteListenerServlet?event=postConstructVerify"),
                "PostConstruct interceptor method not invoked for listener");
    }

    @Test
    @Order(3)
    public void testListenerPreDestroyInterception(
            @ArquillianResource(InitServlet.class) @OperateOnDeployment(REMOTE) URL remoteContextPath) throws IOException,
            ExecutionException, TimeoutException {

        // set the context in InfoClient so that it can send request to InfoServlet
        doGetRequest(remoteContextPath + "/InitServlet?url=" + URLEncoder.encode(infoContextPath.toExternalForm(), "UTF-8"));

        deployer.undeploy(REMOTE);
        assertEquals("1", doGetRequest(infoContextPath + "/InfoServlet?event=preDestroyVerify"),
                "PreDestroy interceptor method not invoked for listener");
    }
}
