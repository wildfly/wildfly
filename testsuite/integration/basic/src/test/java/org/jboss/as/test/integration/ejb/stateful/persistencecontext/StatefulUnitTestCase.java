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
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

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
    private static final int TIME_TO_WAIT_FOR_PASSIVATION_MS = 1000;

    @ArquillianResource
    InitialContext ctx;

    static boolean deployed = false;
    static int test = 0;

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
        try (StatefulRemote remote = (StatefulRemote) ctx.lookup("java:module/" + StatefulBean.class.getSimpleName() + "!" + StatefulRemote.class.getName())) {
            int id = remote.doit();
            try (StatefulRemote remote2 = (StatefulRemote) ctx.lookup("java:module/" + StatefulBean.class.getSimpleName() + "!" + StatefulRemote.class.getName())) {
                Thread.sleep(TIME_TO_WAIT_FOR_PASSIVATION_MS);
                remote.find(id);
            }
        }
    }

    @Test
    public void testTransientStateful() throws Exception {
        try (StatefulRemote remote = (StatefulRemote) ctx.lookup("java:module/" + StatefulTransientBean.class.getSimpleName() + "!" + StatefulRemote.class.getName())) {
            int id = remote.doit();
            try (StatefulRemote remote2 = (StatefulRemote) ctx.lookup("java:module/" + StatefulTransientBean.class.getSimpleName() + "!" + StatefulRemote.class.getName())) {
                Thread.sleep(TIME_TO_WAIT_FOR_PASSIVATION_MS);
                remote.find(id);
            }
        }
    }

    @Test
    public void testNonExtended() throws Exception {
        try (StatefulRemote remote = (StatefulRemote) ctx.lookup("java:module/" + NonExtendedStatefuBean.class.getSimpleName() + "!" + StatefulRemote.class.getName())) {
            int id = remote.doit();
            try (StatefulRemote remote2 = (StatefulRemote) ctx.lookup("java:module/" + NonExtendedStatefuBean.class.getSimpleName() + "!" + StatefulRemote.class.getName())) {
                Thread.sleep(TIME_TO_WAIT_FOR_PASSIVATION_MS);
                remote.find(id);
            }
        }
    }

    /**
     * This test originally depended upon manipulatng the passivation-store for the default (passivating) cache.
     * However, since WFLY-14953, passivation stores have been superceeded by bean-management-providers
     * i.e. use /subsystem=distributable-ejb/infinispan-bean-management=default instead of /subsystem=ejb3/passicvation-store=infinispan
     */
    static class StatefulUnitTestCaseSetup implements ServerSetupTask {

        private static final PathAddress INFINISPAN_BEAN_MANAGEMENT_PATH = PathAddress.pathAddress(PathElement.pathElement("subsystem", "distributable-ejb"),
                PathElement.pathElement("infinispan-bean-management", "default"));

        /*
         * Set the max-active-beans attribute of the bean-management provider to 1 to force passivation.
         */
        @Override
        public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
            ModelNode operation = Util.getWriteAttributeOperation(INFINISPAN_BEAN_MANAGEMENT_PATH, "max-active-beans", 1);
            ModelNode result = managementClient.getControllerClient().execute(operation);
            log.trace("modelnode operation write-attribute max-active-beans=1: " + result);
            Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
            ServerReload.reloadIfRequired(managementClient);
        }

        /*
         * Return max-active-beans to its configured value (10,000).
         * NOTE: the configured default is 10000 but may change over time.
         */
        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            ModelNode operation = Util.getWriteAttributeOperation(INFINISPAN_BEAN_MANAGEMENT_PATH, "max-active-beans", 10000);
            ModelNode result = managementClient.getControllerClient().execute(operation);
            Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
            ServerReload.reloadIfRequired(managementClient);
        }
    }
}
