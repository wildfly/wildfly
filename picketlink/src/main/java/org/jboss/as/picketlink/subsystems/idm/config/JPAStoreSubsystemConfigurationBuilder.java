package org.jboss.as.picketlink.subsystems.idm.config;

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
