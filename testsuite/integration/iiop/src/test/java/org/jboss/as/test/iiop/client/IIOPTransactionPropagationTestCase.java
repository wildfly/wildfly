/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.test.iiop.client;

import java.io.IOException;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;
import javax.transaction.Status;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.management.ManagementOperations;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.integration.management.util.ServerReload;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.CORBA.SystemException;

/**
 * @author Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
@RunAsClient
public class IIOPTransactionPropagationTestCase {

    @ArquillianResource
    private static ContainerController container;

    @ArquillianResource
    private Deployer deployer;

    public static final String DEFAULT_JBOSSAS = "iiop-server";
    private static final String ARCHIVE_NAME = "iiop-jts-ctx-propag-test";

    private static InitialContext context;

    @Deployment(name = ARCHIVE_NAME, managed = true)
    @TargetsContainer(DEFAULT_JBOSSAS)
    public static Archive<?> deploy() throws InvalidName, SystemException {
        JavaArchive jar = ShrinkWrap
                .create(JavaArchive.class, ARCHIVE_NAME + ".jar")
                .addClass(IIOPTestBean.class)
                .addClass(IIOPTestBeanHome.class)
                .addClass(IIOPTestRemote.class)
                .addAsManifestResource(IIOPTransactionPropagationTestCase.class.getPackage(), "jboss-ejb3.xml",
                        "jboss-ejb3.xml");
        // File testPackage = new File("/tmp", ARCHIVE_NAME + ".jar");
        // jar.as(ZipExporter.class).exportTo(testPackage, true);
        return jar;
    }

    @BeforeClass
    public static void beforeClass() throws InvalidName, SystemException, NamingException {
        // // Orb presseting has to be done before the ORB is started to be used
        Util.presetOrb();
        context = Util.getContext();
    }

    @AfterClass
    public static void afterClass() throws NamingException {
        Util.tearDownOrb();
    }

    @Test
    public void testIIOPInvocation() throws Throwable {
        final Object iiopObj = context.lookup(IIOPTestBean.class.getSimpleName());
        final IIOPTestBeanHome beanHome = (IIOPTestBeanHome) PortableRemoteObject.narrow(iiopObj, IIOPTestBeanHome.class);
        final IIOPTestRemote bean = beanHome.create();

        try {
            Util.startCorbaTx();
            Assert.assertEquals(Status.STATUS_ACTIVE, bean.transactionStatus());
            Assert.assertEquals("transaction-attribute-mandatory", bean.callMandatory());
            Util.commitCorbaTx();
        } catch (Throwable e) {
            // Util.rollbackCorbaTx();
            throw e;
        }
    }

    @Test
    public void testIIOPNeverCallInvocation() throws Throwable {
        final Object iiopObj = context.lookup(IIOPTestBean.class.getSimpleName());
        final IIOPTestBeanHome beanHome = (IIOPTestBeanHome) PortableRemoteObject.narrow(iiopObj, IIOPTestBeanHome.class);
        final IIOPTestRemote bean = beanHome.create();

        try {
            Util.startCorbaTx();
            Assert.assertEquals(Status.STATUS_ACTIVE, bean.transactionStatus());
            bean.callNever();
            Assert.fail("Exception is supposed to be here thrown from TransactionAttribute.NEVER method");
        } catch (Exception e) {
            // this is OK - is expected never throwing that TS exists
            Assert.assertEquals(Status.STATUS_ACTIVE, bean.transactionStatus());
        } finally {
            Util.rollbackCorbaTx();
        }
    }

    @Test
    public void testIIOPInvocationWithRollbackOnly() throws Throwable {
        final Object iiopObj = context.lookup(IIOPTestBean.class.getSimpleName());
        final IIOPTestBeanHome beanHome = (IIOPTestBeanHome) PortableRemoteObject.narrow(iiopObj, IIOPTestBeanHome.class);
        final IIOPTestRemote bean = beanHome.create();

        try {
            Util.startCorbaTx();
            Assert.assertEquals(Status.STATUS_ACTIVE, bean.transactionStatus());
            bean.callRollbackOnly();
            Assert.assertEquals(Status.STATUS_MARKED_ROLLBACK, bean.transactionStatus());
        } finally {
            Util.rollbackCorbaTx();
        }
    }

    /**
     * The setup tasks are bound to deploy/undeploy actions.
     */
    static class JTSSetup implements ServerSetupTask {
        public static final String IIOP_TRANSACTIONS_JTA = "spec";
        public static final String IIOP_TRANSACTIONS_JTS = "on";

        private static final Logger log = Logger.getLogger(JTSSetup.class);
        private boolean isTransactionJTS = true;
        private String iiopTransaction = IIOP_TRANSACTIONS_JTA;

        ManagementClient managementClient = null;

        /**
         * The setup method is prepared here just for sure. The jts switching on is done by xslt transformation before the test
         * is launched.
         */
        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            log.trace("JTSSetup.setup");
            this.managementClient = managementClient;
            boolean isNeedReload = false;

            // check what is defined before
            isTransactionJTS = checkJTSOnTransactions();
            iiopTransaction = checkTransactionsOnJacorb();

            if (!isTransactionJTS) {
                setTransactionJTS(true);
                isNeedReload = true;
            }
            if (IIOP_TRANSACTIONS_JTA.equalsIgnoreCase(iiopTransaction)) {
                setJTS(true);
                isNeedReload = true;
            }

            if (isNeedReload) {
                ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient(), 40000);
            }
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            log.trace("JTSSetup.tearDown");
            this.managementClient = managementClient;
            boolean isNeedReload = false;

            // get it back what was defined before
            // if it was not JTS, set it back to JTA (was set to JTS setup())
            // if it was JTA, set it back to JTA (was set to JTS in setup())
            if (IIOP_TRANSACTIONS_JTA.equalsIgnoreCase(iiopTransaction)) {
                setJTS(false);
                isNeedReload = true;
            }
            if (!isTransactionJTS) {
                setTransactionJTS(false);
            }

            if (isNeedReload) {
                ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient(), 40000);
            }
        }

        public boolean checkJTSOnTransactions() throws IOException, MgmtOperationException {
            /* /subsystem=transactions:read-attribute(name=jts) */
            final ModelNode address = new ModelNode();
            address.add("subsystem", "transactions");
            final ModelNode operation = new ModelNode();
            operation.get(OP).set(READ_ATTRIBUTE_OPERATION);
            operation.get(OP_ADDR).set(address);
            operation.get("name").set("jts");

            return executeOperation(operation).asBoolean();
        }

        public String checkTransactionsOnJacorb() throws IOException, MgmtOperationException {
            /* /subsystem=jacorb:read-attribute(name=transactions) */
            final ModelNode address = new ModelNode();
            address.add("subsystem", "iiop-openjdk");
            final ModelNode operation = new ModelNode();
            operation.get(OP).set(READ_ATTRIBUTE_OPERATION);
            operation.get(OP_ADDR).set(address);
            operation.get("name").set("transactions");

            return executeOperation(operation).asString();
        }

        public void setTransactionJTS(boolean enabled) throws IOException, MgmtOperationException {
            /* /subsystem=transactions:write-attribute(name=jts,value=false|true) */
            ModelNode address = new ModelNode();
            address.add("subsystem", "transactions");
            ModelNode operation = new ModelNode();
            operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
            operation.get(OP_ADDR).set(address);
            operation.get("name").set("jts");
            operation.get("value").set(enabled);
            log.trace("operation=" + operation);
            executeOperation(operation);
        }

        public void setJTS(boolean enabled) throws IOException, MgmtOperationException {
            String transactionsOnIIOP = (enabled) ? IIOP_TRANSACTIONS_JTS : IIOP_TRANSACTIONS_JTA;
            ModelNode address = new ModelNode();
            address.add("subsystem", "iiop-openjdk");
            ModelNode operation = new ModelNode();
            operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
            operation.get(OP_ADDR).set(address);
            operation.get("name").set("transactions");
            operation.get("value").set(transactionsOnIIOP);
            log.trace("operation=" + operation);
            executeOperation(operation);
        }

        private ModelNode executeOperation(final ModelNode op) throws IOException, MgmtOperationException {
            return ManagementOperations.executeOperation(managementClient.getControllerClient(), op);
        }
    }
}

