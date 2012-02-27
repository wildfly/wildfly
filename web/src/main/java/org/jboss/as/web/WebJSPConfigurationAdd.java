package org.jboss.as.web;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.dmr.ModelNode;

import static org.jboss.as.web.WebJSPDefinition.JSP_ATTRIBUTES;

/**
 * @author Tomaz Cerar
 * @created 24.2.12 16:52
 */
public class WebJSPConfigurationAdd extends AbstractAddStepHandler {
    protected static final WebJSPConfigurationAdd INSTANCE = new WebJSPConfigurationAdd();

    private WebJSPConfigurationAdd() {

    }


    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (SimpleAttributeDefinition def : JSP_ATTRIBUTES) {
            def.validateAndSet(operation, model);
        }
    }
}