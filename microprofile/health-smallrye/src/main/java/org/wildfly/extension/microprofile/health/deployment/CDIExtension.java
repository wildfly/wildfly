/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.health.deployment;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.AfterDeploymentValidation;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeShutdown;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;
import jakarta.enterprise.util.AnnotationLiteral;

import io.smallrye.health.SmallRyeHealthReporter;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;
import org.eclipse.microprofile.health.Readiness;
import org.eclipse.microprofile.health.Startup;
import org.jboss.as.server.moduleservice.ServiceModuleLoader;
import org.jboss.modules.Module;
import org.wildfly.extension.microprofile.health.MicroProfileHealthReporter;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
public class CDIExtension implements Extension {

    private final String MP_HEALTH_DISABLE_DEFAULT_PROCEDURES = "mp.health.disable-default-procedures";


    private final MicroProfileHealthReporter reporter;
    private final Module module;

    // Use a single Jakarta Contexts and Dependency Injection instance to select and destroy all HealthCheck probes instances
    private Instance<Object> instance;
    private final List<HealthCheck> livenessChecks = new ArrayList<>();
    private final List<HealthCheck> readinessChecks = new ArrayList<>();
    private final List<HealthCheck> startupChecks = new ArrayList<>();
    private HealthCheck defaultReadinessCheck;
    private HealthCheck defaultStartupCheck;
    private final Config config;
    private final String deploymentName;


    public CDIExtension(MicroProfileHealthReporter healthReporter, Module module) {
        this.reporter = healthReporter;
        this.module = module;
        this.config = ConfigProvider.getConfig(this.module.getClassLoader());
        this.deploymentName = module.getName().replace(ServiceModuleLoader.MODULE_PREFIX, "");
    }

    /**
     * Get Jakarta Contexts and Dependency Injection <em>instances</em> of HealthCheck and
     * add them to the {@link MicroProfileHealthReporter}.
     */
    private void afterDeploymentValidation(@Observes final AfterDeploymentValidation avd, BeanManager bm) {
        instance = bm.createInstance();

        addHealthChecks(Liveness.Literal.INSTANCE, reporter::addLivenessCheck, livenessChecks);
        addHealthChecks(Readiness.Literal.INSTANCE, reporter::addReadinessCheck, readinessChecks);
        addHealthChecks(Startup.Literal.INSTANCE, reporter::addStartupCheck, startupChecks);

        final boolean disableDefaultProcedures = config.getOptionalValue(MP_HEALTH_DISABLE_DEFAULT_PROCEDURES, Boolean.class).orElse(false);
        if (readinessChecks.isEmpty()) {
            if (!disableDefaultProcedures) {
                // no readiness probe are present in the deployment. register a readiness check so that the deployment is considered ready
                defaultReadinessCheck = new DefaultReadinessHealthCheck(module.getName());
                reporter.addReadinessCheck(defaultReadinessCheck, module.getClassLoader());
            }
        }
        if (startupChecks.isEmpty()) {
            if (!disableDefaultProcedures) {
                // no startup probes are present in the deployment. register a startup check so that the deployment is considered started
                defaultStartupCheck = new DefaultStartupHealthCheck(module.getName());
                reporter.addStartupCheck(defaultStartupCheck, module.getClassLoader());
            }
        }
        // Track per-deployment configuration.
        // Specifically, see https://issues.redhat.com/browse/WFLY-19147 - let the per-application
        // `mp.health.disable-default-procedures` configuration setting affect the Health checks response global server
        // configuration.
        reporter.addDeploymentConfiguration(deploymentName, disableDefaultProcedures);
    }

    private void addHealthChecks(AnnotationLiteral qualifier,
                                 BiConsumer<HealthCheck, ClassLoader> healthFunction, List<HealthCheck> healthChecks) {
        for (HealthCheck healthCheck : instance.select(HealthCheck.class, qualifier)) {
            healthFunction.accept(healthCheck, module.getClassLoader());
            healthChecks.add(healthCheck);
        }
    }

    /**
     * Called when the deployment is undeployed.
     * <p>
     * Remove all the instances of {@link HealthCheck} from the {@link MicroProfileHealthReporter}.
     */
    public void beforeShutdown(@Observes final BeforeShutdown bs) {
        removeHealthCheck(livenessChecks, reporter::removeLivenessCheck);
        removeHealthCheck(readinessChecks, reporter::removeReadinessCheck);
        removeHealthCheck(startupChecks, reporter::removeStartupCheck);

        if (defaultReadinessCheck != null) {
            reporter.removeReadinessCheck(defaultReadinessCheck);
            defaultReadinessCheck = null;
        }

        if (defaultStartupCheck != null) {
            reporter.removeStartupCheck(defaultStartupCheck);
            defaultStartupCheck = null;
        }
        // Remove a deployment configuration tracker.
        // Specifically, https://issues.redhat.com/browse/WFLY-20325 - remove the per-application
        // `mp.health.disable-default-procedures` configuration setting affect the Health checks response global server
        // configuration
        reporter.removeDeploymentConfiguration(deploymentName);

        instance = null;
    }

    private void removeHealthCheck(List<HealthCheck> healthChecks,
                                   Consumer<HealthCheck> healthFunction) {
        for (HealthCheck healthCheck : healthChecks) {
            healthFunction.accept(healthCheck);
            instance.destroy(healthCheck);
        }
        healthChecks.clear();
    }

    public void vetoSmallryeHealthReporter(@Observes ProcessAnnotatedType<SmallRyeHealthReporter> pat) {
        pat.veto();
    }

    private static final class DefaultReadinessHealthCheck implements HealthCheck {

        private final String deploymentName;

        DefaultReadinessHealthCheck(String deploymentName) {
            this.deploymentName = deploymentName;
        }

        @Override
        public HealthCheckResponse call() {
            return HealthCheckResponse.named("ready-" + deploymentName)
                    .up()
                    .build();
        }
    }

    private static final class DefaultStartupHealthCheck implements HealthCheck {

        private final String deploymentName;

        DefaultStartupHealthCheck(String deploymentName) {
            this.deploymentName = deploymentName;
        }

        @Override
        public HealthCheckResponse call() {
            return HealthCheckResponse.named("started-" + deploymentName)
                .up()
                .build();
        }
    }
}
