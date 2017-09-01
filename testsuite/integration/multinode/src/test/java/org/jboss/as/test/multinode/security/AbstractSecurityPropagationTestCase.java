/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.test.multinode.security;

import java.io.InputStream;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.multinode.security.api.EJBAction;
import org.jboss.as.test.multinode.security.api.EJBRequest;
import org.jboss.as.test.multinode.security.api.RemoteEJBConfig;
import org.jboss.as.test.multinode.security.api.Results;
import org.jboss.as.test.multinode.security.api.SecuritySLSBRemote;
import org.jboss.as.test.multinode.security.api.ServerConfigs;
import org.jboss.as.test.multinode.security.api.TestConfig;
import org.jboss.as.test.multinode.security.config.Deployments;
import org.jboss.as.test.multinode.security.util.EJBUtil;
import org.jboss.as.test.multinode.security.util.URLUtil;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author bmaxwell
 *
 */
public abstract class AbstractSecurityPropagationTestCase {

    protected static final Logger log = Logger.getLogger(AbstractSecurityPropagationTestCase.class);

    @ContainerResource("multinode-server")
    protected ManagementClient serverServer;

    @ContainerResource("multinode-client")
    protected ManagementClient clientServer;

    protected ServerConfigs servers;

    @Before
    public void init() {
        servers = new ServerConfigs(clientServer, TestConfig.MULTINODE_CLIENT, serverServer, TestConfig.MULTINODE_SERVER);
    }

    @Deployment(testable = false, name = "JAR1")
    @TargetsContainer(TestConfig.MULTINODE_SERVER)
    public static Archive<?> createEjbDeployment1() {
        return Deployments.createEjbDeployment(TestConfig.SECURITY_EJB1);
    }

    @Deployment(testable = false, name = "JAR2")
    @TargetsContainer(TestConfig.MULTINODE_CLIENT)
    public static Archive<?> createEjbDeployment2() {
        return Deployments.createEjbDeployment(TestConfig.SECURITY_EJB1);
    }

    @Deployment(testable = false, name = "WAR", order = 2, managed = true)
    @TargetsContainer("multinode-client")
    public static Archive<?> createWarDeploymentUsingRemoteConnections() {
        return Deployments.createWarDeployment("", false, TestConfig.SERVLET_1);
    }

    @Test
    public void testSecurityPropagationSecuredWebClientToRemoteEJB() throws Exception {
        // unit test (WEB_CREDENTIAL) -> servlet1 (Client Server) (NO Credential) -> ejb1 (Server Server)
        InputStream is = null;
        try {
            EJBRequest request = new EJBRequest("Standalone Client", TestConfig.WEB_CREDENTIAL.getUsername());

            RemoteEJBConfig remoteEJBConfig = new RemoteEJBConfig(servers.getServerServer(), TestConfig.CREDENTIALS_NONE);
            request.getActions().add(new EJBAction(remoteEJBConfig, TestConfig.SECURITY_EJB1));

            is = URLUtil.openConnectionWithBasicAuth(servers.getClientServer(), TestConfig.SERVLET_1.getServletSimpleName(), TestConfig.WEB_CREDENTIAL, request);

            String response = URLUtil.readInputStreamToString(is);
            Results results = Results.unmarshall(response);

            // fail if the web / ejb user are not correct
            results.failIfCallerIsNot(1, TestConfig.WEB_USERNAME);
            results.failIfEJBNodeNameIsNot(1, TestConfig.MULTINODE_CLIENT);

            results.failIfCallerIsNot(2, TestConfig.WEB_USERNAME);
            results.failIfEJBNodeNameIsNot(2, TestConfig.MULTINODE_SERVER);

        } finally {
            URLUtil.close(is);
        }
    }

