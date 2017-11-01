/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.ejb.transaction.cmt.timeout;

import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.transaction.TransactionManager;

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
