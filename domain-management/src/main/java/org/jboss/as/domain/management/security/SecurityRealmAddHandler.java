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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONNECTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.JAAS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.KEYSTORE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LDAP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PASSWORD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELATIVE_TO;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SECRET;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_IDENTITY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SSL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TRUSTSTORE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.USER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.USERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.msc.service.ServiceController.Mode.ON_DEMAND;

import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.domain.management.CallbackHandlerFactory;
import org.jboss.as.domain.management.connections.ConnectionManager;
import org.jboss.as.domain.management.connections.ldap.LdapConnectionManagerService;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
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
    public static final String OPERATION_NAME = ModelDescriptionConstants.ADD;

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

        final ModelNode authentication = model.hasDefined(AUTHENTICATION) ? model.get(AUTHENTICATION) : null;
        final ModelNode serverIdentities = model.hasDefined(SERVER_IDENTITY) ? model.get(SERVER_IDENTITY) : null;

        final ServiceTarget serviceTarget = context.getServiceTarget();

        final SecurityRealmService securityRealmService = new SecurityRealmService(realmName);
        final ServiceName realmServiceName = SecurityRealmService.BASE_SERVICE_NAME.append(realmName);
        ServiceBuilder<?> realmBuilder = serviceTarget.addService(realmServiceName, securityRealmService);

        ServiceName authenticationName = null;
        ModelNode authTruststore = null;
        if (authentication != null) {
            // Authentication can have a truststore defined at the same time as a username/password based mechanism.
            //
            // In this case it is expected certificate based authentication will first occur with a fallback to username/password
            // based authentication.
            if (authentication.hasDefined(TRUSTSTORE)) {
                authTruststore = authentication.require(TRUSTSTORE);
            }
            if (authentication.hasDefined(JAAS)) {
                authenticationName = addJaasService(authentication.require(JAAS), realmServiceName, serviceTarget, newControllers);
            } else if (authentication.hasDefined(LDAP)) {
                authenticationName = addLdapService(authentication.require(LDAP), realmServiceName, serviceTarget, newControllers);
            } else if (authentication.hasDefined(PROPERTIES)) {
                authenticationName = addPropertiesService(authentication.require(PROPERTIES), realmServiceName, realmName, serviceTarget, newControllers);
            } else if (authentication.hasDefined(USERS)) {
                authenticationName = addUsersService(context, authentication.require(USERS), realmServiceName, realmName, serviceTarget, newControllers);
            }
        }
        if (authenticationName != null) {
            realmBuilder.addDependency(authenticationName, DomainCallbackHandler.class, securityRealmService.getCallbackHandlerInjector());
        }

        if (serverIdentities != null) {
            if (serverIdentities.hasDefined(SSL)) {
                ServiceName sslServiceName = addSSLService(context, serverIdentities.require(SSL), authTruststore, realmServiceName, serviceTarget, newControllers);
                realmBuilder.addDependency(sslServiceName, SSLIdentityService.class, securityRealmService.getSSLIdentityInjector());
            }
            if (serverIdentities.hasDefined(SECRET)) {
                ServiceName secretServiceName = addSecretService(serverIdentities.require(SECRET),realmServiceName,serviceTarget,newControllers);
                realmBuilder.addDependency(secretServiceName, CallbackHandlerFactory.class,securityRealmService.getSecretCallbackFactory());
            }
        }

        realmBuilder.setInitialMode(ServiceController.Mode.ON_DEMAND);
        ServiceController<?> sc = realmBuilder.install();
        if (newControllers != null) {
            newControllers.add(sc);
        }
    }

    private ServiceName addJaasService(ModelNode jaas, ServiceName realmServiceName, ServiceTarget serviceTarget,
            List<ServiceController<?>> newControllers) {
        ServiceName jaasServiceName = realmServiceName.append(JaasCallbackHandler.SERVICE_SUFFIX);
        JaasCallbackHandler jaasCallbackHandler = new JaasCallbackHandler(jaas.get(NAME).asString());

        ServiceBuilder<?> jaasBuilder = serviceTarget.addService(jaasServiceName, jaasCallbackHandler);
        newControllers.add(jaasBuilder.setInitialMode(ON_DEMAND).install());

        return jaasServiceName;
    }

    private ServiceName addLdapService(ModelNode ldap, ServiceName realmServiceName, ServiceTarget serviceTarget, List<ServiceController<?>> newControllers) {
        ServiceName ldapServiceName = realmServiceName.append(UserLdapCallbackHandler.SERVICE_SUFFIX);
        UserLdapCallbackHandler ldapCallbackHandler = new UserLdapCallbackHandler(ldap);

        ServiceBuilder<?> ldapBuilder = serviceTarget.addService(ldapServiceName, ldapCallbackHandler);
        String connectionManager = ldap.require(CONNECTION).asString();
        ldapBuilder.addDependency(LdapConnectionManagerService.BASE_SERVICE_NAME.append(connectionManager), ConnectionManager.class, ldapCallbackHandler.getConnectionManagerInjector());

        newControllers.add(ldapBuilder.setInitialMode(ON_DEMAND)
                .install());

        return ldapServiceName;
    }

    private ServiceName addPropertiesService(ModelNode properties, ServiceName realmServiceName, String realmName, ServiceTarget serviceTarget, List<ServiceController<?>> newControllers) {
        ServiceName propsServiceName = realmServiceName.append(PropertiesCallbackHandler.SERVICE_SUFFIX);
        PropertiesCallbackHandler propsCallbackHandler = new PropertiesCallbackHandler(realmName, properties);

        ServiceBuilder<?> propsBuilder = serviceTarget.addService(propsServiceName, propsCallbackHandler);
        if (properties.hasDefined(RELATIVE_TO)) {
            propsBuilder.addDependency(pathName(properties.get(RELATIVE_TO).asString()), String.class, propsCallbackHandler.getRelativeToInjector());
        }

        newControllers.add(propsBuilder.setInitialMode(ON_DEMAND)
                .install());

        return propsServiceName;
    }

    private ServiceName addSSLService(OperationContext context, ModelNode ssl, ModelNode trustStore, ServiceName realmServiceName,
            ServiceTarget serviceTarget, List<ServiceController<?>> newControllers) throws OperationFailedException {
        ServiceName sslServiceName = realmServiceName.append(SSLIdentityService.SERVICE_SUFFIX);

        ServiceName keystoreServiceName = null;
        char[] password = null;
        if (ssl.hasDefined(KEYSTORE)) {
            keystoreServiceName = realmServiceName.append(FileKeystoreService.KEYSTORE_SUFFIX);
            password = addFileKeystoreService(context, ssl.require(KEYSTORE), keystoreServiceName, serviceTarget,
                    newControllers);
        }
        ServiceName truststoreServiceName = null;
        if (trustStore != null) {
            truststoreServiceName = realmServiceName.append(FileKeystoreService.TRUSTSTORE_SUFFIX);
            addFileKeystoreService(context, trustStore, truststoreServiceName, serviceTarget, newControllers);
        }

        SSLIdentityService sslIdentityService = new SSLIdentityService(ssl, password);

        ServiceBuilder<?> sslBuilder = serviceTarget.addService(sslServiceName, sslIdentityService);

        if (keystoreServiceName != null) {
            sslBuilder.addDependency(keystoreServiceName, KeyStore.class, sslIdentityService.getKeyStoreInjector());
        }
        if (truststoreServiceName != null) {
            sslBuilder.addDependency(truststoreServiceName, KeyStore.class, sslIdentityService.getTrustStoreInjector());
        }

        newControllers.add(sslBuilder.setInitialMode(ON_DEMAND).install());

        return sslServiceName;
    }

    private char[] addFileKeystoreService(OperationContext context, ModelNode keystore, ServiceName serviceName,
            ServiceTarget serviceTarget, List<ServiceController<?>> newControllers) throws OperationFailedException {
        char[] password = unmaskPassword(context, keystore.require(PASSWORD));

        FileKeystoreService fileKeystoreService = new FileKeystoreService(keystore.require(PATH).asString(), password);

        ServiceBuilder<?> serviceBuilder = serviceTarget.addService(serviceName, fileKeystoreService);
        if (keystore.hasDefined(RELATIVE_TO)) {
            serviceBuilder.addDependency(pathName(keystore.require(RELATIVE_TO).asString()), String.class,
                    fileKeystoreService.getRelativeToInjector());
        }

        newControllers.add(serviceBuilder.setInitialMode(ON_DEMAND).install());

        return password;
    }

    private ServiceName addSecretService(ModelNode secret, ServiceName realmServiceName, ServiceTarget serviceTarget, List<ServiceController<?>> newControllers) {
        ServiceName secretServiceName = realmServiceName.append(SecretIdentityService.SERVICE_SUFFIX);

        String secretValue = secret.require(VALUE).asString();

        SecretIdentityService sis = new SecretIdentityService(secretValue);
        serviceTarget.addService(secretServiceName, sis)
                .setInitialMode(ON_DEMAND)
                .install();

        return secretServiceName;
    }

    private ServiceName addUsersService(OperationContext context, ModelNode users, ServiceName realmServiceName, String realmName, ServiceTarget serviceTarget, List<ServiceController<?>> newControllers) throws OperationFailedException {
        ServiceName usersServiceName = realmServiceName.append(UserDomainCallbackHandler.SERVICE_SUFFIX);

        UserDomainCallbackHandler usersCallbackHandler = new UserDomainCallbackHandler(realmName, unmaskUsersPasswords(context, users));

        ServiceBuilder<?> usersBuilder = serviceTarget.addService(usersServiceName, usersCallbackHandler);


        newControllers.add(usersBuilder.setInitialMode(ServiceController.Mode.ON_DEMAND)
                .install());

        return usersServiceName;
    }

    private static ServiceName pathName(String relativeTo) {
        return ServiceName.JBOSS.append("server", "path", relativeTo);
    }

    private char[] unmaskPassword(OperationContext context, ModelNode password) throws OperationFailedException {
        return context.resolveExpressions(password).asString().toCharArray();
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