    @Test
    public void testSecurityPropagationSecuredWebClientToRemoteEJBToEJB() throws Exception {
        // client (WEB CREDENTIAL) -> EJB1 (Server Server) (NO CREDENTIAL) -> EJB1 (Client Server)
        InputStream is = null;
        try {

            // create an ejb request for the servlet to call the remote ejbs
            EJBRequest ejbRequest = new EJBRequest("Standalone Client", TestConfig.WEB_CREDENTIAL.getUsername());

            // have servlet call EJB1 with no credentials on Server Server
            ejbRequest.getActions().add(new EJBAction(new RemoteEJBConfig(servers.getServerServer(), TestConfig.CREDENTIALS_NONE), TestConfig.SECURITY_EJB1));
            // have EJB1 call EJB1 with no credentials on Client Server
            ejbRequest.getActions().add(new EJBAction(new RemoteEJBConfig(servers.getClientServer(), TestConfig.CREDENTIALS_NONE), TestConfig.SECURITY_EJB1));

            // invoke the servlet on Client Server using Web Credential
            is = URLUtil.openConnectionWithBasicAuth(servers.getClientServer(), TestConfig.SERVLET_1.getServletSimpleName(), TestConfig.WEB_CREDENTIAL, ejbRequest);

            // send EJBRequest to servlet
            // have servlet add to invocation path for web user

            String response = URLUtil.readInputStreamToString(is);
            Results results = Results.unmarshall(response);

            // fail if the web / ejb user are not correct
            results.failIfCallerIsNot(1, TestConfig.WEB_USERNAME);
            results.failIfEJBNodeNameIsNot(1, TestConfig.MULTINODE_CLIENT);

            results.failIfCallerIsNot(2, TestConfig.WEB_USERNAME);
            results.failIfEJBNodeNameIsNot(2, TestConfig.MULTINODE_SERVER);

            results.failIfCallerIsNot(3, TestConfig.WEB_USERNAME);
            results.failIfEJBNodeNameIsNot(3, TestConfig.MULTINODE_CLIENT);
        } finally {
            URLUtil.close(is);
        }
    }


    @Test
    // client WEB User -> Servlet (Use EJB User) -> EJB
    public void testSecurityWebClientOverrideInflowedUserPassword() throws Exception {
        InputStream is = null;
        try {
            EJBRequest ejbRequest = new EJBRequest("Standalone Client", TestConfig.WEB_CREDENTIAL.getUsername());
            RemoteEJBConfig remoteEJBConfig = new RemoteEJBConfig(servers.getServerServer(), TestConfig.EJB_CREDENTIAL);
            ejbRequest.getActions().add(new EJBAction(remoteEJBConfig, TestConfig.SECURITY_EJB1));

            is = URLUtil.openConnectionWithBasicAuth(servers.getClientServer(), TestConfig.SERVLET_1.getServletSimpleName(), TestConfig.WEB_CREDENTIAL, ejbRequest);

            String response = URLUtil.readInputStreamToString(is);
            Results results = Results.unmarshall(response);

            // fail if the web / ejb user are not correct
            // path: 0 (servlet) , 1 (ejb)
            results.failIfCallerIsNot(1, TestConfig.WEB_USERNAME);
            results.failIfEJBNodeNameIsNot(1, TestConfig.MULTINODE_CLIENT);

            results.failIfCallerIsNot(2, TestConfig.EJB_USERNAME);
            results.failIfEJBNodeNameIsNot(2, TestConfig.MULTINODE_SERVER);
        } finally {
            URLUtil.close(is);
        }
    }

