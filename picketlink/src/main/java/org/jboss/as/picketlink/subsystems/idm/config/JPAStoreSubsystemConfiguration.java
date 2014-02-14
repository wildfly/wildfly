package org.jboss.as.picketlink.subsystems.idm.config;

import org.jboss.as.picketlink.PicketLinkMessages;
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
                this.entityModule = moduleLoader.loadModule(ModuleIdentifier.create(entityModuleName));
            } catch (ModuleLoadException e) {
                throw PicketLinkMessages.MESSAGES.idmJpaEntityModuleNotFound(entityModuleName);
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
