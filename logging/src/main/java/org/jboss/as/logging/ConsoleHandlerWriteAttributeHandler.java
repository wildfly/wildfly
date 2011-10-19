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

import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.logmanager.handlers.ConsoleHandler;

import java.util.Locale;

import static org.jboss.as.logging.CommonAttributes.AUTOFLUSH;
import static org.jboss.as.logging.CommonAttributes.TARGET;

/**
 * Date: 12.10.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class ConsoleHandlerWriteAttributeHandler extends LogHandlerWriteAttributeHandler<ConsoleHandler> {
    static final ConsoleHandlerWriteAttributeHandler INSTANCE = new ConsoleHandlerWriteAttributeHandler();

    private ConsoleHandlerWriteAttributeHandler() {
        super(TARGET);
    }

    @Override
    protected boolean applyUpdateToRuntime(final ModelNode operation, final String attributeName, final ModelNode resolvedValue, final ModelNode currentValue, final ConsoleHandler handler) throws OperationFailedException {
        if (TARGET.getName().equals(attributeName)) {
            switch (Target.fromString(TARGET.validateResolvedOperation(operation).asString().toUpperCase(Locale.US))) {
                case SYSTEM_ERR: {
                    handler.setTarget(ConsoleHandler.Target.SYSTEM_ERR);
                    ConsoleHandler.class.cast(handler).setTarget(ConsoleHandler.Target.SYSTEM_ERR);
                    break;
                }
                case SYSTEM_OUT: {
                    handler.setTarget(ConsoleHandler.Target.SYSTEM_OUT);
                    break;
                }
            }
        } else if (AUTOFLUSH.getName().equals(attributeName)) {
            handler.setAutoFlush(AUTOFLUSH.validateResolvedOperation(operation).asBoolean());
        }
        return false;
    }

    @Override
    protected void revertUpdateToRuntime(final ModelNode operation, final String attributeName, final ModelNode valueToRestore, final ModelNode valueToRevert, final ConsoleHandler handler) throws OperationFailedException {
        if (TARGET.getName().equals(attributeName)) {
            switch (Target.fromString(TARGET.validateResolvedOperation(valueToRestore).asString().toUpperCase(Locale.US))) {
                case SYSTEM_ERR: {
                    handler.setTarget(ConsoleHandler.Target.SYSTEM_ERR);
                    ConsoleHandler.class.cast(handler).setTarget(ConsoleHandler.Target.SYSTEM_ERR);
                    break;
                }
                case SYSTEM_OUT: {
                    handler.setTarget(ConsoleHandler.Target.SYSTEM_OUT);
                    break;
                }
            }
        } else if (AUTOFLUSH.getName().equals(attributeName)) {
            handler.setAutoFlush(AUTOFLUSH.validateResolvedOperation(valueToRestore).asBoolean());
        }
    }
}
