/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.lazyconnectionmanager.rar;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.BootstrapContext;
import jakarta.resource.spi.ResourceAdapter;
import jakarta.resource.spi.ResourceAdapterInternalException;
import jakarta.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:jesper.pedersen@ironjacamar.org">Jesper Pedersen</a>
 * @author <a href="mailto:msimka@redhat.com">Martin Simka</a>
 */
public class LazyResourceAdapter implements ResourceAdapter {

    private static Logger logger = Logger.getLogger(LazyResourceAdapter.class);

    private Boolean enable;
    private Boolean localTransaction;
    private Boolean xaTransaction;

    public LazyResourceAdapter() {
        logger.trace("#LazyResourceAdapter");
        enable = Boolean.TRUE;
        localTransaction = Boolean.FALSE;
        xaTransaction = Boolean.FALSE;
    }

    @Override
    public void start(BootstrapContext ctx) throws ResourceAdapterInternalException {
        logger.trace("#LazyResourceAdapter.start");
    }

    @Override
    public void stop() {
        logger.trace("#LazyResourceAdapter.stop");
    }

    @Override
    public void endpointActivation(MessageEndpointFactory endpointFactory, ActivationSpec spec) throws ResourceException {
        logger.trace("#LazyResourceAdapter.endpointActivation");
    }

    @Override
    public void endpointDeactivation(MessageEndpointFactory endpointFactory, ActivationSpec spec) {
        logger.trace("#LazyResourceAdapter.endpointDeactivation");
    }

    @Override
    public XAResource[] getXAResources(ActivationSpec[] specs) throws ResourceException {
        logger.trace("#LazyResourceAdapter.getXAResources");
        return null;
    }

    public Boolean getEnable() {
        return enable;
    }

    public void setEnable(Boolean enable) {
        this.enable = enable;
    }

    public Boolean getXATransaction() {
        return xaTransaction;
    }

    public void setXATransaction(Boolean xaTransaction) {
        this.xaTransaction = xaTransaction;
    }

    public Boolean getLocalTransaction() {
        return localTransaction;
    }

    public void setLocalTransaction(Boolean localTransaction) {
        this.localTransaction = localTransaction;
    }

    @Override
    public int hashCode() {
        int result = 17;
        if (enable != null) { result += 31 * result + 7 * enable.hashCode(); } else { result += 31 * result + 7; }
        if (localTransaction != null) { result += 31 * result + 7 * localTransaction.hashCode(); } else { result += 31 * result + 7; }
        if (xaTransaction != null) { result += 31 * result + 7 * xaTransaction.hashCode(); } else { result += 31 * result + 7; }
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) { return false; }
        if (other == this) { return true; }
        if (!(other instanceof LazyResourceAdapter)) { return false; }
        LazyResourceAdapter obj = (LazyResourceAdapter) other;
        boolean result = true;
        if (result) {
            if (localTransaction == null) { result = obj.getLocalTransaction() == null; } else { result = localTransaction.equals(obj.getLocalTransaction()); }
        }
        if (result) {
            if (xaTransaction == null) { result = obj.getXATransaction() == null; } else { result = xaTransaction.equals(obj.getXATransaction()); }
        }
        return result;
    }
}
