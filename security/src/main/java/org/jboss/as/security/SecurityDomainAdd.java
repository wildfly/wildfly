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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTHENTICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.security.Constants.ACL;
import static org.jboss.as.security.Constants.ACL_MODULES;
import static org.jboss.as.security.Constants.ADDITIONAL_PROPERTIES;
import static org.jboss.as.security.Constants.ALGORITHM;
import static org.jboss.as.security.Constants.AUDIT;
import static org.jboss.as.security.Constants.AUTHORIZATION;
import static org.jboss.as.security.Constants.AUTH_MODULES;
import static org.jboss.as.security.Constants.CACHE_TYPE;
import static org.jboss.as.security.Constants.CIPHER_SUITES;
import static org.jboss.as.security.Constants.CLASSIC;
import static org.jboss.as.security.Constants.CLIENT_ALIAS;
import static org.jboss.as.security.Constants.CLIENT_AUTH;
import static org.jboss.as.security.Constants.CODE;
import static org.jboss.as.security.Constants.FLAG;
import static org.jboss.as.security.Constants.IDENTITY_TRUST;
import static org.jboss.as.security.Constants.JASPI;
import static org.jboss.as.security.Constants.JSSE;
import static org.jboss.as.security.Constants.KEYSTORE;
import static org.jboss.as.security.Constants.LOGIN_MODULES;
import static org.jboss.as.security.Constants.LOGIN_MODULE_STACK;
import static org.jboss.as.security.Constants.LOGIN_MODULE_STACK_REF;
import static org.jboss.as.security.Constants.MAPPING;
import static org.jboss.as.security.Constants.MAPPING_MODULES;
import static org.jboss.as.security.Constants.MODULE;
import static org.jboss.as.security.Constants.MODULE_OPTIONS;
import static org.jboss.as.security.Constants.PASSWORD;
import static org.jboss.as.security.Constants.POLICY_MODULES;
import static org.jboss.as.security.Constants.PROTOCOLS;
import static org.jboss.as.security.Constants.PROVIDER;
import static org.jboss.as.security.Constants.PROVIDER_ARGUMENT;
import static org.jboss.as.security.Constants.PROVIDER_MODULES;
import static org.jboss.as.security.Constants.SERVER_ALIAS;
import static org.jboss.as.security.Constants.SERVICE_AUTH_TOKEN;
import static org.jboss.as.security.Constants.TRUST_MODULES;
import static org.jboss.as.security.Constants.TYPE;
import static org.jboss.as.security.Constants.URL;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;
import javax.security.auth.login.Configuration;
import javax.transaction.TransactionManager;

import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.as.clustering.infinispan.subsystem.EmbeddedCacheManagerService;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.security.plugins.SecurityDomainContext;
import org.jboss.as.security.service.JaasConfigurationService;
import org.jboss.as.security.service.SecurityDomainService;
import org.jboss.as.security.service.SecurityManagementService;
import org.jboss.as.txn.service.TransactionManagerService;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.inject.InjectionException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
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
import org.jboss.security.plugins.TransactionManagerLocator;

/**
 * Add a security domain configuration.
 *
 * @author Marcus Moyses
 * @author Brian Stansberry
 * @author Jason T. Greene
 */
class SecurityDomainAdd extends AbstractAddStepHandler {

    static final String OPERATION_NAME = ADD;

    private static final String CACHE_CONTAINER_NAME = "security";

    static final ModelNode getRecreateOperation(ModelNode address, ModelNode securityDomain) {
        return Util.getOperation(OPERATION_NAME, address, securityDomain);
    }

    static final SecurityDomainAdd INSTANCE = new SecurityDomainAdd();

