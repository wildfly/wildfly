/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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
 * 2110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.txn.service;

import javax.management.MBeanServer;

/**
 * Transaction subsystem runtime configuration provided by capability
 * {@link org.jboss.as.txn.subsystem.TransactionSubsystemRootResourceDefinition#TRANSACTION_RUNTIME_CONFIGURATOR_CAPABILITY}.
 *
 * May be used to set up transaction subsystem runtime configuration
 * that could be passed to different runtime handlers.
 */
public class TransactionRuntimeConfigurator {
    private MBeanServer mBeanServer;
    private boolean isDisableRecoveryBeforeSuspend = false;

    public boolean isDisableRecoveryBeforeSuspend() {
        return isDisableRecoveryBeforeSuspend;
    }

    public TransactionRuntimeConfigurator setDisableRecoveryBeforeSuspend(boolean disableRecoveryBeforeSuspend) {
        isDisableRecoveryBeforeSuspend = disableRecoveryBeforeSuspend;
        return this;
    }

    public MBeanServer getMBeanServer() {
        return mBeanServer;
    }

    public void setmBeanServer(MBeanServer mBeanServer) {
        this.mBeanServer = mBeanServer;
    }
}