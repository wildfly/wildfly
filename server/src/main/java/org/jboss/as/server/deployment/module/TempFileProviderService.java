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

package org.jboss.as.server.deployment.module;

import org.jboss.as.server.ServerMessages;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.vfs.TempFileProvider;
import org.jboss.vfs.VFSUtils;

import java.io.IOException;
import java.util.concurrent.Executors;

/**
 * Service responsible for managing the life-cycle of a TempFileProvider.
 *
 * @author John E. Bailey
 */
public class TempFileProviderService implements Service<TempFileProvider> {
    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("TempFileProvider");

    private static final TempFileProvider PROVIDER;
    static {
       try {
          PROVIDER = TempFileProvider.create("deployment", Executors.newScheduledThreadPool(2));
       }
       catch (final IOException ioe) {
          throw ServerMessages.MESSAGES.failedToCreateTempFileProvider(ioe);
       }
    }

    /**
     * {@inheritDoc}
     */
    public void start(StartContext context) throws StartException {
    }

    /**
     * {@inheritDoc}
     */
    public void stop(StopContext context) {
        VFSUtils.safeClose(PROVIDER);
    }

    /**
     * {@inheritDoc}
     */
    public TempFileProvider getValue() throws IllegalStateException {
        return provider();
    }

    public static TempFileProvider provider() {
        return PROVIDER;
    }
}
