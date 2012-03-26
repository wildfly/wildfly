/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.arquillian.container.domain;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import org.jboss.arquillian.container.spi.client.protocol.metadata.HTTPContext;
import org.jboss.as.arquillian.container.domain.Domain.Server;
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * ManagementClientTestCase
 *
 * @author <a href="mailto:aslak@redhat.com">Aslak Knutsen</a>
 * @version $Revision: $
 */
public class ManagementClientManualTestCase {

    private ManagementClient client;
    private ArchiveDeployer deployer;

    @Test
    public void shouldBeAbleToGetDomain() throws Exception {

        Map<String, String> rename = new HashMap<String, String>();
        rename.put("master:server-one", "backend-1");
        rename.put("master:server-two", "backend-2");
        rename.put("master:server-three", "front-1");
        rename.put("main-server-group", "backend");
        rename.put("other-server-group", "front");
        
        System.out.println("Domain");
        Domain domain = client.createDomain(rename);
        for(Server server: domain.getServers()) {
            System.out.println(server);
            
            System.out.println("Started: " + client.isServerStarted(server));
        }
        
        System.out.println(domain.getHosts());
        System.out.println(domain.getServerGroups());
        
    }


    @Test
    public void shouldBeAbleToReadMetadata() throws Exception {
        String uniqueName = null; 
        try {
            uniqueName = deployer.deploy(
                    ShrinkWrap.create(EnterpriseArchive.class, "test1.ear")
                        .addAsModule(
                                ShrinkWrap.create(WebArchive.class)
                                    .addClass(ManualTestServlet.class)),  
                    "main-server-group");
            
            HTTPContext context = client.getHTTPDeploymentMetaData(
                    new Server("server-one", "master", "main-server-group", false), 
                    "test1.ear");
            
            System.out.println(context);
        }
        finally {
            if(uniqueName != null) {
                deployer.undeploy(uniqueName, "main-server-group");
            }
        }
    }
    
    //@Test
    public void shouldbeAbleToStopAndStartServerGroup() throws Exception
    {
        client.stopServerGroup("main-server-group");
        
        Thread.sleep(5000);
        
        client.startServerGroup("main-server-group");
    }
    
    @Test
    public void shouldbeAbleToStpAndStartServer() throws Exception
    {
        Server server = new Server("server-one", "master", "main-server-group", false);
        client.stopServer(server);
        System.out.println("stopped");
        
        Thread.sleep(5000);
        
        client.startServer(server);
    }

    @Before
    public void createClient() throws Exception {
        InetAddress address = InetAddress.getLocalHost();
        int port = 9999;
        
        DomainClient domainClient = DomainClient.Factory.create(
                address,
                port,
                Authentication.getCallbackHandler());

        client = new ManagementClient(
                domainClient, 
                address.getHostAddress(), 
                port);
        
        deployer = new ArchiveDeployer(domainClient.getDeploymentManager());
    }
    
    @After
    public void closeClient() throws Exception {
        if(client != null) {
            client.close();
        }
    }
}
