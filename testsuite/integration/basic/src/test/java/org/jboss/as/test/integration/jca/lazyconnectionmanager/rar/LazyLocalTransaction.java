/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.lazyconnectionmanager.rar;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.LocalTransaction;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:jesper.pedersen@ironjacamar.org">Jesper Pedersen</a>
 * @author <a href="mailto:msimka@redhat.com">Martin Simka</a>
 */
public class LazyLocalTransaction implements LocalTransaction {
    private static Logger logger = Logger.getLogger(LazyLocalTransaction.class);

    private LazyManagedConnection mc;

    public LazyLocalTransaction(LazyManagedConnection mc) {
        this.mc = mc;
    }

    @Override
    public void begin() throws ResourceException {
        logger.trace("#LazyLocalTransaction.begin");
        mc.setEnlisted(true);
    }

    @Override
    public void commit() throws ResourceException {
        logger.trace("#LazyLocalTransaction.commit");
        mc.setEnlisted(false);
    }

    @Override
    public void rollback() throws ResourceException {
        logger.trace("#LazyLocalTransaction.rollback");
        mc.setEnlisted(false);
    }
}
