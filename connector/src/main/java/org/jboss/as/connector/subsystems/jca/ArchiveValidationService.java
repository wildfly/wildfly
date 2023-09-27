/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.subsystems.jca;

import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * A ResourceAdaptersService.
 * @author <a href="mailto:stefano.maestri@redhat.comdhat.com">Stefano
 *         Maestri</a>
 */
final class ArchiveValidationService implements Service<ArchiveValidationService.ArchiveValidation> {


    private final ArchiveValidation value;
        private final InjectedValue<JcaSubsystemConfiguration> jcaConfig = new InjectedValue<JcaSubsystemConfiguration>();


        /** create an instance **/
        public ArchiveValidationService(ArchiveValidation value) {
            this.value = value;
        }

        @Override
        public ArchiveValidation getValue() throws IllegalStateException {
            return value;
        }

        @Override
        public void start(StartContext context) throws StartException {
            jcaConfig.getValue().setArchiveValidation(value.isEnabled());
            jcaConfig.getValue().setArchiveValidationFailOnError(value.isFailOnError());
            jcaConfig.getValue().setArchiveValidationFailOnWarn(value.isFailOnWarn());

        }

        @Override
        public void stop(StopContext context) {

        }

        public Injector<JcaSubsystemConfiguration> getJcaConfigInjector() {
            return jcaConfig;
        }

    public static class ArchiveValidation {
        private final boolean enabled;
        private final boolean failOnError;
        private final boolean failOnWarn;

        public ArchiveValidation(boolean enabled, boolean failOnError, boolean failOnWarn) {
            this.enabled = enabled;
            this.failOnError = failOnError;
            this.failOnWarn = failOnWarn;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public boolean isFailOnError() {
            return failOnError;
        }

        public boolean isFailOnWarn() {
            return failOnWarn;
        }

    }
}
