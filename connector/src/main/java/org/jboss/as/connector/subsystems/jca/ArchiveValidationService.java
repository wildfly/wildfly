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


    private final ArchiveValidationService.ArchiveValidation value;
        private final InjectedValue<JcaSubsystemConfiguration> jcaConfig = new InjectedValue<JcaSubsystemConfiguration>();


        /** create an instance **/
        public ArchiveValidationService(ArchiveValidationService.ArchiveValidation value) {
            this.value = value;
        }

        @Override
        public ArchiveValidationService.ArchiveValidation getValue() throws IllegalStateException {
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
