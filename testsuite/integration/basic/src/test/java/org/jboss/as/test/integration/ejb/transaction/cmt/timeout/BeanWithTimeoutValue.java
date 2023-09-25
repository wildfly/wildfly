/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.transaction.cmt.timeout;

import java.util.concurrent.TimeUnit;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.transaction.TransactionManager;

import org.jboss.ejb3.annotation.TransactionTimeout;
import org.wildfly.transaction.client.LocalTransaction;

@Stateless
@TransactionTimeout(value=5, unit=TimeUnit.SECONDS)
public class BeanWithTimeoutValue implements TimeoutRemoteView, TimeoutLocalView {

    @Resource(lookup="java:jboss/TransactionManager")
    private TransactionManager transactionManager;

    protected int getTimeout() {
        try {
            return ((LocalTransaction) transactionManager.getTransaction()).getTransactionTimeout();
        } catch (Exception e)
        {
            return -1;
        }
    }

    /**
     * This method should inherit transaction timeout specified on bean-level
     */
    public int getBeanTimeout() {
        return getTimeout();
    }

    /**
     * This method has explicity transaction timeout in bean-class
     */
    @TransactionTimeout(value=6, unit=TimeUnit.SECONDS)
    public int getBeanMethodTimeout() {
        return getTimeout();
    }

    /**
     * This method has method-level timeout specified on remote-view
     */
    @TransactionTimeout(value=7, unit=TimeUnit.SECONDS)
    public int getRemoteMethodTimeout() {
        return getTimeout();
    }

    /**
     * This method has timeout specified on entire local view
     */
    public int getLocalViewTimeout() {
        return getTimeout();
    }
}
