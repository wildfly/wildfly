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

import org.jboss.as.connector.metadata.api.resourceadapter.ActivationSecurityUtil;
import org.jboss.as.core.security.ServerSecurityManager;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.security.SubjectFactory;
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

    protected final InjectedValue<SubjectFactory> subjectFactory = new InjectedValue<SubjectFactory>();
    private final InjectedValue<ServerSecurityManager> secManager = new InjectedValue<ServerSecurityManager>();


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
        if (ActivationSecurityUtil.isLegacySecurityRequired(ra.getValue())) {
            ra.getValue().setSubjectFactory(subjectFactory.getValue());
            ra.getValue().setSecManager(secManager.getOptionalValue());
        }
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

    public Injector<SubjectFactory> getSubjectFactoryInjector() {
        return subjectFactory;
    }

    public Injector<ServerSecurityManager> getServerSecurityManager() {
        return secManager;
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
