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
