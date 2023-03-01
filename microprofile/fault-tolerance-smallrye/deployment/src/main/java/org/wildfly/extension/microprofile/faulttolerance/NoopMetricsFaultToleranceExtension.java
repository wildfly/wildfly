/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.microprofile.faulttolerance;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Vetoed;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;

import io.smallrye.faulttolerance.metrics.MicroProfileMetricsProvider;
import io.smallrye.faulttolerance.metrics.NoopProvider;

/**
 * A CDI extension that adds the {@link NoopProvider} and vetoes the {@link MicroProfileMetricsProvider}
 * that {@link io.smallrye.faulttolerance.FaultToleranceExtension} always registers.
 *
 * @author Radoslav Husar
 */
public class NoopMetricsFaultToleranceExtension implements Extension {

    void registerNoopProviderAnnotatedType(@Observes BeforeBeanDiscovery bbd, BeanManager bm) {
        bbd.addAnnotatedType(bm.createAnnotatedType(NoopProvider.class), NoopProvider.class.getName());
    }

    void vetoMicroProfileMetricsProvider(@Observes ProcessAnnotatedType<MicroProfileMetricsProvider> event) {
        event.configureAnnotatedType().add(Vetoed.Literal.INSTANCE);
        event.configureAnnotatedType().add(Alternative.Literal.INSTANCE);
    }

}
