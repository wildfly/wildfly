package org.jboss.as.undertow.handlers;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import io.undertow.server.HttpHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.undertow.AbstractHandlerResourceDefinition;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
public class FileErrorPageHandler extends AbstractHandlerResourceDefinition {

    private static final AttributeDefinition CODE = new SimpleAttributeDefinitionBuilder("code", ModelType.INT)
            .setAllowExpression(true)
            .setAllowNull(true)
            .build();
    private static final AttributeDefinition FILE = new SimpleAttributeDefinitionBuilder("file", ModelType.STRING)
            .setAllowExpression(true)
            .setAllowNull(true)
            .build();
    /*<file-error-page code="404" file="/my/error/page.html"/>*/

    public FileErrorPageHandler() {
        super("file-error-page");
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(CODE, FILE);
    }

    @Override
    public HttpHandler createHandler(HttpHandler next, OperationContext context, ModelNode model) throws OperationFailedException {
        return new io.undertow.server.handlers.error.FileErrorPageHandler(new File("500.html"), 500);
    }
}