    @Test
    public void testStandaloneClient() throws Exception {
        // Standalone client -> EJB1 on server1
        RemoteEJBConfig config = new RemoteEJBConfig(servers.getClientServer(), TestConfig.EJB_CREDENTIAL);
        SecuritySLSBRemote remote = (SecuritySLSBRemote) EJBUtil.lookupEjb(config, TestConfig.SECURITY_EJB1);
        EJBRequest response = remote.invoke(new EJBRequest(this.getClass().getSimpleName(), "Standalone Client"));

        // throw if the user is not ejb user
        Assert.assertEquals(TestConfig.EJB_USERNAME, response.getInvocationPath().get(1).getCallerPrincipal());
        Assert.assertEquals(TestConfig.MULTINODE_CLIENT, response.getInvocationPath().get(1).getNodeName());

        // Standalone client -> EJB1 on server2
        config = new RemoteEJBConfig(servers.getServerServer(), TestConfig.EJB_CREDENTIAL);
        remote = (SecuritySLSBRemote) EJBUtil.lookupEjb(config, TestConfig.SECURITY_EJB1);
        response = remote.invoke(new EJBRequest(this.getClass().getSimpleName(), "Standalone Client"));

        // throw if the user is not ejb user
        Assert.assertEquals(TestConfig.EJB_USERNAME, response.getInvocationPath().get(1).getCallerPrincipal());
        Assert.assertEquals(TestConfig.MULTINODE_SERVER, response.getInvocationPath().get(1).getNodeName());
    }

    @Test
    // This seems odd since the client is specifying user/pass, it seems odd that the 2nd server is not just allowing it
    public void testStandaloneClientServerToServer() throws Throwable {
        // Standalone client -> EJB1 on server1 -> EJB1 on server2
        RemoteEJBConfig config = new RemoteEJBConfig(servers.getClientServer(), TestConfig.EJB_CREDENTIAL);
        SecuritySLSBRemote remote = (SecuritySLSBRemote) EJBUtil.lookupEjb(config, TestConfig.SECURITY_EJB1);

        EJBRequest request = new EJBRequest(this.getClass().getSimpleName(), "Standalone Client");
        RemoteEJBConfig config2 = new RemoteEJBConfig(servers.getServerServer(), TestConfig.EJB_CREDENTIAL);
        request.addAction(new EJBAction(config2, TestConfig.SECURITY_EJB1));
        EJBRequest response = remote.invoke(request);

        response.throwIfAnyExceptions();

        // throw if the user is not ejb user
        Assert.assertEquals(TestConfig.EJB_USERNAME, response.getInvocationPath().get(1).getCallerPrincipal());
        Assert.assertEquals(TestConfig.MULTINODE_CLIENT, response.getInvocationPath().get(1).getNodeName());

        Assert.assertEquals(TestConfig.EJB_USERNAME, response.getInvocationPath().get(2).getCallerPrincipal());
        Assert.assertEquals(TestConfig.MULTINODE_SERVER, response.getInvocationPath().get(2).getNodeName());
    }

    @Test
    // This test needs both servers configured to for forwarding of the identity
    public void testStandaloneClientServerToServerForwarding() throws Throwable {
        // Standalone client (using EJB_CREDENTIAL) -> EJB1 on server1 (NO CREDENTIALS) -> EJB1 on server2
        RemoteEJBConfig config = new RemoteEJBConfig(servers.getClientServer(), TestConfig.EJB_CREDENTIAL);
        SecuritySLSBRemote remote = (SecuritySLSBRemote) EJBUtil.lookupEjb(config, TestConfig.SECURITY_EJB1);

        EJBRequest request = new EJBRequest(this.getClass().getSimpleName(), "Standalone Client");
        RemoteEJBConfig config2 = new RemoteEJBConfig(servers.getServerServer(), TestConfig.CREDENTIALS_NONE);
        request.addAction(new EJBAction(config2, TestConfig.SECURITY_EJB1));
        EJBRequest response = remote.invoke(request);

        response.throwIfAnyExceptions();

        // throw if the user is not ejb user
        Assert.assertEquals(TestConfig.EJB_USERNAME, response.getInvocationPath().get(1).getCallerPrincipal());
        Assert.assertEquals(TestConfig.MULTINODE_CLIENT, response.getInvocationPath().get(1).getNodeName());

        Assert.assertEquals(TestConfig.EJB_USERNAME, response.getInvocationPath().get(2).getCallerPrincipal());
        Assert.assertEquals(TestConfig.MULTINODE_SERVER, response.getInvocationPath().get(2).getNodeName());
    }
}