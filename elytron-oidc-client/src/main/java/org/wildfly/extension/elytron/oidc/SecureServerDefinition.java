/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.elytron.oidc;

import static org.wildfly.extension.elytron.oidc.ElytronOidcDescriptionConstants.SECURE_SERVER;
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.DISABLE_TRUST_MANAGER;
import static org.wildfly.extension.elytron.oidc.SecureDeploymentDefinition.ALL_ATTRIBUTES;
import static org.wildfly.extension.elytron.oidc.SecureDeploymentDefinition.CLIENT_ID;
import static org.wildfly.extension.elytron.oidc.SecureDeploymentDefinition.RESOURCE;
import static org.wildfly.extension.elytron.oidc._private.ElytronOidcLogger.ROOT_LOGGER;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import io.undertow.io.IoCallback;
import io.undertow.io.Sender;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.resource.Resource;
import io.undertow.server.handlers.resource.ResourceChangeListener;
import io.undertow.server.handlers.resource.ResourceManager;
import io.undertow.util.ETag;
import io.undertow.util.MimeMappings;

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
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.server.mgmt.domain.ExtensibleHttpManagement;
import org.jboss.as.version.Stability;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.security.http.oidc.Oidc;

/**
 * A {@link ResourceDefinition} for securing deployments via OpenID Connect.
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
class SecureServerDefinition extends SimpleResourceDefinition {

    static final ResourceRegistration PATH = ResourceRegistration.of(PathElement.pathElement(SECURE_SERVER), Stability.DEFAULT);
    private static String HTTP_MANAGEMENT_CONTEXT = "http-management-context";

    SecureServerDefinition() {
        super(new Parameters(PATH,
                ElytronOidcExtension.getResourceDescriptionResolver(SECURE_SERVER))
                .setAddHandler(SecureServerDefinition.SecureServerAddHandler.INSTANCE)
                .setRemoveHandler(SecureServerDefinition.SecureServerRemoveHandler.INSTANCE)
                .setAddRestartLevel(OperationEntry.Flag.RESTART_RESOURCE_SERVICES)
                .setRemoveRestartLevel(OperationEntry.Flag.RESTART_RESOURCE_SERVICES));
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        for (AttributeDefinition attribute : ALL_ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attribute, null, SecureServerDefinition.SecureServerWriteAttributeHandler.INSTANCE);
        }
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerSubModel(new CredentialDefinition());
        resourceRegistration.registerSubModel(new RedirectRewriteRuleDefinition());
    }

    static class SecureServerAddHandler extends AbstractAddStepHandler {
        public static SecureServerDefinition.SecureServerAddHandler INSTANCE = new SecureServerDefinition.SecureServerAddHandler();
        static final String HTTP_MANAGEMENT_HTTP_EXTENSIBLE_CAPABILITY = "org.wildfly.management.http.extensible";

        private SecureServerAddHandler() {
            super(ALL_ATTRIBUTES);
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
            OidcConfigService oidcConfigService = OidcConfigService.getInstance();
            oidcConfigService.addSecureDeployment(context.getCurrentAddressValue(), context.resolveExpressions(model));

            ServiceTarget serviceTarget = context.getServiceTarget();
            InjectedValue<ExtensibleHttpManagement> extensibleHttpManagement = new InjectedValue<>();
            String secureServerName = context.getCurrentAddressValue();
            ServiceName serviceName = ServiceName.of(SECURE_SERVER, secureServerName);
            serviceTarget.addService(serviceName.append(HTTP_MANAGEMENT_CONTEXT), createHttpManagementConfigContextService(secureServerName, extensibleHttpManagement))
                    .addDependency(context.getCapabilityServiceName(HTTP_MANAGEMENT_HTTP_EXTENSIBLE_CAPABILITY, ExtensibleHttpManagement.class),
                            ExtensibleHttpManagement.class, extensibleHttpManagement).setInitialMode(ServiceController.Mode.ACTIVE).install();
        }
    }

    static class SecureServerWriteAttributeHandler extends AbstractWriteAttributeHandler<OidcConfigService> {
        public static final SecureServerDefinition.SecureServerWriteAttributeHandler INSTANCE = new SecureServerDefinition.SecureServerWriteAttributeHandler();

        SecureServerWriteAttributeHandler() {
            super(ALL_ATTRIBUTES.toArray(new SimpleAttributeDefinition[ALL_ATTRIBUTES.size()]));
        }

        @Override
        protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                               ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<OidcConfigService> handbackHolder) throws OperationFailedException {
            OidcConfigService oidcConfigService = OidcConfigService.getInstance();
            oidcConfigService.updateSecureDeployment(context.getCurrentAddressValue(), attributeName, resolvedValue);
            handbackHolder.setHandback(oidcConfigService);
            return false;
        }

        @Override
        protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                             ModelNode valueToRestore, ModelNode valueToRevert, OidcConfigService oidcConfigService) throws OperationFailedException {
            oidcConfigService.updateSecureDeployment(context.getCurrentAddressValue(), attributeName, valueToRestore);
        }
    }

    static class SecureServerRemoveHandler extends AbstractRemoveStepHandler {
        public static SecureServerDefinition.SecureServerRemoveHandler INSTANCE = new SecureServerDefinition.SecureServerRemoveHandler();

        SecureServerRemoveHandler() {
        }

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            OidcConfigService oidcConfigService = OidcConfigService.getInstance();
            oidcConfigService.removeSecureDeployment(context.getCurrentAddressValue());
        }
    }

    private static Service<Void> createHttpManagementConfigContextService(final String secureServerName, final InjectedValue<ExtensibleHttpManagement> httpConfigContext) {
        final String contextName = "/oidc/" + secureServerName + "/";
        return new Service<Void>() {
            public void start(StartContext startContext) throws StartException {
                ExtensibleHttpManagement extensibleHttpManagement = (ExtensibleHttpManagement)httpConfigContext.getValue();
                extensibleHttpManagement.addStaticContext(contextName, new ResourceManager() {
                    public Resource getResource(final String path) throws IOException {
                        OidcConfigService oidcConfigService = OidcConfigService.getInstance();
                        // need to convert to realm configuration to work with the Keycloak JavaScript adapter that HAL uses
                        final String config = oidcConfigService.getJSON(secureServerName, true);

                        if (config == null) {
                            return null;
                        }

                        return new Resource() {
                            public String getPath() {
                                return null;
                            }

                            public Date getLastModified() {
                                return null;
                            }

                            public String getLastModifiedString() {
                                return null;
                            }

                            public ETag getETag() {
                                return null;
                            }

                            public String getName() {
                                return null;
                            }

                            public boolean isDirectory() {
                                return false;
                            }

                            public List<Resource> list() {
                                return Collections.emptyList();
                            }

                            public String getContentType(MimeMappings mimeMappings) {
                                return Oidc.JSON_CONTENT_TYPE;
                            }

                            public void serve(Sender sender, HttpServerExchange exchange, IoCallback completionCallback) {
                                sender.send(config);
                            }

                            public Long getContentLength() {
                                return Long.valueOf((long)config.length());
                            }

                            public String getCacheKey() {
                                return null;
                            }

                            public File getFile() {
                                return null;
                            }

                            public Path getFilePath() {
                                return null;
                            }

                            public File getResourceManagerRoot() {
                                return null;
                            }

                            public Path getResourceManagerRootPath() {
                                return null;
                            }

                            public URL getUrl() {
                                return null;
                            }
                        };
                    }

                    public boolean isResourceChangeListenerSupported() {
                        return false;
                    }

                    public void registerResourceChangeListener(ResourceChangeListener listener) {
                    }

                    public void removeResourceChangeListener(ResourceChangeListener listener) {
                    }

                    public void close() throws IOException {
                    }
                });
            }

            public void stop(StopContext stopContext) {
                ((ExtensibleHttpManagement)httpConfigContext.getValue()).removeContext(contextName);
            }

            public Void getValue() throws IllegalStateException, IllegalArgumentException {
                return null;
            }
        };
    }

}
