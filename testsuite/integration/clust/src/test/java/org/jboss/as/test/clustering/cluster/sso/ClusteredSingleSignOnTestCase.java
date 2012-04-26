/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.clustering.cluster.sso;

import java.net.URL;
import org.jboss.arquillian.container.test.api.*;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.clustering.NodeUtil;
import org.jboss.as.test.integration.web.sso.SSOBaseCase;
import org.jboss.as.test.integration.web.sso.SSOTestBase;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.junit.Test;
import static org.jboss.as.test.clustering.ClusteringTestConstants.*;
import org.jboss.as.test.clustering.single.web.SimpleServlet;
import org.jboss.as.test.integration.web.sso.LogoutServlet;
import org.junit.runner.RunWith;

/**
 *
 * @author Dominik Pospisil <dpospisi@redhat.com>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ClusteredSingleSignOnTestCase {
    
    @ArquillianResource
    private ContainerController controller;
    @ArquillianResource
    private Deployer deployer;
    
    private static Logger log = Logger.getLogger(ClusteredSingleSignOnTestCase.class);
    
    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(CONTAINER_1)
    public static EnterpriseArchive deployment1() {
        return SSOTestBase.createSsoEar();
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(CONTAINER_2)
    public static EnterpriseArchive deployment2() {
        return SSOTestBase.createSsoEar();
    }
    
    @Test
    @InSequence(-2)
    public void startServers() throws Exception {
        
        controller.start(CONTAINER_1);
        deployer.deploy(DEPLOYMENT_1);
        controller.start(CONTAINER_2);
        deployer.deploy(DEPLOYMENT_2);        
   
    }

    @Test
    @InSequence(-1)
    public void setupSSO(
            @ArquillianResource @OperateOnDeployment(DEPLOYMENT_1) ManagementClient client1,
            @ArquillianResource @OperateOnDeployment(DEPLOYMENT_2) ManagementClient client2) throws Exception {
        
        // add sso valves
        SSOTestBase.addClusteredSso(client1.getControllerClient());
        SSOTestBase.restartServer(client1.getControllerClient());
        SSOTestBase.addClusteredSso(client2.getControllerClient());
        SSOTestBase.restartServer(client2.getControllerClient());             
    }
            
    @Test
    @InSequence(1)
    public void stopServers() throws Exception {
        deployer.undeploy(DEPLOYMENT_1);
        controller.stop(CONTAINER_1);
        deployer.undeploy(DEPLOYMENT_2);
        controller.stop(CONTAINER_2);
    }
    
    
    /**
     * Test single sign-on across two web apps using form based auth
     */
    @Test
    public void testFormAuthSingleSignOn(
            @ArquillianResource(LogoutServlet.class) @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1,
            @ArquillianResource(LogoutServlet.class) @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2) throws Exception {
        log.info("+++ testFormAuthSingleSignOn");
        SSOBaseCase.executeFormAuthSingleSignOnTest(new URL(baseURL1, "/"), new URL(baseURL2, "/"),  log);
    }

    /**
     * Test single sign-on across two web apps using form based auth
     */
    @Test
    public void testNoAuthSingleSignOn(
            @ArquillianResource(LogoutServlet.class) @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1,
            @ArquillianResource(LogoutServlet.class) @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2) throws Exception {
        log.info("+++ testNoAuthSingleSignOn");
        SSOBaseCase.executeNoAuthSingleSignOnTest(baseURL1, baseURL2, log);
    }
    
}
