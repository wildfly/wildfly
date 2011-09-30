/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.testsuite.integration.tm.mttransactionmanager;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import javax.transaction.RollbackException;

/**
 * Multithreaded tests for the transaction manager
 * <p/>
 * Based on TransactionManagerUnitTestCase
 *
 * @author <a href="dimitris@jboss.org">Dimitris Andreadis</a>
 * @author istudens@redhat.com
 */
@RunWith(Arquillian.class)
public class MTTransactionManagerUnitTestCase {

    public static final String ARCHIVE_NAME = "mttest";

    @Inject
    MTTestBean mtBean;

    @Rule
    public TestName name = new TestName();


    public void runTest(MTOperation[][] ops) throws Exception {
        mtBean.testMTOperations(name.getMethodName(), ops);
    }

    @Deployment
    public static JavaArchive deploy() {
        return ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar")
                .addPackage(MTTransactionManagerUnitTestCase.class.getPackage())
                .addAsManifestResource(EmptyAsset.INSTANCE, ArchivePaths.create("beans.xml"))
                .addAsManifestResource(new StringAsset("Dependencies: org.jboss.jboss-transaction-spi\n"),"MANIFEST.MF");
    }

    /**
     * Start a tx on one thread and commit it on another
     * withouth the 2nd thread actually associating itself
     * with the transaction.
     * <p/>
     * This is an error if the TransactionIntegrity plugin is active
     */
    @Test
    public void testCommitTxStartedOnADifferentThread() throws Exception {
        runTest(new MTOperation[][]
                {
                        {
                                // thread 0
                                new MTOperation(MTOperation.TM_BEGIN, 10),
                                new MTOperation(MTOperation.TM_GET_STATUS),
                                new MTOperation(MTOperation.XX_SLEEP_200), // other thread must commit
                                new MTOperation(MTOperation.TM_GET_STATUS)
                        }
                        ,
                        {
                                // thread 1
                                new MTOperation(MTOperation.XX_WAIT_FOR, 10),
                                new MTOperation(MTOperation.TX_COMMIT, 10),
                                new MTOperation(MTOperation.TM_GET_STATUS),
                        }
                });
    }

    /**
     * Start a tx on one thread, then resume this tx and commit it from
     * another thread. Normally this is allowed, but if the
     * TransactionIntegrity policy is active, then the 2 threads associated
     * with the tx will be detected at commit time and an exception
     * will be thrown.
     */
    @Test
    public void testResumeAndCommitTxStartedOnADifferentThread() throws Exception {
        runTest(new MTOperation[][]
                {
                        {
                                // thread 0
                                new MTOperation(MTOperation.TM_BEGIN, 10),
                                new MTOperation(MTOperation.TM_GET_STATUS),
                        }
                        ,
                        {
                                // thread 1
                                new MTOperation(MTOperation.TM_GET_STATUS),
                                new MTOperation(MTOperation.XX_WAIT_FOR, 10),
                                new MTOperation(MTOperation.TM_RESUME, 10),
                                new MTOperation(MTOperation.TM_GET_STATUS),
                                new MTOperation(MTOperation.TX_COMMIT, 10),
                                new MTOperation(MTOperation.TM_GET_STATUS),
                        }
                });
    }

    /**
     * Start a tx on one thread and commit it on another thread
     * without the 2nd thread actually associating itself with
     * the transaction. The try to commit the tx on the 1st
     * thread as well, thus producing an exception.
     * <p/>
     * This only works when the TransactionIntegrity policy is innactive
     */
    @Test
    public void testCommitSameTxInTwoThreads() throws Exception {
        runTest(new MTOperation[][]
                {
                        {
                                // thread 0
                                new MTOperation(MTOperation.TM_BEGIN, 10),
                                new MTOperation(MTOperation.TM_GET_STATUS),
                                new MTOperation(MTOperation.XX_SLEEP_200),
                                new MTOperation(MTOperation.TM_GET_STATUS),

                                // FIXME - JBTM-558
                                new MTOperation(MTOperation.TM_COMMIT, -1, new RollbackException(), false)

                        }
                        ,
                        {
                                // thread 1
                                new MTOperation(MTOperation.XX_WAIT_FOR, 10),
                                new MTOperation(MTOperation.TX_COMMIT, 10),
                                new MTOperation(MTOperation.TM_GET_STATUS),
                        }
                });
    }

}
