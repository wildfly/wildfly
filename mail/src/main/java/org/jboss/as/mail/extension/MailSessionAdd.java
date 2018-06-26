/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.mail.extension;

import static org.jboss.as.mail.extension.MailServerDefinition.OUTBOUND_SOCKET_BINDING_CAPABILITY_NAME;
import static org.jboss.as.mail.extension.MailSessionDefinition.ATTRIBUTES;
import static org.jboss.as.mail.extension.MailSessionDefinition.SESSION_CAPABILITY;
import static org.jboss.as.mail.extension.MailSubsystemModel.CUSTOM;
import static org.jboss.as.mail.extension.MailSubsystemModel.IMAP;
import static org.jboss.as.mail.extension.MailSubsystemModel.POP3;
import static org.jboss.as.mail.extension.MailSubsystemModel.SERVER_TYPE;
import static org.jboss.as.mail.extension.MailSubsystemModel.SMTP;
import static org.jboss.as.mail.extension.MailSubsystemModel.USER_NAME;

import java.util.Map;

import javax.mail.Session;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.CapabilityServiceTarget;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.security.CredentialReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.service.LifecycleEvent;
import org.jboss.msc.service.LifecycleListener;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * @author Tomaz Cerar
 * @created 27.7.11 0:55
 */
class MailSessionAdd extends AbstractAddStepHandler {

    static final MailSessionAdd INSTANCE = new MailSessionAdd();
    @Deprecated
    static final ServiceName MAIL_SESSION_SERVICE_NAME = ServiceName.JBOSS.append("mail-session");

    protected MailSessionAdd() {
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
        final PathAddress address = context.getCurrentAddress();
        ModelNode fullTree = Resource.Tools.readModel(context.readResource(PathAddress.EMPTY_ADDRESS));
        installRuntimeServices(context, address, fullTree);
    }

    private static void addCredentialStoreReference(ServerConfig serverConfig, OperationContext context, ModelNode model, ServiceBuilder<?> serviceBuilder, String... modelFilter) throws OperationFailedException {
        if (serverConfig != null) {
            ModelNode filteredModelNode = model;
            if (modelFilter != null && modelFilter.length > 0) {
                for (String path : modelFilter) {
                    if (filteredModelNode.get(path).isDefined())
                        filteredModelNode = filteredModelNode.get(path);
                    else
                        break;
                }
            }
            ModelNode value = MailServerDefinition.CREDENTIAL_REFERENCE.resolveModelAttribute(context, filteredModelNode);
            if (value.isDefined()) {
                serverConfig.getCredentialSourceSupplierInjector()
                        .inject(
                                CredentialReference.getCredentialSourceSupplier(context, MailServerDefinition.CREDENTIAL_REFERENCE, filteredModelNode, serviceBuilder));
            }
        }
    }

    static void installRuntimeServices(OperationContext context, PathAddress address, ModelNode fullModel) throws OperationFailedException {
        final String jndiName = getJndiName(fullModel, context);
        final CapabilityServiceTarget serviceTarget = context.getCapabilityServiceTarget();

        final MailSessionConfig config = from(context, fullModel);
        final MailSessionService service = new MailSessionService(config);

        final CapabilityServiceBuilder<Session> mailSessionBuilder = serviceTarget.addCapability(SESSION_CAPABILITY.fromBaseCapability(address.getLastElement().getValue()), service);
        addOutboundSocketDependency(service, mailSessionBuilder, config.getImapServer());
        addCredentialStoreReference(config.getImapServer(), context, fullModel, mailSessionBuilder, MailSubsystemModel.IMAP_SERVER_PATH.getKey(), MailSubsystemModel.IMAP_SERVER_PATH.getValue());
        addOutboundSocketDependency(service, mailSessionBuilder, config.getPop3Server());
        addCredentialStoreReference(config.getPop3Server(), context, fullModel, mailSessionBuilder, MailSubsystemModel.POP3_SERVER_PATH.getKey(), MailSubsystemModel.POP3_SERVER_PATH.getValue());
        addOutboundSocketDependency(service, mailSessionBuilder, config.getSmtpServer());
        addCredentialStoreReference(config.getSmtpServer(), context, fullModel, mailSessionBuilder, MailSubsystemModel.SMTP_SERVER_PATH.getKey(), MailSubsystemModel.SMTP_SERVER_PATH.getValue());
        for (CustomServerConfig server : config.getCustomServers()) {
            if (server.getOutgoingSocketBinding() != null) {
                addOutboundSocketDependency(service, mailSessionBuilder, server);
            }
            addCredentialStoreReference(server, context, fullModel, mailSessionBuilder, MailSubsystemModel.CUSTOM_SERVER_PATH.getKey(), server.getProtocol());
        }
        mailSessionBuilder.addAliases(MAIL_SESSION_SERVICE_NAME.append(address.getLastElement().getValue()));


        final ManagedReferenceFactory valueManagedReferenceFactory = new MailSessionManagedReferenceFactory(service);
        final ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(jndiName);
        final BinderService binderService = new BinderService(bindInfo.getBindName());
        final ServiceBuilder<?> binderBuilder = serviceTarget
                .addService(bindInfo.getBinderServiceName(), binderService)
                .addInjection(binderService.getManagedObjectInjector(), valueManagedReferenceFactory)
                .addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, binderService.getNamingStoreInjector()).addListener(new LifecycleListener() {
                    public void handleEvent(final ServiceController<?> controller, final LifecycleEvent event) {
                        switch (event) {
                            case UP: {
                                MailLogger.ROOT_LOGGER.boundMailSession(jndiName);
                                break;
                            }
                            case DOWN: {
                                MailLogger.ROOT_LOGGER.unboundMailSession(jndiName);
                                break;
                            }
                            case REMOVED: {
                                MailLogger.ROOT_LOGGER.removedMailSession(jndiName);
                                break;
                            }
                        }
                    }
                });

