package org.jboss.as.mail.extension;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.ValueManagedReference;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.ImmediateValue;

import java.util.List;

import static org.jboss.as.mail.extension.MailSubsystemModel.IMAP;
import static org.jboss.as.mail.extension.MailSubsystemModel.JNDI_NAME;
import static org.jboss.as.mail.extension.MailSubsystemModel.OUTBOUND_SOCKET_BINDING_REF;
import static org.jboss.as.mail.extension.MailSubsystemModel.POP3;
import static org.jboss.as.mail.extension.MailSubsystemModel.SERVER_TYPE;
import static org.jboss.as.mail.extension.MailSubsystemModel.SMTP;
import static org.jboss.as.mail.extension.MailSubsystemModel.USER_NAME;

/**
 * @author Tomaz Cerar
 * @created 27.7.11 0:55
 */
public class MailSessionAdd extends AbstractAddStepHandler {

    static final MailSessionAdd INSTANCE = new MailSessionAdd();
    public static final ServiceName SERVICE_NAME_BASE = ServiceName.JBOSS.append("mail-session");

    protected MailSessionAdd() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        MailSessionDefinition.JNDI_NAME.validateAndSet(operation, model);
        MailSessionDefinition.DEBUG.validateAndSet(operation, model);
        MailSessionDefinition.FROM.validateAndSet(operation, model);
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
     * @param verificationHandler step handler that can be added as a listener to any new services installed in order to
     *                            validate the services installed correctly during the
     *                            {@link org.jboss.as.controller.OperationContext.Stage#VERIFY VERIFY stage}
     * @param controllers         holder for the {@link org.jboss.msc.service.ServiceController} for any new services installed by the method. The
     *                            method should add the {@code ServiceController} for any new services to this list. If the
     *                            overall operation needs to be rolled back, the list will be used in
     *                            {@link #rollbackRuntime(org.jboss.as.controller.OperationContext, org.jboss.dmr.ModelNode, org.jboss.dmr.ModelNode, java.util.List)}  to automatically removed
     *                            the newly added services
     * @throws org.jboss.as.controller.OperationFailedException
     *          if {@code operation} is invalid or updating the runtime otherwise fails
     */
    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> controllers) throws OperationFailedException {
        final String jndiName = getJndiName(operation);
        final ServiceTarget serviceTarget = context.getServiceTarget();

        ModelNode fullTree = Resource.Tools.readModel(context.readResource(PathAddress.EMPTY_ADDRESS));
        final MailSessionConfig config = from(context, fullTree);
        final MailSessionService service = new MailSessionService(config);
        final ServiceName serviceName = SERVICE_NAME_BASE.append(jndiName);
        final ServiceBuilder<?> mailSessionBuilder = serviceTarget.addService(serviceName, service);
        addOutboundSocketDependency(service, mailSessionBuilder, config.getImapServer());
        addOutboundSocketDependency(service, mailSessionBuilder, config.getPop3Server());
        addOutboundSocketDependency(service, mailSessionBuilder, config.getSmtpServer());

        final ManagedReferenceFactory valueManagedReferenceFactory = new ManagedReferenceFactory() {

            @Override
            public ManagedReference getReference() {
                return new ValueManagedReference(new ImmediateValue<Object>(service.getValue()));
            }
        };
        final ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(jndiName);
        final BinderService binderService = new BinderService(bindInfo.getBindName());
        final ServiceBuilder<?> binderBuilder = serviceTarget
                .addService(bindInfo.getBinderServiceName(), binderService)
                .addInjection(binderService.getManagedObjectInjector(), valueManagedReferenceFactory)
                .addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, binderService.getNamingStoreInjector()).addListener(new AbstractServiceListener<Object>() {
                    public void transition(final ServiceController<? extends Object> controller, final ServiceController.Transition transition) {
                        switch (transition) {
                            case STARTING_to_UP: {
                                MailLogger.ROOT_LOGGER.boundMailSession(jndiName);
                                break;
                            }
                            case START_REQUESTED_to_DOWN: {
                                MailLogger.ROOT_LOGGER.unboundMailSession(jndiName);
                                break;
                            }
                            case REMOVING_to_REMOVED: {
                                MailLogger.ROOT_LOGGER.removedMailSession(jndiName);
                                break;
                            }
                        }
                    }
                });

        mailSessionBuilder.setInitialMode(ServiceController.Mode.ACTIVE)
                .addListener(verificationHandler);
        binderBuilder.setInitialMode(ServiceController.Mode.ACTIVE)
                .addListener(verificationHandler);
        controllers.add(mailSessionBuilder.install());
        controllers.add(binderBuilder.install());
    }

    /**
     * Extracts the raw JNDI_NAME value from the given model node, and depending on the value and
     * the value of any USE_JAVA_CONTEXT child node, converts the raw name into a compliant jndi name.
     *
     * @param modelNode the model node; either an operation or the model behind a mail session resource
     * @return the compliant jndi name
     */
    public static String getJndiName(final ModelNode modelNode) {
        final String rawJndiName = modelNode.require(JNDI_NAME).asString();
        final String jndiName;
        if (!rawJndiName.startsWith("java:")) {
            jndiName = "java:jboss/mail/" + rawJndiName;
        } else {
            jndiName = rawJndiName;
        }
        return jndiName;
    }


    private void addOutboundSocketDependency(MailSessionService service, ServiceBuilder<?> mailSessionBuilder, MailSessionServer server) {
        if (server != null) {
            final String ref = server.getOutgoingSocketBinding();
            mailSessionBuilder.addDependency(OutboundSocketBinding.OUTBOUND_SOCKET_BINDING_BASE_SERVICE_NAME.append(ref),
                    OutboundSocketBinding.class, service.getSocketBindingInjector(ref));
        }
    }

    static MailSessionConfig from(final OperationContext operationContext, final ModelNode model) throws OperationFailedException {
        MailSessionConfig cfg = new MailSessionConfig();

        cfg.setJndiName(MailSessionDefinition.JNDI_NAME.resolveModelAttribute(operationContext, model).asString());
        cfg.setDebug(MailSessionDefinition.DEBUG.resolveModelAttribute(operationContext, model).asBoolean());
        if (MailSessionDefinition.FROM.resolveModelAttribute(operationContext, model).isDefined()){
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
        return cfg;
    }

    private static MailSessionServer readServerConfig(final OperationContext operationContext, final ModelNode model) throws OperationFailedException {
        final String socket = MailServerDefinition.OUTBOUND_SOCKET_BINDING_REF.resolveModelAttribute(operationContext,model).asString();
        final Credentials credentials = readCredentials(operationContext, model);
        boolean ssl = MailServerDefinition.SSL.resolveModelAttribute(operationContext, model).asBoolean();
        return new MailSessionServer(socket, credentials, ssl);
    }

    private static Credentials readCredentials(final OperationContext operationContext, final ModelNode model) throws OperationFailedException {
        if (model.get(USER_NAME).isDefined()) {
            String un = MailServerDefinition.USERNAME.resolveModelAttribute(operationContext, model).asString();
            String pw = MailServerDefinition.PASSWORD.resolveModelAttribute(operationContext, model).asString();
            return new Credentials(un, pw);
        }
        return null;
    }
}
