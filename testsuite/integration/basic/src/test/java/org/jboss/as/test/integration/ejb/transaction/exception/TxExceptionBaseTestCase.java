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

package org.jboss.as.test.integration.ejb.transaction.exception;

import static org.jboss.as.test.integration.ejb.transaction.exception.TestConfig.EjbType.EJB2;
import static org.jboss.as.test.integration.ejb.transaction.exception.TestConfig.EjbType.EJB3;
import static org.jboss.as.test.integration.ejb.transaction.exception.TestConfig.TxContext.RUN_IN_TX_STARTED_BY_CALLER;
import static org.jboss.as.test.integration.ejb.transaction.exception.TestConfig.TxContext.START_BEAN_MANAGED_TX;
import static org.jboss.as.test.integration.ejb.transaction.exception.TestConfig.TxContext.START_CONTAINER_MANAGED_TX;
import static org.jboss.as.test.integration.ejb.transaction.exception.TestConfig.TxManagerException.HEURISTIC_CAUSED_BY_RM_SPECIFIC_XA_EXCEPTION;
import static org.jboss.as.test.integration.ejb.transaction.exception.TestConfig.TxManagerException.HEURISTIC_CAUSED_BY_XA_EXCEPTION;
import static org.jboss.as.test.integration.ejb.transaction.exception.TestConfig.TxManagerException.NONE;
import static org.jboss.as.test.integration.ejb.transaction.exception.TestConfig.TxManagerException.ROLLBACK_CAUSED_BY_RM_SPECIFIC_XA_EXCEPTION;
import static org.jboss.as.test.integration.ejb.transaction.exception.TestConfig.TxManagerException.ROLLBACK_CAUSED_BY_XA_EXCEPTION;
import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

import java.rmi.RemoteException;
import java.security.AllPermission;

import javax.transaction.Status;
import javax.transaction.UserTransaction;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.as.test.integration.ejb.transaction.exception.TestConfig.TxManagerException;
import org.jboss.as.test.integration.transactions.TestXAResource;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;

/**
 * Base class for testing whether client gets exceptions according to the
 * specification.
 *
 * @author dsimko@redhat.com
 */
public abstract class TxExceptionBaseTestCase {

    private static Logger LOG = Logger.getLogger(TxExceptionBaseTestCase.class);

    protected static final String APP_NAME = "tx-exception-test";
    protected static final String MODULE_NAME = "ejb";

