/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2016, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.jca.statistics.xa;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.junit.Assert.assertEquals;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.test.integration.management.ManagementOperations;
import org.jboss.as.test.integration.transactions.TxTestUtil;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.ejb.client.EJBClient;
import org.jboss.ejb.client.StatelessEJBLocator;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * XA Data source statistics testCase
 *
 * @author dsimko@redhat.com
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(XaDataSourceSetupStep.class)
public class XaDataSourcePoolStatisticsTestCase {

    private static final String ARCHIVE_NAME = "xa_transactions";
    private static final String APP_NAME = "xa-datasource-pool-statistics-test";

    private static final String ATTRIBUTE_XA_COMMIT_COUNT = "XACommitCount";
    private static final String ATTRIBUTE_XA_ROLLBACK_COUNT = "XARollbackCount";
    private static final String ATTRIBUTE_XA_START_COUNT = "XAStartCount";

    private static final int COUNT = 10;

    @ContainerResource
    private ManagementClient managementClient;

    @Deployment
    public static Archive<?> deploy() {
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, APP_NAME + ".ear");
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addClasses(TestEntity.class, SLSB1.class, SLSB.class, TimeoutUtil.class);
        jar.addPackage(TxTestUtil.class.getPackage());
        jar.addAsManifestResource(XaDataSourcePoolStatisticsTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
        ear.addAsModule(jar);
        ear.addAsManifestResource(new StringAsset("Dependencies: com.h2database.h2\n"), "MANIFEST.MF");
        return ear;
    }

    @Before
    public void beforeTest() throws Exception {
        // TODO Elytron: Determine how this should be adapted once the transaction client changes are in
        //final EJBClientTransactionContext localUserTxContext = EJBClientTransactionContext.createLocal();
        //EJBClientTransactionContext.setGlobalContext(localUserTxContext);
    }

    /**
     * Tests increasing XACommitCount, XACommitAverageTime and XAStartCount
     * statistical attributes.
     */
    @Test
    public void testXACommit() throws Exception {
        int xaStartCountBefore = readStatisticalAttribute(ATTRIBUTE_XA_START_COUNT);
        int xaCommitCount = readStatisticalAttribute(ATTRIBUTE_XA_COMMIT_COUNT);

        assertEquals(ATTRIBUTE_XA_COMMIT_COUNT + " is " + xaCommitCount + " but should be 0", 0, xaCommitCount);

        SLSB slsb = getBean();
        for (int i = 0; i < COUNT; i++) {
            slsb.commit();
        }

        xaCommitCount = readStatisticalAttribute(ATTRIBUTE_XA_COMMIT_COUNT);
        int xaStartCountAfter = readStatisticalAttribute(ATTRIBUTE_XA_START_COUNT);
        int total = xaStartCountBefore + COUNT;

        assertEquals(ATTRIBUTE_XA_COMMIT_COUNT + " is " + xaCommitCount + " but should be " + COUNT, COUNT, xaCommitCount);
        assertEquals(ATTRIBUTE_XA_START_COUNT + " is " + xaStartCountAfter + " but should be " + total, total, xaStartCountAfter);
    }

    /**
     * Tests increasing XARollbackCount statistical attribute.
     */
    @Test
    public void testXARollback() throws Exception {
        int xaRollbackCount = readStatisticalAttribute(ATTRIBUTE_XA_ROLLBACK_COUNT);

        assertEquals(ATTRIBUTE_XA_ROLLBACK_COUNT + " is " + xaRollbackCount + " but should be 0", 0, xaRollbackCount);

        SLSB slsb = getBean();
        for (int i = 0; i < COUNT; i++) {
            slsb.rollback();
        }

        xaRollbackCount = readStatisticalAttribute(ATTRIBUTE_XA_ROLLBACK_COUNT);

        assertEquals(ATTRIBUTE_XA_ROLLBACK_COUNT + " is " + xaRollbackCount + " but should be " + COUNT, COUNT, xaRollbackCount);
    }

    private SLSB getBean() {
        final StatelessEJBLocator<SLSB> locator = new StatelessEJBLocator<SLSB>(SLSB.class, APP_NAME, ARCHIVE_NAME, SLSB1.class.getSimpleName(), "");
        return EJBClient.createProxy(locator);
    }

    private ModelNode getStaticticsAddress() {
        return PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, "datasources"),
                PathElement.pathElement("xa-data-source", XaDataSourceSetupStep.XA_DATASOURCE_NAME), PathElement.pathElement("statistics", "pool")).toModelNode();
    }

    private int readStatisticalAttribute(String attributeName) throws Exception {
        return readAttribute(getStaticticsAddress(), attributeName).asInt();
    }

    private ModelNode readAttribute(ModelNode address, String attributeName) throws Exception {
        ModelNode op = new ModelNode();
        op.get(OP).set(READ_ATTRIBUTE_OPERATION);
        op.get(NAME).set(attributeName);
        op.get(OP_ADDR).set(address);
        return ManagementOperations.executeOperation(managementClient.getControllerClient(), op);
    }

}