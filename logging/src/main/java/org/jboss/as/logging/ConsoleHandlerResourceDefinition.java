/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
import static org.jboss.as.logging.CommonAttributes.TARGET;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.logmanager.handlers.ConsoleHandler;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
class ConsoleHandlerResourceDefinition extends AbstractHandlerDefinition {

    static final AttributeDefinition[] ATTRIBUTES = appendDefaultWritableAttributes(AUTOFLUSH, TARGET);

    /**
     * Operation step handlers for {@link org.jboss.logmanager.handlers.ConsoleHandler}
     */
    static final HandlerOperations.HandlerAddOperationStepHandler ADD_CONSOLE_HANDLER = new HandlerOperations.HandlerAddOperationStepHandler(ConsoleHandler.class, ATTRIBUTES);

    static final ConsoleHandlerResourceDefinition INSTANCE = new ConsoleHandlerResourceDefinition();

    private ConsoleHandlerResourceDefinition() {
        super(LoggingExtension.CONSOLE_HANDLER_PATH,
                CommonAttributes.CONSOLE_HANDLER,
                ADD_CONSOLE_HANDLER,
                ATTRIBUTES);
    }

}
