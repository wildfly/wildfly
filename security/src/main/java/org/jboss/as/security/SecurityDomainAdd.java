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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.security.Constants.ACL;
import static org.jboss.as.security.Constants.ADDITIONAL_PROPERTIES;
import static org.jboss.as.security.Constants.AUDIT;
import static org.jboss.as.security.Constants.AUTHENTICATION;
import static org.jboss.as.security.Constants.AUTHENTICATION_JASPI;
import static org.jboss.as.security.Constants.AUTHORIZATION;
import static org.jboss.as.security.Constants.AUTH_MODULE;
import static org.jboss.as.security.Constants.CACHE_TYPE;
import static org.jboss.as.security.Constants.CIPHER_SUITES;
import static org.jboss.as.security.Constants.CLIENT_ALIAS;
import static org.jboss.as.security.Constants.CLIENT_AUTH;
import static org.jboss.as.security.Constants.CODE;
import static org.jboss.as.security.Constants.FLAG;
import static org.jboss.as.security.Constants.IDENTITY_TRUST;
import static org.jboss.as.security.Constants.JSSE;
import static org.jboss.as.security.Constants.KEYSTORE_PASSWORD;
import static org.jboss.as.security.Constants.KEYSTORE_PROVIDER;
import static org.jboss.as.security.Constants.KEYSTORE_PROVIDER_ARGUMENT;
import static org.jboss.as.security.Constants.KEYSTORE_TYPE;
import static org.jboss.as.security.Constants.KEYSTORE_URL;
import static org.jboss.as.security.Constants.KEY_MANAGER_FACTORY_ALGORITHM;
import static org.jboss.as.security.Constants.KEY_MANAGER_FACTORY_PROVIDER;
import static org.jboss.as.security.Constants.LOGIN_MODULE_STACK;
import static org.jboss.as.security.Constants.LOGIN_MODULE_STACK_REF;
import static org.jboss.as.security.Constants.MAPPING;
import static org.jboss.as.security.Constants.MODULE_OPTIONS;
import static org.jboss.as.security.Constants.NAME;
import static org.jboss.as.security.Constants.PROTOCOLS;
import static org.jboss.as.security.Constants.SERVER_ALIAS;
import static org.jboss.as.security.Constants.SERVICE_AUTH_TOKEN;
import static org.jboss.as.security.Constants.TRUSTSTORE_PASSWORD;
import static org.jboss.as.security.Constants.TRUSTSTORE_PROVIDER;
import static org.jboss.as.security.Constants.TRUSTSTORE_PROVIDER_ARGUMENT;
import static org.jboss.as.security.Constants.TRUSTSTORE_TYPE;
import static org.jboss.as.security.Constants.TRUSTSTORE_URL;
import static org.jboss.as.security.Constants.TRUST_MANAGER_FACTORY_ALGORITHM;
import static org.jboss.as.security.Constants.TRUST_MANAGER_FACTORY_PROVIDER;
import static org.jboss.as.security.Constants.TYPE;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
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
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.security.plugins.SecurityDomainContext;
import org.jboss.as.security.service.JaasConfigurationService;
import org.jboss.as.security.service.SecurityDomainService;
import org.jboss.as.security.service.SecurityManagementService;
import org.jboss.as.txn.TransactionManagerService;
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
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 * @author Brian Stansberry
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

    protected void populateModel(ModelNode operation, ModelNode model) {
        Util.copyParamsToModel(operation, model);
    }

    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) {
        PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String securityDomain = address.getLastElement().getValue();
        final ApplicationPolicy applicationPolicy = createApplicationPolicy(securityDomain, operation);
        final JSSESecurityDomain jsseSecurityDomain = createJSSESecurityDomain(securityDomain, operation);
        final String cacheType = getAuthenticationCacheType(operation);

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
                .addDependency(TransactionManagerService.SERVICE_NAME, TransactionManager.class,
                        transactionManagerInjector);

        if ("infinispan".equals(cacheType)) {
            builder.addDependency(EmbeddedCacheManagerService.getServiceName(CACHE_CONTAINER_NAME),
                    EmbeddedCacheManager.class, securityDomainService.getCacheManagerInjector());
        }
        newControllers.add(builder.addListener(verificationHandler)
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install());
    }

    private ApplicationPolicy createApplicationPolicy(String securityDomain, ModelNode operation) {
        ApplicationPolicy applicationPolicy = null;
        ModelNode node = null;
        List<ModelNode> modules;

        // authentication
        node = operation.get(AUTHENTICATION);
        if (node.isDefined()) {
            if (applicationPolicy == null)
                applicationPolicy = new ApplicationPolicy(securityDomain);
            AuthenticationInfo authenticationInfo = new AuthenticationInfo(securityDomain);
            modules = node.asList();
            for (ModelNode module : modules) {
                String codeName = module.require(CODE).asString();
                if (ModulesMap.AUTHENTICATION_MAP.containsKey(codeName))
                    codeName = ModulesMap.AUTHENTICATION_MAP.get(codeName);
                LoginModuleControlFlag controlFlag = getControlFlag(module.require(FLAG).asString());
                Map<String, Object> options = new HashMap<String, Object>();
                if (module.hasDefined(MODULE_OPTIONS)) {
                    for (Property prop : module.get(MODULE_OPTIONS).asPropertyList()) {
                        options.put(prop.getName(), prop.getValue().asString());
                    }
                }
                AppConfigurationEntry entry = new AppConfigurationEntry(codeName, controlFlag, options);
                authenticationInfo.addAppConfigurationEntry(entry);
            }
            applicationPolicy.setAuthenticationInfo(authenticationInfo);
        }

        // acl
        node = operation.get(ACL);
        if (node.isDefined()) {
            if (applicationPolicy == null)
                applicationPolicy = new ApplicationPolicy(securityDomain);
            ACLInfo aclInfo = new ACLInfo(securityDomain);
            modules = node.asList();
            for (ModelNode module : modules) {
                String codeName = module.require(CODE).asString();
                ControlFlag controlFlag = ControlFlag.valueOf(module.require(FLAG).asString());
                Map<String, Object> options = new HashMap<String, Object>();
                if (module.hasDefined(MODULE_OPTIONS)) {
                    for (Property prop : module.get(MODULE_OPTIONS).asPropertyList()) {
                        options.put(prop.getName(), prop.getValue().asString());
                    }
                }
                ACLProviderEntry entry = new ACLProviderEntry(codeName, options);
                entry.setControlFlag(controlFlag);
                aclInfo.add(entry);

            }
            applicationPolicy.setAclInfo(aclInfo);
        }

        // audit
        node = operation.get(AUDIT);
        if (node.isDefined()) {
            if (applicationPolicy == null)
                applicationPolicy = new ApplicationPolicy(securityDomain);
            AuditInfo auditInfo = new AuditInfo(securityDomain);
            modules = node.asList();
            for (ModelNode module : modules) {
                String codeName = module.require(CODE).asString();
                Map<String, Object> options = new HashMap<String, Object>();
                if (module.hasDefined(MODULE_OPTIONS)) {
                    for (Property prop : module.get(MODULE_OPTIONS).asPropertyList()) {
                        options.put(prop.getName(), prop.getValue().asString());
                    }
                }
                AuditProviderEntry entry = new AuditProviderEntry(codeName, options);
                auditInfo.add(entry);

            }
            applicationPolicy.setAuditInfo(auditInfo);
        }

        // authorization
        node = operation.get(AUTHORIZATION);
        if (node.isDefined()) {
            if (applicationPolicy == null)
                applicationPolicy = new ApplicationPolicy(securityDomain);
            AuthorizationInfo authorizationInfo = new AuthorizationInfo(securityDomain);
            modules = node.asList();
            for (ModelNode module : modules) {
                String codeName = module.require(CODE).asString();
                if (ModulesMap.AUTHORIZATION_MAP.containsKey(codeName))
                    codeName = ModulesMap.AUTHORIZATION_MAP.get(codeName);
                ControlFlag controlFlag = ControlFlag.valueOf(module.require(FLAG).asString());
                Map<String, Object> options = new HashMap<String, Object>();
                if (module.hasDefined(MODULE_OPTIONS)) {
                    for (Property prop : module.get(MODULE_OPTIONS).asPropertyList()) {
                        options.put(prop.getName(), prop.getValue().asString());
                    }
                }
                AuthorizationModuleEntry entry = new AuthorizationModuleEntry(codeName, options);
                entry.setControlFlag(controlFlag);
                authorizationInfo.add(entry);

            }
            applicationPolicy.setAuthorizationInfo(authorizationInfo);
        }

        // identity trust
        node = operation.get(IDENTITY_TRUST);
        if (node.isDefined()) {
            if (applicationPolicy == null)
                applicationPolicy = new ApplicationPolicy(securityDomain);
            IdentityTrustInfo identityTrustInfo = new IdentityTrustInfo(securityDomain);
            modules = node.asList();
            for (ModelNode module : modules) {
                String codeName = module.require(CODE).asString();
                ControlFlag controlFlag = ControlFlag.valueOf(module.require(FLAG).asString());
                Map<String, Object> options = new HashMap<String, Object>();
                if (module.hasDefined(MODULE_OPTIONS)) {
                    for (Property prop : module.get(MODULE_OPTIONS).asPropertyList()) {
                        options.put(prop.getName(), prop.getValue().asString());
                    }
                }
                IdentityTrustModuleEntry entry = new IdentityTrustModuleEntry(codeName, options);
                entry.setControlFlag(controlFlag);
                identityTrustInfo.add(entry);

            }
            applicationPolicy.setIdentityTrustInfo(identityTrustInfo);
        }

        // mapping
        node = operation.get(MAPPING);
        if (node.isDefined()) {
            if (applicationPolicy == null)
                applicationPolicy = new ApplicationPolicy(securityDomain);
            modules = node.asList();
            String mappingType = null;
            for (ModelNode module : modules) {
                MappingInfo mappingInfo = new MappingInfo(securityDomain);
                String codeName = module.require(CODE).asString();
                if (ModulesMap.MAPPING_MAP.containsKey(codeName))
                    codeName = ModulesMap.MAPPING_MAP.get(codeName);
                if (module.hasDefined(TYPE))
                    mappingType = module.get(TYPE).asString();
                else
                    mappingType = MappingType.ROLE.toString();
                Map<String, Object> options = new HashMap<String, Object>();
                if (module.hasDefined(MODULE_OPTIONS)) {
                    for (Property prop : module.get(MODULE_OPTIONS).asPropertyList()) {
                        options.put(prop.getName(), prop.getValue().asString());
                    }
                }
                MappingModuleEntry entry = new MappingModuleEntry(codeName, options, mappingType);
                mappingInfo.add(entry);
                applicationPolicy.setMappingInfo(mappingType, mappingInfo);
            }
        }

        // authentication-jaspi
        node = operation.get(AUTHENTICATION_JASPI);
        if (node.isDefined()) {
            if (applicationPolicy == null)
                applicationPolicy = new ApplicationPolicy(securityDomain);
            JASPIAuthenticationInfo authenticationInfo = new JASPIAuthenticationInfo(securityDomain);
            Map<String, LoginModuleStackHolder> holders = new HashMap<String, LoginModuleStackHolder>();
            ModelNode moduleStack = node.get(LOGIN_MODULE_STACK);
            modules = moduleStack.asList();
            for (ModelNode loginModuleStack : modules) {
                List<ModelNode> nodes = loginModuleStack.asList();
                Iterator<ModelNode> iter = nodes.iterator();
                ModelNode nameNode = iter.next();
                String name = nameNode.get(NAME).asString();
                LoginModuleStackHolder holder = new LoginModuleStackHolder(name, null);
                holders.put(name, holder);
                authenticationInfo.add(holder);
                while (iter.hasNext()) {
                    ModelNode lmsNode = iter.next();
                    List<ModelNode> lms = lmsNode.asList();
                    for (ModelNode lmNode : lms) {
                        String code = lmNode.require(CODE).asString();
                        if (ModulesMap.AUTHENTICATION_MAP.containsKey(code))
                            code = ModulesMap.AUTHENTICATION_MAP.get(code);
                        LoginModuleControlFlag controlFlag = getControlFlag(lmNode.require(FLAG).asString());
                        Map<String, Object> options = new HashMap<String, Object>();
                        if (lmNode.hasDefined(MODULE_OPTIONS)) {
                            for (Property prop : lmNode.get(MODULE_OPTIONS).asPropertyList()) {
                                options.put(prop.getName(), prop.getValue().asString());
                            }
                        }
                        AppConfigurationEntry entry = new AppConfigurationEntry(code, controlFlag, options);
                        holder.addAppConfigurationEntry(entry);
                    }
                }
            }
            ModelNode authModuleNode = node.get(AUTH_MODULE);
            List<ModelNode> authModules = authModuleNode.asList();
            for (ModelNode authModule : authModules) {
                String code = authModule.require(CODE).asString();
                if (ModulesMap.AUTHENTICATION_MAP.containsKey(code))
                    code = ModulesMap.AUTHENTICATION_MAP.get(code);
                String loginStackRef = null;
                if (authModule.hasDefined(LOGIN_MODULE_STACK_REF))
                    loginStackRef = authModule.get(LOGIN_MODULE_STACK_REF).asString();
                Map<String, Object> options = new HashMap<String, Object>();
                if (authModule.hasDefined(MODULE_OPTIONS)) {
                    for (Property prop : authModule.get(MODULE_OPTIONS).asPropertyList()) {
                        options.put(prop.getName(), prop.getValue().asString());
                    }
                }
                AuthModuleEntry entry = new AuthModuleEntry(code, options, loginStackRef);
                if (loginStackRef != null) {
                    if (!holders.containsKey(loginStackRef)) {
                        throw new IllegalArgumentException("auth-module references a login module stack that doesn't exist: "
                                + loginStackRef);
                    }
                    entry.setLoginModuleStackHolder(holders.get(loginStackRef));
                }
                authenticationInfo.add(entry);
            }
            applicationPolicy.setAuthenticationInfo(authenticationInfo);
        }

        return applicationPolicy;
    }

    private JSSESecurityDomain createJSSESecurityDomain(String securityDomain, ModelNode operation) {
        JBossJSSESecurityDomain jsseSecurityDomain = null;
        ModelNode node = operation.get(JSSE);
        if (node.isDefined()) {
            jsseSecurityDomain = new JBossJSSESecurityDomain(securityDomain);
            String value = null;
            if (node.hasDefined(KEYSTORE_PASSWORD)) {
                value = node.get(KEYSTORE_PASSWORD).asString();
                try {
                    jsseSecurityDomain.setKeyStorePassword(value);
                } catch (Exception e) {
                    throw new IllegalArgumentException(e);
                }
            }
            if (node.hasDefined(KEYSTORE_TYPE)) {
                value = node.get(KEYSTORE_TYPE).asString();
                jsseSecurityDomain.setKeyStoreType(value);
            }
            if (node.hasDefined(KEYSTORE_URL)) {
                value = node.get(KEYSTORE_URL).asString();
                try {
                    jsseSecurityDomain.setKeyStoreURL(value);
                } catch (IOException ioe) {
                    throw new IllegalArgumentException(ioe);
                }
            }
            if (node.hasDefined(KEYSTORE_PROVIDER)) {
                value = node.get(KEYSTORE_PROVIDER).asString();
                jsseSecurityDomain.setKeyStoreProvider(value);
            }
            if (node.hasDefined(KEYSTORE_PROVIDER_ARGUMENT)) {
                value = node.get(KEYSTORE_PROVIDER_ARGUMENT).asString();
                jsseSecurityDomain.setKeyStoreProviderArgument(value);
            }
            if (node.hasDefined(KEY_MANAGER_FACTORY_PROVIDER)) {
                value = node.get(KEY_MANAGER_FACTORY_PROVIDER).asString();
                jsseSecurityDomain.setKeyManagerFactoryProvider(value);
            }
            if (node.hasDefined(KEY_MANAGER_FACTORY_ALGORITHM)) {
                value = node.get(KEY_MANAGER_FACTORY_ALGORITHM).asString();
                jsseSecurityDomain.setKeyManagerFactoryAlgorithm(value);
            }
            if (node.hasDefined(TRUSTSTORE_PASSWORD)) {
                value = node.get(TRUSTSTORE_PASSWORD).asString();
                try {
                    jsseSecurityDomain.setTrustStorePassword(value);
                } catch (Exception e) {
                    throw new IllegalArgumentException(e);
                }
            }
            if (node.hasDefined(TRUSTSTORE_TYPE)) {
                value = node.get(TRUSTSTORE_TYPE).asString();
                jsseSecurityDomain.setTrustStoreType(value);
            }
            if (node.hasDefined(TRUSTSTORE_URL)) {
                value = node.get(TRUSTSTORE_URL).asString();
                try {
                    jsseSecurityDomain.setTrustStoreURL(value);
                } catch (IOException ioe) {
                    throw new IllegalArgumentException(ioe);
                }
            }
            if (node.hasDefined(TRUSTSTORE_PROVIDER)) {
                value = node.get(TRUSTSTORE_PROVIDER).asString();
                jsseSecurityDomain.setTrustStoreProvider(value);
            }
            if (node.hasDefined(TRUSTSTORE_PROVIDER_ARGUMENT)) {
                value = node.get(TRUSTSTORE_PROVIDER_ARGUMENT).asString();
                jsseSecurityDomain.setTrustStoreProviderArgument(value);
            }
            if (node.hasDefined(TRUST_MANAGER_FACTORY_PROVIDER)) {
                value = node.get(TRUST_MANAGER_FACTORY_PROVIDER).asString();
                jsseSecurityDomain.setTrustManagerFactoryProvider(value);
            }
            if (node.hasDefined(TRUST_MANAGER_FACTORY_ALGORITHM)) {
                value = node.get(TRUST_MANAGER_FACTORY_ALGORITHM).asString();
                jsseSecurityDomain.setTrustManagerFactoryAlgorithm(value);
            }
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
                    throw new IllegalArgumentException(e);
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
                value = node.get(ADDITIONAL_PROPERTIES).asString();
                // remove line breaks and tab
                value = value.replaceAll("\\r", "").replaceAll("\\n", "").replaceAll("\\t", "");
                String[] entries = value.split(";");
                Properties properties = new Properties();
                for (int i = 0; i < entries.length; i++) {
                    String tmp = entries[i];
                    // trim leading white spaces
                    tmp = tmp.replaceAll("^\\s+", "");
                    String[] entry = tmp.split("=");
                    properties.put(entry[0], entry[1]);
                }
                jsseSecurityDomain.setAdditionalProperties(properties);
            }
        }

        return jsseSecurityDomain;
    }

    private LoginModuleControlFlag getControlFlag(String flag) {
        if ("required".equalsIgnoreCase(flag))
            return LoginModuleControlFlag.REQUIRED;
        if ("sufficient".equalsIgnoreCase(flag))
            return LoginModuleControlFlag.SUFFICIENT;
        if ("optional".equalsIgnoreCase(flag))
            return LoginModuleControlFlag.OPTIONAL;
        if ("requisite".equalsIgnoreCase(flag))
            return LoginModuleControlFlag.REQUISITE;
        throw new RuntimeException(flag + " is not recognized");
    }

    private String getAuthenticationCacheType(ModelNode operation) {
        String type = null;
        if (operation.hasDefined(CACHE_TYPE)) {
            type = operation.get(CACHE_TYPE).asString();
        }

        return type;
    }

}
