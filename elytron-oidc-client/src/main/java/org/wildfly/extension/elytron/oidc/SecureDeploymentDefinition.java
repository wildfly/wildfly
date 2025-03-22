/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.elytron.oidc;

import static org.jboss.as.server.security.SecurityMetaData.OPERATION_CONTEXT_ATTACHMENT_KEY;
import static org.jboss.as.server.security.VirtualDomainMarkerUtility.virtualDomainName;
import static org.jboss.as.server.security.VirtualDomainUtil.VIRTUAL;
import static org.jboss.as.web.common.VirtualHttpServerMechanismFactoryMarkerUtility.virtualMechanismFactoryName;
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.AUTHENTICATION_REQUEST_FORMAT;
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.DISABLE_TRUST_MANAGER;
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.REQUEST_OBJECT_ENCRYPTION_ALG_VALUE;
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.REQUEST_OBJECT_ENCRYPTION_ENC_VALUE;
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.REQUEST_OBJECT_SIGNING_ALGORITHM;
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.REQUEST_OBJECT_SIGNING_KEY_ALIAS;
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.REQUEST_OBJECT_SIGNING_KEY_PASSWORD;
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.REQUEST_OBJECT_SIGNING_KEYSTORE_FILE;
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.REQUEST_OBJECT_SIGNING_KEYSTORE_PASSWORD;
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.REQUEST_OBJECT_SIGNING_KEYSTORE_TYPE;
import static org.wildfly.extension.elytron.oidc._private.ElytronOidcLogger.ROOT_LOGGER;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.security.AdvancedSecurityMetaData;
import org.jboss.as.server.security.VirtualDomainMarkerUtility;
import org.jboss.as.version.Stability;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.security.auth.permission.LoginPermission;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.http.HttpServerAuthenticationMechanismFactory;
import org.wildfly.security.http.oidc.OidcClientConfigurationBuilder;
import org.wildfly.security.http.oidc.OidcClientContext;
import org.wildfly.security.http.oidc.OidcMechanismFactory;
import org.wildfly.security.http.oidc.OidcSecurityRealm;

