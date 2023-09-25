/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
