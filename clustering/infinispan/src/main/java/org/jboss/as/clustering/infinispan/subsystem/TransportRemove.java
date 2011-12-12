package org.jboss.as.clustering.infinispan.subsystem;

import java.util.Locale;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;

/**
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class TransportRemove extends AbstractRemoveStepHandler implements DescriptionProvider {

    private static final Logger log = Logger.getLogger(TransportRemove.class.getPackage().getName());
    public static final TransportRemove INSTANCE = new TransportRemove();

    public TransportRemove() {
        super();
    }

/*
    @Override
    protected void performRemove(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        // need to remove the transport submodel

        // check for corresponding ModelNode in parent
        PathAddress transportAddress = PathAddress.pathAddress(operation.get(OP_ADDR)) ;
        String transportName = transportAddress.getLastElement().getValue() ;
        if (!transportName.equals(ModelKeys.TRANSPORT)) {
            ModelNode exception = new ModelNode() ;
            exception.get(FAILURE_DESCRIPTION).set("Add operation failed: sanity check failure on transport name.") ;
            throw new OperationFailedException(exception);
        }

        PathAddress containerAddress = transportAddress.subAddress(0, transportAddress.size()-1) ;
        ModelNode container = context.getRootResource().navigate(containerAddress).getModel();

        // check if the unique anonymous ModelNode does not exist
        if (!container.hasDefined(ModelKeys.TRANSPORT)) {
            ModelNode exception = new ModelNode() ;
            exception.get(FAILURE_DESCRIPTION).set("Add operation failed: singleton transport does not exist.") ;
            throw new OperationFailedException(exception);
        }

        // remove the transport child
        container.remove(ModelKeys.TRANSPORT) ;
    }
*/



    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        // need to set reload-required on the server
        log.debug("performing runtime") ;
        context.reloadRequired();
    }

    public ModelNode getModelDescription(Locale locale) {
        return InfinispanDescriptions.getTransportRemoveDescription(locale) ;
    }
}
