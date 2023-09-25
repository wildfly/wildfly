/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.lazyconnectionmanager.rar;

import jakarta.resource.spi.ConnectionManager;
import jakarta.resource.spi.ConnectionRequestInfo;
import jakarta.resource.spi.LazyAssociatableConnectionManager;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:jesper.pedersen@ironjacamar.org">Jesper Pedersen</a>
 * @author <a href="mailto:msimka@redhat.com">Martin Simka</a>
 */
public class LazyConnectionImpl implements LazyConnection {

    private static Logger logger = Logger.getLogger(LazyConnectionImpl.class);

    private ConnectionManager cm;
    private LazyManagedConnection mc;
    private LazyManagedConnectionFactory mcf;
    private ConnectionRequestInfo cri;

    public LazyConnectionImpl(ConnectionManager cm, LazyManagedConnection mc, LazyManagedConnectionFactory mcf, ConnectionRequestInfo cri) {
        logger.trace("#LazyConnectionImpl");
        this.cm = cm;
        this.mc = mc;
        this.mcf = mcf;
        this.cri = cri;
    }

    @Override
    public boolean isManagedConnectionSet() {
        logger.trace("#LazyConnectionImpl.isManagedConnectionSet");
        return mc != null;
    }

    @Override
    public boolean closeManagedConnection() {
        logger.trace("#LazyConnectionImpl.closeManagedConnection");
        if (isManagedConnectionSet()) {
            try {
                mc.dissociateConnections();
                mc = null;
                return true;
            } catch (Throwable t) {
                logger.error("closeManagedConnection()", t);
            }
        }
        return false;
    }

    @Override
    public boolean associate() {
        logger.trace("#LazyConnectionImpl.associate");
        if (mc == null) {
            if (cm instanceof LazyAssociatableConnectionManager) {
                try {
                    LazyAssociatableConnectionManager lcm = (LazyAssociatableConnectionManager) cm;
                    lcm.associateConnection(this, mcf, cri);
                    return true;
                } catch (Throwable t) {
                    logger.error("associate()", t);
                }
            }
        }
        return false;
    }

    @Override
    public boolean isEnlisted() {
        logger.trace("#LazyConnectionImpl.isEnlisted");
        return mc.isEnlisted();
    }

    @Override
    public boolean enlist() {
        logger.trace("#LazyConnectionImpl.enlist");
        return mc.enlist();
    }

    @Override
    public void close() {
        logger.trace("#LazyConnectionImpl.close");
        if (mc != null) {
            mc.closeHandle(this);
        } else {
            if (cm instanceof LazyAssociatableConnectionManager) {
                LazyAssociatableConnectionManager lacm = (LazyAssociatableConnectionManager) cm;
                lacm.inactiveConnectionClosed(this, mcf);
            }
        }

    }

    public void setManagedConnection(LazyManagedConnection mc) {
        logger.trace("#LazyConnectionImpl.setManagedConnection");
        this.mc = mc;
    }
}
