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

package org.jboss.as.security;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTHENTICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.security.Constants.ACL;
import static org.jboss.as.security.Constants.ACL_MODULE;
import static org.jboss.as.security.Constants.ADDITIONAL_PROPERTIES;
import static org.jboss.as.security.Constants.ALGORITHM;
import static org.jboss.as.security.Constants.AUDIT;
import static org.jboss.as.security.Constants.AUTHORIZATION;
import static org.jboss.as.security.Constants.AUTH_MODULE;
import static org.jboss.as.security.Constants.CACHE_TYPE;
import static org.jboss.as.security.Constants.CIPHER_SUITES;
import static org.jboss.as.security.Constants.CLASSIC;
import static org.jboss.as.security.Constants.CLIENT_ALIAS;
import static org.jboss.as.security.Constants.CLIENT_AUTH;
import static org.jboss.as.security.Constants.FLAG;
import static org.jboss.as.security.Constants.IDENTITY_TRUST;
import static org.jboss.as.security.Constants.JASPI;
import static org.jboss.as.security.Constants.JSSE;
import static org.jboss.as.security.Constants.KEYSTORE;
import static org.jboss.as.security.Constants.LOGIN_MODULE;
import static org.jboss.as.security.Constants.LOGIN_MODULE_STACK;
import static org.jboss.as.security.Constants.LOGIN_MODULE_STACK_REF;
import static org.jboss.as.security.Constants.MAPPING;
import static org.jboss.as.security.Constants.MAPPING_MODULE;
import static org.jboss.as.security.Constants.PASSWORD;
import static org.jboss.as.security.Constants.POLICY_MODULE;
import static org.jboss.as.security.Constants.PROTOCOLS;
import static org.jboss.as.security.Constants.PROVIDER;
import static org.jboss.as.security.Constants.PROVIDER_ARGUMENT;
import static org.jboss.as.security.Constants.PROVIDER_MODULE;
import static org.jboss.as.security.Constants.SERVER_ALIAS;
import static org.jboss.as.security.Constants.SERVICE_AUTH_TOKEN;
import static org.jboss.as.security.Constants.TRUST_MODULE;
import static org.jboss.as.security.Constants.TYPE;
import static org.jboss.as.security.Constants.URL;
import static org.jboss.as.security.SecurityDomainResourceDefinition.CACHE_CONTAINER_NAME;
import static org.jboss.as.security.SecurityDomainResourceDefinition.LEGACY_SECURITY_DOMAIN;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;
import javax.security.auth.login.Configuration;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.security.logging.SecurityLogger;
import org.jboss.as.security.plugins.SecurityDomainContext;
import org.jboss.as.security.service.JaasConfigurationService;
import org.jboss.as.security.service.SecurityDomainService;
import org.jboss.as.security.service.SecurityManagementService;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.security.ISecurityManagement;
import org.jboss.security.JBossJSSESecurityDomain;
import org.jboss.security.JSSESecurityDomain;
import org.jboss.security.acl.config.ACLProviderEntry;
import org.jboss.security.audit.config.AuditProviderEntry;
import org.jboss.security.auth.container.config.AuthModuleEntry;
import org.jboss.security.auth.login.AuthenticationInfo;
import org.jboss.security.auth.login.BaseAuthenticationInfo;
import org.jboss.security.auth.login.JASPIAuthenticationInfo;
import org.jboss.security.auth.login.LoginModuleStackHolder;
import org.jboss.security.authorization.config.AuthorizationModuleEntry;
import org.jboss.security.config.ACLInfo;
import org.jboss.security.config.ApplicationPolicy;
import org.jboss.security.config.AuditInfo;
import org.jboss.security.config.AuthorizationInfo;
import org.jboss.security.config.ControlFlag;
import org.jboss.security.config.IdentityTrustInfo;
import org.jboss.security.config.MappingInfo;
import org.jboss.security.identitytrust.config.IdentityTrustModuleEntry;
import org.jboss.security.mapping.MappingType;
import org.jboss.security.mapping.config.MappingModuleEntry;
import org.wildfly.clustering.infinispan.spi.InfinispanCacheRequirement;
import org.wildfly.clustering.infinispan.spi.InfinispanDefaultCacheRequirement;
import org.wildfly.clustering.infinispan.spi.InfinispanRequirement;
import org.wildfly.clustering.infinispan.spi.service.CacheServiceConfigurator;
import org.wildfly.clustering.infinispan.spi.service.TemplateConfigurationServiceConfigurator;

