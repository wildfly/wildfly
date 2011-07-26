package org.jboss.as.mail.extension;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.NamingStore;
import org.jboss.as.naming.service.BinderService;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

import javax.mail.Session;
import java.util.List;

/**
 * @author Tomaz Cerar
 * @created 27.7.11 0:55
 */
public class MailSessionAdd extends AbstractAddStepHandler {

    static final MailSessionAdd INSTANCE = new MailSessionAdd();
    public static final ServiceName SERVICE_NAME_BASE = ServiceName.JBOSS.append("mail-session");

    private final Logger log = Logger.getLogger(MailSubsystemAdd.class);

    private MailSessionAdd() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        log.info("Populating the model");
        model.setEmptyObject();
    }

    /**
     * Make any runtime changes necessary to effect the changes indicated by the given {@code operation}. Executes
     * after {@link #populateModel(org.jboss.dmr.ModelNode, org.jboss.dmr.ModelNode)}, so the given {@code model}
     * parameter will reflect any changes made in that method.
     * <p>
     * This default implementation does nothing.
     * </p>
     *
     * @param context             the operation context
     * @param operation           the operation being executed
     * @param model               persistent configuration model node that corresponds to the address of {@code operation}
     * @param verificationHandler step handler that can be added as a listener to any new services installed in order to
     *                            validate the services installed correctly during the
     *                            {@link org.jboss.as.controller.OperationContext.Stage#VERIFY VERIFY stage}
     * @param controllers      holder for the {@link org.jboss.msc.service.ServiceController} for any new services installed by the method. The
     *                            method should add the {@code ServiceController} for any new services to this list. If the
     *                            overall operation needs to be rolled back, the list will be used in
     *                            {@link #rollbackRuntime(org.jboss.as.controller.OperationContext, org.jboss.dmr.ModelNode, org.jboss.dmr.ModelNode, java.util.List)}  to automatically removed
     *                            the newly added services
     * @throws org.jboss.as.controller.OperationFailedException
     *          if {@code operation} is invalid or updating the runtime otherwise fails
     */
    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> controllers) throws OperationFailedException {
        //super.performRuntime(context, operation, model, verificationHandler, newControllers);
        log.info("starting mail runtime");
        log.info("operation: " + operation);

        final String jndiName = Util.getJndiName(operation);

        final ServiceTarget serviceTarget = context.getServiceTarget();


        MailSessionService service = createDataSourceService(operation);
        final ServiceName serviceName = SERVICE_NAME_BASE.append(jndiName);
        final ServiceBuilder<?> mailSessionBuilder = serviceTarget
                .addService(serviceName, service);


        //controllers.add(startConfigAndAddDependency(dataSourceServiceBuilder, service, jndiName, serviceTarget, operation));




        final MailSessionReferenceFactoryService referenceFactoryService = new MailSessionReferenceFactoryService();
        final ServiceName referenceFactoryServiceName = MailSessionReferenceFactoryService.SERVICE_NAME_BASE.append(jndiName);
        final ServiceBuilder<?> referenceBuilder = serviceTarget.addService(referenceFactoryServiceName,
                referenceFactoryService).addDependency(serviceName, Session.class,
                referenceFactoryService.getDataSourceInjector());

        final ServiceName binderServiceName = Util.getBinderServiceName(jndiName);
        final BinderService binderService = new BinderService(binderServiceName.getSimpleName());
        final ServiceBuilder<?> binderBuilder = serviceTarget
                .addService(binderServiceName, binderService)
                .addDependency(referenceFactoryServiceName, ManagedReferenceFactory.class, binderService.getManagedObjectInjector())
                .addDependency(binderServiceName.getParent(), NamingStore.class, binderService.getNamingStoreInjector()).addListener(new AbstractServiceListener<Object>() {
                    public void transition(final ServiceController<? extends Object> controller, final ServiceController.Transition transition) {
                        switch (transition) {
                            case STARTING_to_UP: {
                                log.infof("Bound mail session [%s]", jndiName);
                                break;
                            }
                            case START_REQUESTED_to_DOWN: {
                                log.infof("Unbound mail session [%s]", jndiName);
                                break;
                            }
                            case REMOVING_to_REMOVED: {
                                log.debugf("Removed mail session [%s]", jndiName);
                                break;
                            }
                        }
                    }
                });

        mailSessionBuilder.setInitialMode(ServiceController.Mode.ACTIVE)
                .addListener(verificationHandler);
        referenceBuilder.setInitialMode(ServiceController.Mode.ACTIVE)
                .addListener(verificationHandler);
        binderBuilder.setInitialMode(ServiceController.Mode.ACTIVE)
                .addListener(verificationHandler);
        controllers.add(mailSessionBuilder.install());
        controllers.add(referenceBuilder.install());
        controllers.add(binderBuilder.install());


    }

    protected MailSessionService createDataSourceService(final ModelNode operation) throws OperationFailedException {

        final MailSessionConfig config = Util.from(operation);
        MailSessionService service = new MailSessionService(config);
        return service;
    }
/*
      protected ServiceController<?> startConfigAndAddDependency(ServiceBuilder<?> dataSourceServiceBuilder,
            MailSessionService mailSessionService, String jndiName, ServiceTarget serviceTarget, final ModelNode operation)
            throws OperationFailedException {
        final MailSessionConfig config = Util.from(operation);
        final ServiceName dataSourceCongServiceName = DataSourceConfigService.SERVICE_NAME_BASE.append(jndiName);
        final DataSourceConfigService configService = new DataSourceConfigService(dataSourceConfig);

        ServiceController<?> svcController = serviceTarget.addService(config, configService).setInitialMode(ServiceController.Mode.ACTIVE).install();

        dataSourceServiceBuilder.addDependency(dataSourceCongServiceName, DataSource.class,
                ((LocalDataSourceService) mailSessionService).getDataSourceConfigInjector());

        return svcController;
    }*/
}
