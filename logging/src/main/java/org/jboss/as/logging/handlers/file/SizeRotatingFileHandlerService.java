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

import org.jboss.logmanager.handlers.SizeRotatingFileHandler;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class SizeRotatingFileHandlerService extends AbstractFileHandlerService<SizeRotatingFileHandler> {

    private int maxBackupIndex;
    private long rotateSize;

    @Override
    protected SizeRotatingFileHandler createHandler() throws StartException {
        return new SizeRotatingFileHandler();
    }

    @Override
    protected void start(final StartContext context, final SizeRotatingFileHandler handler) throws StartException {
        super.start(context, handler);
        handler.setMaxBackupIndex(maxBackupIndex);
        handler.setRotateSize(rotateSize);
    }

    public synchronized int getMaxBackupIndex() {
        return maxBackupIndex;
    }

    public synchronized void setMaxBackupIndex(final int maxBackupIndex) {
        this.maxBackupIndex = maxBackupIndex;
        final SizeRotatingFileHandler handler = getValue();
        if (handler != null) handler.setMaxBackupIndex(maxBackupIndex);
    }

    public synchronized long getRotateSize() {
        return rotateSize;
    }

    public synchronized void setRotateSize(final long rotateSize) {
        this.rotateSize = rotateSize;
        final SizeRotatingFileHandler handler = getValue();
        if (handler != null) handler.setRotateSize(rotateSize);
    }
}
