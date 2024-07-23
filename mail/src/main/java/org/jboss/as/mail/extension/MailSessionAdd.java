/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.mail.extension;

import static org.jboss.as.controller.security.CredentialReference.KEY_DELIMITER;
import static org.jboss.as.controller.security.CredentialReference.rollbackCredentialStoreUpdate;
import static org.jboss.as.mail.extension.MailSessionDefinition.ATTRIBUTES;
import static org.jboss.as.mail.extension.MailSessionDefinition.SESSION_CAPABILITY;
import static org.jboss.as.mail.extension.MailSubsystemModel.CUSTOM;
import static org.jboss.as.mail.extension.MailSubsystemModel.IMAP;
import static org.jboss.as.mail.extension.MailSubsystemModel.POP3;
import static org.jboss.as.mail.extension.MailSubsystemModel.SERVER_TYPE;
import static org.jboss.as.mail.extension.MailSubsystemModel.SMTP;
import static org.jboss.as.mail.extension.MailSubsystemModel.USER_NAME;

import java.util.Map;
import java.util.function.Supplier;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.registry.Resource.ResourceEntry;
import org.jboss.as.controller.security.CredentialReference;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.Service;
import org.jboss.msc.service.LifecycleEvent;
import org.jboss.msc.service.LifecycleListener;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * Add operation handler for the session resource.
 * @author Tomaz Cerar
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @created 27.7.11 0:55
 */
class MailSessionAdd extends AbstractAddStepHandler {

    MailSessionAdd() {
        super(ATTRIBUTES);
    }

