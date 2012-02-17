/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.connector.mdr;

import org.jboss.as.connector.ConnectorServices;
import org.jboss.jca.core.mdr.SimpleMetadataRepository;
import org.jboss.jca.core.spi.mdr.MetadataRepository;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

import static org.jboss.as.connector.ConnectorLogger.MDR_LOGGER;

/**
 * A MdrService. it provide access to IronJacamar's metadata repository
 * @author <a href="mailto:stefano.maestri@redhat.com">Stefano Maestri</a>
 */
public final class MdrService implements Service<MetadataRepository> {

    private final MetadataRepository value;

    /**
     * Create instance
     */
    public MdrService() {
        this.value = new SimpleMetadataRepository();
    }

    @Override
    public MetadataRepository getValue() throws IllegalStateException {
        return ConnectorServices.notNull(value);
    }

    @Override
    public void start(StartContext context) throws StartException {
        MDR_LOGGER.debugf("Starting service MDR");
    }

    @Override
    public void stop(StopContext context) {

    }

}
