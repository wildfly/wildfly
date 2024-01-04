/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.transaction.exception;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import jakarta.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.transaction.UserTransaction;

import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.ejb.transaction.exception.TestConfig.TxManagerException;
import org.jboss.as.test.integration.ejb.transaction.exception.bean.TestBean;
import org.jboss.as.test.integration.ejb.transaction.exception.bean.ejb2.BmtEjb2;
import org.jboss.as.test.integration.ejb.transaction.exception.bean.ejb2.CmtEjb2;
import org.jboss.as.test.integration.ejb.transaction.exception.bean.ejb2.TestBean2Local;
import org.jboss.as.test.integration.ejb.transaction.exception.bean.ejb3.BmtEjb3;
import org.jboss.as.test.integration.ejb.transaction.exception.bean.ejb3.CmtEjb3;
import org.junit.runner.RunWith;

/**
 * Tests that container behaves according to the specification when exception is
 * thrown.
 *
 * @author dsimko@redhat.com
 */
@RunWith(Arquillian.class)
public class TxExceptionTestCase extends TxExceptionBaseTestCase {

    @ArquillianResource
    protected InitialContext iniCtx;

    @Inject
    private UserTransaction userTransaction;

    @Override
    protected void checkReceivedException(TestConfig testCnf, Exception receivedEx) {
        switch (testCnf.getTxContext()) {
        case RUN_IN_TX_STARTED_BY_CALLER:
            switch (testCnf.getTxManagerException()) {
            case NONE:
                switch (testCnf.getEjbType()) {
                case EJB2:
                    assertThat(receivedEx.getClass(), equalTo(jakarta.ejb.TransactionRolledbackLocalException.class));
                    break;
                case EJB3:
                    assertThat(receivedEx.getClass(), equalTo(jakarta.ejb.EJBTransactionRolledbackException.class));
                    break;
                }
                break;
            case HEURISTIC_CAUSED_BY_XA_EXCEPTION:
            case HEURISTIC_CAUSED_BY_RM_SPECIFIC_XA_EXCEPTION:
                assertThat(receivedEx.getClass(), equalTo(jakarta.transaction.HeuristicMixedException.class));
                break;
            case ROLLBACK_CAUSED_BY_XA_EXCEPTION:
            case ROLLBACK_CAUSED_BY_RM_SPECIFIC_XA_EXCEPTION:
                assertThat(receivedEx.getClass(), equalTo(jakarta.transaction.RollbackException.class));
                break;
            }
            break;
        case START_CONTAINER_MANAGED_TX:
        case START_BEAN_MANAGED_TX:
            switch (testCnf.getTxManagerException()) {
            case NONE:
                assertThat(receivedEx.getClass(), equalTo(jakarta.ejb.EJBException.class));
                break;
            case HEURISTIC_CAUSED_BY_XA_EXCEPTION:
            case HEURISTIC_CAUSED_BY_RM_SPECIFIC_XA_EXCEPTION:
                assertThat(receivedEx.getClass(), equalTo(jakarta.ejb.EJBException.class));
                assertThat(receivedEx.getCause().getClass(), equalTo(jakarta.transaction.HeuristicMixedException.class));
                break;
            case ROLLBACK_CAUSED_BY_XA_EXCEPTION:
            case ROLLBACK_CAUSED_BY_RM_SPECIFIC_XA_EXCEPTION:
                assertThat(receivedEx.getClass(), equalTo(jakarta.ejb.EJBTransactionRolledbackException.class));
                assertThat(receivedEx.getCause().getClass(), equalTo(jakarta.transaction.RollbackException.class));
            }
        }
    }

    @Override
    protected UserTransaction getUserTransaction() {
        return userTransaction;
    }

    @Override
    protected void throwExceptionFromCmtEjb2() {
        getBean(TestBean2Local.class, CmtEjb2.class.getSimpleName()).throwRuntimeException();
    }

    @Override
    protected void throwExceptionFromCmtEjb3() {
        getBean(TestBean.class, CmtEjb3.class.getSimpleName()).throwRuntimeException();
    }

    @Override
    protected void throwExceptionFromBmtEjb2() {
        getBean(TestBean2Local.class, BmtEjb2.class.getSimpleName()).throwRuntimeException();
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
        try {
            return viewType.cast(iniCtx.lookup("java:global/" + APP_NAME + "/" + MODULE_NAME + "/" + beanName + "!" + viewType.getName()));
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

}