    /**
     * Private to ensure a singleton.
     */
    private SecurityDomainAdd() {
    }

    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        SecurityDomainResourceDefinition.CACHE_TYPE.validateAndSet(operation, model);
    }

    protected void performRuntime(OperationContext context, ModelNode operation, final ModelNode model, final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) {
        PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String securityDomain = address.getLastElement().getValue();

        // This needs to run after all child resources so that they can detect a fresh state
        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                final Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
                launchServices(context, securityDomain, Resource.Tools.readModel(resource), verificationHandler, newControllers);
                context.completeStep();
            }
        }, OperationContext.Stage.RUNTIME);
    }

    void launchServices(OperationContext context, String securityDomain, ModelNode model) {
        launchServices(context, securityDomain, model, null, null);
    }

    public void launchServices(OperationContext context, String securityDomain, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) {
        final ApplicationPolicy applicationPolicy = createApplicationPolicy(securityDomain, model);
        final JSSESecurityDomain jsseSecurityDomain = createJSSESecurityDomain(context, securityDomain, model);
        final String cacheType = getAuthenticationCacheType(model);

        final SecurityDomainService securityDomainService = new SecurityDomainService(securityDomain,
                applicationPolicy, jsseSecurityDomain, cacheType);
        final ServiceTarget target = context.getServiceTarget();
        // some login modules may require the TransactionManager
        final Injector<TransactionManager> transactionManagerInjector = new Injector<TransactionManager>() {
            public void inject(final TransactionManager value) throws InjectionException {
                TransactionManagerLocator.setTransactionManager(value);
            }

            public void uninject() {
            }
        };
        ServiceBuilder<SecurityDomainContext> builder = target
                .addService(SecurityDomainService.SERVICE_NAME.append(securityDomain), securityDomainService)
                .addDependency(SecurityManagementService.SERVICE_NAME, ISecurityManagement.class,
                        securityDomainService.getSecurityManagementInjector())
                .addDependency(JaasConfigurationService.SERVICE_NAME, Configuration.class,
                        securityDomainService.getConfigurationInjector())
                .addDependency(ServiceBuilder.DependencyType.OPTIONAL, TransactionManagerService.SERVICE_NAME, TransactionManager.class,
                        transactionManagerInjector);

        if ("infinispan".equals(cacheType)) {
            builder.addDependency(EmbeddedCacheManagerService.getServiceName(CACHE_CONTAINER_NAME),
                    EmbeddedCacheManager.class, securityDomainService.getCacheManagerInjector());
        }

        if (verificationHandler != null) {
            builder.addListener(verificationHandler);
        }

        ServiceController<SecurityDomainContext> controller = builder.setInitialMode(ServiceController.Mode.ACTIVE).install();
        if (newControllers != null) {
            newControllers.add(controller);
        }
    }

    private ApplicationPolicy createApplicationPolicy(String securityDomain, final ModelNode model) {
        final ApplicationPolicy applicationPolicy = new ApplicationPolicy(securityDomain);

        boolean create;

        create  = processClassicAuth(securityDomain, model, applicationPolicy);
        create |= processJASPIAuth(securityDomain, model, applicationPolicy);
        create |= processAuthorization(securityDomain, model,applicationPolicy);
        create |= processACL(securityDomain, model, applicationPolicy);
        create |= processAudit(securityDomain, model, applicationPolicy);
        create |= processIdentityTrust(securityDomain, model, applicationPolicy);
        create |= processMapping(securityDomain, model, applicationPolicy);

        return create ? applicationPolicy : null;
    }

    private boolean processMapping(String securityDomain, ModelNode node, ApplicationPolicy applicationPolicy) {
        node = peek(node, MAPPING, CLASSIC);
        if (node == null)
            return false;

        List<ModelNode> modules = node.get(MAPPING_MODULES).asList();

        for (ModelNode module : modules) {
            MappingInfo mappingInfo = new MappingInfo(securityDomain);
            String codeName = extractCode(module, ModulesMap.MAPPING_MAP);

            String mappingType;
            if (module.hasDefined(TYPE))
                mappingType = module.get(TYPE).asString();
            else
                mappingType = MappingType.ROLE.toString();

            Map<String, Object> options = extractOptions(module);
            MappingModuleEntry entry = new MappingModuleEntry(codeName, options, mappingType);
            mappingInfo.add(entry);
            applicationPolicy.setMappingInfo(mappingType, mappingInfo);

            String moduleName = module.get(MODULE).asString();
            if(module.hasDefined(MODULE) && moduleName != null &&  moduleName.length() > 0 ) {
                mappingInfo.setJBossModuleName(moduleName);
            }
        }

        return true;
    }

    private boolean processIdentityTrust(String securityDomain, ModelNode node, ApplicationPolicy applicationPolicy) {
        node = peek(node, IDENTITY_TRUST, CLASSIC);
        if (node == null)
            return false;

        IdentityTrustInfo identityTrustInfo = new IdentityTrustInfo(securityDomain);
        List<ModelNode> modules = node.get(TRUST_MODULES).asList();
        for (ModelNode module : modules) {
            String codeName = module.require(CODE).asString();
            ControlFlag controlFlag = ControlFlag.valueOf(module.require(FLAG).asString());
            Map<String, Object> options = extractOptions(module);
            IdentityTrustModuleEntry entry = new IdentityTrustModuleEntry(codeName, options);
            entry.setControlFlag(controlFlag);
            identityTrustInfo.add(entry);

            String moduleName = module.get(MODULE).asString();
            if(module.hasDefined(MODULE) && moduleName != null &&  moduleName.length() > 0 ) {
                identityTrustInfo.setJBossModuleName(moduleName);
            }
        }
        applicationPolicy.setIdentityTrustInfo(identityTrustInfo);
        return true;
    }

    private boolean processAudit(String securityDomain, ModelNode node, ApplicationPolicy applicationPolicy) {
        node = peek(node, AUDIT, CLASSIC);
        if (node == null)
            return false;

        AuditInfo auditInfo = new AuditInfo(securityDomain);
        List<ModelNode> modules = node.get(PROVIDER_MODULES).asList();
        for (ModelNode module : modules) {
            String codeName = module.require(CODE).asString();
            Map<String, Object> options = extractOptions(module);
            AuditProviderEntry entry = new AuditProviderEntry(codeName, options);
            auditInfo.add(entry);

            String moduleName = module.get(MODULE).asString();
            if(module.hasDefined(MODULE) && moduleName != null &&  moduleName.length() > 0 ) {
                auditInfo.setJBossModuleName(moduleName);
            }
        }
        applicationPolicy.setAuditInfo(auditInfo);
        return true;
    }

    private boolean processACL(String securityDomain, ModelNode node, ApplicationPolicy applicationPolicy) {
        node = peek(node, ACL, CLASSIC);
        if (node == null)
            return false;

        ACLInfo aclInfo = new ACLInfo(securityDomain);
        List<ModelNode> modules = node.get(ACL_MODULES).asList();
        for (ModelNode module : modules) {
            String codeName = module.require(CODE).asString();
            ControlFlag controlFlag = ControlFlag.valueOf(module.require(FLAG).asString());
            Map<String, Object> options = extractOptions(module);
            ACLProviderEntry entry = new ACLProviderEntry(codeName, options);
            entry.setControlFlag(controlFlag);
            aclInfo.add(entry);

            String moduleName = module.get(MODULE).asString();
            if(module.hasDefined(MODULE) && moduleName != null &&  moduleName.length() > 0 ) {
                aclInfo.setJBossModuleName(moduleName);
            }

        }
        applicationPolicy.setAclInfo(aclInfo);
        return true;
    }

    private boolean processAuthorization(String securityDomain, ModelNode node, ApplicationPolicy applicationPolicy) {
        node = peek(node, AUTHORIZATION, CLASSIC);
        if (node == null)
            return false;

        AuthorizationInfo authzInfo = new AuthorizationInfo(securityDomain);
        List<ModelNode> modules = node.get(POLICY_MODULES).asList();
        for (ModelNode module : modules) {
            String codeName = this.extractCode(module, ModulesMap.AUTHORIZATION_MAP);
            ControlFlag controlFlag = ControlFlag.valueOf(module.require(FLAG).asString());
            Map<String, Object> options = extractOptions(module);
            AuthorizationModuleEntry authzModuleEntry = new AuthorizationModuleEntry(codeName, options);
            authzModuleEntry.setControlFlag(controlFlag);
            authzInfo.add(authzModuleEntry);

            String moduleName = module.get(MODULE).asString();
            if(module.hasDefined(MODULE) && moduleName != null &&  moduleName.length() > 0 ) {
                authzInfo.setJBossModuleName(moduleName);
            }
        }

        applicationPolicy.setAuthorizationInfo(authzInfo);
        return true;
    }

    private boolean processJASPIAuth(String securityDomain, ModelNode node, ApplicationPolicy applicationPolicy) {
        node = peek(node, AUTHENTICATION, JASPI);
        if (node == null)
            return false;

        JASPIAuthenticationInfo authenticationInfo = new JASPIAuthenticationInfo(securityDomain);
        Map<String, LoginModuleStackHolder> holders = new HashMap<String, LoginModuleStackHolder>();
        List<Property> stacks = node.get(LOGIN_MODULE_STACK).asPropertyList();
        for (Property stack : stacks) {
            String name = stack.getName();
            List<ModelNode> nodes = stack.getValue().get(LOGIN_MODULES).asList();

            final LoginModuleStackHolder holder = new LoginModuleStackHolder(name, null);
            holders.put(name, holder);
            authenticationInfo.add(holder);
            for (ModelNode login : nodes) {
                processLoginModules(login, authenticationInfo, new LoginModuleContainer() {
                    public void addAppConfigurationEntry(AppConfigurationEntry entry) {
                        holder.addAppConfigurationEntry(entry);
                    }
                });
            }
        }
        List<ModelNode> authModules = node.get(AUTH_MODULES).asList();
        for (ModelNode authModule : authModules) {
            String code = extractCode(authModule, ModulesMap.AUTHENTICATION_MAP);
            String loginStackRef = null;
            if (authModule.hasDefined(LOGIN_MODULE_STACK_REF))
                loginStackRef = authModule.get(LOGIN_MODULE_STACK_REF).asString();
            Map<String, Object> options = extractOptions(authModule) ;
            AuthModuleEntry entry = new AuthModuleEntry(code, options, loginStackRef);
            if (loginStackRef != null) {
                if (!holders.containsKey(loginStackRef)) {
                    throw SecurityMessages.MESSAGES.loginModuleStackIllegalArgument(loginStackRef);
                }
                entry.setLoginModuleStackHolder(holders.get(loginStackRef));
            }
            authenticationInfo.add(entry);
        }
        applicationPolicy.setAuthenticationInfo(authenticationInfo);
        return true;
    }

    private String extractCode(ModelNode node, Map<String, String> substitutions) {
        String code = node.require(CODE).asString();
        if (substitutions.containsKey(code))
            code = substitutions.get(code);
        return code;
    }

    private ModelNode peek(ModelNode node, String... args) {

        for (String arg : args) {
            if (!node.hasDefined(arg))
                return null;

            node = node.get(arg);
        }

        return node;
    }

    private boolean processClassicAuth(String securityDomain, ModelNode node, ApplicationPolicy applicationPolicy) {
        node = peek(node, AUTHENTICATION, CLASSIC);
        if (node == null)
            return false;

        final AuthenticationInfo authenticationInfo = new AuthenticationInfo(securityDomain);
        if (node.hasDefined(Constants.LOGIN_MODULES)) {
            processLoginModules(node.get(LOGIN_MODULES), authenticationInfo, new LoginModuleContainer() {
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

    private void processLoginModules(ModelNode node, BaseAuthenticationInfo authInfo, LoginModuleContainer container) {
        List<ModelNode> modules;
        modules = node.asList();
        for (ModelNode module : modules) {
            String codeName = extractCode(module, ModulesMap.AUTHENTICATION_MAP);
            LoginModuleControlFlag controlFlag = getControlFlag(module.require(FLAG).asString());
            Map<String, Object> options = extractOptions(module);
            AppConfigurationEntry entry = new AppConfigurationEntry(codeName, controlFlag, options);
            container.addAppConfigurationEntry(entry);
            String moduleName = module.get(MODULE).asString();
            if(module.hasDefined(MODULE) && moduleName != null && moduleName.length() > 0 ) {
                authInfo.setJBossModuleName(moduleName);
            }
        }
    }

    private Map<String, Object> extractOptions(ModelNode module) {
        Map<String, Object> options = new HashMap<String, Object>();
        if (module.hasDefined(MODULE_OPTIONS)) {
            for (Property prop : module.get(MODULE_OPTIONS).asPropertyList()) {
                options.put(prop.getName(), prop.getValue().asString());
            }
        }
        return options;
    }

    private JSSESecurityDomain createJSSESecurityDomain(OperationContext context, String securityDomain, ModelNode node) {
        node = peek(node, JSSE, CLASSIC);
        if (node == null)
            return null;

        final JBossJSSESecurityDomain jsseSecurityDomain = new JBossJSSESecurityDomain(securityDomain);
        String value = null;

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

        processKeyManager(node, Constants.KEY_MANAGER, new KeyManagerConfig() {
            public void setKeyManagerFactoryAlgorithm(String value) {
                jsseSecurityDomain.setKeyManagerFactoryAlgorithm(value);
            }
            public void setKeyManagerFactoryProvider(String value) {
                jsseSecurityDomain.setKeyManagerFactoryProvider(value);
            }
        });

         processKeyManager(node, Constants.TRUST_MANAGER, new KeyManagerConfig() {
            public void setKeyManagerFactoryAlgorithm(String value) {
                jsseSecurityDomain.setTrustManagerFactoryAlgorithm(value);
            }
            public void setKeyManagerFactoryProvider(String value) {
                jsseSecurityDomain.setTrustManagerFactoryProvider(value);
            }
        });

        if (node.hasDefined(CLIENT_ALIAS)) {
            value = node.get(CLIENT_ALIAS).asString();
            jsseSecurityDomain.setClientAlias(value);
        }
        if (node.hasDefined(SERVER_ALIAS)) {
            value = node.get(SERVER_ALIAS).asString();
            jsseSecurityDomain.setServerAlias(value);
        }
        if (node.hasDefined(CLIENT_AUTH)) {
            boolean clientAuth = node.get(CLIENT_AUTH).asBoolean();
            jsseSecurityDomain.setClientAuth(clientAuth);
        }
        if (node.hasDefined(SERVICE_AUTH_TOKEN)) {
            value = node.get(SERVICE_AUTH_TOKEN).asString();
            try {
                jsseSecurityDomain.setServiceAuthToken(value);
            } catch (Exception e) {
                throw SecurityMessages.MESSAGES.runtimeException(e);
            }
        }
        if (node.hasDefined(CIPHER_SUITES)) {
            value = node.get(CIPHER_SUITES).asString();
            jsseSecurityDomain.setCipherSuites(value);
        }
        if (node.hasDefined(PROTOCOLS)) {
            value = node.get(PROTOCOLS).asString();
            jsseSecurityDomain.setProtocols(value);
        }
        if (node.hasDefined(ADDITIONAL_PROPERTIES)) {
            Properties properties = new Properties();
            for (Property prop : node.get(ADDITIONAL_PROPERTIES).asPropertyList()) {
                properties.setProperty(prop.getName(), prop.getValue().asString());
            }
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

    private void processKeyStore(OperationContext context, ModelNode node, String name, KeyStoreConfig config) {
        final ModelNode value = peek(node, name, PASSWORD);
        final ModelNode type = peek(node, name, TYPE);
        final ModelNode url = peek(node, name, URL);
        final ModelNode provider = peek(node, name, PROVIDER);
        final ModelNode providerArgument = peek(node, name, PROVIDER_ARGUMENT);

        if (value != null) {
            try {
                config.setKeyStorePassword(value.asString());
            } catch (Exception e) {
                throw SecurityMessages.MESSAGES.runtimeException(e);
            }
        }
        if (type != null) {
            config.setKeyStoreType(type.asString());
        }
        if (url != null) {
            try {
                config.setKeyStoreURL(url.asString());
            } catch (IOException e) {
                throw SecurityMessages.MESSAGES.runtimeException(e);
            }
        }

        if (provider != null) {
            config.setKeyStoreProvider(provider.asString());
        }

        if (providerArgument != null) {
            config.setKeyStoreProviderArgument(providerArgument.asString());
        }
    }

     private interface KeyManagerConfig {
        void setKeyManagerFactoryAlgorithm(String value);
        void setKeyManagerFactoryProvider(String value);
    }

    private void processKeyManager(ModelNode node, String name, KeyManagerConfig config) {
        final ModelNode algorithm = peek(node, name, ALGORITHM);
        final ModelNode provider = peek(node, name, PROVIDER);

        if (algorithm != null) {
            config.setKeyManagerFactoryAlgorithm(algorithm.asString());
        }

        if (provider != null) {
            config.setKeyManagerFactoryProvider(provider.asString());
        }
    }


    private LoginModuleControlFlag getControlFlag(String flag) {
        switch (ModuleFlag.valueOf(flag.toUpperCase())) {
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
