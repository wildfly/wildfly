/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.lifecycle.servlet;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Matus Abaffy
 */
@RunAsClient
@RunWith(Arquillian.class)
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
    @InSequence(1)
    public void deployRemoteArchive() {
        // In order to use @ArquillianResource URL from the unmanaged deployment we need to deploy the test archive first
        deployer.deploy(REMOTE);
    }

    @Test
    @InSequence(2)
    public void testListenerPostConstructInterception(
            @ArquillianResource @OperateOnDeployment(REMOTE) URL remoteContextPath) throws IOException,
            ExecutionException, TimeoutException {

        assertEquals("PostConstruct interceptor method not invoked for listener", "1",
                doGetRequest(remoteContextPath + "/RemoteListenerServlet?event=postConstructVerify"));
    }

    @Test
    @InSequence(3)
    public void testListenerPreDestroyInterception(
            @ArquillianResource(InitServlet.class) @OperateOnDeployment(REMOTE) URL remoteContextPath) throws IOException,
            ExecutionException, TimeoutException {

        // set the context in InfoClient so that it can send request to InfoServlet
        doGetRequest(remoteContextPath + "/InitServlet?url=" + URLEncoder.encode(infoContextPath.toExternalForm(), "UTF-8"));

        deployer.undeploy(REMOTE);
        assertEquals("PreDestroy interceptor method not invoked for listener", "1",
                doGetRequest(infoContextPath + "/InfoServlet?event=preDestroyVerify"));
    }
}
