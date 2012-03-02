package org.jboss.as.web;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;

/**
 * @author Tomaz Cerar
 * @created 24.2.12 15:49
 */
public class WebContainerAdd extends AbstractAddStepHandler {
    protected static final WebContainerAdd INSTANCE = new WebContainerAdd();
    private WebContainerAdd(){

    }


    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        WebContainerDefinition.WELCOME_FILES.validateAndSet(operation,model);
        WebContainerDefinition.MIME_MAPPINGS.validateAndSet(operation,model);
    }
}
