/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.txn.service;

import org.jboss.as.txn.integration.JBossContextXATerminator;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.tm.JBossXATerminator;
import org.wildfly.transaction.client.LocalTransactionContext;

/**
 * The XATerminator service for wildfly transaction client XATerminator.
 *
 * @author <a href="mailto:ochaloup@redhat.com">Ondrej Chaloupka</a>
 */
public final class JBossContextXATerminatorService implements Service<JBossContextXATerminator> {

    private volatile JBossContextXATerminator value;

    private final InjectedValue<JBossXATerminator> jbossXATerminatorInjector = new InjectedValue<>();
    private final InjectedValue<LocalTransactionContext> localTransactionContextInjector = new InjectedValue<>();

    public void start(final StartContext context) throws StartException {
        this.value = new JBossContextXATerminator(
            localTransactionContextInjector.getValue(), jbossXATerminatorInjector.getValue());
    }

    public void stop(final StopContext context) {
        this.value = null;
    }

    public InjectedValue<LocalTransactionContext> getLocalTransactionContextInjector() {
        return localTransactionContextInjector;
    }


    public InjectedValue<JBossXATerminator> getJBossXATerminatorInjector() {
        return jbossXATerminatorInjector;
    }

    public JBossContextXATerminator getValue() throws IllegalStateException {
        return TxnServices.notNull(value);
    }

}
