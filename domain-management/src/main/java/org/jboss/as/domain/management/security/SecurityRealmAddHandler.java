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
import static org.jboss.as.domain.management.ModelDescriptionConstants.PROPERTY;
import static org.jboss.as.domain.management.ModelDescriptionConstants.PLUG_IN;
import static org.jboss.msc.service.ServiceController.Mode.ON_DEMAND;

import java.security.KeyStore;
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
import org.jboss.as.controller.security.ServerSecurityManager;
import org.jboss.as.domain.management.AuthenticationMechanism;
import org.jboss.as.domain.management.CallbackHandlerFactory;
import org.jboss.as.domain.management.connections.ConnectionManager;
import org.jboss.as.domain.management.connections.ldap.LdapConnectionManagerService;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

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
        context.createResource(PathAddress.EMPTY_ADDRESS);

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

        final SecurityRealmService securityRealmService = new SecurityRealmService(realmName);
        final ServiceName realmServiceName = SecurityRealmService.BASE_SERVICE_NAME.append(realmName);
        ServiceBuilder<?> realmBuilder = serviceTarget.addService(realmServiceName, securityRealmService);

        ServiceName plugInLoaderName = null;
        ServiceName authenticationName = null;
        ServiceName authorizationName = null;
        ModelNode authTruststore = null;
        if (plugIns != null) {
            plugInLoaderName = addPlugInLoaderService(realmServiceName, plugIns, serviceTarget, newControllers);
        }
        if (authentication != null) {
            // Authentication can have a truststore defined at the same time as a username/password based mechanism.
            //
            // In this case it is expected certificate based authentication will first occur with a fallback to username/password
            // based authentication.
            if (authentication.hasDefined(TRUSTSTORE)) {
                authTruststore = authentication.require(TRUSTSTORE);
                ServiceName ccName = addClientCertService(realmServiceName, serviceTarget, newControllers);
                realmBuilder.addDependency(ccName, CallbackHandlerService.class, securityRealmService.getCallbackHandlerService().injector());
            }
            if (authentication.hasDefined(LOCAL)) {
                ServiceName localName = addLocalService(context, authentication.require(LOCAL), realmServiceName, serviceTarget,
                        newControllers);
                realmBuilder.addDependency(localName, CallbackHandlerService.class, securityRealmService.getCallbackHandlerService().injector());
            }
            if (authentication.hasDefined(JAAS)) {
                authenticationName = addJaasService(context, authentication.require(JAAS), realmServiceName,
                        serviceTarget, newControllers, context.isNormalServer());
            } else if (authentication.hasDefined(LDAP)) {
                authenticationName = addLdapService(context, authentication.require(LDAP), realmServiceName,
                        serviceTarget, newControllers);
            } else if (authentication.hasDefined(PLUG_IN)) {
                authenticationName = addPlugInAuthenticationService(context, authentication.require(PLUG_IN), realmServiceName,
                        plugInLoaderName, securityRealmService, serviceTarget, newControllers);
            } else if (authentication.hasDefined(PROPERTIES)) {
                authenticationName = addPropertiesAuthenticationService(context, authentication.require(PROPERTIES),
                        realmServiceName, realmName, serviceTarget, newControllers);
            } else if (authentication.hasDefined(USERS)) {
                authenticationName = addUsersService(context, authentication.require(USERS), realmServiceName, realmName, serviceTarget, newControllers);
            }
        }
        if (authorization != null) {
            if (authorization.hasDefined(PROPERTIES)) {
                authorizationName = addPropertiesAuthorizationService(context, authorization.require(PROPERTIES), realmServiceName,
                        serviceTarget, newControllers);
            } else if (authorization.hasDefined(PLUG_IN)) {
                authorizationName = addPlugInAuthorizationService(context, authorization.require(PLUG_IN), realmServiceName,
                        plugInLoaderName, realmName, serviceTarget, newControllers);
            }
        }
        if (authenticationName != null) {
            realmBuilder.addDependency(authenticationName, CallbackHandlerService.class, securityRealmService.getCallbackHandlerService().injector());
        }
        if (authorizationName != null) {
            realmBuilder.addDependency(authorizationName, SubjectSupplementalService.class, securityRealmService.getSubjectSupplementalInjector());
        }

        ModelNode ssl = null;
        if (serverIdentities != null) {
            if (serverIdentities.hasDefined(SSL)) {
                ssl = serverIdentities.require(SSL);
            }
            if (serverIdentities.hasDefined(SECRET)) {
                ServiceName secretServiceName = addSecretService(context, serverIdentities.require(SECRET), realmServiceName,serviceTarget,newControllers);
                realmBuilder.addDependency(secretServiceName, CallbackHandlerFactory.class,securityRealmService.getSecretCallbackFactory());
            }
        }

        if (ssl != null || authTruststore != null) {
            ServiceName sslServiceName = addSSLService(context, ssl, authTruststore, realmServiceName, serviceTarget, newControllers);
            realmBuilder.addDependency(sslServiceName, SSLIdentityService.class, securityRealmService.getSSLIdentityInjector());
        }

        realmBuilder.setInitialMode(Mode.ACTIVE);
        ServiceController<?> sc = realmBuilder.install();
        if (newControllers != null) {
            newControllers.add(sc);
        }
    }

    private ServiceName addPlugInLoaderService(ServiceName realmServiceName, ModelNode plugInModel,
            ServiceTarget serviceTarget, List<ServiceController<?>> newControllers) {
        ServiceName plugInLoaderName = realmServiceName.append(PlugInLoaderService.SERVICE_SUFFIX);

        List<Property> plugIns = plugInModel.asPropertyList();
        ArrayList<String> knownNames = new ArrayList<String>(plugIns.size());
        for (Property current : plugIns) {
            knownNames.add(current.getName());
        }
        PlugInLoaderService loaderService = new PlugInLoaderService(Collections.unmodifiableList(knownNames));
        ServiceBuilder<PlugInLoaderService> builder = serviceTarget.addService(plugInLoaderName, loaderService);
        newControllers.add(builder.setInitialMode(Mode.ON_DEMAND).install());

        return plugInLoaderName;
    }

    private ServiceName addClientCertService(ServiceName realmServiceName, ServiceTarget serviceTarget,
            List<ServiceController<?>> newControllers) {
        ServiceName clientCertServiceName = realmServiceName.append(ClientCertCallbackHandler.SERVICE_SUFFIX);
        ClientCertCallbackHandler clientCertCallbackHandler = new ClientCertCallbackHandler();

        ServiceBuilder<?> ccBuilder = serviceTarget.addService(clientCertServiceName, clientCertCallbackHandler);

        newControllers.add(ccBuilder.setInitialMode(ON_DEMAND).install());

        return clientCertServiceName;
    }

    private ServiceName addJaasService(OperationContext context, ModelNode jaas, ServiceName realmServiceName, ServiceTarget serviceTarget,
            List<ServiceController<?>> newControllers, boolean injectServerManager) throws OperationFailedException {
        ServiceName jaasServiceName = realmServiceName.append(JaasCallbackHandler.SERVICE_SUFFIX);
        String name = JaasAuthenticationResourceDefinition.NAME.resolveModelAttribute(context, jaas).asString();
        JaasCallbackHandler jaasCallbackHandler = new JaasCallbackHandler(name);

        ServiceBuilder<?> jaasBuilder = serviceTarget.addService(jaasServiceName, jaasCallbackHandler);
        if (injectServerManager) {
            jaasBuilder.addDependency(ServiceName.JBOSS.append("security", "simple-security-manager"),
                    ServerSecurityManager.class, jaasCallbackHandler.getSecurityManagerValue());
        }

        newControllers.add(jaasBuilder.setInitialMode(ON_DEMAND).install());

        return jaasServiceName;
    }

    private ServiceName addLdapService(OperationContext context, ModelNode ldap, ServiceName realmServiceName, ServiceTarget serviceTarget, List<ServiceController<?>> newControllers) throws OperationFailedException {
        ServiceName ldapServiceName = realmServiceName.append(UserLdapCallbackHandler.SERVICE_SUFFIX);

        final String baseDn = LdapAuthenticationResourceDefinition.BASE_DN.resolveModelAttribute(context, ldap).asString();
        ModelNode node = LdapAuthenticationResourceDefinition.USERNAME_FILTER.resolveModelAttribute(context, ldap);
        final String usernameAttribute = node.isDefined() ? node.asString() : null;
        node = LdapAuthenticationResourceDefinition.ADVANCED_FILTER.resolveModelAttribute(context, ldap);
        final String advancedFilter = node.isDefined() ? node.asString() : null;
        final boolean recursive = LdapAuthenticationResourceDefinition.RECURSIVE.resolveModelAttribute(context, ldap).asBoolean();
        final String userDn = LdapAuthenticationResourceDefinition.USER_DN.resolveModelAttribute(context, ldap).asString();
        UserLdapCallbackHandler ldapCallbackHandler = new UserLdapCallbackHandler(baseDn, usernameAttribute, advancedFilter, recursive, userDn);

        ServiceBuilder<?> ldapBuilder = serviceTarget.addService(ldapServiceName, ldapCallbackHandler);
        String connectionManager = LdapAuthenticationResourceDefinition.CONNECTION.resolveModelAttribute(context, ldap).asString();
        ldapBuilder.addDependency(LdapConnectionManagerService.BASE_SERVICE_NAME.append(connectionManager), ConnectionManager.class, ldapCallbackHandler.getConnectionManagerInjector());

        final ServiceController<?> serviceController = ldapBuilder.setInitialMode(ON_DEMAND)
                .install();
        if(newControllers != null) {
            newControllers.add(serviceController);
        }

        return ldapServiceName;
    }

    private ServiceName addLocalService(OperationContext context, ModelNode local, ServiceName realmServiceName, ServiceTarget serviceTarget,
            List<ServiceController<?>> newControllers) throws OperationFailedException {
        ServiceName localServiceName = realmServiceName.append(LocalCallbackHandlerService.SERVICE_SUFFIX);

        ModelNode node = LocalAuthenticationResourceDefinition.DEFAULT_USER.resolveModelAttribute(context, local);
        String defaultUser = node.isDefined() ? node.asString() : null;
        node = LocalAuthenticationResourceDefinition.ALLOWED_USERS.resolveModelAttribute(context, local);
        String allowedUsers = node.isDefined() ? node.asString() : null;
        LocalCallbackHandlerService localCallbackHandler = new LocalCallbackHandlerService(defaultUser, allowedUsers);

        ServiceBuilder<?> jaasBuilder = serviceTarget.addService(localServiceName, localCallbackHandler);

        newControllers.add(jaasBuilder.setInitialMode(ON_DEMAND).install());

        return localServiceName;
    }

    private ServiceName addPlugInAuthenticationService(OperationContext context, ModelNode model, ServiceName realmServiceName,
            ServiceName plugInLoaderName, SecurityRealmService registry, ServiceTarget serviceTarget,
            List<ServiceController<?>> newControllers) throws OperationFailedException {
        ServiceName plugInServiceName = realmServiceName.append(PlugInAuthenticationCallbackHandler.SERVICE_SUFFIX);

        final String pluginName = PlugInAuthorizationResourceDefinition.NAME.resolveModelAttribute(context, model).asString();
        final Map<String, String> properties = resolveProperties(context, model);
        String mechanismName = PlugInAuthenticationResourceDefinition.MECHANISM.resolveModelAttribute(context, model).asString();
        AuthenticationMechanism mechanism = AuthenticationMechanism.valueOf(mechanismName);
        PlugInAuthenticationCallbackHandler plugInService = new PlugInAuthenticationCallbackHandler(registry.getName(),
                pluginName, properties, mechanism);

        ServiceBuilder<CallbackHandlerService> plugInBuilder = serviceTarget.addService(plugInServiceName, plugInService);
        plugInBuilder.addDependency(plugInLoaderName, PlugInLoaderService.class, plugInService.getPlugInLoaderServiceValue());

        newControllers.add(plugInBuilder.setInitialMode(ON_DEMAND).install());

        return plugInServiceName;
    }

    private ServiceName addPropertiesAuthenticationService(OperationContext context, ModelNode properties, ServiceName realmServiceName,
            String realmName, ServiceTarget serviceTarget, List<ServiceController<?>> newControllers) throws OperationFailedException {

        ServiceName propsServiceName = realmServiceName.append(PropertiesCallbackHandler.SERVICE_SUFFIX);

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

        return propsServiceName;
    }

    private ServiceName addPropertiesAuthorizationService(OperationContext context, ModelNode properties, ServiceName realmServiceName,
            ServiceTarget serviceTarget, List<ServiceController<?>> newControllers) throws OperationFailedException {
        ServiceName propsServiceName = realmServiceName.append(PropertiesSubjectSupplemental.SERVICE_SUFFIX);

        final String path = PropertiesAuthorizationResourceDefinition.PATH.resolveModelAttribute(context, properties).asString();
        final ModelNode relativeTo = PropertiesAuthorizationResourceDefinition.RELATIVE_TO.resolveModelAttribute(context, properties);
        PropertiesSubjectSupplemental propsSubjectSupplemental = new PropertiesSubjectSupplemental(path);

        ServiceBuilder<?> propsBuilder = serviceTarget.addService(propsServiceName, propsSubjectSupplemental);
        if (relativeTo.isDefined()) {
            propsBuilder.addDependency(pathName(relativeTo.asString()), String.class,
                    propsSubjectSupplemental.getRelativeToInjector());
        }

        final ServiceController<?> serviceController = propsBuilder.setInitialMode(ON_DEMAND).install();
        if(newControllers != null) {
            newControllers.add(serviceController);
        }

        return propsServiceName;
    }

    private ServiceName addPlugInAuthorizationService(OperationContext context, ModelNode model, ServiceName realmServiceName,
            ServiceName plugInLoaderName, String realmName, ServiceTarget serviceTarget,
            List<ServiceController<?>> newControllers) throws OperationFailedException {

        ServiceName plugInServiceName = realmServiceName.append(PlugInSubjectSupplemental.SERVICE_SUFFIX);
        final String pluginName = PlugInAuthorizationResourceDefinition.NAME.resolveModelAttribute(context, model).asString();
        final Map<String, String> properties = resolveProperties(context, model);
        PlugInSubjectSupplemental plugInSubjectSupplemental = new PlugInSubjectSupplemental(realmName, pluginName, properties);

        ServiceBuilder<?> plugInBuilder = serviceTarget.addService(plugInServiceName, plugInSubjectSupplemental);
        plugInBuilder.addDependency(plugInLoaderName, PlugInLoaderService.class,
                plugInSubjectSupplemental.getPlugInLoaderServiceValue());

        final ServiceController<?> serviceController = plugInBuilder.setInitialMode(ON_DEMAND).install();
        if (newControllers != null) {
            newControllers.add(serviceController);
        }

        return plugInServiceName;
    }

    private ServiceName addSSLService(OperationContext context, ModelNode ssl, ModelNode trustStore, ServiceName realmServiceName,
            ServiceTarget serviceTarget, List<ServiceController<?>> newControllers) throws OperationFailedException {

        // Use undefined structures for null ssl model
        ssl = (ssl == null) ? new ModelNode() : ssl;

        ServiceName sslServiceName = realmServiceName.append(SSLIdentityService.SERVICE_SUFFIX);

        ServiceName keystoreServiceName = null;
        KeyPair pair = null;
        if (ssl.hasDefined(KEYSTORE_PATH)) {
            keystoreServiceName = realmServiceName.append(FileKeystoreService.KEYSTORE_SUFFIX);
            pair = addFileKeystoreService(context, ssl, keystoreServiceName, serviceTarget, newControllers);
        }
        ServiceName truststoreServiceName = null;
        if (trustStore != null) {
            truststoreServiceName = realmServiceName.append(FileKeystoreService.TRUSTSTORE_SUFFIX);
            addFileKeystoreService(context, trustStore, truststoreServiceName, serviceTarget, newControllers);
        }

        String protocol = SSLServerIdentityResourceDefinition.PROTOCOL.resolveModelAttribute(context, ssl).asString();
        SSLIdentityService sslIdentityService = new SSLIdentityService(protocol, pair == null ? null : pair.keystorePassword,
                pair == null ? null : pair.keyPassword);

        ServiceBuilder<?> sslBuilder = serviceTarget.addService(sslServiceName, sslIdentityService);

        if (keystoreServiceName != null) {
            sslBuilder.addDependency(keystoreServiceName, KeyStore.class, sslIdentityService.getKeyStoreInjector());
        }
        if (truststoreServiceName != null) {
            sslBuilder.addDependency(truststoreServiceName, KeyStore.class, sslIdentityService.getTrustStoreInjector());
        }

        final ServiceController<?> serviceController = sslBuilder.setInitialMode(ON_DEMAND).install();
        if(newControllers != null) {
            newControllers.add(serviceController);
        }

        return sslServiceName;
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

    private ServiceName addSecretService(OperationContext context, ModelNode secret, ServiceName realmServiceName, ServiceTarget serviceTarget, List<ServiceController<?>> newControllers) throws OperationFailedException {
        ServiceName secretServiceName = realmServiceName.append(SecretIdentityService.SERVICE_SUFFIX);

        ModelNode secretValueNode = SecretServerIdentityResourceDefinition.VALUE.resolveModelAttribute(context, secret);
        String resolvedValue = context.resolveExpressions(secretValueNode).asString();

        SecretIdentityService sis = new SecretIdentityService(resolvedValue, secretValueNode.asString().equals(resolvedValue));
        final ServiceController<CallbackHandlerFactory> serviceController = serviceTarget.addService(secretServiceName, sis)
                .setInitialMode(ON_DEMAND)
                .install();
        if(newControllers != null) {
            newControllers.add(serviceController);
        }

        return secretServiceName;
    }

    private ServiceName addUsersService(OperationContext context, ModelNode users, ServiceName realmServiceName, String realmName, ServiceTarget serviceTarget, List<ServiceController<?>> newControllers) throws OperationFailedException {
        ServiceName usersServiceName = realmServiceName.append(UserDomainCallbackHandler.SERVICE_SUFFIX);

        UserDomainCallbackHandler usersCallbackHandler = new UserDomainCallbackHandler(realmName, unmaskUsersPasswords(context, users));

        ServiceBuilder<?> usersBuilder = serviceTarget.addService(usersServiceName, usersCallbackHandler);


        final ServiceController<?> serviceController = usersBuilder.setInitialMode(ServiceController.Mode.ON_DEMAND)
                .install();
        if(newControllers != null) {
            newControllers.add(serviceController);
        }

        return usersServiceName;
    }

    private static ServiceName pathName(String relativeTo) {
        return ServiceName.JBOSS.append("server", "path", relativeTo);
    }

    private ModelNode unmaskUsersPasswords(OperationContext context, ModelNode users) throws OperationFailedException {
        users = users.clone();
        for (Property property : users.get(USER).asPropertyList()) {
            ModelNode user = property.getValue();
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
