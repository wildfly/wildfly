/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.subsystems.resourceadapters;

import static org.jboss.as.connector.logging.ConnectorLogger.SUBSYSTEM_RA_LOGGER;

import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * A ResourceAdaptersService.
 * @author <a href="mailto:stefano.maestri@redhat.comdhat.com">Stefano
 *         Maestri</a>
 */
final class AdminObjectService implements Service<ModifiableAdminObject> {

    private final ModifiableAdminObject value;
    private final InjectedValue<ModifiableResourceAdapter> ra = new InjectedValue<ModifiableResourceAdapter>();


    /** create an instance **/
    public AdminObjectService(ModifiableAdminObject value) {
        this.value = value;
    }

    @Override
    public ModifiableAdminObject getValue() throws IllegalStateException {
        return value;
    }

    @Override
    public void start(StartContext context) throws StartException {
        ra.getValue().addAdminObject(value);
        SUBSYSTEM_RA_LOGGER.debugf("Starting ResourceAdapters Service");
    }

    @Override
    public void stop(StopContext context) {

    }

    public Injector<ModifiableResourceAdapter> getRaInjector() {
        return ra;
    }



}
