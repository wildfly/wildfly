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
import org.wildfly.common.function.ExceptionSupplier;
import org.wildfly.security.credential.source.CredentialSource;

/**
 * A ResourceAdaptersService.
 * @author <a href="mailto:stefano.maestri@redhat.comdhat.com">Stefano
 *         Maestri</a>
 */
final class ConnectionDefinitionService implements Service<ModifiableConnDef> {

    private final InjectedValue<ModifiableConnDef> value = new InjectedValue<>();
    private final InjectedValue<ExceptionSupplier<ModifiableConnDef, Exception>> connectionDefinitionSupplier = new InjectedValue<>();
    private final InjectedValue<ModifiableResourceAdapter> ra = new InjectedValue<ModifiableResourceAdapter>();
    private final InjectedValue<ExceptionSupplier<CredentialSource, Exception>> credentialSourceSupplier = new InjectedValue<>();

    /** create an instance **/
    public ConnectionDefinitionService() {
    }

    @Override
    public ModifiableConnDef getValue() throws IllegalStateException {
        return value.getValue();
    }

    @Override
    public void start(StartContext context) throws StartException {
        createConnectionDefinition();
        ra.getValue().addConnectionDefinition(getValue());

        SUBSYSTEM_RA_LOGGER.debugf("Starting ResourceAdapters Service");
    }

    @Override
    public void stop(StopContext context) {

    }

    public Injector<ModifiableResourceAdapter> getRaInjector() {
        return ra;
    }

    public InjectedValue<ExceptionSupplier<CredentialSource, Exception>> getCredentialSourceSupplier() {
        return credentialSourceSupplier;
    }

    public InjectedValue<ExceptionSupplier<ModifiableConnDef, Exception>> getConnectionDefinitionSupplierInjector() {
        return connectionDefinitionSupplier;
    }


    private void createConnectionDefinition() throws IllegalStateException {
        ExceptionSupplier<ModifiableConnDef, Exception> connDefSupplier = connectionDefinitionSupplier.getValue();
        try {
            if (connDefSupplier != null)
                value.inject(connDefSupplier.get());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

    }

}
