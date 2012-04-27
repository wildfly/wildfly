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

package org.jboss.as.logging.handlers.file;

import org.jboss.logmanager.handlers.PeriodicRotatingFileHandler;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class PeriodicRotatingFileHandlerService extends AbstractFileHandlerService<PeriodicRotatingFileHandler> {
    private String suffix;

    @Override
    protected PeriodicRotatingFileHandler createHandler() throws StartException {
        return new PeriodicRotatingFileHandler();
    }

    @Override
    protected void start(final StartContext context, final PeriodicRotatingFileHandler handler) throws StartException {
        super.start(context, handler);
        handler.setSuffix(suffix);
    }

    public synchronized String getSuffix() {
        return suffix;
    }

    public synchronized void setSuffix(final String suffix) {
        this.suffix = suffix;
        final PeriodicRotatingFileHandler handler = getValue();
        if (handler != null) {
            handler.setSuffix(suffix);
        }
    }
}
