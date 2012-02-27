/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.stateful.persistencecontext;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

import java.net.UnknownHostException;

import javax.naming.InitialContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.shared.TestUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Extended persistent context passivated in SFSB. 
 * Part of the migration of tests from EJB3 testsuite to AS7 testsuite [JBQA-5483].
 * 
 * @author Bill Burke, Ondrej Chaloupka
 */

@RunWith(Arquillian.class)
public class StatefulUnitTestCase {
    private static final Logger log = Logger.getLogger(StatefulUnitTestCase.class);
    private static int TIME_TO_WAIT_FOR_PASSIVATION_MS = 2000;

    @ArquillianResource
    InitialContext ctx;

    static boolean deployed = false;
    static int test = 0;
    
    private static ModelControllerClient client;

    @Deployment
    public static Archive<?> deploy() throws Exception {
        setPassivationAttributes();
        
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "persistentcontext-test.jar");
        jar.addPackage(StatefulUnitTestCase.class.getPackage());
        jar.addAsManifestResource(StatefulUnitTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
        jar.addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller-client, org.jboss.dmr, org.jboss.marshalling \n"), "MANIFEST.MF");
        log.info(jar.toString(true));
        return jar;
    }
       
    private static ModelNode getAddress() {
        ModelNode address = new ModelNode();
        address.add("subsystem", "ejb3");
        address.add("file-passivation-store", "file");
        address.protect();
        return address;
    }
    
    private static ModelControllerClient getModelControllerClient() throws UnknownHostException {
        if (client == null) {
            client = TestUtils.getModelControllerClient();
        }
        return client;
    }
    
    private static void setPassivationAttributes() throws Exception {
        ModelNode address = getAddress();
        
        ModelNode operation = new ModelNode();
        operation.get(OP).set("write-attribute");
        operation.get(OP_ADDR).set(address);
        operation.get("name").set("idle-timeout");
        operation.get("value").set(1);
        ModelNode result = getModelControllerClient().execute(operation);
        log.info("modelnode operation write-attribute idle-timeout=1: " + result);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
    }
    
    @AfterClass
    public static void unsetPassivationAttributes() throws Exception {
        ModelNode address = getAddress();
        
        ModelNode operation = new ModelNode();
        operation.get(OP).set("undefine-attribute");
        operation.get(OP_ADDR).set(address);
        operation.get("name").set("idle-timeout");
        ModelNode result = getModelControllerClient().execute(operation);
        getModelControllerClient().close();
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
    }

    @Test
    public void testStateful() throws Exception {
        StatefulRemote remote = (StatefulRemote) ctx.lookup("java:module/" + StatefulBean.class.getSimpleName() + "!"
                + StatefulRemote.class.getName());
        System.out.println("Before DOIT testStateful");
        int id = remote.doit();
        System.out.println("After DOIT testStateful");
        Thread.sleep(TIME_TO_WAIT_FOR_PASSIVATION_MS);
        remote.find(id);
        System.out.println("After find testStateful");
    }

    @Test
    public void testTransientStateful() throws Exception {
        StatefulRemote remote = (StatefulRemote) ctx.lookup("java:module/" + StatefulTransientBean.class.getSimpleName()
                + "!" + StatefulRemote.class.getName());
        int id = remote.doit();
        Thread.sleep(TIME_TO_WAIT_FOR_PASSIVATION_MS);
        remote.find(id);
    }

    @Test
    public void testNonExtended() throws Exception {
        StatefulRemote remote = (StatefulRemote) ctx.lookup("java:module/" + NonExtendedStatefuBean.class.getSimpleName()
                + "!" + StatefulRemote.class.getName());
        int id = remote.doit();
        Thread.sleep(TIME_TO_WAIT_FOR_PASSIVATION_MS);
        remote.find(id);
    }
}
