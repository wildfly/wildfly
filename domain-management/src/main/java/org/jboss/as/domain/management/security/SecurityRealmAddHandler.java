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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADVANCED_FILTER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTHENTICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTHORIZATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.GROUP_SEARCH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.GROUP_TO_PRINCIPAL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.JAAS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LDAP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LOCAL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PRINCIPAL_TO_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SECRET;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_IDENTITY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SSL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TRUSTSTORE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.USER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.USERNAME_FILTER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.USERNAME_IS_DN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.USERNAME_TO_DN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.USERS;
import static org.jboss.as.domain.management.ModelDescriptionConstants.BY_ACCESS_TIME;
import static org.jboss.as.domain.management.ModelDescriptionConstants.BY_SEARCH_TIME;
import static org.jboss.as.domain.management.ModelDescriptionConstants.CACHE;
import static org.jboss.as.domain.management.ModelDescriptionConstants.JKS;
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

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.core.security.ServerSecurityManager;
import org.jboss.as.domain.management.AuthMechanism;
import org.jboss.as.domain.management.CallbackHandlerFactory;
import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.as.domain.management.connections.ldap.LdapConnectionManagerService;
import org.jboss.as.domain.management.security.BaseLdapGroupSearchResource.GroupName;
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

        // Add a step validating that we have the correct authentication and authorization child resources
        ModelNode validationOp = AuthenticationValidatingHandler.createOperation(operation);
        context.addStep(validationOp, AuthenticationValidatingHandler.INSTANCE, OperationContext.Stage.MODEL);
        validationOp = AuthorizationValidatingHandler.createOperation(operation);
        context.addStep(validationOp, AuthorizationValidatingHandler.INSTANCE, OperationContext.Stage.MODEL);

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
                addLdapAuthorizationService(context, authorization.require(LDAP), realmName, serviceTarget, newControllers, realmBuilder, securityRealmService.getSubjectSupplementalInjector(), shareLdapConnections);
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
            addSSLServices(context, ssl, authTruststore, realmName, serviceTarget, newControllers, realmBuilder, securityRealmService.getSSLContextInjector());
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

    private <R, K> LdapCacheService<R, K> createCacheService(OperationContext context, LdapSearcher<R, K> searcher,
            ModelNode cache) throws OperationFailedException {
        if (cache != null && cache.isDefined()) {
            ModelNode cacheDefinition = null;
            boolean byAccessTime = false;
            if (cache.hasDefined(BY_ACCESS_TIME)) {
                cacheDefinition = cache.require(BY_ACCESS_TIME);
                byAccessTime = true;
            } else if (cache.hasDefined(BY_SEARCH_TIME)) {
                cacheDefinition = cache.require(BY_SEARCH_TIME);
            }
            if (cacheDefinition != null) {
                int evictionTime = LdapCacheResourceDefinition.EVICTION_TIME.resolveModelAttribute(context, cacheDefinition).asInt();
                boolean cacheFailures = LdapCacheResourceDefinition.CACHE_FAILURES.resolveModelAttribute(context, cacheDefinition)
                        .asBoolean();
                int maxSize = LdapCacheResourceDefinition.MAX_CACHE_SIZE.resolveModelAttribute(context, cacheDefinition).asInt();

                return byAccessTime ? LdapCacheService.createByAccessCacheService(searcher, evictionTime, cacheFailures,
                        maxSize) : LdapCacheService.createBySearchCacheService(searcher, evictionTime, cacheFailures, maxSize);
            }
        }

        return LdapCacheService.createNoCacheService(searcher);
    }

    private void addLdapService(OperationContext context, ModelNode ldap, String realmName, ServiceTarget serviceTarget,
            List<ServiceController<?>> newControllers, ServiceBuilder<?> realmBuilder, Injector<CallbackHandlerService> injector, boolean shareConnection) throws OperationFailedException {
        ServiceName ldapServiceName = UserLdapCallbackHandler.ServiceUtil.createServiceName(realmName);

        final String baseDn = LdapAuthenticationResourceDefinition.BASE_DN.resolveModelAttribute(context, ldap).asString();
        ModelNode node = LdapAuthenticationResourceDefinition.USERNAME_FILTER.resolveModelAttribute(context, ldap);
        final String usernameAttribute = node.isDefined() ? node.asString() : null;
        node = LdapAuthenticationResourceDefinition.ADVANCED_FILTER.resolveModelAttribute(context, ldap);
        final String advancedFilter = node.isDefined() ? node.asString() : null;
        node = LdapAuthenticationResourceDefinition.USERNAME_LOAD.resolveModelAttribute(context, ldap);
        final String usernameLoad = node.isDefined() ? node.asString() : null;
        final boolean recursive = LdapAuthenticationResourceDefinition.RECURSIVE.resolveModelAttribute(context, ldap).asBoolean();
        final boolean allowEmptyPasswords = LdapAuthenticationResourceDefinition.ALLOW_EMPTY_PASSWORDS.resolveModelAttribute(context, ldap).asBoolean();
        final String userDn = LdapAuthenticationResourceDefinition.USER_DN.resolveModelAttribute(context, ldap).asString();
        UserLdapCallbackHandler ldapCallbackHandler = new UserLdapCallbackHandler(allowEmptyPasswords, shareConnection);

        final LdapSearcher<LdapEntry, String> userSearcher;
        if (usernameAttribute != null) {
            userSearcher = LdapUserSearcherFactory.createForUsernameFilter(baseDn, recursive, userDn, usernameAttribute, usernameLoad);
        } else {
            userSearcher = LdapUserSearcherFactory.createForAdvancedFilter(baseDn, recursive, userDn, advancedFilter, usernameLoad);
        }
        final LdapCacheService<LdapEntry, String> cacheService = createCacheService(context, userSearcher, ldap.get(CACHE));

        ServiceName userSearcherCacheName = LdapSearcherCache.ServiceUtil.createServiceName(true, true, realmName);
        ServiceController<LdapSearcherCache<LdapEntry, String>> cacheServiceController = serviceTarget.addService(userSearcherCacheName, cacheService).setInitialMode(ON_DEMAND).install();
        newControllers.add(cacheServiceController);

        ServiceBuilder<?> ldapBuilder = serviceTarget.addService(ldapServiceName, ldapCallbackHandler);
        String connectionManager = LdapAuthenticationResourceDefinition.CONNECTION.resolveModelAttribute(context, ldap).asString();
        LdapConnectionManagerService.ServiceUtil.addDependency(ldapBuilder, ldapCallbackHandler.getConnectionManagerInjector(), connectionManager, false);
        LdapSearcherCache.ServiceUtil.addDependency(ldapBuilder, LdapSearcherCache.class, ldapCallbackHandler.getLdapUserSearcherInjector(), true, true, realmName);

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
        node = LocalAuthenticationResourceDefinition.SKIP_GROUP_LOADING.resolveModelAttribute(context, local);
        boolean skipGroupLoading = node.asBoolean();
        LocalCallbackHandlerService localCallbackHandler = new LocalCallbackHandlerService(defaultUser, allowedUsers, skipGroupLoading);

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

    private void addLdapAuthorizationService(OperationContext context, ModelNode ldap, String realmName, ServiceTarget serviceTarget,
            List<ServiceController<?>> controllers, ServiceBuilder<?> realmBuilder,
            InjectedValue<SubjectSupplementalService> injector, boolean shareConnection) throws OperationFailedException {

        ServiceName ldapName = LdapSubjectSupplementalService.ServiceUtil.createServiceName(realmName);

        LdapSearcher<LdapEntry, String> userSearcher = null;
        boolean forceUserDnSearch = false;
        ModelNode userCache = null;

        if (ldap.hasDefined(USERNAME_TO_DN)) {
            ModelNode usernameToDn = ldap.require(USERNAME_TO_DN);
            if (usernameToDn.hasDefined(USERNAME_IS_DN)) {
                ModelNode usernameIsDn = usernameToDn.require(USERNAME_IS_DN);
                userCache = usernameIsDn.get(CACHE);
                forceUserDnSearch = UserIsDnResourceDefintion.FORCE.resolveModelAttribute(context, usernameIsDn).asBoolean();

                userSearcher = LdapUserSearcherFactory.createForUsernameIsDn();
            } else if (usernameToDn.hasDefined(USERNAME_FILTER)) {
                ModelNode usernameFilter = usernameToDn.require(USERNAME_FILTER);
                userCache = usernameFilter.get(CACHE);
                forceUserDnSearch = UserSearchResourceDefintion.FORCE.resolveModelAttribute(context, usernameFilter).asBoolean();
                String baseDn = UserSearchResourceDefintion.BASE_DN.resolveModelAttribute(context, usernameFilter).asString();
                boolean recursive =  UserSearchResourceDefintion.RECURSIVE.resolveModelAttribute(context, usernameFilter).asBoolean();
                String userDnAttribute = UserSearchResourceDefintion.USER_DN_ATTRIBUTE.resolveModelAttribute(context, usernameFilter).asString();
                String usernameAttribute = UserSearchResourceDefintion.ATTRIBUTE.resolveModelAttribute(context, usernameFilter).asString();

                userSearcher = LdapUserSearcherFactory.createForUsernameFilter(baseDn, recursive, userDnAttribute, usernameAttribute, null);
            } else if (usernameToDn.hasDefined(ADVANCED_FILTER)) {
                ModelNode advancedFilter = usernameToDn.require(ADVANCED_FILTER);
                userCache = advancedFilter.get(CACHE);
                forceUserDnSearch = AdvancedUserSearchResourceDefintion.FORCE.resolveModelAttribute(context, advancedFilter).asBoolean();
                String baseDn = AdvancedUserSearchResourceDefintion.BASE_DN.resolveModelAttribute(context, advancedFilter).asString();
                boolean recursive =  AdvancedUserSearchResourceDefintion.RECURSIVE.resolveModelAttribute(context, advancedFilter).asBoolean();
                String userDnAttribute = AdvancedUserSearchResourceDefintion.USER_DN_ATTRIBUTE.resolveModelAttribute(context, advancedFilter).asString();
                String filter = AdvancedUserSearchResourceDefintion.FILTER.resolveModelAttribute(context, advancedFilter).asString();

                userSearcher = LdapUserSearcherFactory.createForAdvancedFilter(baseDn, recursive, userDnAttribute, filter, null);
            }
        }

        if (userSearcher != null) {
            LdapCacheService<LdapEntry, String> userSearcherCache = createCacheService(context, userSearcher, userCache);

            ServiceName userSearcherCacheName = LdapSearcherCache.ServiceUtil.createServiceName(false, true, realmName);
            ServiceController<LdapSearcherCache<LdapEntry, String>> userSearcherController = serviceTarget
                    .addService(userSearcherCacheName, userSearcherCache).setInitialMode(ON_DEMAND).install();
            controllers.add(userSearcherController);
        }

        ModelNode groupSearch = ldap.require(GROUP_SEARCH);
        LdapSearcher<LdapEntry[], LdapEntry> groupSearcher;
        boolean iterative = false;
        GroupName groupName = GroupName.DISTINGUISHED_NAME;
        ModelNode groupCache = null;
        if (groupSearch.hasDefined(GROUP_TO_PRINCIPAL)) {
            ModelNode groupToPrincipal = groupSearch.require(GROUP_TO_PRINCIPAL);
            groupCache = groupToPrincipal.get(CACHE);
            String baseDn = GroupToPrincipalResourceDefinition.BASE_DN.resolveModelAttribute(context, groupToPrincipal).asString();
            String groupDnAttribute = GroupToPrincipalResourceDefinition.GROUP_DN_ATTRIBUTE.resolveModelAttribute(context, groupToPrincipal).asString();
            groupName = GroupName.valueOf(GroupToPrincipalResourceDefinition.GROUP_NAME.resolveModelAttribute(context, groupToPrincipal).asString());
            String groupNameAttribute = GroupToPrincipalResourceDefinition.GROUP_NAME_ATTRIBUTE.resolveModelAttribute(context, groupToPrincipal).asString();
            iterative = GroupToPrincipalResourceDefinition.ITERATIVE.resolveModelAttribute(context, groupToPrincipal).asBoolean();
            String principalAttribute = GroupToPrincipalResourceDefinition.PRINCIPAL_ATTRIBUTE.resolveModelAttribute(context, groupToPrincipal).asString();
            boolean recursive = GroupToPrincipalResourceDefinition.RECURSIVE.resolveModelAttribute(context, groupToPrincipal).asBoolean();
            GroupName searchBy = GroupName.valueOf(GroupToPrincipalResourceDefinition.SEARCH_BY.resolveModelAttribute(context, groupToPrincipal).asString());

            groupSearcher = LdapGroupSearcherFactory.createForGroupToPrincipal(baseDn, groupDnAttribute, groupNameAttribute, principalAttribute, recursive, searchBy);
        } else {
            ModelNode principalToGroup = groupSearch.require(PRINCIPAL_TO_GROUP);
            groupCache = principalToGroup.get(CACHE);
            String groupAttribute = PrincipalToGroupResourceDefinition.GROUP_ATTRIBUTE.resolveModelAttribute(context, principalToGroup).asString();
            // TODO - Why was this never used?
            String groupDnAttribute = PrincipalToGroupResourceDefinition.GROUP_DN_ATTRIBUTE.resolveModelAttribute(context, principalToGroup).asString();
            groupName = GroupName.valueOf(PrincipalToGroupResourceDefinition.GROUP_NAME.resolveModelAttribute(context, principalToGroup).asString());
            String groupNameAttribute = PrincipalToGroupResourceDefinition.GROUP_NAME_ATTRIBUTE.resolveModelAttribute(context, principalToGroup).asString();
            iterative = PrincipalToGroupResourceDefinition.ITERATIVE.resolveModelAttribute(context, principalToGroup).asBoolean();

            groupSearcher = LdapGroupSearcherFactory.createForPrincipalToGroup(groupAttribute, groupNameAttribute);
        }

        LdapCacheService<LdapEntry[], LdapEntry> groupCacheService = createCacheService(context, groupSearcher, groupCache);

        ServiceName groupCacheServiceName = LdapSearcherCache.ServiceUtil.createServiceName(false, false, realmName);
        ServiceController<LdapSearcherCache<LdapEntry[], LdapEntry>> groupSearcherCacheController = serviceTarget
                .addService(groupCacheServiceName, groupCacheService).setInitialMode(ON_DEMAND).install();
        controllers.add(groupSearcherCacheController);

        String connectionName = LdapAuthorizationResourceDefinition.CONNECTION.resolveModelAttribute(context, ldap).asString();

        LdapSubjectSupplementalService service = new LdapSubjectSupplementalService(realmName, shareConnection, forceUserDnSearch, iterative, groupName);
        ServiceBuilder<SubjectSupplementalService> ldapBuilder = serviceTarget.addService(ldapName, service)
                .setInitialMode(ON_DEMAND);
        LdapConnectionManagerService.ServiceUtil.addDependency(ldapBuilder, service.getConnectionManagerInjector(), connectionName, false);
        if (userSearcher != null) {
            LdapSearcherCache.ServiceUtil.addDependency(ldapBuilder, LdapSearcherCache.class, service.getLdapUserSearcherInjector(), false, true, realmName);
        }
        LdapSearcherCache.ServiceUtil.addDependency(ldapBuilder, LdapSearcherCache.class, service.getLdapGroupSearcherInjector(), false, false, realmName);

        controllers.add(ldapBuilder.install());

        SubjectSupplementalService.ServiceUtil.addDependency(realmBuilder, injector, ldapName, false);
    }

    private void addSSLServices(OperationContext context, ModelNode ssl, ModelNode trustStore, String realmName,
            ServiceTarget serviceTarget, List<ServiceController<?>> controllers, ServiceBuilder<?> realmBuilder,
            InjectedValue<SSLContext> injector) throws OperationFailedException {

        // Use undefined structures for null ssl model
        ssl = (ssl == null) ? new ModelNode() : ssl;

        ServiceName keyManagerServiceName = null;

        String provider = KeystoreAttributes.KEYSTORE_PROVIDER.resolveModelAttribute(context, ssl).asString();
        if (ssl.hasDefined(KEYSTORE_PATH) || (JKS.equals(provider) == false)) {
            keyManagerServiceName = AbstractKeyManagerService.ServiceUtil.createServiceName(SecurityRealm.ServiceUtil.createServiceName(realmName));
            addKeyManagerService(context, ssl, keyManagerServiceName, serviceTarget, controllers);
        }

        ServiceName trustManagerServiceName = null;
        if (trustStore != null) {
            trustManagerServiceName = AbstractTrustManagerService.ServiceUtil.createServiceName(SecurityRealm.ServiceUtil.createServiceName(realmName));
            addTrustManagerService(context, trustStore, trustManagerServiceName, serviceTarget, controllers);
        }

        String protocol = SSLServerIdentityResourceDefinition.PROTOCOL.resolveModelAttribute(context, ssl).asString();

        /*
         * At this point we register two SSLContextService instances, one linked to both the key and trust store and the other just for trust.
         *
         * Subsequent dependencies will trigger which (or both) are actually started.
         */
        ServiceName fullServiceName = SSLContextService.ServiceUtil.createServiceName(SecurityRealm.ServiceUtil.createServiceName(realmName), false);
        ServiceName trustOnlyServiceName = SSLContextService.ServiceUtil.createServiceName(SecurityRealm.ServiceUtil.createServiceName(realmName), true);

        if (keyManagerServiceName != null) {
            // An alias will not be set on the trust based SSLContext.
            SSLContextService fullSSLContextService = new SSLContextService(protocol);
            ServiceBuilder<SSLContext> fullBuilder = serviceTarget.addService(fullServiceName, fullSSLContextService);
            AbstractKeyManagerService.ServiceUtil.addDependency(fullBuilder, fullSSLContextService.getKeyManagerInjector(), SecurityRealm.ServiceUtil.createServiceName(realmName));
            if (trustManagerServiceName != null) {
                AbstractTrustManagerService.ServiceUtil.addDependency(fullBuilder, fullSSLContextService.getTrustManagerInjector(), SecurityRealm.ServiceUtil.createServiceName(realmName));
            }

            ServiceController<SSLContext> fullController = fullBuilder.setInitialMode(ON_DEMAND).install();
            controllers.add(fullController);
        }

        // Always register this one - if no KeyStore is defined we can add an alias to this.
        SSLContextService trustOnlySSLContextService = new SSLContextService(protocol);
        ServiceBuilder<SSLContext> trustBuilder = serviceTarget.addService(trustOnlyServiceName, trustOnlySSLContextService);
        if (keyManagerServiceName == null) {
            // No KeyStore so just alias to this.
            trustBuilder.addAliases(fullServiceName);
        }
        if (trustManagerServiceName != null) {
            AbstractTrustManagerService.ServiceUtil.addDependency(trustBuilder, trustOnlySSLContextService.getTrustManagerInjector(), SecurityRealm.ServiceUtil.createServiceName(realmName));
        }
        ServiceController<SSLContext> trustController = trustBuilder.setInitialMode(ON_DEMAND).install();
        controllers.add(trustController);

        SSLContextService.ServiceUtil.addDependency(realmBuilder, injector, SecurityRealm.ServiceUtil.createServiceName(realmName), false);
    }

    private void addKeyManagerService(OperationContext context, ModelNode ssl, ServiceName serviceName,
            ServiceTarget serviceTarget, List<ServiceController<?>> controllers) throws OperationFailedException {

        char[] keystorePassword = KeystoreAttributes.KEYSTORE_PASSWORD.resolveModelAttribute(context, ssl).asString().toCharArray();
        final ServiceBuilder<KeyManager[]> serviceBuilder;

        String provider = KeystoreAttributes.KEYSTORE_PROVIDER.resolveModelAttribute(context, ssl).asString();

        ModelNode pathNode = KeystoreAttributes.KEYSTORE_PATH.resolveModelAttribute(context, ssl);
        if (pathNode.isDefined() == false) {
            ProviderKeyManagerService keyManagerService = new ProviderKeyManagerService(provider, keystorePassword);

            serviceBuilder = serviceTarget.addService(serviceName, keyManagerService);
        } else {
            String path = pathNode.asString();

            final char[] keyPassword;
            ModelNode pwordNode = KeystoreAttributes.KEY_PASSWORD.resolveModelAttribute(context, ssl);
            if (pwordNode.isDefined()) {
                keyPassword = pwordNode.asString().toCharArray();
            } else {
                keyPassword = null;
            }
            ModelNode aliasNode = KeystoreAttributes.ALIAS.resolveModelAttribute(context, ssl);
            String alias = aliasNode.isDefined() ? aliasNode.asString() : null;

            FileKeyManagerService keyManagerService = new FileKeyManagerService(provider, path, keystorePassword, keyPassword, alias);

            serviceBuilder = serviceTarget.addService(serviceName, keyManagerService);
            ModelNode relativeTo = KeystoreAttributes.KEYSTORE_RELATIVE_TO.resolveModelAttribute(context, ssl);
            if (relativeTo.isDefined()) {
                serviceBuilder.addDependency(pathName(relativeTo.asString()), String.class,
                        keyManagerService.getRelativeToInjector());
            }
        }

        final ServiceController<?> serviceController = serviceBuilder.setInitialMode(ON_DEMAND).install();
        if(controllers != null) {
            controllers.add(serviceController);
        }
    }

    private void addTrustManagerService(OperationContext context, ModelNode ssl, ServiceName serviceName,
            ServiceTarget serviceTarget, List<ServiceController<?>> controllers) throws OperationFailedException {

        final ServiceBuilder<TrustManager[]> serviceBuilder;
        char[] keystorePassword = KeystoreAttributes.KEYSTORE_PASSWORD.resolveModelAttribute(context, ssl).asString()
                .toCharArray();
        String provider = KeystoreAttributes.KEYSTORE_PROVIDER.resolveModelAttribute(context, ssl).asString();

        if (JKS.equals(provider) == false) {
            ProviderTrustManagerService trustManagerService = new ProviderTrustManagerService(provider, keystorePassword);

            serviceBuilder = serviceTarget.addService(serviceName, trustManagerService);
        } else {
            String path = KeystoreAttributes.KEYSTORE_PATH.resolveModelAttribute(context, ssl).asString();

            FileTrustManagerService trustManagerService = new FileTrustManagerService(provider, path, keystorePassword);

            serviceBuilder = serviceTarget.addService(serviceName, trustManagerService);
            ModelNode relativeTo = KeystoreAttributes.KEYSTORE_RELATIVE_TO.resolveModelAttribute(context, ssl);
            if (relativeTo.isDefined()) {
                serviceBuilder.addDependency(pathName(relativeTo.asString()), String.class,
                        trustManagerService.getRelativeToInjector());
            }
        }

        final ServiceController<?> serviceController = serviceBuilder.setInitialMode(ON_DEMAND).install();
        if (controllers != null) {
            controllers.add(serviceController);
        }
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