    @Deployment
    public static Archive<?> createDeployment() {
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, APP_NAME + ".ear");
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        jar.addPackages(true, TxExceptionBaseTestCase.class.getPackage());
        jar.addPackage(TestXAResource.class.getPackage());
        jar.addPackages(true, "javassist");
        // this test needs to create a new public class thru javassist so AllPermission is needed here
        ear.addAsManifestResource(createPermissionsXmlAsset(new AllPermission()), "permissions.xml");
        ear.addAsModule(jar);
        return ear;
    }

    @Test
    public void exceptionThrownFromEjb2WhichRunsInTxStartedByCaller() throws Exception {
        runTest(new TestConfig(EJB2, RUN_IN_TX_STARTED_BY_CALLER, tmEx -> throwExceptionFromCmtEjb2()));
    }

    @Test
    public void exceptionThrownFromEjb3WhichRunsInTxStartedByCaller() throws Exception {
        runTest(new TestConfig(EJB3, RUN_IN_TX_STARTED_BY_CALLER, tmEx -> throwExceptionFromCmtEjb3()));
    }

    @Test
    public void exceptionThrownFromEjb2WhichStartsContainerManagedTx() throws Exception {
        runTest(new TestConfig(EJB2, START_CONTAINER_MANAGED_TX, tmEx -> throwExceptionFromCmtEjb2()));
    }

    @Test
    public void exceptionThrownFromEjb3WhichStartsContainerManagedTx() throws Exception {
        runTest(new TestConfig(EJB3, START_CONTAINER_MANAGED_TX, tmEx -> throwExceptionFromCmtEjb3()));
    }

    @Test
    public void exceptionThrownFromEjb2WhichStartsBeanManagedTx() throws Exception {
        runTest(new TestConfig(EJB2, START_BEAN_MANAGED_TX, tmEx -> throwExceptionFromBmtEjb2()));
    }

    @Test
    public void exceptionThrownFromEjb3WhichStartsBeanManagedTx() throws Exception {
        runTest(new TestConfig(EJB3, START_BEAN_MANAGED_TX, tmEx -> throwExceptionFromBmtEjb3()));
    }

    @Test
    public void heuristicExceptionThrownFromTmInCallerTx() throws Exception {
        runTest(new TestConfig(EJB3, RUN_IN_TX_STARTED_BY_CALLER, tmEx -> throwExceptionFromTm(tmEx), HEURISTIC_CAUSED_BY_XA_EXCEPTION));
    }

    @Test
    public void heuristicExceptionWithSpecificCauseThrownFromTmInCallerTx() throws Exception {
        runTest(new TestConfig(EJB3, RUN_IN_TX_STARTED_BY_CALLER, tmEx -> throwExceptionFromTm(tmEx), HEURISTIC_CAUSED_BY_RM_SPECIFIC_XA_EXCEPTION));
    }

    @Test
    public void heuristicExceptionThrownFromTm() throws Exception {
        runTest(new TestConfig(EJB3, START_CONTAINER_MANAGED_TX, tmEx -> throwExceptionFromTm(tmEx), HEURISTIC_CAUSED_BY_XA_EXCEPTION));
    }

    @Test
    public void heuristicExceptionWithSpecificCauseThrownFromTm() throws Exception {
        runTest(new TestConfig(EJB3, START_CONTAINER_MANAGED_TX, tmEx -> throwExceptionFromTm(tmEx), HEURISTIC_CAUSED_BY_RM_SPECIFIC_XA_EXCEPTION));
    }

    @Test
    public void rollbackExceptionThrownFromTm() throws Exception {
        runTest(new TestConfig(EJB3, START_CONTAINER_MANAGED_TX, tmEx -> throwExceptionFromTm(tmEx), ROLLBACK_CAUSED_BY_XA_EXCEPTION));
    }

    @Test
    public void rollbackExceptionWithSpecificCauseThrownFromTm() throws Exception {
        runTest(new TestConfig(EJB3, START_CONTAINER_MANAGED_TX, tmEx -> throwExceptionFromTm(tmEx), ROLLBACK_CAUSED_BY_RM_SPECIFIC_XA_EXCEPTION));
    }

    @Test
    public void rollbackExceptionThrownFromTmInCallerTx() throws Exception {
        runTest(new TestConfig(EJB3, RUN_IN_TX_STARTED_BY_CALLER, tmEx -> throwExceptionFromTm(tmEx), ROLLBACK_CAUSED_BY_XA_EXCEPTION));
    }

    @Test
    public void rollbackExceptionWithSpecificCauseThrownFromTmInCallerTx() throws Exception {
        runTest(new TestConfig(EJB3, RUN_IN_TX_STARTED_BY_CALLER, tmEx -> throwExceptionFromTm(tmEx), ROLLBACK_CAUSED_BY_RM_SPECIFIC_XA_EXCEPTION));
    }

    private void runTest(TestConfig testType) throws Exception {
        final UserTransaction userTransaction = getUserTransaction();
        try {
            if (testType.getTxContext() == RUN_IN_TX_STARTED_BY_CALLER) {
                // clean up possible tx from previous tests
                rollbackIfExists(userTransaction);
                userTransaction.begin();
            }

            testType.getEjbMethod().invoke(testType.getTxManagerException());

            if (testType.getTxContext() == RUN_IN_TX_STARTED_BY_CALLER && testType.getTxManagerException() != NONE) {

                userTransaction.commit();
            }
            Assert.fail("An exception was expected.");
        } catch (Exception e) {
            LOG.debugf(e, "Received exception: %s. Test type %s", e.getClass(), testType);
            checkReceivedException(testType, e);
        } finally {
            if (testType.getTxContext() == RUN_IN_TX_STARTED_BY_CALLER && testType.getTxManagerException() == NONE) {
                userTransaction.rollback();
            }
        }
    }

    private void rollbackIfExists(UserTransaction userTransaction) throws Exception {
        LOG.debugf("UserTransaction status is %s.", userTransaction.getStatus());
        if (userTransaction.getStatus() != Status.STATUS_NO_TRANSACTION) {
            userTransaction.rollback();
        }
    }

    protected abstract UserTransaction getUserTransaction();

    protected abstract void throwExceptionFromCmtEjb2() throws RemoteException;

    protected abstract void throwExceptionFromCmtEjb3();

    protected abstract void throwExceptionFromBmtEjb2() throws RemoteException;

    protected abstract void throwExceptionFromBmtEjb3();

    protected abstract void throwExceptionFromTm(TxManagerException txManagerException) throws Exception;

    protected abstract void checkReceivedException(TestConfig testType, Exception receivedException);

}
