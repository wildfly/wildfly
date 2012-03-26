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
package org.jboss.as.arquillian.container.domain.remote.test;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
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
public class RemoteDomainTestCase {

    @Deployment(name = "dep1") @TargetsContainer("main-server-group") 
    public static JavaArchive create1() {
        return ShrinkWrap.create(JavaArchive.class);
    }

    @Test @InSequence(1) @OperateOnDeployment("dep1") @TargetsContainer("master:server-one")
    public void shouldRunInContainer1() throws Exception {
        System.out.println("in..container");
    }

    @Test @InSequence(2) @OperateOnDeployment("dep1") @TargetsContainer("master:server-one")
    public void shouldStartContainer(@ArquillianResource ContainerController controller) throws Exception {
        Assert.assertFalse(controller.isStarted("master:server-two"));
        controller.start("master:server-two");
    }

    @Test @InSequence(3) @OperateOnDeployment("dep1") @TargetsContainer("master:server-two")
    public void shouldRunInContainer2(@ArquillianResource ContainerController controller) throws Exception {
        Assert.assertTrue(controller.isStarted("master:server-two"));
    }

    @Test @InSequence(4) @RunAsClient
    public void shouldBeAbleToStop(@ArquillianResource ContainerController controller) throws Exception {
        controller.stop("master:server-two");
        Assert.assertFalse(controller.isStarted("master:server-two"));
    }
}
