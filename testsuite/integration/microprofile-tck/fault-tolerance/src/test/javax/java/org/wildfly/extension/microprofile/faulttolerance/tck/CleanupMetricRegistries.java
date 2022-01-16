/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.microprofile.faulttolerance.tck;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Destroyed;
import javax.enterprise.event.Observes;

import io.smallrye.metrics.MetricRegistries;

/**
 * Adapted from SmallRye Fault Tolerance project.
 *
 * Note this is different from the upstream solution which uses {@link org.jboss.arquillian.core.spi.LoadableExtension}
 * to register an observer, however, the TCK in WildFly runs in container thus that cleans the registries in a test
 * execution JVM instead of the server.
 *
 * @author Radoslav Husar
 */
@ApplicationScoped
public class CleanupMetricRegistries {

    public void destroy(@Observes @Destroyed(ApplicationScoped.class) Object ignore) {

        // TODO
        //  In MP FT 2.1, metrics are added to the "application" scope, which is automatically dropped
        //  by SmallRye Metrics when application is undeployed. Since MP FT 3.0, metrics are added to the "base" scope,
        //  which persists across application undeployments (see https://github.com/smallrye/smallrye-metrics/issues/12).
        //  However, MP FT TCK expects that this isn't the case. Specifically, AllMetricsTest and MetricsDisabledTest
        //  both use the same bean, AllMetricsBean, so if AllMetricsTest runs first, some histograms are created,
        //  and then MetricsDisabledTest fails, because those histograms are not expected to exist. Here, we drop all
        //  metric registries before each test class, to work around that.

        MetricRegistries.dropAll();
    }

}
