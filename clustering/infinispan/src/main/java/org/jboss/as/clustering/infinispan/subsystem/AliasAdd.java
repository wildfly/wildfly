package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;

import java.util.List;
import java.util.Locale;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceController;

/**
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class AliasAdd extends AbstractAddStepHandler implements DescriptionProvider {

    private static final Logger log = Logger.getLogger(AliasAdd.class.getPackage().getName());
    public static final AliasAdd INSTANCE = new AliasAdd();

    static ModelNode createOperation(ModelNode address, ModelNode existing) {
        ModelNode operation = Util.getEmptyOperation(ADD, address);
        populate(existing, operation);
        return operation;
    }

    public AliasAdd() {
        super();
    }

    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        // copy operation data to the model
        populate(operation, model);
    }


    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        //
        context.reloadRequired();
    }

    public ModelNode getModelDescription(Locale locale) {
        return InfinispanDescriptions.getAliasAddDescription(locale) ;
    }

    private static void populate(ModelNode operation, ModelNode model) {
        // alias resources do not hold data
    }
}
