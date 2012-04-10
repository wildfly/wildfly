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
package org.jboss.as.arquillian.container.domain.managed.test;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * For Domain server DeployableContianer implementations, the DeployableContainer will register 
 * all groups/individual servers it controls as Containers in Arquillians Registry during start.
 * 
 * @author <a href="mailto:aslak@redhat.com">Aslak Knutsen</a>
 * @version $Revision: $
 */
@RunWith(Arquillian.class)
public class ManagedDomainTestCase {

    @Deployment(name = "dep1") @TargetsContainer("backend")
    public static WebArchive create1() {
        return ShrinkWrap.create(WebArchive.class);
    }

    @Deployment(name = "dep2") @TargetsContainer("other-server-group")
    public static WebArchive create2() {
        return ShrinkWrap.create(WebArchive.class);
    }

    @ArquillianResource
    private ContainerController controller;
    
    /*
     * ProtocolMetaData returned by deploy to main-server-group contains multiple HttpContext 
     * named after the individual servers in the group. Adding @TargetsContainer specifies which 
     * server info to use (container names can be overwritten using containerNameMap in configuration)
     */
    @Test @InSequence(1) @OperateOnDeployment("dep1") @TargetsContainer("backend-1")
    public void shouldBeAbleToRunInTargetedServer() throws Exception {
        System.out.println("in..container");
    }

    @Test @InSequence(2)
    public void shouldBeAbleToStartServer() {
        // server-groups are registered as STARTED by default for managed deployments to work.. 
        // In the eyes of the Domain they are always 'started' it's the servers in their group that are started or stopped, 
        // but Arquillian see them as one Container and won't actually call start unless it is registered as stopped.... ??
        //controller.stop("other-server-group");
        //controller.start("other-server-group");
        controller.start("frontend-1");
    }

    @Test @InSequence(3) @OperateOnDeployment("dep2")
    public void shouldBeAbleToRunInUndefinedServer() throws Exception {
        System.out.println("in..container 2");
    }

    @Test @InSequence(4)
    public void shouldBeAbleToStopServer() {
        controller.stop("frontend-1");
    }
}
