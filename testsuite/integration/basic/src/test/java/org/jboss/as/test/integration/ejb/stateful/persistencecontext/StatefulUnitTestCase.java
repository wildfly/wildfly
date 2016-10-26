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

import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

/**
 * Extended persistent context passivated in SFSB.
 * Part of the migration of tests from EJB3 testsuite to AS7 testsuite [JBQA-5483].
 *
 * @author Bill Burke, Ondrej Chaloupka
 */

@RunWith(Arquillian.class)
@ServerSetup(StatefulUnitTestCase.StatefulUnitTestCaseSetup.class)
public class StatefulUnitTestCase {
    private static final Logger log = Logger.getLogger(StatefulUnitTestCase.class);
    private static int TIME_TO_WAIT_FOR_PASSIVATION_MS = 1000;

    @ArquillianResource
    InitialContext ctx;

    static boolean deployed = false;
    static int test = 0;

    static class StatefulUnitTestCaseSetup implements ServerSetupTask {

        @Override
        public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
            ModelNode address = getAddress();

            ModelNode operation = new ModelNode();
            operation.get(OP).set("write-attribute");
            operation.get(OP_ADDR).set(address);
            operation.get("name").set("max-size");
            operation.get("value").set(1);
            ModelNode result = managementClient.getControllerClient().execute(operation);
            log.trace("modelnode operation write-attribute max-size=0: " + result);
            Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            ModelNode address = getAddress();
            ModelNode operation = new ModelNode();
            operation.get(OP).set("undefine-attribute");
            operation.get(OP_ADDR).set(address);
            operation.get("name").set("max-size");
            ModelNode result = managementClient.getControllerClient().execute(operation);
            Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
        }

        private static ModelNode getAddress() {
            ModelNode address = new ModelNode();
            address.add("subsystem", "ejb3");
            address.add("passivation-store", "infinispan");
            address.protect();
            return address;
        }
    }

    @Deployment
    public static Archive<?> deploy() throws Exception {

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "persistentcontext-test.jar");
        jar.addPackage(StatefulUnitTestCase.class.getPackage());
        jar.addAsManifestResource(StatefulUnitTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
        jar.addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller-client, org.jboss.dmr, org.jboss.marshalling \n"), "MANIFEST.MF");
        return jar;
    }

    @Test
    public void testStateful() throws Exception {
        StatefulRemote remote = (StatefulRemote) ctx.lookup("java:module/" + StatefulBean.class.getSimpleName() + "!" + StatefulRemote.class.getName());
        int id = remote.doit();
        ctx.lookup("java:module/" + StatefulBean.class.getSimpleName() + "!" + StatefulRemote.class.getName());
        Thread.sleep(TIME_TO_WAIT_FOR_PASSIVATION_MS);
        remote.find(id);
    }

    @Test
    public void testTransientStateful() throws Exception {
        StatefulRemote remote = (StatefulRemote) ctx.lookup("java:module/" + StatefulTransientBean.class.getSimpleName() + "!" + StatefulRemote.class.getName());
        int id = remote.doit();
        ctx.lookup("java:module/" + StatefulTransientBean.class.getSimpleName() + "!" + StatefulRemote.class.getName());
        Thread.sleep(TIME_TO_WAIT_FOR_PASSIVATION_MS);
        remote.find(id);
    }

    @Test
    public void testNonExtended() throws Exception {
        StatefulRemote remote = (StatefulRemote) ctx.lookup("java:module/" + NonExtendedStatefuBean.class.getSimpleName() + "!" + StatefulRemote.class.getName());
        int id = remote.doit();
        ctx.lookup("java:module/" + NonExtendedStatefuBean.class.getSimpleName() + "!" + StatefulRemote.class.getName());
        Thread.sleep(TIME_TO_WAIT_FOR_PASSIVATION_MS);
        remote.find(id);
    }
}
