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

import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.UserTransaction;

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
                    assertThat(receivedEx.getClass(), equalTo(javax.ejb.TransactionRolledbackLocalException.class));
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
                assertThat(receivedEx.getClass(), equalTo(javax.ejb.EJBException.class));
                break;
            case HEURISTIC_CAUSED_BY_XA_EXCEPTION:
            case HEURISTIC_CAUSED_BY_RM_SPECIFIC_XA_EXCEPTION:
                assertThat(receivedEx.getClass(), equalTo(javax.ejb.EJBException.class));
                assertThat(receivedEx.getCause().getClass(), equalTo(javax.transaction.HeuristicMixedException.class));
                break;
            case ROLLBACK_CAUSED_BY_XA_EXCEPTION:
            case ROLLBACK_CAUSED_BY_RM_SPECIFIC_XA_EXCEPTION:
                assertThat(receivedEx.getClass(), equalTo(javax.ejb.EJBTransactionRolledbackException.class));
                assertThat(receivedEx.getCause().getClass(), equalTo(javax.transaction.RollbackException.class));
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
