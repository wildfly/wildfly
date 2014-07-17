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

package org.jboss.as.connector.subsystems.resourceadapters;

import static org.jboss.as.connector.logging.ConnectorLogger.SUBSYSTEM_RA_LOGGER;

import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.connector.util.CopyOnWriteArrayListMultiMap;
import org.jboss.jca.common.api.metadata.resourceadapter.Activation;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * A ResourceAdaptersService.
 * @author <a href="mailto:stefano.maestri@redhat.comdhat.com">Stefano
 *         Maestri</a>
 */
final class ResourceAdapterService implements Service<Activation> {

    private final Activation value;
    private final String name;
    private final InjectedValue<ResourceAdaptersService.ModifiableResourceAdaptors> resourceAdapters = new InjectedValue<ResourceAdaptersService.ModifiableResourceAdaptors>();
    private final InjectedValue<CopyOnWriteArrayListMultiMap> resourceAdaptersMap = new InjectedValue<CopyOnWriteArrayListMultiMap>();


    /** create an instance **/
    public ResourceAdapterService(ModifiableResourceAdapter value, String name) {
        this.value = value;
        this.name = name;
    }

    @Override
    public Activation getValue() throws IllegalStateException {
        return value;
    }

    @Override
    public void start(StartContext context) throws StartException {
        resourceAdapters.getValue().addActivation(value);
        resourceAdaptersMap.getValue().putIfAbsent(value.getArchive(), ServiceName.of(ConnectorServices.RA_SERVICE, name));
        SUBSYSTEM_RA_LOGGER.debugf("Starting ResourceAdapter Service");
    }

    @Override
    public void stop(StopContext context) {
        resourceAdapters.getValue().removeActivation(value);
        resourceAdaptersMap.getValue().remove(value.getArchive(), ServiceName.of(ConnectorServices.RA_SERVICE, name));
        SUBSYSTEM_RA_LOGGER.debugf("Stopping ResourceAdapter Service");
    }

    public Injector<ResourceAdaptersService.ModifiableResourceAdaptors> getResourceAdaptersInjector() {
        return resourceAdapters;
    }

    public Injector<CopyOnWriteArrayListMultiMap> getResourceAdaptersMapInjector() {
            return resourceAdaptersMap;
        }

}
