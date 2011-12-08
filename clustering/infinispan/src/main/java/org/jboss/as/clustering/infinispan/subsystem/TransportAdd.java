package org.jboss.as.clustering.infinispan.subsystem;

import java.util.List;
import java.util.Locale;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceController;

/**
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class TransportAdd extends AbstractAddStepHandler implements DescriptionProvider {

    private static final Logger log = Logger.getLogger(TransportAdd.class.getPackage().getName());
    public static final TransportAdd INSTANCE = new TransportAdd();

    public TransportAdd() {
        super();
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        //
        log.info("populating model") ;
        log.infof("Operation = %s", operation.toString());
        log.infof("Model = %s", model.toString());
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        //
        log.info("performing runtime") ;
    }

    public ModelNode getModelDescription(Locale locale) {
        return InfinispanDescriptions.getTransportAddDescription(locale) ;
    }
}
