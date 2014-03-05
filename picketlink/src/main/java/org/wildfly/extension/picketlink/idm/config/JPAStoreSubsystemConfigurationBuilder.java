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

import org.picketlink.idm.config.IdentityStoresConfigurationBuilder;
import org.picketlink.idm.config.JPAStoreConfigurationBuilder;

/**
 * @author Pedro Igor
 */
public class JPAStoreSubsystemConfigurationBuilder extends JPAStoreConfigurationBuilder {

    private String entityModule;
    private String entityModuleUnitName;
    private String dataSourceJndiUrl;
    private String entityManagerFactoryJndiName;

    public JPAStoreSubsystemConfigurationBuilder(final IdentityStoresConfigurationBuilder builder) {
        super(builder);
    }

    public JPAStoreSubsystemConfigurationBuilder entityModule(String entityModule) {
        this.entityModule = entityModule;
        return this;
    }

    public JPAStoreSubsystemConfigurationBuilder entityModuleUnitName(String entityModuleUnitName) {
        this.entityModuleUnitName = entityModuleUnitName;
        return this;
    }

    public JPAStoreSubsystemConfigurationBuilder dataSourceJndiUrl(String dataSourceJndiUrl) {
        this.dataSourceJndiUrl = dataSourceJndiUrl;
        return this;
    }

    public JPAStoreSubsystemConfigurationBuilder entityManagerFactoryJndiName(String entityManagerFactoryJndiName) {
        this.entityManagerFactoryJndiName = entityManagerFactoryJndiName;
        return this;
    }

    @Override
    public JPAStoreSubsystemConfiguration create() {
        return new JPAStoreSubsystemConfiguration(this.entityModule, this.entityModuleUnitName, this.dataSourceJndiUrl,
                                                         this.entityManagerFactoryJndiName, getMappedEntities(), getSupportedTypes(),
                                                         getUnsupportedTypes(), getContextInitializers(), getCredentialHandlerProperties(), getCredentialHandlers(),
                                                         isSupportAttributes(), isSupportCredentials(), isSupportPermissions());
    }

}
