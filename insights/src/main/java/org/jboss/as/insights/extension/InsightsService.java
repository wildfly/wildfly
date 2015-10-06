/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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
package org.jboss.as.insights.extension;


import static org.jboss.as.insights.logger.InsightsLogger.ROOT_LOGGER;

import java.util.concurrent.ScheduledExecutorService;
import org.jboss.as.insights.api.InsightsScheduler;
import org.jboss.as.jdr.JdrReportCollector;
import org.jboss.as.jdr.JdrReportService;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

import org.jboss.msc.value.InjectedValue;

/**
 * @author <a href="mailto:jkinlaw@redhat.com">Josh Kinlaw</a>
 */
public class InsightsService implements Service<InsightsScheduler> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append(InsightsExtension.SUBSYSTEM_NAME);

    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String URL = "url";
    public static final String PROXY_USER = "proxyUser";
    public static final String PROXY_PASSWORD = "proxyPassword";
    public static final String PROXY_URL = "proxyUrl";
    public static final String PROXY_PORT = "proxyPort";
    public static final String INSIGHTS_ENDPOINT = "insightsEndpoint";
    public static final String SYSTEM_ENDPOINT = "systemEndpoint";
    public static final String USER_AGENT = "userAgent";

    public static final String JBOSS_PROPERTY_DIR = "jboss.server.data.dir";

    public static final String INSIGHTS_PROPERTY_FILE_NAME = "insights.properties";

    public static final String INSIGHTS_DESCRIPTION = "Properties file consisting of RHN login information and insights/insights URL";

    public static final int DEFAULT_SCHEDULE_INTERVAL = 1;
    public static final String DEFAULT_BASE_URL = "https://api.access.redhat.com";
    public static final String DEFAULT_INSIGHTS_ENDPOINT = "/r/insights/v1/uploads/";
    public static final String DEFAULT_SYSTEM_ENDPOINT = "/r/insights/v1/systems/";
    public static final String DEFAULT_USER_AGENT = "redhat-support-lib-java";

    private InsightsJdrScheduler scheduler;
    private boolean enabled = true;
    private ScheduledExecutorService executor;

    private InjectedValue<JdrReportCollector> jdrCollector = new InjectedValue<>();

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void start(StartContext context) throws StartException {
        try {
            if (enabled) {
                scheduler.startScheduler();
            }
        } catch (IllegalStateException e) {
            throw new StartException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void stop(StopContext context) {
        final InsightsJdrScheduler scheduler = this.scheduler;
        this.scheduler = null;
        scheduler.stopScheduler();
        this.executor.shutdownNow();
    }

    private InsightsService(ScheduledExecutorService executor, boolean enabled, InsightsConfiguration config) {
        this.executor = executor;
        this.enabled = enabled;
        this.scheduler = new InsightsJdrScheduler(executor, config, jdrCollector);
    }

    public static  ServiceController<InsightsScheduler> addService(ServiceTarget serviceTarget, ScheduledExecutorService executor, boolean enabled,
            InsightsConfiguration config) {
        InsightsService service = new InsightsService(executor, enabled, config);
        return serviceTarget.addService(SERVICE_NAME, service)
                .addDependency(JdrReportService.SERVICE_NAME, JdrReportCollector.class, service.getJdrReportCollector())
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install();
    }

    @Override
    public InsightsScheduler getValue() throws IllegalStateException, IllegalArgumentException {
        return scheduler;
    }

    void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (enabled) {
            ROOT_LOGGER.insightsEnabled();
            try {
                start(null);
            } catch (StartException e) {
                ROOT_LOGGER.couldNotStartThread(e);
            }
        } else if (!enabled) {
            ROOT_LOGGER.insightsDisabled();
        }
    }

    InjectedValue<JdrReportCollector> getJdrReportCollector() {
        return this.jdrCollector;
    }
}