        mailSessionBuilder
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install();
        binderBuilder
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install();
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

    private static void addOutboundSocketDependency(MailSessionService service, CapabilityServiceBuilder<?> mailSessionBuilder, ServerConfig server) {
        if (server != null) {
            final String ref = server.getOutgoingSocketBinding();
            mailSessionBuilder.addCapabilityRequirement(OUTBOUND_SOCKET_BINDING_CAPABILITY_NAME, OutboundSocketBinding.class, service.getSocketBindingInjector(ref), ref);
        }
    }

    static MailSessionConfig from(final OperationContext operationContext, final ModelNode model) throws OperationFailedException {
        MailSessionConfig cfg = new MailSessionConfig();

        cfg.setJndiName(MailSessionDefinition.JNDI_NAME.resolveModelAttribute(operationContext, model).asString());
        cfg.setDebug(MailSessionDefinition.DEBUG.resolveModelAttribute(operationContext, model).asBoolean());
        if (MailSessionDefinition.FROM.resolveModelAttribute(operationContext, model).isDefined()) {
            cfg.setFrom(MailSessionDefinition.FROM.resolveModelAttribute(operationContext, model).asString());
        }
        if (model.hasDefined(SERVER_TYPE)) {
            ModelNode server = model.get(SERVER_TYPE);
            if (server.hasDefined(SMTP)) {
                cfg.setSmtpServer(readServerConfig(operationContext, server.get(SMTP)));
            }
            if (server.hasDefined(POP3)) {
                cfg.setPop3Server(readServerConfig(operationContext, server.get(POP3)));
            }
            if (server.hasDefined(IMAP)) {
                cfg.setImapServer(readServerConfig(operationContext, server.get(IMAP)));
            }
        }
        if (model.hasDefined(CUSTOM)) {
            for (Property server : model.get(CUSTOM).asPropertyList()) {
                cfg.addCustomServer(readCustomServerConfig(server.getName(), operationContext, server.getValue()));
            }
        }
        return cfg;
    }

    private static ServerConfig readServerConfig(final OperationContext operationContext, final ModelNode model) throws OperationFailedException {
        final String socket = MailServerDefinition.OUTBOUND_SOCKET_BINDING_REF.resolveModelAttribute(operationContext, model).asString();
        final Credentials credentials = readCredentials(operationContext, model);
        boolean ssl = MailServerDefinition.SSL.resolveModelAttribute(operationContext, model).asBoolean();
        boolean tls = MailServerDefinition.TLS.resolveModelAttribute(operationContext, model).asBoolean();
        return new ServerConfig(socket, credentials, ssl, tls, null);
    }

    private static CustomServerConfig readCustomServerConfig(final String protocol, final OperationContext operationContext, final ModelNode model) throws OperationFailedException {
        final ModelNode socketModel = MailServerDefinition.OUTBOUND_SOCKET_BINDING_REF_OPTIONAL.resolveModelAttribute(operationContext, model);
        final String socket = socketModel.isDefined() ? socketModel.asString() : null;
        final Credentials credentials = readCredentials(operationContext, model);
        boolean ssl = MailServerDefinition.SSL.resolveModelAttribute(operationContext, model).asBoolean();
        boolean tls = MailServerDefinition.TLS.resolveModelAttribute(operationContext, model).asBoolean();
        Map<String, String> properties = MailServerDefinition.PROPERTIES.unwrap(operationContext, model);
        return new CustomServerConfig(protocol, socket, credentials, ssl, tls, properties);
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
