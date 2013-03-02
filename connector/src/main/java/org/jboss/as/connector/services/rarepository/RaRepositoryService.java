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

package org.jboss.as.connector.services.rarepository;

import static org.jboss.as.connector.logging.ConnectorLogger.MDR_LOGGER;

import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.jca.core.rar.SimpleResourceAdapterRepository;
import org.jboss.jca.core.spi.mdr.MetadataRepository;
import org.jboss.jca.core.spi.rar.ResourceAdapterRepository;
import org.jboss.jca.core.spi.transaction.TransactionIntegration;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * A MdrService. it provide access to IronJacamar's metadata repository
 * @author <a href="mailto:stefano.maestri@redhat.com">Stefano Maestri</a>
 */
public final class RaRepositoryService implements Service<ResourceAdapterRepository> {

    private final ResourceAdapterRepository value;

    private final InjectedValue<MetadataRepository> mdrValue = new InjectedValue<MetadataRepository>();
    private final InjectedValue<TransactionIntegration> tiValue = new InjectedValue<TransactionIntegration>();

    /**
     * Create instance
     */
    public RaRepositoryService() {
        this.value = new SimpleResourceAdapterRepository();

    }

    @Override
    public ResourceAdapterRepository getValue() {
        return ConnectorServices.notNull(value);
    }

    @Override
    public void start(StartContext context) throws StartException {
        ((SimpleResourceAdapterRepository) value).setMetadataRepository(mdrValue.getValue());
        ((SimpleResourceAdapterRepository) value).setTransactionIntegration(tiValue.getValue());
        MDR_LOGGER.debugf("Starting service RaRepositoryService");
    }

    @Override
    public void stop(StopContext context) {

    }

    public Injector<MetadataRepository> getMdrInjector() {
        return mdrValue;
    }

    public Injector<TransactionIntegration> getTransactionIntegrationInjector() {
        return tiValue;
    }

}
