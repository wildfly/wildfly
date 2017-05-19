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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.rmi.RemoteException;

import javax.transaction.UserTransaction;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.ejb.remote.common.EJBManagementUtil;
import org.jboss.as.test.integration.ejb.transaction.exception.TestConfig.TxManagerException;
import org.jboss.as.test.integration.ejb.transaction.exception.bean.TestBean;
import org.jboss.as.test.integration.ejb.transaction.exception.bean.ejb2.BmtEjb2;
import org.jboss.as.test.integration.ejb.transaction.exception.bean.ejb2.CmtEjb2;
import org.jboss.as.test.integration.ejb.transaction.exception.bean.ejb2.TestBean2Remote;
import org.jboss.as.test.integration.ejb.transaction.exception.bean.ejb3.BmtEjb3;
import org.jboss.as.test.integration.ejb.transaction.exception.bean.ejb3.CmtEjb3;
import org.jboss.ejb.client.EJBClient;
import org.jboss.ejb.client.StatelessEJBLocator;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

/**
 * Tests that EJB client propagates appropriate exceptions.
 *
 * @author dsimko@redhat.com
 */
@RunAsClient
@RunWith(Arquillian.class)
public class TxExceptionEjbClientTestCase extends TxExceptionBaseTestCase {

    private static String nodeName;

    @BeforeClass
    public static void beforeClass() throws Exception {
        nodeName = EJBManagementUtil.getNodeName();
    }

    @Override
    protected void checkReceivedException(TestConfig testCnf, Exception receivedEx) {
        switch (testCnf.getTxContext()) {
        case RUN_IN_TX_STARTED_BY_CALLER:
            switch (testCnf.getTxManagerException()) {
            case NONE:
                switch (testCnf.getEjbType()) {
                case EJB2:
                    assertThat(receivedEx.getClass(), equalTo(javax.transaction.TransactionRolledbackException.class));
                    break;
                case EJB3:
                    assertThat(receivedEx.getClass(), equalTo(javax.ejb.EJBTransactionRolledbackException.class));
                    break;
                }
                break;
            case HEURISTIC_CAUSED_BY_XA_EXCEPTION:
            case HEURISTIC_CAUSED_BY_RM_SPECIFIC_XA_EXCEPTION:
                assertThat(receivedEx.getClass(), equalTo(javax.transaction.HeuristicMixedException.class));
                break;
            case ROLLBACK_CAUSED_BY_XA_EXCEPTION:
            case ROLLBACK_CAUSED_BY_RM_SPECIFIC_XA_EXCEPTION:
                assertThat(receivedEx.getClass(), equalTo(javax.transaction.RollbackException.class));
                break;
            }
            break;
        case START_CONTAINER_MANAGED_TX:
        case START_BEAN_MANAGED_TX:
            switch (testCnf.getTxManagerException()) {
            case NONE:
                switch (testCnf.getEjbType()) {
                case EJB2:
                    assertThat(receivedEx.getClass(), equalTo(java.rmi.RemoteException.class));
                    break;
                case EJB3:
                    assertThat(receivedEx.getClass(), equalTo(javax.ejb.EJBException.class));
                    break;
                }
                break;
            case HEURISTIC_CAUSED_BY_XA_EXCEPTION:
                assertThat(receivedEx.getClass(), equalTo(javax.ejb.EJBException.class));
                assertThat(receivedEx.getCause().getClass(), equalTo(javax.transaction.HeuristicMixedException.class));
                break;
            case HEURISTIC_CAUSED_BY_RM_SPECIFIC_XA_EXCEPTION:
                assertThat(receivedEx.getClass(), equalTo(javax.ejb.EJBException.class));
                // FIXME WFLY-8794
                // assertThat("HeuristicMixedException should be cause.", receivedEx.getCause().getClass(), equalTo(javax.transaction.HeuristicMixedException.class));
                assertThat(receivedEx.getCause().getClass(), equalTo(java.lang.ClassNotFoundException.class));
                break;
            case ROLLBACK_CAUSED_BY_XA_EXCEPTION:
                assertThat(receivedEx.getClass(), equalTo(javax.ejb.EJBTransactionRolledbackException.class));
                assertThat(receivedEx.getCause().getClass(), equalTo(javax.transaction.RollbackException.class));
                break;
            case ROLLBACK_CAUSED_BY_RM_SPECIFIC_XA_EXCEPTION:
                // FIXME should be EJBTransactionRolledbackException
                assertThat(receivedEx.getClass(), equalTo(javax.ejb.EJBException.class));
                // FIXME WFLY-8794
                // assertThat("HeuristicMixedException should be cause.", receivedEx.getCause().getClass(), equalTo(javax.transaction.HeuristicMixedException.class));
                assertThat(receivedEx.getCause().getClass(), equalTo(java.lang.ClassNotFoundException.class));
                break;
            }
        }
    }

    @Override
    protected UserTransaction getUserTransaction() {
        return EJBClient.getUserTransaction(nodeName);
    }

    @Override
    protected void throwExceptionFromCmtEjb2() throws RemoteException {
        getBean(TestBean2Remote.class, CmtEjb2.class.getSimpleName()).throwRuntimeException();
    }

    @Override
    protected void throwExceptionFromCmtEjb3() {
        getBean(TestBean.class, CmtEjb3.class.getSimpleName()).throwRuntimeException();
    }

    @Override
    protected void throwExceptionFromBmtEjb2() throws RemoteException {
        getBean(TestBean2Remote.class, BmtEjb2.class.getSimpleName()).throwRuntimeException();
    }

    @Override
    protected void throwExceptionFromBmtEjb3() {
        getBean(TestBean.class, BmtEjb3.class.getSimpleName()).throwRuntimeException();
    }

    @Override
    protected void throwExceptionFromTm(TxManagerException txManagerException) throws Exception {
        getBean(TestBean.class, CmtEjb3.class.getSimpleName()).throwExceptionFromTm(txManagerException);
    }

    private <T> T getBean(Class<T> viewType, String beanName) {
        final StatelessEJBLocator<T> remoteBeanLocator = new StatelessEJBLocator<T>(viewType, APP_NAME, MODULE_NAME, beanName, "");
        return EJBClient.createProxy(remoteBeanLocator);
    }

}
