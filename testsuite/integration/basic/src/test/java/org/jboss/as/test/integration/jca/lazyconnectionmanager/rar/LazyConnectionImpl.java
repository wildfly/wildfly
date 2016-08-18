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

import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.LazyAssociatableConnectionManager;

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
