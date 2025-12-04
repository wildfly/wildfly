/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
    private final InjectedValue<CopyOnWriteArrayListMultiMap> configuredAdaptersService = new InjectedValue<>();


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
        configuredAdaptersService.getValue().putIfAbsent(value.getArchive(), ServiceName.of(ConnectorServices.RA_SERVICE, name));
        SUBSYSTEM_RA_LOGGER.debugf("Starting ResourceAdapter Service");
    }

    @Override
    public void stop(StopContext context) {
        resourceAdapters.getValue().removeActivation(value);
        configuredAdaptersService.getValue().remove(value.getArchive());
        SUBSYSTEM_RA_LOGGER.debugf("Stopping ResourceAdapter Service");
    }

    public Injector<ResourceAdaptersService.ModifiableResourceAdaptors> getResourceAdaptersInjector() {
        return resourceAdapters;
    }

    public Injector<CopyOnWriteArrayListMultiMap> getResourceAdaptersSubsystemInjector() {
            return configuredAdaptersService;
        }

}
