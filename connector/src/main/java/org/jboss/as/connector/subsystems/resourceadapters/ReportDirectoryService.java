/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.connector.subsystems.resourceadapters;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

import java.io.File;

public class ReportDirectoryService implements Service<File> {

    private File reportDirectory;

    public ReportDirectoryService(File reportDirectory) {
        this.reportDirectory = reportDirectory;
    }

    public void setReportDirectory(File reportDirectory) {
        this.reportDirectory = reportDirectory;
    }

    @Override
    public File getValue() throws IllegalStateException, IllegalArgumentException {
        return reportDirectory;
    }

    @Override
    public void start(StartContext startContext) throws StartException {
    }

    @Override
    public void stop(StopContext stopContext) {
    }
}
