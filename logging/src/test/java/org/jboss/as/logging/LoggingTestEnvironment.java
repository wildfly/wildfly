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

import java.io.File;

import org.jboss.as.controller.RunningMode;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.ControllerInitializer;

/**
 * Date: 06.01.2012
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class LoggingTestEnvironment extends AdditionalInitialization {
    private static final LoggingTestEnvironment INSTANCE;
    private static final LoggingTestEnvironment MANAGEMENT_INSTANCE;

    static {
        final File configDir = new File(System.getProperty("jboss.server.config.dir", "target/config"));
        final File logDir = new File(System.getProperty("jboss.server.log.dir", "target/logs"));
        logDir.mkdirs();
        configDir.mkdirs();
        INSTANCE = new LoggingTestEnvironment(logDir, configDir, RunningMode.NORMAL);
        MANAGEMENT_INSTANCE = new LoggingTestEnvironment(logDir, configDir, RunningMode.ADMIN_ONLY);
    }

    private final File logDir;
    private final File configDir;
    private final RunningMode runningMode;

    private LoggingTestEnvironment(final File logDir, final File configDir, final RunningMode runningMode) {
        this.logDir = logDir;
        this.configDir = configDir;
        this.runningMode = runningMode;
    }

    public static LoggingTestEnvironment get() {
        return INSTANCE;
    }

    public static LoggingTestEnvironment getManagementInstance() {
        return MANAGEMENT_INSTANCE;
    }

    public File getLogDir() {
        return logDir;
    }

    public File getConfigDir() {
        return configDir;
    }
    @Override
    protected RunningMode getRunningMode() {
        return runningMode;
    }

    @Override
    protected ControllerInitializer createControllerInitializer() {
        return new ControllerInitializer() {
            {
                addPath("jboss.server.log.dir", logDir.getAbsolutePath(), null);
                addPath("jboss.server.config.dir", configDir.getAbsolutePath(), null);
            }
        };
    }
}