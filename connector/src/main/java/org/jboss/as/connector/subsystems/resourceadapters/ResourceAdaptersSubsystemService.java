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

package org.jboss.as.connector.subsystems.resourceadapters;

import static org.jboss.as.connector.logging.ConnectorLogger.SUBSYSTEM_RA_LOGGER;

import org.jboss.as.connector.util.CopyOnWriteArrayListMultiMap;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

import java.io.File;

/**
 * A ResourceAdaptersSubsystem Service.
 *
 * @author <a href="mailto:stefano.maestri@redhat.comdhat.com">Stefano
 *         Maestri</a>
 */
public final class ResourceAdaptersSubsystemService implements Service<ResourceAdaptersSubsystemService> {


    public static final AttachmentKey<ResourceAdaptersSubsystemService> ATTACHMENT_KEY = AttachmentKey
            .create(ResourceAdaptersSubsystemService.class);

    private final CopyOnWriteArrayListMultiMap<String, ServiceName> adapters = new CopyOnWriteArrayListMultiMap<>();

    private File reportDirectory;

    public ResourceAdaptersSubsystemService(final File reportDirectory){
        this.reportDirectory = reportDirectory;
    }


    @Override
    public ResourceAdaptersSubsystemService getValue() throws IllegalStateException {
        return this;
    }


    @Override
    public void start(StartContext context) throws StartException {
        SUBSYSTEM_RA_LOGGER.debugf("Starting ResourceAdaptersSubsystem Service");
    }

    @Override
    public void stop(StopContext context) {
        SUBSYSTEM_RA_LOGGER.debugf("Stopping ResourceAdaptersSubsystem Service");
    }

    public CopyOnWriteArrayListMultiMap<String, ServiceName> getAdapters() {
        return adapters;
    }

    public File getReportDirectory() {
        return reportDirectory;
    }

    public void setReportDirectory(File reportDirectory) {
        this.reportDirectory = reportDirectory;
    }
}
