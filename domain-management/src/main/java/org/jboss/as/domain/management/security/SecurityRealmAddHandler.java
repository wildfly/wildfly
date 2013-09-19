/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.domain.management.security;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTHENTICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTHORIZATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.JAAS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LDAP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LOCAL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SECRET;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_IDENTITY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SSL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TRUSTSTORE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.USER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.USERS;
import static org.jboss.as.domain.management.ModelDescriptionConstants.KEYSTORE_PATH;
import static org.jboss.as.domain.management.ModelDescriptionConstants.PASSWORD;
import static org.jboss.as.domain.management.ModelDescriptionConstants.PLUG_IN;
import static org.jboss.as.domain.management.ModelDescriptionConstants.PROPERTY;
import static org.jboss.msc.service.ServiceController.Mode.ON_DEMAND;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.core.security.ServerSecurityManager;
import org.jboss.as.domain.management.AuthMechanism;
import org.jboss.as.domain.management.CallbackHandlerFactory;
import org.jboss.as.domain.management.SSLIdentity;
import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.as.domain.management.connections.ldap.LdapConnectionManagerService;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedSetValue;
import org.jboss.msc.value.InjectedValue;

/**
 * Handler to add security realm definitions and register the service.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class SecurityRealmAddHandler implements OperationStepHandler {

    public static final SecurityRealmAddHandler INSTANCE = new SecurityRealmAddHandler();

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        ModelNode model = context.createResource(PathAddress.EMPTY_ADDRESS).getModel();
        SecurityRealmResourceDefinition.MAP_GROUPS_TO_ROLES.validateAndSet(operation, model);

        // Add a step validating that we have the correct authentication child resources
        ModelNode validationOp = AuthenticationValidatingHandler.createOperation(operation);
        context.addStep(validationOp, AuthenticationValidatingHandler.INSTANCE, OperationContext.Stage.MODEL);

        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                // Install another RUNTIME handler to actually install the services. This will run after the
                // RUNTIME handler for any child resources. Doing this will ensure that child resource handlers don't
                // see the installed services and can just ignore doing any RUNTIME stage work
                context.addStep(ServiceInstallStepHandler.INSTANCE, OperationContext.Stage.RUNTIME);
                context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
            }
        }, OperationContext.Stage.RUNTIME);

        context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
    }

    protected void installServices(final OperationContext context, final String realmName, final ModelNode model,
                                   final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers)
            throws OperationFailedException {
        final ModelNode plugIns = model.hasDefined(PLUG_IN) ? model.get(PLUG_IN) : null;
        final ModelNode authentication = model.hasDefined(AUTHENTICATION) ? model.get(AUTHENTICATION) : null;
        final ModelNode authorization = model.hasDefined(AUTHORIZATION) ? model.get(AUTHORIZATION) : null;
        final ModelNode serverIdentities = model.hasDefined(SERVER_IDENTITY) ? model.get(SERVER_IDENTITY) : null;

        final ServiceTarget serviceTarget = context.getServiceTarget();

        final boolean mapGroupsToRoles = SecurityRealmResourceDefinition.MAP_GROUPS_TO_ROLES.resolveModelAttribute(context, model).asBoolean();
        final SecurityRealmService securityRealmService = new SecurityRealmService(realmName, mapGroupsToRoles);
        final ServiceName realmServiceName = SecurityRealm.ServiceUtil.createServiceName(realmName);
        ServiceBuilder<?> realmBuilder = serviceTarget.addService(realmServiceName, securityRealmService);

        final boolean shareLdapConnections = shareLdapConnection(context, authentication, authorization);
        ModelNode authTruststore = null;
        if (plugIns != null) {
            addPlugInLoaderService(realmName, plugIns, serviceTarget, newControllers);
        }
        InjectedSetValue<CallbackHandlerService> injectorSet = securityRealmService.getCallbackHandlerService();
        if (authentication != null) {
            // Authentication can have a truststore defined at the same time as a username/password based mechanism.
            //
            // In this case it is expected certificate based authentication will first occur with a fallback to username/password
            // based authentication.
            if (authentication.hasDefined(TRUSTSTORE)) {
                authTruststore = authentication.require(TRUSTSTORE);
                addClientCertService(realmName, serviceTarget, newControllers, realmBuilder, injectorSet.injector());
            }
            if (authentication.hasDefined(LOCAL)) {
                addLocalService(context, authentication.require(LOCAL), realmName, serviceTarget, newControllers, realmBuilder, injectorSet.injector());
            }
            if (authentication.hasDefined(JAAS)) {
                addJaasService(context, authentication.require(JAAS), realmName, serviceTarget, newControllers, context.isNormalServer(), realmBuilder, injectorSet.injector());
            } else if (authentication.hasDefined(LDAP)) {
                addLdapService(context, authentication.require(LDAP), realmName, serviceTarget, newControllers, realmBuilder, injectorSet.injector(), shareLdapConnections);
            } else if (authentication.hasDefined(PLUG_IN)) {
                addPlugInAuthenticationService(context, authentication.require(PLUG_IN), realmName, securityRealmService, serviceTarget, newControllers, realmBuilder, injectorSet.injector());
            } else if (authentication.hasDefined(PROPERTIES)) {
                addPropertiesAuthenticationService(context, authentication.require(PROPERTIES), realmName, serviceTarget, newControllers, realmBuilder, injectorSet.injector());
            } else if (authentication.hasDefined(USERS)) {
                addUsersService(context, authentication.require(USERS), realmName, serviceTarget, newControllers, realmBuilder, injectorSet.injector());
            }
        }
        if (authorization != null) {
            if (authorization.hasDefined(PROPERTIES)) {
                addPropertiesAuthorizationService(context, authorization.require(PROPERTIES), realmName, serviceTarget, newControllers, realmBuilder, securityRealmService.getSubjectSupplementalInjector());
            } else if (authorization.hasDefined(PLUG_IN)) {
                addPlugInAuthorizationService(context, authorization.require(PLUG_IN), realmName, serviceTarget, newControllers, realmBuilder, securityRealmService.getSubjectSupplementalInjector());
            } else if (authorization.hasDefined(LDAP)) {
                addLdapAuthorizationService(context, authorization.require(LDAP), realmServiceName,realmName, serviceTarget, newControllers, realmBuilder, securityRealmService.getSubjectSupplementalInjector(), shareLdapConnections);
            }
        }

        ModelNode ssl = null;
        if (serverIdentities != null) {
            if (serverIdentities.hasDefined(SSL)) {
                ssl = serverIdentities.require(SSL);
            }
            if (serverIdentities.hasDefined(SECRET)) {
                addSecretService(context, serverIdentities.require(SECRET), realmName,serviceTarget,newControllers, realmBuilder, securityRealmService.getSecretCallbackFactory());
            }
        }

        if (ssl != null || authTruststore != null) {
            addSSLService(context, ssl, authTruststore, realmName, serviceTarget, newControllers, realmBuilder, securityRealmService.getSSLIdentityInjector());
        }

        realmBuilder.setInitialMode(Mode.ACTIVE);
        ServiceController<?> sc = realmBuilder.install();
        if (newControllers != null) {
            newControllers.add(sc);
        }
    }

    private boolean shareLdapConnection(final OperationContext context, final ModelNode authentication,
            final ModelNode authorization) throws OperationFailedException {
        if (authentication == null || authorization == null || authentication.hasDefined(LDAP) == false
                || authorization.hasDefined(LDAP) == false) {
            return false;
        }

        String authConnectionManager = LdapAuthenticationResourceDefinition.CONNECTION.resolveModelAttribute(context,
                authentication.require(LDAP)).asString();
        String authzConnectionManager = LdapAuthorizationResourceDefinition.CONNECTION.resolveModelAttribute(context,
                authorization.require(LDAP)).asString();

        return authConnectionManager.equals(authzConnectionManager);
    }

    private ServiceName addPlugInLoaderService(String realmName, ModelNode plugInModel,
            ServiceTarget serviceTarget, List<ServiceController<?>> newControllers) {
        ServiceName plugInLoaderName = PlugInLoaderService.ServiceUtil.createServiceName(realmName);

        List<Property> plugIns = plugInModel.asPropertyList();
        ArrayList<String> knownNames = new ArrayList<String>(plugIns.size());
        for (Property current : plugIns) {
            knownNames.add(current.getName());
        }
        PlugInLoaderService loaderService = new PlugInLoaderService(Collections.unmodifiableList(knownNames));
        ServiceBuilder<PlugInLoaderService> builder = serviceTarget.addService(plugInLoaderName, loaderService);
        final ServiceController<PlugInLoaderService> sc = builder.setInitialMode(Mode.ON_DEMAND).install();
        if(newControllers != null) {
            newControllers.add(sc);
        }

        return plugInLoaderName;
    }

    private void addClientCertService(String realmName, ServiceTarget serviceTarget,
            List<ServiceController<?>> newControllers, ServiceBuilder<?> realmBuilder, Injector<CallbackHandlerService> injector) {
        ServiceName clientCertServiceName = ClientCertCallbackHandler.ServiceUtil.createServiceName(realmName);
        ClientCertCallbackHandler clientCertCallbackHandler = new ClientCertCallbackHandler();

        ServiceBuilder<?> ccBuilder = serviceTarget.addService(clientCertServiceName, clientCertCallbackHandler);
        final ServiceController<?> sc = ccBuilder.setInitialMode(ON_DEMAND).install();
        if(newControllers != null) {
            newControllers.add(sc);
        }

        CallbackHandlerService.ServiceUtil.addDependency(realmBuilder, injector, clientCertServiceName, false);
    }

    private void addJaasService(OperationContext context, ModelNode jaas, String realmName, ServiceTarget serviceTarget,
            List<ServiceController<?>> newControllers, boolean injectServerManager, ServiceBuilder<?> realmBuilder, Injector<CallbackHandlerService> injector) throws OperationFailedException {
        ServiceName jaasServiceName = JaasCallbackHandler.ServiceUtil.createServiceName(realmName);
        String name = JaasAuthenticationResourceDefinition.NAME.resolveModelAttribute(context, jaas).asString();
        JaasCallbackHandler jaasCallbackHandler = new JaasCallbackHandler(name);

        ServiceBuilder<?> jaasBuilder = serviceTarget.addService(jaasServiceName, jaasCallbackHandler);
        if (injectServerManager) {
            jaasBuilder.addDependency(ServiceName.JBOSS.append("security", "simple-security-manager"),
                    ServerSecurityManager.class, jaasCallbackHandler.getSecurityManagerValue());
        }

        final ServiceController<?> sc = jaasBuilder.setInitialMode(ON_DEMAND).install();
        if(newControllers != null) {
            newControllers.add(sc);
        }

        CallbackHandlerService.ServiceUtil.addDependency(realmBuilder, injector, jaasServiceName, false);
    }

    private void addLdapService(OperationContext context, ModelNode ldap, String realmName, ServiceTarget serviceTarget,
            List<ServiceController<?>> newControllers, ServiceBuilder<?> realmBuilder, Injector<CallbackHandlerService> injector, boolean shareConnection) throws OperationFailedException {
        ServiceName ldapServiceName = UserLdapCallbackHandler.ServiceUtil.createServiceName(realmName);

        final String baseDn = LdapAuthenticationResourceDefinition.BASE_DN.resolveModelAttribute(context, ldap).asString();
        ModelNode node = LdapAuthenticationResourceDefinition.USERNAME_FILTER.resolveModelAttribute(context, ldap);
        final String usernameAttribute = node.isDefined() ? node.asString() : null;
        node = LdapAuthenticationResourceDefinition.ADVANCED_FILTER.resolveModelAttribute(context, ldap);
        final String advancedFilter = node.isDefined() ? node.asString() : null;
        final boolean recursive = LdapAuthenticationResourceDefinition.RECURSIVE.resolveModelAttribute(context, ldap).asBoolean();
        final boolean allowEmptyPasswords = LdapAuthenticationResourceDefinition.ALLOW_EMPTY_PASSWORDS.resolveModelAttribute(context, ldap).asBoolean();
        final String userDn = LdapAuthenticationResourceDefinition.USER_DN.resolveModelAttribute(context, ldap).asString();
        UserLdapCallbackHandler ldapCallbackHandler = new UserLdapCallbackHandler(baseDn, usernameAttribute, advancedFilter, recursive, userDn, allowEmptyPasswords, shareConnection);

        ServiceBuilder<?> ldapBuilder = serviceTarget.addService(ldapServiceName, ldapCallbackHandler);
        String connectionManager = LdapAuthenticationResourceDefinition.CONNECTION.resolveModelAttribute(context, ldap).asString();
        LdapConnectionManagerService.ServiceUtil.addDependency(ldapBuilder, ldapCallbackHandler.getConnectionManagerInjector(), connectionManager, false);

        final ServiceController<?> serviceController = ldapBuilder.setInitialMode(ON_DEMAND)
                .install();
        if(newControllers != null) {
            newControllers.add(serviceController);
        }

        CallbackHandlerService.ServiceUtil.addDependency(realmBuilder, injector, ldapServiceName, false);
    }

    private void addLocalService(OperationContext context, ModelNode local, String realmName, ServiceTarget serviceTarget,
            List<ServiceController<?>> newControllers, ServiceBuilder<?> realmBuilder, Injector<CallbackHandlerService> injector) throws OperationFailedException {
        ServiceName localServiceName = LocalCallbackHandlerService.ServiceUtil.createServiceName(realmName);

        ModelNode node = LocalAuthenticationResourceDefinition.DEFAULT_USER.resolveModelAttribute(context, local);
        String defaultUser = node.isDefined() ? node.asString() : null;
        node = LocalAuthenticationResourceDefinition.ALLOWED_USERS.resolveModelAttribute(context, local);
        String allowedUsers = node.isDefined() ? node.asString() : null;
        LocalCallbackHandlerService localCallbackHandler = new LocalCallbackHandlerService(defaultUser, allowedUsers);

        ServiceBuilder<?> jaasBuilder = serviceTarget.addService(localServiceName, localCallbackHandler);
        final ServiceController<?> serviceController = jaasBuilder.setInitialMode(ON_DEMAND).install();
        if(newControllers != null) {
            newControllers.add(serviceController);
        }

        CallbackHandlerService.ServiceUtil.addDependency(realmBuilder, injector, localServiceName, false);
    }

    private void addPlugInAuthenticationService(OperationContext context, ModelNode model, String realmName,
            SecurityRealmService registry, ServiceTarget serviceTarget, List<ServiceController<?>> newControllers,
            ServiceBuilder<?> realmBuilder, Injector<CallbackHandlerService> injector) throws OperationFailedException {
        ServiceName plugInServiceName = PlugInAuthenticationCallbackHandler.ServiceUtil.createServiceName(realmName);

        final String pluginName = PlugInAuthorizationResourceDefinition.NAME.resolveModelAttribute(context, model).asString();
        final Map<String, String> properties = resolveProperties(context, model);
        String mechanismName = PlugInAuthenticationResourceDefinition.MECHANISM.resolveModelAttribute(context, model).asString();
        AuthMechanism mechanism = AuthMechanism.valueOf(mechanismName);
        PlugInAuthenticationCallbackHandler plugInService = new PlugInAuthenticationCallbackHandler(registry.getName(),
                pluginName, properties, mechanism);

        ServiceBuilder<CallbackHandlerService> plugInBuilder = serviceTarget.addService(plugInServiceName, plugInService);
        PlugInLoaderService.ServiceUtil.addDependency(plugInBuilder, plugInService.getPlugInLoaderServiceValue(), realmName, false);

        final ServiceController<CallbackHandlerService> sc = plugInBuilder.setInitialMode(ON_DEMAND).install();
        if(newControllers != null) {
            newControllers.add(sc);
        }

        CallbackHandlerService.ServiceUtil.addDependency(realmBuilder, injector, plugInServiceName, false);
    }

    private void addPropertiesAuthenticationService(OperationContext context, ModelNode properties, String realmName,
            ServiceTarget serviceTarget, List<ServiceController<?>> newControllers, ServiceBuilder<?> realmBuilder,
            Injector<CallbackHandlerService> injector) throws OperationFailedException {

        ServiceName propsServiceName = PropertiesCallbackHandler.ServiceUtil.createServiceName(realmName);

        final String path = PropertiesAuthenticationResourceDefinition.PATH.resolveModelAttribute(context, properties).asString();
        final ModelNode relativeTo = PropertiesAuthenticationResourceDefinition.RELATIVE_TO.resolveModelAttribute(context, properties);
        final boolean plainText = PropertiesAuthenticationResourceDefinition.PLAIN_TEXT.resolveModelAttribute(context, properties).asBoolean();

        PropertiesCallbackHandler propsCallbackHandler = new PropertiesCallbackHandler(realmName, path, plainText);

        ServiceBuilder<?> propsBuilder = serviceTarget.addService(propsServiceName, propsCallbackHandler);

        if (relativeTo.isDefined()) {
            propsBuilder.addDependency(pathName(relativeTo.asString()), String.class, propsCallbackHandler.getRelativeToInjector());
        }

        final ServiceController<?> serviceController = propsBuilder.setInitialMode(ON_DEMAND)
                .install();

        if(newControllers != null) {
            newControllers.add(serviceController);
        }

        CallbackHandlerService.ServiceUtil.addDependency(realmBuilder, injector, propsServiceName, false);
    }

    private void addPropertiesAuthorizationService(OperationContext context, ModelNode properties,
            String realmName, ServiceTarget serviceTarget, List<ServiceController<?>> newControllers, ServiceBuilder<?> realmBuilder,
            InjectedValue<SubjectSupplementalService> injector) throws OperationFailedException {
        ServiceName propsServiceName = PropertiesSubjectSupplemental.ServiceUtil.createServiceName(realmName);

        final String path = PropertiesAuthorizationResourceDefinition.PATH.resolveModelAttribute(context, properties).asString();
        final ModelNode relativeTo = PropertiesAuthorizationResourceDefinition.RELATIVE_TO.resolveModelAttribute(context, properties);
        PropertiesSubjectSupplemental propsSubjectSupplemental = new PropertiesSubjectSupplemental(realmName, path);

        ServiceBuilder<?> propsBuilder = serviceTarget.addService(propsServiceName, propsSubjectSupplemental);
        if (relativeTo.isDefined()) {
            propsBuilder.addDependency(pathName(relativeTo.asString()), String.class,
                    propsSubjectSupplemental.getRelativeToInjector());
        }

        final ServiceController<?> serviceController = propsBuilder.setInitialMode(ON_DEMAND).install();
        if(newControllers != null) {
            newControllers.add(serviceController);
        }

        SubjectSupplementalService.ServiceUtil.addDependency(realmBuilder, injector, propsServiceName, false);
    }

    private void addPlugInAuthorizationService(OperationContext context, ModelNode model, String realmName,
            ServiceTarget serviceTarget, List<ServiceController<?>> newControllers, ServiceBuilder<?> realmBuilder,
            InjectedValue<SubjectSupplementalService> injector) throws OperationFailedException {

        ServiceName plugInServiceName = PlugInSubjectSupplemental.ServiceUtil.createServiceName(realmName);
        final String pluginName = PlugInAuthorizationResourceDefinition.NAME.resolveModelAttribute(context, model).asString();
        final Map<String, String> properties = resolveProperties(context, model);
        PlugInSubjectSupplemental plugInSubjectSupplemental = new PlugInSubjectSupplemental(realmName, pluginName, properties);

        ServiceBuilder<?> plugInBuilder = serviceTarget.addService(plugInServiceName, plugInSubjectSupplemental);
        PlugInLoaderService.ServiceUtil.addDependency(plugInBuilder, plugInSubjectSupplemental.getPlugInLoaderServiceValue(), realmName, false);

        final ServiceController<?> serviceController = plugInBuilder.setInitialMode(ON_DEMAND).install();
        if (newControllers != null) {
            newControllers.add(serviceController);
        }

        SubjectSupplementalService.ServiceUtil.addDependency(realmBuilder, injector, plugInServiceName, false);
    }

    private void addLdapAuthorizationService(OperationContext context, ModelNode ldap, ServiceName realmServiceName, String realmName, ServiceTarget serviceTarget,
            List<ServiceController<?>> newControllers, ServiceBuilder<?> realmBuilder,
            InjectedValue<SubjectSupplementalService> injector, boolean shareConnection) throws OperationFailedException {
        ServiceName ldapServiceName = realmServiceName.append(LdapSubjectSupplementalService.SERVICE_SUFFIX);

        final String baseDn = LdapAuthorizationResourceDefinition.BASE_DN.resolveModelAttribute(context, ldap).asString();
        ModelNode node = LdapAuthorizationResourceDefinition.USERNAME.resolveModelAttribute(context, ldap);
        final String usernameAttribute = node.isDefined() ? node.asString() : null;
        node = LdapAuthorizationResourceDefinition.ADVANCED_FILTER.resolveModelAttribute(context, ldap);
        final String advancedFilter = node.isDefined() ? node.asString() : null;
        final boolean recursive = LdapAuthorizationResourceDefinition.RECURSIVE.resolveModelAttribute(context, ldap).asBoolean();
        final boolean reverseGroup = LdapAuthorizationResourceDefinition.REVERSE_GROUP.resolveModelAttribute(context, ldap).asBoolean();
        final String rolesDn = LdapAuthorizationResourceDefinition.GROUPS_DN.resolveModelAttribute(context, ldap).asString();
        final String pattern = LdapAuthorizationResourceDefinition.PATTERN.resolveModelAttribute(context, ldap).asString();
        final String userDn = LdapAuthorizationResourceDefinition.USER_DN.resolveModelAttribute(context,ldap).asString();
        ModelNode groupNode = LdapAuthorizationResourceDefinition.GROUP.resolveModelAttribute(context, ldap);
        final int group = groupNode.isDefined() ? LdapAuthorizationResourceDefinition.GROUP.resolveModelAttribute(context, ldap).asInt() : 0;
        final ModelNode resultPatternNode = LdapAuthorizationResourceDefinition.RESULT_PATTERN.resolveModelAttribute(context, ldap);
        final String resultPattern = resultPatternNode.isDefined() ? resultPatternNode.asString(): null;
        LdapSubjectSupplementalService ldapSubjectHandler = new LdapSubjectSupplementalService(recursive, rolesDn, baseDn,
                userDn, usernameAttribute, advancedFilter, pattern, group, resultPattern, reverseGroup, shareConnection);

        ServiceBuilder<?> ldapBuilder = serviceTarget.addService(ldapServiceName, ldapSubjectHandler);
        String connectionManager = LdapAuthorizationResourceDefinition.CONNECTION.resolveModelAttribute(context, ldap).asString();

        LdapConnectionManagerService.ServiceUtil.addDependency(ldapBuilder, ldapSubjectHandler.getConnectionManagerInjector(), connectionManager, false);

        final ServiceController<?> serviceController = ldapBuilder.setInitialMode(ON_DEMAND)
                .install();
        if(newControllers != null) {
            newControllers.add(serviceController);
        }

        SubjectSupplementalService.ServiceUtil.addDependency(realmBuilder, injector, ldapServiceName, false);
    }

    private void addSSLService(OperationContext context, ModelNode ssl, ModelNode trustStore, String realmName,
            ServiceTarget serviceTarget, List<ServiceController<?>> newControllers, ServiceBuilder<?> realmBuilder,
            InjectedValue<SSLIdentity> injector) throws OperationFailedException {

        // Use undefined structures for null ssl model
        ssl = (ssl == null) ? new ModelNode() : ssl;

        ServiceName sslServiceName = SSLIdentity.ServiceUtil.createServiceName(realmName);

        ServiceName keystoreServiceName = null;
        KeyPair pair = null;
        if (ssl.hasDefined(KEYSTORE_PATH)) {
            keystoreServiceName = FileKeystore.ServiceUtil.createKeystoreServiceName(realmName);
            pair = addFileKeystoreService(context, ssl, keystoreServiceName, serviceTarget, newControllers);
        }
        ServiceName truststoreServiceName = null;
        if (trustStore != null) {
            truststoreServiceName = FileKeystore.ServiceUtil.createTrusttoreServiceName(realmName);
            addFileKeystoreService(context, trustStore, truststoreServiceName, serviceTarget, newControllers);
        }

        String protocol = SSLServerIdentityResourceDefinition.PROTOCOL.resolveModelAttribute(context, ssl).asString();
        SSLIdentityService sslIdentityService = new SSLIdentityService(protocol, pair == null ? null : pair.keystorePassword,
                pair == null ? null : pair.keyPassword);

        ServiceBuilder<?> sslBuilder = serviceTarget.addService(sslServiceName, sslIdentityService);

        if (keystoreServiceName != null) {
            FileKeystore.ServiceUtil.addDependency(sslBuilder, sslIdentityService.getKeyStoreInjector(), keystoreServiceName, false);
        }
        if (truststoreServiceName != null) {
            FileKeystore.ServiceUtil.addDependency(sslBuilder, sslIdentityService.getTrustStoreInjector(), truststoreServiceName, false);
        }

        final ServiceController<?> serviceController = sslBuilder.setInitialMode(ON_DEMAND).install();
        if(newControllers != null) {
            newControllers.add(serviceController);
        }

        SSLIdentity.ServiceUtil.addDependency(realmBuilder, injector, realmName, false);
    }

    private static class KeyPair {
        private char[] keystorePassword;
        private char[] keyPassword;
    }

    private KeyPair addFileKeystoreService(OperationContext context, ModelNode ssl, ServiceName serviceName,
            ServiceTarget serviceTarget, List<ServiceController<?>> newControllers) throws OperationFailedException {
        char[] keystorePassword = KeystoreAttributes.KEYSTORE_PASSWORD.resolveModelAttribute(context, ssl).asString().toCharArray();
        char[] keyPassword = null;
        ModelNode pwordNode = KeystoreAttributes.KEY_PASSWORD.resolveModelAttribute(context, ssl);
        if (pwordNode.isDefined()) {
            keyPassword = pwordNode.asString().toCharArray();
        }

        String path = KeystoreAttributes.KEYSTORE_PATH.resolveModelAttribute(context, ssl).asString();
        ModelNode aliasNode = KeystoreAttributes.ALIAS.resolveModelAttribute(context, ssl);
        String alias = aliasNode.isDefined() ? aliasNode.asString() : null;
        FileKeystoreService fileKeystoreService = new FileKeystoreService(path, keystorePassword, alias, keyPassword);

        ServiceBuilder<?> serviceBuilder = serviceTarget.addService(serviceName, fileKeystoreService);
        ModelNode relativeTo = KeystoreAttributes.KEYSTORE_RELATIVE_TO.resolveModelAttribute(context, ssl);
        if (relativeTo.isDefined()) {
            serviceBuilder.addDependency(pathName(relativeTo.asString()), String.class,
                    fileKeystoreService.getRelativeToInjector());
        }

        final ServiceController<?> serviceController = serviceBuilder.setInitialMode(ON_DEMAND).install();
        if(newControllers != null) {
            newControllers.add(serviceController);
        }

        KeyPair pair = new KeyPair();
        pair.keystorePassword = keystorePassword;
        pair.keyPassword = keyPassword;
        return pair;
    }

    private void addSecretService(OperationContext context, ModelNode secret, String realmName, ServiceTarget serviceTarget,
            List<ServiceController<?>> newControllers, ServiceBuilder<?> realmBuilder, Injector<CallbackHandlerFactory> injector) throws OperationFailedException {
        ServiceName secretServiceName = SecretIdentityService.ServiceUtil.createServiceName(realmName);

        ModelNode resolvedValueNode = SecretServerIdentityResourceDefinition.VALUE.resolveModelAttribute(context, secret);
        boolean base64 = secret.get(SecretServerIdentityResourceDefinition.VALUE.getName()).getType() != ModelType.EXPRESSION;

        SecretIdentityService sis = new SecretIdentityService(resolvedValueNode.asString(), base64);
        final ServiceController<CallbackHandlerFactory> serviceController = serviceTarget.addService(secretServiceName, sis)
                .setInitialMode(ON_DEMAND)
                .install();
        if(newControllers != null) {
            newControllers.add(serviceController);
        }

        CallbackHandlerFactory.ServiceUtil.addDependency(realmBuilder, injector, secretServiceName, false);
    }

    private void addUsersService(OperationContext context, ModelNode users, String realmName, ServiceTarget serviceTarget,
            List<ServiceController<?>> newControllers, ServiceBuilder<?> realmBuilder, Injector<CallbackHandlerService> injector) throws OperationFailedException {
        ServiceName usersServiceName = UserDomainCallbackHandler.ServiceUtil.createServiceName(realmName);

        UserDomainCallbackHandler usersCallbackHandler = new UserDomainCallbackHandler(realmName, unmaskUsersPasswords(context, users));

        ServiceBuilder<?> usersBuilder = serviceTarget.addService(usersServiceName, usersCallbackHandler);


        final ServiceController<?> serviceController = usersBuilder.setInitialMode(ServiceController.Mode.ON_DEMAND)
                .install();
        if(newControllers != null) {
            newControllers.add(serviceController);
        }

        CallbackHandlerService.ServiceUtil.addDependency(realmBuilder, injector, usersServiceName, false);
    }



    private static ServiceName pathName(String relativeTo) {
        return ServiceName.JBOSS.append("server", "path", relativeTo);
    }

    private ModelNode unmaskUsersPasswords(OperationContext context, ModelNode users) throws OperationFailedException {
        users = users.clone();
        for (Property property : users.get(USER).asPropertyList()) {
            // Don't use the value from property as it is a clone and does not update the returned users ModelNode.
            ModelNode user = users.get(USER, property.getName());
            if (user.hasDefined(PASSWORD)) {
                //TODO This will be cleaned up once it uses attribute definitions
                user.set(PASSWORD, context.resolveExpressions(user.get(PASSWORD)).asString());
            }
        }
        return users;
    }

    private static Map<String, String> resolveProperties( final OperationContext context, final ModelNode model) throws OperationFailedException {
        Map<String, String> configurationProperties;
        if (model.hasDefined(PROPERTY)) {
            List<Property> propertyList = model.require(PROPERTY).asPropertyList();
            configurationProperties = new HashMap<String, String>(propertyList.size());

            for (Property current : propertyList) {
                String propertyName = current.getName();
                ModelNode valueNode = PropertyResourceDefinition.VALUE.resolveModelAttribute(context, current.getValue());
                String value = valueNode.isDefined() ? valueNode.asString() : null;
                configurationProperties.put(propertyName, value);
            }
            configurationProperties = Collections.unmodifiableMap(configurationProperties);
        } else {
            configurationProperties = Collections.emptyMap();
        }
        return configurationProperties;
    }

    private static class ServiceInstallStepHandler implements OperationStepHandler {

        private static final ServiceInstallStepHandler INSTANCE = new ServiceInstallStepHandler();

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            final List<ServiceController<?>> newControllers = new ArrayList<ServiceController<?>>();
            final String realmName = ManagementUtil.getSecurityRealmName(operation);
            final ModelNode model = Resource.Tools.readModel(context.readResource(PathAddress.EMPTY_ADDRESS));
            SecurityRealmAddHandler.INSTANCE.installServices(context, realmName, model, new ServiceVerificationHandler(), newControllers);
            context.completeStep(new OperationContext.RollbackHandler() {
                @Override
                public void handleRollback(OperationContext context, ModelNode operation) {
                    for (ServiceController<?> sc : newControllers) {
                        context.removeService(sc);
                    }
                }
            });
        }
    }
}
