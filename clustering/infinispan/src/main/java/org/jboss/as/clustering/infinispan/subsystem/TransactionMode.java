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

package org.jboss.as.clustering.infinispan.subsystem;

/**
 * @author Paul Ferraro
 */
public enum TransactionMode {
    NONE(org.infinispan.transaction.TransactionMode.NON_TRANSACTIONAL, false, false),
    NON_XA(org.infinispan.transaction.TransactionMode.TRANSACTIONAL, false, false),
    NON_DURABLE_XA(org.infinispan.transaction.TransactionMode.TRANSACTIONAL, true, false),
    FULL_XA(org.infinispan.transaction.TransactionMode.TRANSACTIONAL, true, true),
    ;
    private final org.infinispan.transaction.TransactionMode mode;
    private final boolean xaEnabled;
    private final boolean recoveryEnabled;

    private TransactionMode(org.infinispan.transaction.TransactionMode mode, boolean xaEnabled, boolean recoveryEnabled) {
        this.mode = mode;
        this.xaEnabled = xaEnabled;
        this.recoveryEnabled = recoveryEnabled;
    }

    public org.infinispan.transaction.TransactionMode getMode() {
        return this.mode;
    }

    public boolean isXAEnabled() {
        return this.xaEnabled;
    }

    public boolean isRecoveryEnabled() {
        return this.recoveryEnabled;
    }
}
