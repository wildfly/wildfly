package org.wildfly.extension.undertow.handlers;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;

import io.undertow.UndertowLogger;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.wildfly.extension.undertow.Constants;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
public class FileHandler extends Handler {
    /*<file path="/opt/data" cache-buffer-size="1024" cache-buffers="1024"/>*/
    private static final AttributeDefinition PATH = new SimpleAttributeDefinitionBuilder(Constants.PATH, ModelType.STRING)
            .setAllowNull(true)
            .setAllowExpression(true)
            .build();
    private static final AttributeDefinition CACHE_BUFFER_SIZE = new SimpleAttributeDefinitionBuilder("cache-buffer-size", ModelType.LONG)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(1024))
            .build();
    private static final AttributeDefinition CACHE_BUFFERS = new SimpleAttributeDefinitionBuilder("cache-buffers", ModelType.LONG)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(1024))
            .build();
    private static final AttributeDefinition DIRECTORY_LISTING = new SimpleAttributeDefinitionBuilder(Constants.DIRECTORY_LISTING, ModelType.BOOLEAN)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(false))
            .build();

    FileHandler() {
        super("file");
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(PATH, CACHE_BUFFER_SIZE, CACHE_BUFFERS, DIRECTORY_LISTING);
    }

    @Override
    public HttpHandler createHandler(final OperationContext context, ModelNode model) throws OperationFailedException {
        String path = PATH.resolveModelAttribute(context, model).asString();
        boolean directoryListing = DIRECTORY_LISTING.resolveModelAttribute(context, model).asBoolean();
        UndertowLogger.ROOT_LOGGER.infof("Creating file handler for path %s", path);
        FileResourceManager resourceManager = new FileResourceManager(Paths.get(path));
        ResourceHandler handler = new ResourceHandler();
        handler.setResourceManager(resourceManager);
        handler.setDirectoryListingEnabled(directoryListing);
        return handler;
    }
}
