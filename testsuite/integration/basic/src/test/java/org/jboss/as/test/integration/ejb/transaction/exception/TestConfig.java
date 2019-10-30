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
