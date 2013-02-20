package org.jboss.as.undertow.handlers;

import java.nio.file.Paths;

import io.undertow.UndertowLogger;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.undertow.AbstractHandlerResourceDefinition;
import org.jboss.as.undertow.Constants;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
public class FileHandler extends AbstractHandlerResourceDefinition {
    /*<file path="/opt/data" cache-buffer-size="1024" cache-buffers="1024"/>*/
    private static SimpleAttributeDefinition PATH = new SimpleAttributeDefinitionBuilder(Constants.PATH, ModelType.STRING)
            .setAllowNull(true)
            .setAllowExpression(true)
            .build();
    private static SimpleAttributeDefinition CACHE_BUFFER_SIZE = new SimpleAttributeDefinitionBuilder("cache-buffer-size", ModelType.LONG)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(1024))
            .build();
    private static SimpleAttributeDefinition CACHE_BUFFERS = new SimpleAttributeDefinitionBuilder("cache-buffers", ModelType.LONG)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(1024))
            .build();

    public FileHandler() {
        super("file");
    }

    @Override
    public AttributeDefinition[] getAttributes() {
        return new AttributeDefinition[]{PATH, CACHE_BUFFER_SIZE, CACHE_BUFFERS};
    }

    @Override
    public HttpHandler createHandler(HttpHandler next, final OperationContext context, ModelNode model) throws OperationFailedException {
        String path = PATH.resolveModelAttribute(context, model).asString();
        UndertowLogger.ROOT_LOGGER.infof("Creating file handler for path %s", path);
        FileResourceManager resourceManager = new FileResourceManager(Paths.get(path));
        ResourceHandler handler = new ResourceHandler();
        handler.setResourceManager(resourceManager);
        handler.setDirectoryListingEnabled(true);
        return handler;
    }
}
