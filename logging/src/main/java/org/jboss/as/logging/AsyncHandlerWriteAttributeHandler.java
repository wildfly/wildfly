/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.logging;

import static org.jboss.as.logging.CommonAttributes.AUTOFLUSH;
import static org.jboss.as.logging.CommonAttributes.OVERFLOW_ACTION;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.logmanager.handlers.AsyncHandler;

import java.util.Locale;

/**
 * Date: 12.10.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class AsyncHandlerWriteAttributeHandler extends AbstractLogHandlerWriteAttributeHandler<AsyncHandler> {

    public static final AsyncHandlerWriteAttributeHandler INSTANCE = new AsyncHandlerWriteAttributeHandler();

    private AsyncHandlerWriteAttributeHandler() {
        super(OVERFLOW_ACTION, AUTOFLUSH);
    }

    @Override
    protected boolean doApplyUpdateToRuntime(final ModelNode operation, final String attributeName, final ModelNode resolvedValue, final ModelNode currentValue, final AsyncHandler handler) throws OperationFailedException {
        if (OVERFLOW_ACTION.getName().equals(attributeName)) {
            handler.setOverflowAction(AsyncHandler.OverflowAction.valueOf(resolvedValue.asString().toUpperCase(Locale.US)));
        } else if (AUTOFLUSH.getName().equals(attributeName)) {
            handler.setAutoFlush(resolvedValue.asBoolean());
        }
        return false;
    }

    @Override
    protected void doRevertUpdateToRuntime(final ModelNode operation, final String attributeName, final ModelNode valueToRestore, final ModelNode valueToRevert, final AsyncHandler handler) throws OperationFailedException {
        if (OVERFLOW_ACTION.getName().equals(attributeName)) {
            handler.setOverflowAction(AsyncHandler.OverflowAction.valueOf(valueToRestore.asString().toUpperCase(Locale.US)));
        } else if (AUTOFLUSH.getName().equals(attributeName)) {
            handler.setAutoFlush(valueToRestore.asBoolean());
        }
    }
}
