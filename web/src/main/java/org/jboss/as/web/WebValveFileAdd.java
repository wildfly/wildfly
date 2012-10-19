package org.jboss.as.web;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;

import static org.jboss.as.web.WebValveFileDefinition.PATH;
import static org.jboss.as.web.WebValveFileDefinition.RELATIVE_TO;

/**
 * @author Jean-Frederic Clere
 */
public class WebValveFileAdd extends AbstractAddStepHandler {

    static final WebValveFileAdd INSTANCE = new WebValveFileAdd();

    private WebValveFileAdd() {
        //
    }


    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        PATH.validateAndSet(operation,model);
        RELATIVE_TO.validateAndSet(operation, model);
    }
}
