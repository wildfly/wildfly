package org.jboss.as.mail.extension;

import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.ValueManagedReference;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.as.server.services.net.OutboundSocketBindingService;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.ImmediateValue;

/**
 * @author Tomaz Cerar
 * @created 27.7.11 0:55
 */
public class MailSessionAdd extends AbstractAddStepHandler {

    static final MailSessionAdd INSTANCE = new MailSessionAdd();
    public static final ServiceName SERVICE_NAME_BASE = ServiceName.JBOSS.append("mail-session");

    private MailSessionAdd() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void populateModel(ModelNode existingModel, ModelNode newModel) throws OperationFailedException {
        copyModel(existingModel, newModel, ModelKeys.JNDI_NAME, ModelKeys.DEBUG);
        if (existingModel.hasDefined(ModelKeys.SMTP_SERVER)) {
            newModel.get(ModelKeys.SMTP_SERVER).set(existingModel.get(ModelKeys.SMTP_SERVER));
        }
        if (existingModel.hasDefined(ModelKeys.POP3_SERVER)) {
            newModel.get(ModelKeys.POP3_SERVER).set(existingModel.get(ModelKeys.POP3_SERVER));
        }
        if (existingModel.hasDefined(ModelKeys.IMAP_SERVER)) {
            newModel.get(ModelKeys.IMAP_SERVER).set(existingModel.get(ModelKeys.IMAP_SERVER));
        }

    }

    static void copyModel(ModelNode src, ModelNode target, String... params) {
        for (String p : params) {
            target.get(p).set(src.get(p).asString());
        }
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
        final String jndiName = Util.getJndiName(operation);
        final ServiceTarget serviceTarget = context.getServiceTarget();

        final MailSessionConfig config = Util.from(context, operation);
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
        final ContextNames.BindInfo bindInfo =  ContextNames.bindInfoFor(jndiName);
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

    private void addOutboundSocketDependency(MailSessionService service, ServiceBuilder<?> mailSessionBuilder, MailSessionServer server) {
        if (server != null) {
            final String ref = server.getOutgoingSocketBinding();
            mailSessionBuilder.addDependency(OutboundSocketBinding.OUTBOUND_SOCKET_BINDING_BASE_SERVICE_NAME.append(ref),
                    OutboundSocketBinding.class, service.getSocketBindingInjector(ref));
        }
    }
}