/**
 * Add a security domain configuration.
 *
 * @author Marcus Moyses
 * @author Brian Stansberry
 * @author Jason T. Greene
 */
class SecurityDomainAdd extends AbstractAddStepHandler {

    private static final String DEFAULT_MODULE = "org.picketbox";
    private static final String LEGACY_CACHE_NAME = "auth-cache";

    static final SecurityDomainAdd INSTANCE = new SecurityDomainAdd();

    /**
     * Private to ensure a singleton.
     */
    private SecurityDomainAdd() {
        super(SecurityDomainResourceDefinition.CACHE_TYPE);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, final ModelNode model) {
        PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String securityDomain = address.getLastElement().getValue();

        // This needs to run after all child resources so that they can detect a fresh state
        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                final Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
                launchServices(context, securityDomain, Resource.Tools.readModel(resource));
                // Rollback handled by the parent step
                context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
            }
        }, OperationContext.Stage.RUNTIME);
    }

    @Override
    protected void recordCapabilitiesAndRequirements(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
        super.recordCapabilitiesAndRequirements(context, operation, resource);
        String cacheType = getAuthenticationCacheType(resource.getModel());
        if (SecurityDomainResourceDefinition.INFINISPAN_CACHE_TYPE.equals(cacheType)) {
            context.registerAdditionalCapabilityRequirement(InfinispanRequirement.CONTAINER.resolve(CACHE_CONTAINER_NAME),
                    LEGACY_SECURITY_DOMAIN.getDynamicName(context.getCurrentAddressValue()),
                    SecurityDomainResourceDefinition.CACHE_TYPE.getName());
        }
    }

    public void launchServices(OperationContext context, String securityDomain, ModelNode model) throws OperationFailedException {
        final ApplicationPolicy applicationPolicy = createApplicationPolicy(context, securityDomain, model);
        final JSSESecurityDomain jsseSecurityDomain = createJSSESecurityDomain(context, securityDomain, model);
        final String cacheType = getAuthenticationCacheType(model);

        final SecurityDomainService securityDomainService = new SecurityDomainService(securityDomain,
                applicationPolicy, jsseSecurityDomain, cacheType);
        final ServiceTarget target = context.getServiceTarget();
        ServiceBuilder<SecurityDomainContext> builder = target
                .addService(SecurityDomainService.SERVICE_NAME.append(securityDomain), securityDomainService)
                .addAliases(LEGACY_SECURITY_DOMAIN.getCapabilityServiceName(securityDomain))
                .addDependency(SecurityManagementService.SERVICE_NAME, ISecurityManagement.class,
                        securityDomainService.getSecurityManagementInjector())
                .addDependency(JaasConfigurationService.SERVICE_NAME, Configuration.class,
                        securityDomainService.getConfigurationInjector());

        if (jsseSecurityDomain != null) {
            builder.addDependency(ContextNames.JBOSS_CONTEXT_SERVICE_NAME.append("jaas"));
        }

        if (SecurityDomainResourceDefinition.INFINISPAN_CACHE_TYPE.equals(cacheType)) {
            String defaultCacheRequirementName = InfinispanDefaultCacheRequirement.CONFIGURATION.resolve(CACHE_CONTAINER_NAME);
            String legacyCacheRequirementName = InfinispanCacheRequirement.CONFIGURATION.resolve(CACHE_CONTAINER_NAME, LEGACY_CACHE_NAME);
            String capabilityName = LEGACY_SECURITY_DOMAIN.getDynamicName(context.getCurrentAddress());
            String cacheTypeAttributeName = SecurityDomainResourceDefinition.CACHE_TYPE.getName();
            String templateCacheName = null;

            if (!context.hasOptionalCapability(defaultCacheRequirementName, capabilityName, cacheTypeAttributeName) && context.hasOptionalCapability(legacyCacheRequirementName, capabilityName, cacheTypeAttributeName)) {
                SecurityLogger.ROOT_LOGGER.defaultCacheRequirementMissing(CACHE_CONTAINER_NAME, LEGACY_CACHE_NAME);
                templateCacheName = LEGACY_CACHE_NAME;
            }

            context.requireOptionalCapability(InfinispanCacheRequirement.CONFIGURATION.resolve(CACHE_CONTAINER_NAME, templateCacheName), capabilityName, cacheTypeAttributeName);

            ServiceName configurationServiceName = InfinispanCacheRequirement.CONFIGURATION.getServiceName(context, CACHE_CONTAINER_NAME, securityDomain);
            new TemplateConfigurationServiceConfigurator(configurationServiceName, CACHE_CONTAINER_NAME, securityDomain, templateCacheName).configure(context).build(target).install();
            ServiceName cacheServiceName = InfinispanCacheRequirement.CACHE.getServiceName(context, CACHE_CONTAINER_NAME, securityDomain);
            new CacheServiceConfigurator<>(cacheServiceName, CACHE_CONTAINER_NAME, securityDomain).configure(context).build(target).install();

            builder.addDependency(cacheServiceName, ConcurrentMap.class, securityDomainService.getCacheInjector());
        }

        builder.setInitialMode(ServiceController.Mode.ACTIVE).install();
    }

    private ApplicationPolicy createApplicationPolicy(OperationContext context, String securityDomain, final ModelNode model)
            throws OperationFailedException {
        final ApplicationPolicy applicationPolicy = new ApplicationPolicy(securityDomain);

        boolean create;

        create = processClassicAuth(context, securityDomain, model, applicationPolicy);
        create |= processJASPIAuth(context, securityDomain, model, applicationPolicy);
        create |= processAuthorization(context, securityDomain, model, applicationPolicy);
        create |= processACL(context, securityDomain, model, applicationPolicy);
        create |= processAudit(context, securityDomain, model, applicationPolicy);
        create |= processIdentityTrust(context, securityDomain, model, applicationPolicy);
        create |= processMapping(context, securityDomain, model, applicationPolicy);

        return create ? applicationPolicy : null;
    }

    private boolean processMapping(OperationContext context, String securityDomain, ModelNode node, ApplicationPolicy applicationPolicy)
            throws OperationFailedException {
        node = peek(node, MAPPING, CLASSIC, MAPPING_MODULE);
        if (node == null) { return false; }

        for (Property moduleProperty : node.asPropertyList()) {
            ModelNode module = moduleProperty.getValue();
            MappingInfo mappingInfo = new MappingInfo(securityDomain);
            String codeName = extractCode(context, module, ModulesMap.MAPPING_MAP);

            String mappingType;
            if (module.hasDefined(TYPE)) {
                mappingType = MappingModuleDefinition.TYPE.resolveModelAttribute(context, module).asString();
            } else {
                mappingType = MappingType.ROLE.toString();
            }

            Map<String, Object> options = extractOptions(context, module);
            MappingModuleEntry entry = new MappingModuleEntry(codeName, options, mappingType);
            mappingInfo.add(entry);
            applicationPolicy.setMappingInfo(mappingType, mappingInfo);

            ModelNode moduleName = LoginModuleResourceDefinition.MODULE.resolveModelAttribute(context, module);
            if (moduleName.isDefined() && !moduleName.asString().isEmpty()) {
                mappingInfo.addJBossModuleName(moduleName.asString());
            } else {
                mappingInfo.addJBossModuleName(DEFAULT_MODULE);
            }
        }

        return true;
    }

    private boolean processIdentityTrust(OperationContext context, String securityDomain, ModelNode node, ApplicationPolicy applicationPolicy)
            throws OperationFailedException {
        node = peek(node, IDENTITY_TRUST, CLASSIC, TRUST_MODULE);
        if (node == null) { return false; }

        IdentityTrustInfo identityTrustInfo = new IdentityTrustInfo(securityDomain);
        for (Property moduleProperty : node.asPropertyList()) {
            ModelNode module = moduleProperty.getValue();
            String codeName = LoginModuleResourceDefinition.CODE.resolveModelAttribute(context, module).asString();
            String flag = LoginModuleResourceDefinition.FLAG.resolveModelAttribute(context, module).asString();
            ControlFlag controlFlag = ControlFlag.valueOf(flag);
            Map<String, Object> options = extractOptions(context, module);
            IdentityTrustModuleEntry entry = new IdentityTrustModuleEntry(codeName, options);
            entry.setControlFlag(controlFlag);
            identityTrustInfo.add(entry);

            ModelNode moduleName = LoginModuleResourceDefinition.MODULE.resolveModelAttribute(context, module);
            if (moduleName.isDefined() && !moduleName.asString().isEmpty()) {
                identityTrustInfo.addJBossModuleName(moduleName.asString());
            } else {
                identityTrustInfo.addJBossModuleName(DEFAULT_MODULE);
            }
        }
        applicationPolicy.setIdentityTrustInfo(identityTrustInfo);
        return true;
    }

    private boolean processAudit(OperationContext context, String securityDomain, ModelNode node, ApplicationPolicy applicationPolicy)
            throws OperationFailedException {
        node = peek(node, AUDIT, CLASSIC, PROVIDER_MODULE);
        if (node == null) { return false; }

        AuditInfo auditInfo = new AuditInfo(securityDomain);
        for (Property moduleProperty : node.asPropertyList()) {
            ModelNode module = moduleProperty.getValue();
            String codeName = MappingProviderModuleDefinition.CODE.resolveModelAttribute(context, module).asString();
            Map<String, Object> options = extractOptions(context, module);
            AuditProviderEntry entry = new AuditProviderEntry(codeName, options);
            auditInfo.add(entry);

            ModelNode moduleName = MappingProviderModuleDefinition.MODULE.resolveModelAttribute(context, module);
            if (moduleName.isDefined() && !moduleName.asString().isEmpty()) {
                auditInfo.addJBossModuleName(moduleName.asString());
            } else {
                auditInfo.addJBossModuleName(DEFAULT_MODULE);
            }
        }
        applicationPolicy.setAuditInfo(auditInfo);
        return true;
    }

    private boolean processACL(OperationContext context, String securityDomain, ModelNode node, ApplicationPolicy applicationPolicy)
            throws OperationFailedException {
        node = peek(node, ACL, CLASSIC, ACL_MODULE);
        if (node == null) { return false; }

        ACLInfo aclInfo = new ACLInfo(securityDomain);
        for (Property moduleProperty : node.asPropertyList()) {
            ModelNode module = moduleProperty.getValue();
            String codeName = LoginModuleResourceDefinition.CODE.resolveModelAttribute(context, module).asString();
            String flag = LoginModuleResourceDefinition.FLAG.resolveModelAttribute(context, module).asString();
            ControlFlag controlFlag = ControlFlag.valueOf(flag);
            Map<String, Object> options = extractOptions(context, module);
            ACLProviderEntry entry = new ACLProviderEntry(codeName, options);
            entry.setControlFlag(controlFlag);
            aclInfo.add(entry);

            ModelNode moduleName = LoginModuleResourceDefinition.MODULE.resolveModelAttribute(context, module);
            if (moduleName.isDefined() && !moduleName.asString().isEmpty()) {
                aclInfo.addJBossModuleName(moduleName.asString());
            } else {
                aclInfo.addJBossModuleName(DEFAULT_MODULE);
            }

        }
        applicationPolicy.setAclInfo(aclInfo);
        return true;
    }

    private boolean processAuthorization(OperationContext context, String securityDomain, ModelNode node, ApplicationPolicy applicationPolicy)
            throws OperationFailedException {
        node = peek(node, AUTHORIZATION, CLASSIC, POLICY_MODULE);
        if (node == null) { return false; }

        AuthorizationInfo authzInfo = new AuthorizationInfo(securityDomain);
        for (Property moduleProperty : node.asPropertyList()) {
            ModelNode module = moduleProperty.getValue();
            String codeName = extractCode(context, module, ModulesMap.AUTHORIZATION_MAP);
            String flag = LoginModuleResourceDefinition.FLAG.resolveModelAttribute(context, module).asString();
            ControlFlag controlFlag = ControlFlag.valueOf(flag);
            Map<String, Object> options = extractOptions(context, module);
            AuthorizationModuleEntry authzModuleEntry = new AuthorizationModuleEntry(codeName, options);
            authzModuleEntry.setControlFlag(controlFlag);
            authzInfo.add(authzModuleEntry);

            ModelNode moduleName = LoginModuleResourceDefinition.MODULE.resolveModelAttribute(context, module);
            if (moduleName.isDefined() && !moduleName.asString().isEmpty()) {
                authzInfo.addJBossModuleName(moduleName.asString());
            } else {
                authzInfo.addJBossModuleName(DEFAULT_MODULE);
            }
        }

        applicationPolicy.setAuthorizationInfo(authzInfo);
        return true;
    }

    private boolean processJASPIAuth(OperationContext context, String securityDomain, ModelNode node, ApplicationPolicy applicationPolicy)
            throws OperationFailedException {
        node = peek(node, AUTHENTICATION, JASPI);
        if (node == null) { return false; }

        JASPIAuthenticationInfo authenticationInfo = new JASPIAuthenticationInfo(securityDomain);
        Map<String, LoginModuleStackHolder> holders = new HashMap<String, LoginModuleStackHolder>();
        if(node.hasDefined(LOGIN_MODULE_STACK)){
            List<Property> stacks = node.get(LOGIN_MODULE_STACK).asPropertyList();
            for (Property stack : stacks) {
                String name = stack.getName();
                ModelNode stackNode = stack.getValue();

                final LoginModuleStackHolder holder = new LoginModuleStackHolder(name, null);
                holders.put(name, holder);
                authenticationInfo.add(holder);
                if (stackNode.hasDefined(LOGIN_MODULE)) {
                    processLoginModules(context, stackNode.get(LOGIN_MODULE), authenticationInfo, new LoginModuleContainer() {
                        public void addAppConfigurationEntry(AppConfigurationEntry entry) {
                            holder.addAppConfigurationEntry(entry);
                        }
                    });
                }
            }
        }
        for (Property moduleProperty : node.get(AUTH_MODULE).asPropertyList()) {
            ModelNode authModule = moduleProperty.getValue();
            String code = extractCode(context, authModule, ModulesMap.AUTHENTICATION_MAP);
            String loginStackRef = null;
            if (authModule.hasDefined(LOGIN_MODULE_STACK_REF)) {
                loginStackRef = JASPIMappingModuleDefinition.LOGIN_MODULE_STACK_REF.resolveModelAttribute(context, authModule).asString();
            }
            Map<String, Object> options = extractOptions(context, authModule);
            AuthModuleEntry entry = new AuthModuleEntry(code, options, loginStackRef);
            if (authModule.hasDefined(FLAG)) {
                String flag = LoginModuleResourceDefinition.FLAG.resolveModelAttribute(context, authModule).asString();
                entry.setControlFlag(ControlFlag.valueOf(flag));
            }
            if (loginStackRef != null) {
                if (!holders.containsKey(loginStackRef)) {
                    throw SecurityLogger.ROOT_LOGGER.loginModuleStackIllegalArgument(loginStackRef);
                }
                entry.setLoginModuleStackHolder(holders.get(loginStackRef));
            }
            authenticationInfo.add(entry);

            ModelNode moduleName = LoginModuleResourceDefinition.MODULE.resolveModelAttribute(context, authModule);
            if (moduleName.isDefined() && !moduleName.asString().isEmpty()) {
                authenticationInfo.addJBossModuleName(moduleName.asString());
            } else {
                authenticationInfo.addJBossModuleName(DEFAULT_MODULE);
            }
        }
        applicationPolicy.setAuthenticationInfo(authenticationInfo);
        return true;
    }

    private static String extractCode(OperationContext context, ModelNode node, Map<String, String> substitutions) throws OperationFailedException {
        String code = LoginModuleResourceDefinition.CODE.resolveModelAttribute(context, node).asString();
        if (substitutions.containsKey(code)) { code = substitutions.get(code); }
        return code;
    }

    private ModelNode peek(ModelNode node, String... args) {
        for (String arg : args) {
            if (!node.hasDefined(arg)) { return null; }
            node = node.get(arg);
        }
        return node;
    }

    private boolean processClassicAuth(OperationContext context, String securityDomain, ModelNode node, ApplicationPolicy applicationPolicy)
            throws OperationFailedException {
        node = peek(node, AUTHENTICATION, CLASSIC);
        if (node == null) { return false; }

        final AuthenticationInfo authenticationInfo = new AuthenticationInfo(securityDomain);
        if (node.hasDefined(Constants.LOGIN_MODULE)) {
            processLoginModules(context, node.get(LOGIN_MODULE), authenticationInfo, new LoginModuleContainer() {
                public void addAppConfigurationEntry(AppConfigurationEntry entry) {
                    authenticationInfo.add(entry);
                }
            });
        }
        //Check for module
        applicationPolicy.setAuthenticationInfo(authenticationInfo);

        return true;
    }

    private interface LoginModuleContainer {
        void addAppConfigurationEntry(AppConfigurationEntry entry);
    }

    private void processLoginModules(OperationContext context, ModelNode node, BaseAuthenticationInfo authInfo, LoginModuleContainer container)
            throws OperationFailedException {
        for (Property moduleProperty : node.asPropertyList()) {
            ModelNode module = moduleProperty.getValue();
            String codeName = extractCode(context, module, ModulesMap.AUTHENTICATION_MAP);
            String flag = LoginModuleResourceDefinition.FLAG.resolveModelAttribute(context, module).asString();
            LoginModuleControlFlag controlFlag = getControlFlag(flag);
            Map<String, Object> options = extractOptions(context, module);
            AppConfigurationEntry entry = new AppConfigurationEntry(codeName, controlFlag, options);
            container.addAppConfigurationEntry(entry);
            ModelNode moduleName = LoginModuleResourceDefinition.MODULE.resolveModelAttribute(context, module);
            if (moduleName.isDefined() && !moduleName.asString().isEmpty()) {
                authInfo.addJBossModuleName(moduleName.asString());
            } else {
                authInfo.addJBossModuleName(DEFAULT_MODULE);
            }
        }
    }

    private Map<String, Object> extractOptions(OperationContext context, ModelNode module) throws OperationFailedException {
        return new LinkedHashMap<String, Object>(MappingModuleDefinition.MODULE_OPTIONS.unwrap(context, module));
    }

    private JSSESecurityDomain createJSSESecurityDomain(OperationContext context, String securityDomain, ModelNode node)
            throws OperationFailedException {
        node = peek(node, JSSE, CLASSIC);
        if (node == null) { return null; }

        final JBossJSSESecurityDomain jsseSecurityDomain = new JBossJSSESecurityDomain(securityDomain);


        processKeyStore(context, node, KEYSTORE, new KeyStoreConfig() {
            public void setKeyStorePassword(String value) throws Exception {
                jsseSecurityDomain.setKeyStorePassword(value);
            }

            public void setKeyStoreType(String value) {
                jsseSecurityDomain.setKeyStoreType(value);
            }

            public void setKeyStoreURL(String value) throws IOException {
                jsseSecurityDomain.setKeyStoreURL(value);
            }

            public void setKeyStoreProvider(String value) {
                jsseSecurityDomain.setKeyStoreProvider(value);
            }

            public void setKeyStoreProviderArgument(String value) {
                jsseSecurityDomain.setKeyStoreProviderArgument(value);
            }
        });

        processKeyStore(context, node, Constants.TRUSTSTORE, new KeyStoreConfig() {
            public void setKeyStorePassword(String value) throws Exception {
                jsseSecurityDomain.setTrustStorePassword(value);
            }

            public void setKeyStoreType(String value) {
                jsseSecurityDomain.setTrustStoreType(value);
            }

            public void setKeyStoreURL(String value) throws IOException {
                jsseSecurityDomain.setTrustStoreURL(value);
            }

            public void setKeyStoreProvider(String value) {
                jsseSecurityDomain.setTrustStoreProvider(value);
            }

            public void setKeyStoreProviderArgument(String value) {
                jsseSecurityDomain.setTrustStoreProviderArgument(value);
            }
        });

        processKeyManager(context, node, Constants.KEY_MANAGER, new KeyManagerConfig() {
            public void setKeyManagerFactoryAlgorithm(String value) {
                jsseSecurityDomain.setKeyManagerFactoryAlgorithm(value);
            }

            public void setKeyManagerFactoryProvider(String value) {
                jsseSecurityDomain.setKeyManagerFactoryProvider(value);
            }
        });

        processKeyManager(context, node, Constants.TRUST_MANAGER, new KeyManagerConfig() {
            public void setKeyManagerFactoryAlgorithm(String value) {
                jsseSecurityDomain.setTrustManagerFactoryAlgorithm(value);
            }

            public void setKeyManagerFactoryProvider(String value) {
                jsseSecurityDomain.setTrustManagerFactoryProvider(value);
            }
        });
        String value;
        if (node.hasDefined(CLIENT_ALIAS)) {
            value = JSSEResourceDefinition.CLIENT_ALIAS.resolveModelAttribute(context, node).asString();
            jsseSecurityDomain.setClientAlias(value);
        }
        if (node.hasDefined(SERVER_ALIAS)) {
            value = JSSEResourceDefinition.SERVER_ALIAS.resolveModelAttribute(context, node).asString();
            jsseSecurityDomain.setServerAlias(value);
        }
        if (node.hasDefined(CLIENT_AUTH)) {
            boolean clientAuth = JSSEResourceDefinition.CLIENT_AUTH.resolveModelAttribute(context, node).asBoolean();
            jsseSecurityDomain.setClientAuth(clientAuth);
        }
        if (node.hasDefined(SERVICE_AUTH_TOKEN)) {
            value = JSSEResourceDefinition.SERVICE_AUTH_TOKEN.resolveModelAttribute(context, node).asString();
            try {
                jsseSecurityDomain.setServiceAuthToken(value);
            } catch (Exception e) {
                throw SecurityLogger.ROOT_LOGGER.runtimeException(e);
            }
        }
        if (node.hasDefined(CIPHER_SUITES)) {
            value = JSSEResourceDefinition.CIPHER_SUITES.resolveModelAttribute(context, node).asString();
            jsseSecurityDomain.setCipherSuites(value);
        }
        if (node.hasDefined(PROTOCOLS)) {
            value = JSSEResourceDefinition.PROTOCOLS.resolveModelAttribute(context, node).asString();
            jsseSecurityDomain.setProtocols(value);
        }
        if (node.hasDefined(ADDITIONAL_PROPERTIES)) {
            Properties properties = new Properties();
            properties.putAll(JSSEResourceDefinition.ADDITIONAL_PROPERTIES.unwrap(context, node));
            jsseSecurityDomain.setAdditionalProperties(properties);
        }

        return jsseSecurityDomain;
    }

    private interface KeyStoreConfig {
        void setKeyStorePassword(String value) throws Exception;

        void setKeyStoreType(String value);

        void setKeyStoreURL(String value) throws IOException;

        void setKeyStoreProvider(String value);

        void setKeyStoreProviderArgument(String value);
    }

    private void processKeyStore(OperationContext context, ModelNode node, String name, KeyStoreConfig config)
            throws OperationFailedException {

        final ModelNode value = peek(node, name, PASSWORD);
        final ModelNode type = peek(node, name, TYPE);
        final ModelNode url = peek(node, name, URL);
        final ModelNode provider = peek(node, name, PROVIDER);
        final ModelNode providerArgument = peek(node, name, PROVIDER_ARGUMENT);

        if (value != null) {
            try {
                config.setKeyStorePassword(context.resolveExpressions(value).asString());
            } catch (Exception e) {
                throw SecurityLogger.ROOT_LOGGER.runtimeException(e);
            }
        }

        if (type != null) {
            config.setKeyStoreType(context.resolveExpressions(type).asString());
        }

        if (url != null) {
            try {
                config.setKeyStoreURL(context.resolveExpressions(url).asString());
            } catch (Exception e) {
                throw SecurityLogger.ROOT_LOGGER.runtimeException(e);
            }
        }

        if (provider != null) {
            config.setKeyStoreProvider(context.resolveExpressions(provider).asString());
        }

        if (providerArgument != null) {
            config.setKeyStoreProviderArgument(context.resolveExpressions(providerArgument).asString());
        }
    }

    private interface KeyManagerConfig {
        void setKeyManagerFactoryAlgorithm(String value);

        void setKeyManagerFactoryProvider(String value);
    }

    private void processKeyManager(OperationContext context, ModelNode node, String name, KeyManagerConfig config)
            throws OperationFailedException {

        final ModelNode algorithm = peek(node, name, ALGORITHM);
        final ModelNode provider = peek(node, name, PROVIDER);

        if (algorithm != null) {
            config.setKeyManagerFactoryAlgorithm(context.resolveExpressions(algorithm).asString());
        }

        if (provider != null) {
            config.setKeyManagerFactoryProvider(context.resolveExpressions(provider).asString());
        }
    }


    private LoginModuleControlFlag getControlFlag(String flag) {
        switch (ModuleFlag.valueOf(flag.toUpperCase(Locale.ENGLISH))) {
            case SUFFICIENT:
                return LoginModuleControlFlag.SUFFICIENT;
            case OPTIONAL:
                return LoginModuleControlFlag.OPTIONAL;
            case REQUISITE:
                return LoginModuleControlFlag.REQUISITE;
            case REQUIRED:
            default:
                return LoginModuleControlFlag.REQUIRED;
        }
    }

    static String getAuthenticationCacheType(ModelNode node) {
        String type = null;
        if (node.hasDefined(CACHE_TYPE)) {
            type = node.get(CACHE_TYPE).asString();
        }

        return type;
    }
}
