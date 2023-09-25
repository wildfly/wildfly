/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.transaction.exception;

/**
 * Represents configuration for one particular test method.
 *
 * @author dsimko@redhat.com
 */
public class TestConfig {

    @FunctionalInterface
    public interface EjbMethod {
        void invoke(TxManagerException txManagerException) throws Exception;
    }

    public static enum EjbType {
        EJB2, EJB3
    }

    public static enum TxContext {
        RUN_IN_TX_STARTED_BY_CALLER, START_CONTAINER_MANAGED_TX, START_BEAN_MANAGED_TX
    }

    public static enum TxManagerException {
        NONE, HEURISTIC_CAUSED_BY_XA_EXCEPTION, HEURISTIC_CAUSED_BY_RM_SPECIFIC_XA_EXCEPTION, ROLLBACK_CAUSED_BY_XA_EXCEPTION, ROLLBACK_CAUSED_BY_RM_SPECIFIC_XA_EXCEPTION
    }

    private final EjbType ejbType;
    private final TxContext txContext;
    private final EjbMethod action;
    private final TxManagerException tmException;

    public TestConfig(EjbType ejbType, TxContext txContext, EjbMethod action) {
        this(ejbType, txContext, action, TxManagerException.NONE);
    }

    public TestConfig(EjbType ejbType, TxContext txContext, EjbMethod action, TxManagerException txManagerException) {
        this.ejbType = ejbType;
        this.txContext = txContext;
        this.action = action;
        this.tmException = txManagerException;
    }

    public EjbType getEjbType() {
        return ejbType;
    }

    public TxContext getTxContext() {
        return txContext;
    }

    public EjbMethod getEjbMethod() {
        return action;
    }

    public TxManagerException getTxManagerException() {
        return tmException;
    }

    @Override
    public String toString() {
        return "TestConfig [ejbType=" + ejbType + ", txContext=" + txContext + ", action=" + action + ", tmException=" + tmException + "]";
    }

}
