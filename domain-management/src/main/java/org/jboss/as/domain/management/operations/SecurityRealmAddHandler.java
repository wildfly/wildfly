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
package org.jboss.as.domain.management.operations;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTHENTICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONNECTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.KEYSTORE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LDAP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PASSWORD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELATIVE_TO;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SECRET;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_IDENTITIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SSL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.USER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.USERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.msc.service.ServiceController.Mode.ON_DEMAND;

import java.util.List;
import java.util.Locale;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.common.ManagementDescription;
import org.jboss.as.domain.management.CallbackHandlerFactory;
import org.jboss.as.domain.management.connections.ConnectionManager;
import org.jboss.as.domain.management.security.DomainCallbackHandler;
import org.jboss.as.domain.management.security.LdapConnectionManagerService;
import org.jboss.as.domain.management.security.PropertiesCallbackHandler;
import org.jboss.as.domain.management.security.SSLIdentityService;
import org.jboss.as.domain.management.security.SecretIdentityService;
import org.jboss.as.domain.management.security.SecurityRealmService;
import org.jboss.as.domain.management.security.UserDomainCallbackHandler;
import org.jboss.as.domain.management.security.UserLdapCallbackHandler;
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
 */
public class SecurityRealmAddHandler extends AbstractAddStepHandler implements DescriptionProvider {

    public static final SecurityRealmAddHandler INSTANCE = new SecurityRealmAddHandler();
    public static final String OPERATION_NAME = ModelDescriptionConstants.ADD;

    protected void populateModel(ModelNode operation, ModelNode model) {
        final ModelNode authentication = operation.hasDefined(AUTHENTICATION) ? operation.get(AUTHENTICATION) : null;
        final ModelNode serverIdentities = operation.hasDefined(SERVER_IDENTITIES) ? operation.get(SERVER_IDENTITIES) : null;

        if (serverIdentities != null) {
            model.get(SERVER_IDENTITIES).set(serverIdentities);
        }
        if (authentication != null) {
            model.get(AUTHENTICATION).set(authentication);
        }
    }

    @Override
    protected boolean requiresRuntime(OperationContext context) {
        return true;
    }

    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String realmName = address.getLastElement().getValue();

        final ModelNode authentication = operation.hasDefined(AUTHENTICATION) ? operation.get(AUTHENTICATION) : null;
        final ModelNode serverIdentities = operation.hasDefined(SERVER_IDENTITIES) ? operation.get(SERVER_IDENTITIES) : null;

        final ServiceTarget serviceTarget = context.getServiceTarget();

        final SecurityRealmService securityRealmService = new SecurityRealmService(realmName);
        final ServiceName realmServiceName = SecurityRealmService.BASE_SERVICE_NAME.append(realmName);
        ServiceBuilder<?> realmBuilder = serviceTarget.addService(realmServiceName, securityRealmService);

        ServiceName authenticationName = null;
        if (authentication != null) {
            if (authentication.hasDefined(LDAP)) {
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
                ServiceName sslServiceName = addSSLService(context, serverIdentities.require(SSL), realmServiceName, serviceTarget, newControllers);
                realmBuilder.addDependency(sslServiceName, SSLIdentityService.class, securityRealmService.getSSLIdentityInjector());
            }
            if (serverIdentities.hasDefined(SECRET)) {
                ServiceName secretServiceName = addSecretService(serverIdentities.require(SECRET),realmServiceName,serviceTarget,newControllers);
                realmBuilder.addDependency(secretServiceName, CallbackHandlerFactory.class,securityRealmService.getSecretCallbackFactory());
            }
        }

        newControllers.add(realmBuilder.setInitialMode(ServiceController.Mode.ON_DEMAND)
                .install());
    }

    // TODO - These need to be split into more fine grained services so allow service specific checks.


    public ModelNode getModelDescription(Locale locale) {
        return ManagementDescription.getAddManagementSecurityRealmDescription(locale);
    }

    protected boolean requiresRuntimeVerification() {
        return false;
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

    // TODO - The operation will also be split out into it's own handler when I make the operations more fine grained.
    private ServiceName addSSLService(OperationContext context, ModelNode ssl, ServiceName realmServiceName, ServiceTarget serviceTarget, List<ServiceController<?>> newControllers) throws OperationFailedException {
        ServiceName sslServiceName = realmServiceName.append(SSLIdentityService.SERVICE_SUFFIX);
        SSLIdentityService sslIdentityService = new SSLIdentityService(ssl, unmaskSslKeystorePassword(context, ssl));

        ServiceBuilder<?> sslBuilder = serviceTarget.addService(sslServiceName, sslIdentityService);

        if (ssl.hasDefined(KEYSTORE) && ssl.get(KEYSTORE).hasDefined(RELATIVE_TO)) {
            sslBuilder.addDependency(pathName(ssl.get(KEYSTORE, RELATIVE_TO).asString()), String.class, sslIdentityService.getRelativeToInjector());
        }

        newControllers.add(sslBuilder.setInitialMode(ON_DEMAND)
                .install());

        return sslServiceName;
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

    private String unmaskSslKeystorePassword(OperationContext context, ModelNode ssl) throws OperationFailedException {
        if (!ssl.hasDefined(KEYSTORE)) {
            return null;
        }
        if (!ssl.hasDefined(PASSWORD)) {
            return null;
        }
        return context.resolveExpressions(ssl.get(KEYSTORE, PASSWORD)).asString();
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
}
