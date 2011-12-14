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
public class AliasRemove extends AbstractRemoveStepHandler implements DescriptionProvider {

    private static final Logger log = Logger.getLogger(AliasRemove.class.getPackage().getName());
    public static final AliasRemove INSTANCE = new AliasRemove();

    public AliasRemove() {
        super();
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        // need to set reload-required on the server
        context.reloadRequired();
    }

    public ModelNode getModelDescription(Locale locale) {
        return InfinispanDescriptions.getAliasRemoveDescription(locale) ;
    }
}
