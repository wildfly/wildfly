/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.jca.lazyconnectionmanager.rar;

import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.endpoint.MessageEndpointFactory;
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
