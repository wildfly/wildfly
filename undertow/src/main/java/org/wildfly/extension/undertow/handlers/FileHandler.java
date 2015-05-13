/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.extension.undertow.handlers;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.undertow.Constants;
import org.wildfly.extension.undertow.logging.UndertowLogger;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
public class FileHandler extends Handler {

    public static final FileHandler INSTANCE = new FileHandler();

    /*<file path="/opt/data" cache-buffer-size="1024" cache-buffers="1024"/>*/
    public static final AttributeDefinition PATH = new SimpleAttributeDefinitionBuilder(Constants.PATH, ModelType.STRING)
            .setAllowNull(true)
            .setAllowExpression(true)
            .build();
    public static final AttributeDefinition CACHE_BUFFER_SIZE = new SimpleAttributeDefinitionBuilder("cache-buffer-size", ModelType.LONG)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(1024))
            .build();
    public static final AttributeDefinition CACHE_BUFFERS = new SimpleAttributeDefinitionBuilder("cache-buffers", ModelType.LONG)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(1024))
            .build();
    public static final AttributeDefinition DIRECTORY_LISTING = new SimpleAttributeDefinitionBuilder(Constants.DIRECTORY_LISTING, ModelType.BOOLEAN)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(false))
            .build();

    private FileHandler() {
        super(Constants.FILE);
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(PATH, CACHE_BUFFER_SIZE, CACHE_BUFFERS, DIRECTORY_LISTING);
    }

    @Override
    public HttpHandler createHandler(final OperationContext context, ModelNode model) throws OperationFailedException {
        String path = PATH.resolveModelAttribute(context, model).asString();
        boolean directoryListing = DIRECTORY_LISTING.resolveModelAttribute(context, model).asBoolean();
        UndertowLogger.ROOT_LOGGER.creatingFileHandler(path);
        FileResourceManager resourceManager = new FileResourceManager(new File(path), 1024 * 1024, true, new String[0]);
        ResourceHandler handler = new ResourceHandler(resourceManager);
        handler.setDirectoryListingEnabled(directoryListing);
        return handler;
    }
}
