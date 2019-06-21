/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.microprofile.health.deployment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.Unmanaged;

import io.smallrye.health.SmallRyeHealthReporter;
import org.eclipse.microprofile.health.Health;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.Liveness;
import org.eclipse.microprofile.health.Readiness;
import org.wildfly.extension.microprofile.health.HealthReporter;
import org.wildfly.extension.microprofile.health._private.MicroProfileHealthLogger;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
public class CDIExtension implements Extension {

    private final HealthReporter reporter;

    private List<AnnotatedType<? extends HealthCheck>> healthDelegates = new ArrayList<>();
    private Collection<Unmanaged.UnmanagedInstance<HealthCheck>> healthInstances = new ArrayList<>();

    private List<AnnotatedType<? extends HealthCheck>> livenessDelegates = new ArrayList<>();
    private Collection<Unmanaged.UnmanagedInstance<HealthCheck>> livenessInstances = new ArrayList<>();

    private List<AnnotatedType<? extends HealthCheck>> readinessDelegates = new ArrayList<>();
    private Collection<Unmanaged.UnmanagedInstance<HealthCheck>> readinessInstances = new ArrayList<>();


    public CDIExtension(HealthReporter healthReporter) {
        this.reporter = healthReporter;
    }

    /**
     * Discover all classes that implements HealthCheckProcedure
     */
    public void observeResources(@Observes ProcessAnnotatedType<? extends HealthCheck> event) {
        Class<? extends HealthCheck> javaClass = event.getAnnotatedType().getJavaClass();
        if (event.getAnnotatedType().isAnnotationPresent(Health.class)) {
            MicroProfileHealthLogger.LOGGER.debugf("Discovered health check procedure %s", javaClass);
            healthDelegates.add(event.getAnnotatedType());
        } else if (event.getAnnotatedType().isAnnotationPresent(Liveness.class)) {
            MicroProfileHealthLogger.LOGGER.debugf("Discovered liveness check procedure %s", javaClass);
            livenessDelegates.add(event.getAnnotatedType());
        } else if (event.getAnnotatedType().isAnnotationPresent(Readiness.class)) {
            MicroProfileHealthLogger.LOGGER.debugf("Discovered liveness check procedure %s", javaClass);
            readinessDelegates.add(event.getAnnotatedType());
        }
    }


    /**
     * Instantiates <em>unmanaged instances</em> of HealthCheck and
     * handle manually their CDI creation lifecycle.
     * Add them to the {@link HealthReporter}.
     */
    private void afterDeploymentValidation(@Observes final AfterDeploymentValidation avd, BeanManager bm) {
        addHealthCheck(bm, healthDelegates, healthInstances, reporter::addHealthCheck);
        addHealthCheck(bm, readinessDelegates, readinessInstances, reporter::addReadinessCheck);
        addHealthCheck(bm, livenessDelegates, livenessInstances, reporter::addLivenessCheck);
    }

    private void addHealthCheck(BeanManager bm,
                                List<AnnotatedType<? extends HealthCheck>> delegates,
                                Collection<Unmanaged.UnmanagedInstance<HealthCheck>> instances,
                                Consumer<HealthCheck> healthFunction) {
        for (AnnotatedType delegate : delegates) {
            try {
                Unmanaged<HealthCheck> unmanagedHealthCheck = new Unmanaged<HealthCheck>(bm, delegate.getJavaClass());
                Unmanaged.UnmanagedInstance<HealthCheck> healthCheckInstance = unmanagedHealthCheck.newInstance();
                HealthCheck healthCheck = healthCheckInstance.produce().inject().postConstruct().get();
                instances.add(healthCheckInstance);
                healthFunction.accept(healthCheck);
            } catch (Exception e) {
                throw new RuntimeException("Failed to register health bean", e);
            }
        }
    }

    /**
     * Called when the deployment is undeployed.
     * <p>
     * Remove all the instances of {@link HealthCheck} from the {@link SmallRyeHealthReporter}.
     * Handle manually their CDI destroy lifecycle.
     */
    public void beforeShutdown(@Observes final BeforeShutdown bs) {
        removeHealthCheck(healthInstances, reporter::removeHealthCheck);
        removeHealthCheck(readinessInstances, reporter::removeReadinessCheck);
        removeHealthCheck(livenessInstances, reporter::removeLivenessCheck);
    }

    private void removeHealthCheck(Collection<Unmanaged.UnmanagedInstance<HealthCheck>> instances,
                                   Consumer<HealthCheck> healthFunction) {
        instances.forEach(healthCheck -> {
            healthFunction.accept(healthCheck.get());
            healthCheck.preDestroy().dispose();
        });
        instances.clear();
    }
}