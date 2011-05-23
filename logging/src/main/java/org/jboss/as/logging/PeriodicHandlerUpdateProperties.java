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
import static org.jboss.as.logging.CommonAttributes.SUFFIX;
import org.jboss.dmr.ModelNode;
import org.jboss.logmanager.handlers.PeriodicRotatingFileHandler;

/**
 * Operation responsible for updating the properties of a periodic rotating log handler.
 *
 * @author John Bailey
 */
public class PeriodicHandlerUpdateProperties extends FlushingHandlerUpdateProperties {
    static final PeriodicHandlerUpdateProperties INSTANCE = new PeriodicHandlerUpdateProperties();

    protected void updateModel(final ModelNode operation, final ModelNode model) {
        super.updateModel(operation, model);

        if (operation.hasDefined(SUFFIX)) {
            apply(operation, model, SUFFIX);
        }
    }

    protected void updateRuntime(final ModelNode operation, final Handler handler) {
        super.updateRuntime(operation, handler);
        if (operation.hasDefined(SUFFIX)) {
            PeriodicRotatingFileHandler.class.cast(handler).setSuffix(operation.get(SUFFIX).asString());
        }
    }
}
