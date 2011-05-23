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

import java.util.logging.Handler;
import static org.jboss.as.logging.CommonAttributes.TARGET;
import org.jboss.dmr.ModelNode;
import org.jboss.logmanager.handlers.ConsoleHandler;

/**
 * Operation responsible for updating the properties of a console logging handler.
 *
 * @author John Bailey
 */
public class ConsoleHandlerUpdateProperties extends FlushingHandlerUpdateProperties {
    static final ConsoleHandlerUpdateProperties INSTANCE = new ConsoleHandlerUpdateProperties();

    protected void updateModel(final ModelNode operation, ModelNode model) {
        super.updateModel(operation, model);
        if (operation.hasDefined(TARGET)) {
            apply(operation, model, TARGET);
        }
    }

    protected void updateRuntime(final ModelNode operation, final Handler handler) {
        super.updateRuntime(operation, handler);
        if (operation.hasDefined(TARGET)) {
            switch (Target.fromString(operation.get(TARGET).asString())) {
                case SYSTEM_ERR: {
                    ConsoleHandler.class.cast(handler).setTarget(ConsoleHandler.Target.SYSTEM_ERR);
                    break;
                }
                case SYSTEM_OUT: {
                    ConsoleHandler.class.cast(handler).setTarget(ConsoleHandler.Target.SYSTEM_OUT);
                    break;
                }
            }
        }
    }
}
