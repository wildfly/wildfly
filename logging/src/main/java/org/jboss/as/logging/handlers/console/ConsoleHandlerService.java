/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

import org.jboss.as.logging.handlers.FlushingHandlerService;
import org.jboss.logmanager.handlers.ConsoleHandler;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ConsoleHandlerService extends FlushingHandlerService<ConsoleHandler> {

    private Target target;

    @Override
    protected ConsoleHandler createHandler() {
        return new ConsoleHandler();
    }

    @Override
    protected void start(final StartContext context, final ConsoleHandler handler) throws StartException {
        handler.setAutoFlush(isAutoflush());
        setTarget(handler, target);
    }

    private void setTarget(final ConsoleHandler handler, final Target target) {
        if (handler == null || target == null) return;
        switch (target) {
            case SYSTEM_ERR: {
                handler.setTarget(ConsoleHandler.Target.SYSTEM_ERR);
                break;
            }
            case SYSTEM_OUT: {
                handler.setTarget(ConsoleHandler.Target.SYSTEM_OUT);
                break;
            }
        }
    }

    public synchronized Target getTarget() {
        return target;
    }

    public synchronized void setTarget(final Target target) {
        this.target = target;
        final ConsoleHandler handler = getValue();
        setTarget(handler, target);
    }
}
