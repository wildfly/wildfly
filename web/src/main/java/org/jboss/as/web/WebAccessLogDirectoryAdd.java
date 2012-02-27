package org.jboss.as.web;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;

import static org.jboss.as.web.WebAccessLogDirectoryDefinition.PATH;
import static org.jboss.as.web.WebAccessLogDirectoryDefinition.RELATIVE_TO;

/**
 * @author Tomaz Cerar
 * @created 24.2.12 19:53
 */
public class WebAccessLogDirectoryAdd extends AbstractAddStepHandler {

    static final WebAccessLogDirectoryAdd INSTANCE = new WebAccessLogDirectoryAdd();

    private WebAccessLogDirectoryAdd() {
        //
    }


    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        PATH.validateAndSet(operation,model);
        RELATIVE_TO.validateAndSet(operation, model);
    }
}