    /**
     * Make any runtime changes necessary to effect the changes indicated by the given {@code operation}. E
     * <p>
     * It constructs a MailSessionService that provides mail session and registers it to Naming service.
     * </p>
     *
     * @param context             the operation context
     * @param operation           the operation being executed
     * @param model               persistent configuration model node that corresponds to the address of {@code operation}
     * @throws org.jboss.as.controller.OperationFailedException
     *          if {@code operation} is invalid or updating the runtime otherwise fails
     */
    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        ModelNode fullModel = Resource.Tools.readModel(context.readResource(PathAddress.EMPTY_ADDRESS));
        installRuntimeServices(context, fullModel);
    }

    @Override
    protected void rollbackRuntime(OperationContext context, ModelNode operation, Resource resource) {
        try {
            MailSessionRemove.removeRuntimeServices(context, resource.getModel());

            for (ResourceEntry entry : resource.getChildren(MailSubsystemModel.SERVER_TYPE)) {
                ModelNode resolvedValue = MailServerDefinition.CREDENTIAL_REFERENCE.resolveModelAttribute(context, entry.getModel());
                if (resolvedValue.isDefined()) {
                    rollbackCredentialStoreUpdate(MailServerDefinition.CREDENTIAL_REFERENCE, context, resolvedValue);
                }
            }
        } catch (OperationFailedException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void addCredentialStoreReference(ServerConfig serverConfig, OperationContext context, ModelNode model, ServiceBuilder<?> serviceBuilder, final PathElement credRefParentAddress) throws OperationFailedException {
        if (serverConfig != null) {
            final String servertype = credRefParentAddress.getKey();
            final String serverprotocol = credRefParentAddress.getValue();
            ModelNode filteredModelNode = model;

            if (filteredModelNode.hasDefined(servertype, serverprotocol)) {
                filteredModelNode = filteredModelNode.get(servertype, serverprotocol);
            } else {
                return;
            }

            String keySuffix = servertype + KEY_DELIMITER + serverprotocol;
            ModelNode value = MailServerDefinition.CREDENTIAL_REFERENCE.resolveModelAttribute(context, filteredModelNode);
            if (value.isDefined()) {
                serverConfig.getCredentialSourceSupplierInjector()
                        .inject(CredentialReference.getCredentialSourceSupplier(context, MailServerDefinition.CREDENTIAL_REFERENCE, filteredModelNode, serviceBuilder, keySuffix));
            }
        }
    }

    static void installSessionProviderService(OperationContext context, ModelNode fullModel) throws OperationFailedException {
        installSessionProviderService(context, context.getCurrentAddress(), fullModel);
    }

    static void installSessionProviderService(OperationContext context, PathAddress address, ModelNode fullModel) throws OperationFailedException {
        ServiceName serviceName = SESSION_CAPABILITY.getCapabilityServiceName(address).append("provider");

        ServiceBuilder<?> builder = context.getServiceTarget().addService(serviceName);

        MailSessionConfig config = from(context, fullModel, builder);

        addCredentialStoreReference(config.getImapServer(), context, fullModel, builder, MailSubsystemModel.IMAP_SERVER_PATH);
        addCredentialStoreReference(config.getPop3Server(), context, fullModel, builder, MailSubsystemModel.POP3_SERVER_PATH);
        addCredentialStoreReference(config.getSmtpServer(), context, fullModel, builder, MailSubsystemModel.SMTP_SERVER_PATH);
        for (CustomServerConfig server : config.getCustomServers()) {
            addCredentialStoreReference(server, context, fullModel, builder, PathElement.pathElement(MailSubsystemModel.CUSTOM_SERVER_PATH.getKey(), server.getProtocol()));
        }
        Service providerService = new ConfigurableSessionProviderService(builder.provides(serviceName), config);
        builder.setInstance(providerService).setInitialMode(ServiceController.Mode.ON_DEMAND).install();
    }

    static void installBinderService(OperationContext context, ModelNode fullModel) throws OperationFailedException {
        String jndiName = getJndiName(fullModel, context);

        ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(jndiName);
        String bindName = bindInfo.getBindName();
        BinderService service = new BinderService(bindName);
        ServiceBuilder<?> builder = context.getServiceTarget().addService(bindInfo.getBinderServiceName(), service).addAliases(ContextNames.JAVA_CONTEXT_SERVICE_NAME.append(bindName));
        Supplier<SessionProvider> provider = builder.requires(SESSION_CAPABILITY.getCapabilityServiceName(context.getCurrentAddress()).append("provider"));
        service.getManagedObjectInjector().inject(new MailSessionManagedReferenceFactory(provider));
        builder.addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, service.getNamingStoreInjector());
        builder.addListener(new LifecycleListener() {
            private volatile boolean bound;
            @Override
            public void handleEvent(final ServiceController<?> controller, final LifecycleEvent event) {
                switch (event) {
                    case UP: {
                        MailLogger.ROOT_LOGGER.boundMailSession(jndiName);
                        bound = true;
                        break;
                    }
                    case DOWN: {
                        if (bound) {
                            MailLogger.ROOT_LOGGER.unboundMailSession(jndiName);
                        }
                        break;
                    }
                    case REMOVED: {
                        MailLogger.ROOT_LOGGER.removedMailSession(jndiName);
                        break;
                    }
                }
            }
        });
        builder.setInitialMode(ServiceController.Mode.ACTIVE).install();
    }

    static void installRuntimeServices(OperationContext context, ModelNode fullModel) throws OperationFailedException {
        installSessionProviderService(context, fullModel);

        ServiceName sessionServiceName = SESSION_CAPABILITY.getCapabilityServiceName(context.getCurrentAddress());
        ServiceName providerServiceName = sessionServiceName.append("provider");

        // TODO Consider removing this service (and either changing or removing its corresponding capability) - it is never referenced.
        CapabilityServiceBuilder<?> mailSessionBuilder = context.getCapabilityServiceTarget().addCapability(SESSION_CAPABILITY);
        Service mailService = new MailSessionService(mailSessionBuilder.provides(sessionServiceName), mailSessionBuilder.requires(providerServiceName));
        mailSessionBuilder.setInstance(mailService).setInitialMode(ServiceController.Mode.ON_DEMAND).install();

        installBinderService(context, fullModel);
    }

    /**
     * Extracts the raw JNDI_NAME value from the given model node, and depending on the value and
     * the value of any USE_JAVA_CONTEXT child node, converts the raw name into a compliant jndi name.
     *
     * @param modelNode the model node; either an operation or the model behind a mail session resource
     * @return the compliant jndi name
     */
    static String getJndiName(final ModelNode modelNode, OperationContext context) throws OperationFailedException {
        final String rawJndiName = MailSessionDefinition.JNDI_NAME.resolveModelAttribute(context, modelNode).asString();
        return getJndiName(rawJndiName);
    }

    public static String getJndiName(final String rawJndiName) {
        final String jndiName;
        if (!rawJndiName.startsWith("java:")) {
            jndiName = "java:jboss/mail/" + rawJndiName;
        } else {
            jndiName = rawJndiName;
        }
        return jndiName;
    }

    private static Supplier<OutboundSocketBinding> requireOutboundSocketBinding(OperationContext context, ServiceBuilder<?> builder, String ref) {
        return (ref != null) ? builder.requires(context.getCapabilityServiceName(OutboundSocketBinding.SERVICE_DESCRIPTOR, ref)) : null;
    }

    static MailSessionConfig from(final OperationContext operationContext, final ModelNode model, ServiceBuilder<?> builder) throws OperationFailedException {
        MailSessionConfig cfg = new MailSessionConfig();

        cfg.setJndiName(MailSessionDefinition.JNDI_NAME.resolveModelAttribute(operationContext, model).asString());
        cfg.setDebug(MailSessionDefinition.DEBUG.resolveModelAttribute(operationContext, model).asBoolean());
        if (MailSessionDefinition.FROM.resolveModelAttribute(operationContext, model).isDefined()) {
            cfg.setFrom(MailSessionDefinition.FROM.resolveModelAttribute(operationContext, model).asString());
        }
        if (model.hasDefined(SERVER_TYPE)) {
            ModelNode server = model.get(SERVER_TYPE);
            if (server.hasDefined(SMTP)) {
                cfg.setSmtpServer(readServerConfig(operationContext, server.get(SMTP), builder));
            }
            if (server.hasDefined(POP3)) {
                cfg.setPop3Server(readServerConfig(operationContext, server.get(POP3), builder));
            }
            if (server.hasDefined(IMAP)) {
                cfg.setImapServer(readServerConfig(operationContext, server.get(IMAP), builder));
            }
        }
        if (model.hasDefined(CUSTOM)) {
            for (Property server : model.get(CUSTOM).asPropertyList()) {
                cfg.addCustomServer(readCustomServerConfig(server.getName(), operationContext, server.getValue(), builder));
            }
        }
        return cfg;
    }

    private static ServerConfig readServerConfig(final OperationContext operationContext, final ModelNode model, ServiceBuilder<?> builder) throws OperationFailedException {
        final String socket = MailServerDefinition.OUTBOUND_SOCKET_BINDING_REF.resolveModelAttribute(operationContext, model).asString();
        final Credentials credentials = readCredentials(operationContext, model);
        boolean ssl = MailServerDefinition.SSL.resolveModelAttribute(operationContext, model).asBoolean();
        boolean tls = MailServerDefinition.TLS.resolveModelAttribute(operationContext, model).asBoolean();
        return new ServerConfig(requireOutboundSocketBinding(operationContext, builder, socket), credentials, ssl, tls, null);
    }

    private static CustomServerConfig readCustomServerConfig(final String protocol, final OperationContext operationContext, final ModelNode model, ServiceBuilder<?> builder) throws OperationFailedException {
        final String socket = MailServerDefinition.OUTBOUND_SOCKET_BINDING_REF_OPTIONAL.resolveModelAttribute(operationContext, model).asStringOrNull();
        final Credentials credentials = readCredentials(operationContext, model);
        boolean ssl = MailServerDefinition.SSL.resolveModelAttribute(operationContext, model).asBoolean();
        boolean tls = MailServerDefinition.TLS.resolveModelAttribute(operationContext, model).asBoolean();
        Map<String, String> properties = MailServerDefinition.PROPERTIES.unwrap(operationContext, model);
        return new CustomServerConfig(protocol, requireOutboundSocketBinding(operationContext, builder, socket), credentials, ssl, tls, properties);
    }

    private static Credentials readCredentials(final OperationContext operationContext, final ModelNode model) throws OperationFailedException {
        if (model.get(USER_NAME).isDefined()) {
            String un = MailServerDefinition.USERNAME.resolveModelAttribute(operationContext, model).asString();
            String pw = MailServerDefinition.PASSWORD.resolveModelAttribute(operationContext, model).asStringOrNull();
            ModelNode value = MailServerDefinition.CREDENTIAL_REFERENCE.resolveValue(operationContext, model);
            String secret = null;
            if (value.isDefined()) {
                secret = CredentialReference.credentialReferencePartAsStringIfDefined(value, CredentialReference.CLEAR_TEXT);
            }
            if (secret != null) {
                return new Credentials(un, secret);
            } else {
                return new Credentials(un, pw);
            }
        }
        return null;
    }

}
