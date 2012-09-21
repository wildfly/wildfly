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
package org.jboss.as.domain.controller.operations.deployment;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.BYTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INPUT_STREAM_INDEX;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.URL;

import java.util.Arrays;
import java.util.List;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
abstract class AbstractDeploymentHandler {

    protected static final List<String> CONTENT_ADDITION_PARAMETERS = Arrays.asList(INPUT_STREAM_INDEX, BYTES, URL);

    protected static String asString(final ModelNode node, final String name) {
        return node.has(name) ? node.require(name).asString() : null;
    }

    protected static OperationFailedException createFailureException(String format, Object... params) {
        return createFailureException(String.format(format, params));
    }

    protected static OperationFailedException createFailureException(Throwable cause, String format, Object... params) {
        return createFailureException(cause, String.format(format, params));
    }

    protected static OperationFailedException createFailureException(String msg) {
        return new OperationFailedException(new ModelNode().set(msg));
    }

    protected static OperationFailedException createFailureException(Throwable cause, String msg) {
        return new OperationFailedException(cause, new ModelNode().set(msg));
    }
}
