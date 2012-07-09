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

package org.jboss.as.logging.handlers.console;

import static org.jboss.as.logging.CommonAttributes.AUTOFLUSH;
import static org.jboss.as.logging.CommonAttributes.TARGET;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.logging.handlers.AbstractLogHandlerWriteAttributeHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.logmanager.handlers.ConsoleHandler;

/**
 * Date: 12.10.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class ConsoleHandlerWriteAttributeHandler extends AbstractLogHandlerWriteAttributeHandler<ConsoleHandlerService> {
    public static final ConsoleHandlerWriteAttributeHandler INSTANCE = new ConsoleHandlerWriteAttributeHandler();

    private ConsoleHandlerWriteAttributeHandler() {
        super(AUTOFLUSH, TARGET);
    }

    @Override
    protected boolean doApplyUpdateToRuntime(final OperationContext context, final ModelNode operation, final String attributeName, final ModelNode resolvedValue, final ModelNode currentValue, final String handlerName, final ConsoleHandlerService handlerService) throws OperationFailedException {
        if (TARGET.getName().equals(attributeName)) {
            handlerService.setTarget(Target.fromString(resolvedValue.asString()));
        } else if (AUTOFLUSH.getName().equals(attributeName)) {
            handlerService.setAutoFlush(resolvedValue.asBoolean());
        }
        return false;
    }

    @Override
    protected void doRevertUpdateToRuntime(final OperationContext context, final ModelNode operation, final String attributeName, final ModelNode valueToRestore, final ModelNode valueToRevert, final String handlerName, final ConsoleHandlerService handlerService) throws OperationFailedException {
        if (TARGET.getName().equals(attributeName)) {
            handlerService.setTarget(Target.fromString(valueToRestore.asString()));
        } else if (AUTOFLUSH.getName().equals(attributeName)) {
            handlerService.setAutoFlush(valueToRestore.asBoolean());
        }
    }
}
