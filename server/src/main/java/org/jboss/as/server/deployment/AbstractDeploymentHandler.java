/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.server.deployment;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ARCHIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.BYTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HASH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INPUT_STREAM_INDEX;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELATIVE_TO;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.URL;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.server.ServerMessages;
import org.jboss.dmr.ModelNode;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
abstract class AbstractDeploymentHandler {

    protected static final List<String> CONTENT_ADDITION_PARAMETERS = Arrays.asList(INPUT_STREAM_INDEX, BYTES, URL);

    protected static String asString(final ModelNode node, final String name) {
        return node.has(name) ? node.require(name).asString() : null;
    }

    protected static OperationFailedException createFailureException(String msg) {
        return new OperationFailedException(new ModelNode(msg));
    }

    protected static OperationFailedException createFailureException(Throwable cause, String msg) {
        return new OperationFailedException(cause, new ModelNode(msg));
    }

    protected static DeploymentHandlerUtil.ContentItem[] getContents(ModelNode contentNode) {
        final List<ModelNode> nodes = contentNode.asList();
        final DeploymentHandlerUtil.ContentItem[] contents = new DeploymentHandlerUtil.ContentItem[nodes.size()];
        for(int i = 0; i < contents.length; i++) {
            final ModelNode node = nodes.get(i);
            if (node.has(HASH)) {
                contents[i] = new DeploymentHandlerUtil.ContentItem(node.require(HASH).asBytes());
            } else {
                contents[i] = new DeploymentHandlerUtil.ContentItem(node.require(PATH).asString(), asString(node, RELATIVE_TO), node.require(ARCHIVE).asBoolean());
            }
        }
        return contents;
    }

    protected static InputStream getInputStream(OperationContext context, ModelNode operation) throws OperationFailedException {
        InputStream in = null;
        if (operation.hasDefined(INPUT_STREAM_INDEX)) {
            int streamIndex = operation.get(INPUT_STREAM_INDEX).asInt();
            int maxIndex = context.getAttachmentStreamCount();
            if (streamIndex > maxIndex) {
                throw ServerMessages.MESSAGES.invalidStreamIndex(INPUT_STREAM_INDEX, streamIndex, maxIndex);
            }
            in = context.getAttachmentStream(streamIndex);
        } else if (operation.hasDefined(BYTES)) {
            try {
                in = new ByteArrayInputStream(operation.get(BYTES).asBytes());
            } catch (IllegalArgumentException iae) {
                throw ServerMessages.MESSAGES.invalidStreamBytes(BYTES);
            }
        } else if (operation.hasDefined(URL)) {
            final String urlSpec = operation.get(URL).asString();
            try {
                in = new URL(urlSpec).openStream();
            } catch (MalformedURLException e) {
                throw ServerMessages.MESSAGES.invalidStreamURL(e, urlSpec);
            } catch (IOException e) {
                throw ServerMessages.MESSAGES.invalidStreamURL(e, urlSpec);
            }
        }
        if (in == null) {
            // Won't happen, as we call hasValidContentAdditionParameterDefined first
            throw new IllegalStateException();
        }
        return in;
    }

    /**
     * Checks to see if a valid deployment parameter has been defined.
     *
     * @param operation the operation to check.
     *
     * @return {@code true} of the parameter is valid, otherwise {@code false}.
     */
    protected static boolean hasValidContentAdditionParameterDefined(ModelNode operation) {
        for (String s : CONTENT_ADDITION_PARAMETERS) {
            if (operation.hasDefined(s)) {
                return true;
            }
        }
        return false;
    }

    protected static void validateOnePieceOfContent(final ModelNode content) throws OperationFailedException {
        // TODO: implement overlays
        if (content.asList().size() != 1)
            throw ServerMessages.MESSAGES.multipleContentItemsNotSupported();
    }
}
