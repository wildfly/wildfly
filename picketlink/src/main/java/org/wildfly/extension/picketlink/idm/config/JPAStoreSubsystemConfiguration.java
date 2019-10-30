/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.picketlink.idm.config;

import org.wildfly.extension.picketlink.logging.PicketLinkLogger;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.picketlink.idm.config.JPAIdentityStoreConfiguration;
import org.picketlink.idm.config.SecurityConfigurationException;
import org.picketlink.idm.credential.handler.CredentialHandler;
import org.picketlink.idm.jpa.internal.JPAIdentityStore;
import org.picketlink.idm.model.AttributedType;
import org.picketlink.idm.spi.ContextInitializer;
import org.picketlink.idm.spi.IdentityStore;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Pedro Igor
 */
public class JPAStoreSubsystemConfiguration extends JPAIdentityStoreConfiguration {

    private final Module entityModule;
    private final String dataSourceJndiUrl;
    private final String entityManagerFactoryJndiName;
    private String entityModuleUnitName = "identity";

    JPAStoreSubsystemConfiguration(final String entityModuleName, final String entityModuleUnitName,
                                                final String dataSourceJndiUrl, final String entityManagerFactoryJndiName,
                                                final Set<Class<?>> entityTypes,
                                                final Map<Class<? extends AttributedType>, Set<IdentityOperation>> supportedTypes,
                                                final Map<Class<? extends AttributedType>, Set<IdentityOperation>> unsupportedTypes,
                                                final List<ContextInitializer> contextInitializers, final Map<String, Object> credentialHandlerProperties,
                                                final Set<Class<? extends CredentialHandler>> credentialHandlers, final boolean supportsAttribute,
                                                final boolean supportsCredential,
                                                final boolean supportsPermissions) throws SecurityConfigurationException {
        super(entityTypes, supportedTypes, unsupportedTypes, contextInitializers, credentialHandlerProperties, credentialHandlers, supportsAttribute, supportsCredential, supportsPermissions);

        if (entityModuleName != null) {
            ModuleLoader moduleLoader = Module.getBootModuleLoader();

            try {
                this.entityModule = moduleLoader.loadModule(ModuleIdentifier.fromString(entityModuleName));
            } catch (ModuleLoadException e) {
                throw PicketLinkLogger.ROOT_LOGGER.idmJpaEntityModuleNotFound(entityModuleName);
            }
        } else {
            this.entityModule = null;
        }

        if (entityModuleUnitName != null) {
            this.entityModuleUnitName = entityModuleUnitName;
        }

        this.dataSourceJndiUrl = dataSourceJndiUrl;
        this.entityManagerFactoryJndiName = entityManagerFactoryJndiName;
    }

    @Override
    public Class<? extends IdentityStore> getIdentityStoreType() {
        return JPAIdentityStore.class;
    }

    public Module getEntityModule() {
        return this.entityModule;
    }

    public String getDataSourceJndiUrl() {
        return this.dataSourceJndiUrl;
    }

    public String getEntityModuleUnitName() {
        return this.entityModuleUnitName;
    }

    public String getEntityManagerFactoryJndiName() {
        return entityManagerFactoryJndiName;
    }
}