/**
 * A {@link ResourceDefinition} for securing deployments via OpenID Connect.
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
class SecureDeploymentDefinition extends SimpleResourceDefinition {

    static final ResourceRegistration PATH = ResourceRegistration.of(PathElement.pathElement(ElytronOidcDescriptionConstants.SECURE_DEPLOYMENT), Stability.DEFAULT);
    protected static List<SimpleAttributeDefinition> NON_DEFAULT_ATTRIBUTES = new ArrayList<>();
    protected static final SimpleAttributeDefinition REALM =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.REALM, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, true))
                    .build();

    protected static final SimpleAttributeDefinition PROVIDER =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.PROVIDER, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, true))
                    .build();

    protected static final SimpleAttributeDefinition RESOURCE =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.RESOURCE, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, true))
                    .setAlternatives(ElytronOidcDescriptionConstants.CLIENT_ID)
                    .build();

    protected static final SimpleAttributeDefinition CLIENT_ID =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.CLIENT_ID, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, true))
                    .setAlternatives(ElytronOidcDescriptionConstants.RESOURCE)
                    .build();

    protected static final SimpleAttributeDefinition SCOPE =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.SCOPE, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, true))
                    .setStability(Stability.PREVIEW)
                    .build();

    protected static final SimpleAttributeDefinition USE_RESOURCE_ROLE_MAPPINGS =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.USE_RESOURCE_ROLE_MAPPINGS, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setDefaultValue(ModelNode.FALSE)
                    .build();

    protected static final SimpleAttributeDefinition BEARER_ONLY =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.BEARER_ONLY, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setDefaultValue(ModelNode.FALSE)
                    .build();

    protected static final SimpleAttributeDefinition ENABLE_BASIC_AUTH =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.ENABLE_BASIC_AUTH, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setDefaultValue(ModelNode.FALSE)
                    .build();

    protected static final SimpleAttributeDefinition PUBLIC_CLIENT =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.PUBLIC_CLIENT, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setDefaultValue(ModelNode.FALSE)
                    .build();

    protected static final SimpleAttributeDefinition TURN_OFF_CHANGE_SESSION_ID_ON_LOGIN =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.TURN_OFF_CHANGE_SESSION_ID_ON_LOGIN, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setDefaultValue(ModelNode.FALSE)
                    .build();

    protected static final SimpleAttributeDefinition TOKEN_MINIMUM_TIME_TO_LIVE =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.TOKEN_MINIMUM_TIME_TO_LIVE, ModelType.INT, true)
                    .setValidator(new IntRangeValidator(-1, true))
                    .setAllowExpression(true)
                    .build();

    protected static final SimpleAttributeDefinition MIN_TIME_BETWEEN_JWKS_REQUESTS =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.MIN_TIME_BETWEEN_JWKS_REQUESTS, ModelType.INT, true)
                    .setValidator(new IntRangeValidator(-1, true))
                    .setAllowExpression(true)
                    .build();

    protected static final SimpleAttributeDefinition PUBLIC_KEY_CACHE_TTL =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.PUBLIC_KEY_CACHE_TTL, ModelType.INT, true)
                    .setAllowExpression(true)
                    .setValidator(new IntRangeValidator(-1, true))
                    .build();

    protected static final SimpleAttributeDefinition ADAPTER_STATE_COOKIE_PATH =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.ADAPTER_STATE_COOKIE_PATH, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, true))
                    .build();

    protected static final SimpleAttributeDefinition LOGOUT_PATH =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.LOGOUT_PATH, ModelType.STRING, true)
                    .setValidator(new StringLengthValidator(0, Integer.MAX_VALUE, true, false))
                    .setStability(Stability.PREVIEW)
                    .setAllowExpression(true)
                    .build();
    protected static final SimpleAttributeDefinition LOGOUT_CALLBACK_PATH =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.LOGOUT_CALLBACK_PATH, ModelType.STRING, true)
                    .setValidator(new StringLengthValidator(0, Integer.MAX_VALUE, true, false))
                    .setStability(Stability.PREVIEW)
                    .setAllowExpression(true)
                    .build();
    protected static final SimpleAttributeDefinition POST_LOGOUT_REDIRECT_URI =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.POST_LOGOUT_REDIRECT_URI, ModelType.STRING, true)
                    .setValidator(new StringLengthValidator(0, Integer.MAX_VALUE, true, false))
                    .setStability(Stability.PREVIEW)
                    .setAllowExpression(true)
                    .build();
    // rls start
    protected static final SimpleAttributeDefinition BACK_CHANNEL_LOGOUT_SESSION_INVALIDATION_LIMIT =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.BACK_CHANNEL_LOGOUT_SESSION_INVALIDATION_LIMIT, ModelType.INT, true)
                    .setAllowExpression(true)
                    .setValidator(new IntRangeValidator(-1, true))
                    .build();
    // rls end
    protected static final SimpleAttributeDefinition LOGOUT_SESSION_REQUIRED =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.LOGOUT_SESSION_REQUIRED, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setDefaultValue(ModelNode.TRUE)
                    .build();

    static final List<SimpleAttributeDefinition> ALL_ATTRIBUTES = new ArrayList();
    static {
        ALL_ATTRIBUTES.add(REALM);
        ALL_ATTRIBUTES.add(PROVIDER);
        ALL_ATTRIBUTES.add(RESOURCE);
        ALL_ATTRIBUTES.add(CLIENT_ID);
        ALL_ATTRIBUTES.add(USE_RESOURCE_ROLE_MAPPINGS);
        ALL_ATTRIBUTES.add(BEARER_ONLY);
        ALL_ATTRIBUTES.add(ENABLE_BASIC_AUTH);
        ALL_ATTRIBUTES.add(PUBLIC_CLIENT);
        ALL_ATTRIBUTES.add(TURN_OFF_CHANGE_SESSION_ID_ON_LOGIN);
        ALL_ATTRIBUTES.add(TOKEN_MINIMUM_TIME_TO_LIVE);
        ALL_ATTRIBUTES.add(MIN_TIME_BETWEEN_JWKS_REQUESTS);
        ALL_ATTRIBUTES.add(PUBLIC_KEY_CACHE_TTL);
        ALL_ATTRIBUTES.add(ADAPTER_STATE_COOKIE_PATH);
        ALL_ATTRIBUTES.add(CredentialDefinition.CREDENTIAL);
        ALL_ATTRIBUTES.add(SCOPE);
        ALL_ATTRIBUTES.add(RedirectRewriteRuleDefinition.REDIRECT_REWRITE_RULE);
        ALL_ATTRIBUTES.add(LOGOUT_PATH);
        ALL_ATTRIBUTES.add(LOGOUT_CALLBACK_PATH);
        ALL_ATTRIBUTES.add(POST_LOGOUT_REDIRECT_URI);
        ALL_ATTRIBUTES.add(LOGOUT_SESSION_REQUIRED);
        ALL_ATTRIBUTES.add(BACK_CHANNEL_LOGOUT_SESSION_INVALIDATION_LIMIT);
        for (SimpleAttributeDefinition attribute : ProviderAttributeDefinitions.ATTRIBUTES) {
            ALL_ATTRIBUTES.add(attribute);
        }
    }

    private static final String WAR_FILE_EXTENSION = ".war";

    SecureDeploymentDefinition() {
        super(new Parameters(PATH,
                ElytronOidcExtension.getResourceDescriptionResolver(ElytronOidcDescriptionConstants.SECURE_DEPLOYMENT))
                .setAddHandler(SecureDeploymentAddHandler.INSTANCE)
                .setRemoveHandler(SecureDeploymentRemoveHandler.INSTANCE)
                .setAddRestartLevel(OperationEntry.Flag.RESTART_RESOURCE_SERVICES)
                .setRemoveRestartLevel(OperationEntry.Flag.RESTART_RESOURCE_SERVICES));
        NON_DEFAULT_ATTRIBUTES.add(SCOPE);
        NON_DEFAULT_ATTRIBUTES.add(AUTHENTICATION_REQUEST_FORMAT);
        NON_DEFAULT_ATTRIBUTES.add(REQUEST_OBJECT_ENCRYPTION_ENC_VALUE);
        NON_DEFAULT_ATTRIBUTES.add(REQUEST_OBJECT_ENCRYPTION_ALG_VALUE);
        NON_DEFAULT_ATTRIBUTES.add(REQUEST_OBJECT_SIGNING_KEYSTORE_FILE);
        NON_DEFAULT_ATTRIBUTES.add(REQUEST_OBJECT_SIGNING_KEY_ALIAS);
        NON_DEFAULT_ATTRIBUTES.add(REQUEST_OBJECT_SIGNING_KEY_PASSWORD);
        NON_DEFAULT_ATTRIBUTES.add(REQUEST_OBJECT_SIGNING_KEYSTORE_PASSWORD);
        NON_DEFAULT_ATTRIBUTES.add(REQUEST_OBJECT_SIGNING_KEYSTORE_TYPE);
        NON_DEFAULT_ATTRIBUTES.add(REQUEST_OBJECT_SIGNING_ALGORITHM);
        NON_DEFAULT_ATTRIBUTES.add(LOGOUT_PATH);
        NON_DEFAULT_ATTRIBUTES.add(LOGOUT_CALLBACK_PATH);
        NON_DEFAULT_ATTRIBUTES.add(POST_LOGOUT_REDIRECT_URI);
        NON_DEFAULT_ATTRIBUTES.add(LOGOUT_SESSION_REQUIRED);
        NON_DEFAULT_ATTRIBUTES.add(BACK_CHANNEL_LOGOUT_SESSION_INVALIDATION_LIMIT);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        for (AttributeDefinition attribute : ALL_ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attribute, null, SecureDeploymentWriteAttributeHandler.INSTANCE);
        }
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerSubModel(new CredentialDefinition());
        resourceRegistration.registerSubModel(new RedirectRewriteRuleDefinition());
    }

    static class SecureDeploymentAddHandler extends AbstractAddStepHandler {
        public static SecureDeploymentAddHandler INSTANCE = new SecureDeploymentAddHandler();

        private SecureDeploymentAddHandler() {
            super(ALL_ATTRIBUTES);
        }

        @Override
        protected void populateModel(final OperationContext context, final ModelNode operation, final Resource resource) throws  OperationFailedException {
            super.populateModel(context, operation, resource);
            if (! isWarDeployment(context)) {
                VirtualDomainMarkerUtility.virtualDomainRequired(context);
                AdvancedSecurityMetaData advancedSecurityMetaData = new AdvancedSecurityMetaData();
                advancedSecurityMetaData.setHttpServerAuthenticationMechanismFactory(virtualMechanismFactoryName(context));
                advancedSecurityMetaData.setSecurityDomain(virtualDomainName(context));
                context.attach(OPERATION_CONTEXT_ATTACHMENT_KEY, advancedSecurityMetaData);
            }
        }

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            super.performRuntime(context, operation, model);
            String clientId = CLIENT_ID.resolveModelAttribute(context, model).asStringOrNull();
            String resource = RESOURCE.resolveModelAttribute(context, model).asStringOrNull();
            if (clientId == null && resource == null) {
                throw ROOT_LOGGER.resourceOrClientIdMustBeConfigured();
            }

            boolean disableTrustManager = DISABLE_TRUST_MANAGER.resolveModelAttribute(context, model).asBoolean();
            if (disableTrustManager) {
                ROOT_LOGGER.disableTrustManagerSetToTrue();
            }
            String secureDeploymentName = context.getCurrentAddressValue();
            OidcConfigService oidcConfigService = OidcConfigService.getInstance();
            oidcConfigService.addSecureDeployment(secureDeploymentName, context.resolveExpressions(model));

            if (! isWarDeployment(context)) {
                ServiceTarget serviceTarget = context.getServiceTarget();

                ServiceName virtualMechanismFactoryName = virtualMechanismFactoryName(context);
                ServiceBuilder<?> serviceBuilder = serviceTarget.addService(virtualMechanismFactoryName);

                final OidcClientContext oidcClientContext = new OidcClientContext(OidcClientConfigurationBuilder
                        .build(new ByteArrayInputStream(oidcConfigService.getJSON(secureDeploymentName).getBytes())));
                final HttpServerAuthenticationMechanismFactory virtualMechanismFactory = new OidcMechanismFactory(oidcClientContext);

                final Consumer<HttpServerAuthenticationMechanismFactory> mechanismFactoryConsumer = serviceBuilder.provides(virtualMechanismFactoryName);
                serviceBuilder.setInstance(Service.newInstance(mechanismFactoryConsumer, virtualMechanismFactory));
                serviceBuilder.setInitialMode(ServiceController.Mode.ON_DEMAND);
                serviceBuilder.install();

                ServiceName virtualDomainName = VirtualDomainMarkerUtility.virtualDomainName(context);
                serviceBuilder = serviceTarget.addService(virtualDomainName);

                SecurityDomain virtualDomain = SecurityDomain.builder()
                        .addRealm(VIRTUAL, new OidcSecurityRealm()).build()
                        .setDefaultRealmName(VIRTUAL)
                        .setPermissionMapper((permissionMappable, roles) -> LoginPermission.getInstance())
                        .build();

                Consumer<SecurityDomain> securityDomainConsumer = serviceBuilder.provides(new ServiceName[]{virtualDomainName});
                serviceBuilder.setInstance(Service.newInstance(securityDomainConsumer, virtualDomain));
                serviceBuilder.setInitialMode(ServiceController.Mode.ON_DEMAND);
                serviceBuilder.install();

                if (! context.isBooting()) {
                    context.reloadRequired();
                }
            }
        }
    }

    static class SecureDeploymentWriteAttributeHandler extends AbstractWriteAttributeHandler<OidcConfigService> {
        public static final SecureDeploymentWriteAttributeHandler INSTANCE = new SecureDeploymentWriteAttributeHandler();

        SecureDeploymentWriteAttributeHandler() {
            super(ALL_ATTRIBUTES.toArray(new SimpleAttributeDefinition[ALL_ATTRIBUTES.size()]));
        }

        @Override
        protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                               ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<OidcConfigService> handbackHolder) throws OperationFailedException {
            OidcConfigService oidcConfigService = OidcConfigService.getInstance();
            oidcConfigService.updateSecureDeployment(context.getCurrentAddressValue(), attributeName, resolvedValue);
            handbackHolder.setHandback(oidcConfigService);
            return ! isWarDeployment(context);
        }

        @Override
        protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                             ModelNode valueToRestore, ModelNode valueToRevert, OidcConfigService oidcConfigService) throws OperationFailedException {
            oidcConfigService.updateSecureDeployment(context.getCurrentAddressValue(), attributeName, valueToRestore);
        }
    }

    static class SecureDeploymentRemoveHandler extends AbstractRemoveStepHandler {
        public static SecureDeploymentRemoveHandler INSTANCE = new SecureDeploymentRemoveHandler();

        SecureDeploymentRemoveHandler() {
        }

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            OidcConfigService oidcConfigService = OidcConfigService.getInstance();
            oidcConfigService.removeSecureDeployment(context.getCurrentAddressValue());
            if (! isWarDeployment(context)) {
                context.reloadRequired();
            }
        }
    }

    static boolean isWarDeployment(OperationContext context) {
        return context.getCurrentAddressValue().endsWith(WAR_FILE_EXTENSION);
    }
}
