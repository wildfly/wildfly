/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.subsystem;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.security.auth.server.SecurityDomain;

/**
 * A {@link Service} that manages a security domain mapping.
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
public class ApplicationSecurityDomainService implements Service<ApplicationSecurityDomainService.ApplicationSecurityDomain> {

    private final InjectedValue<SecurityDomain> securityDomainInjector = new InjectedValue<>();
    private final Set<RegistrationImpl> registrations = new HashSet<>();

    private ApplicationSecurityDomain applicationSecurityDomain;

    @Override
    public void start(StartContext context) throws StartException {
        applicationSecurityDomain = new ApplicationSecurityDomain(securityDomainInjector.getValue());
    }

    @Override
    public void stop(StopContext context) {
        applicationSecurityDomain = null;
    }

    @Override
    public ApplicationSecurityDomain getValue() throws IllegalStateException, IllegalArgumentException {
        return applicationSecurityDomain;
    }

    Injector<SecurityDomain> getSecurityDomainInjector() {
        return securityDomainInjector;
    }

    public String[] getDeployments() {
        synchronized(registrations) {
            Set<String> deploymentNames = registrations.stream().map(r -> r.deploymentName).collect(Collectors.toSet());
            return deploymentNames.toArray(new String[deploymentNames.size()]);
        }
    }

    private class RegistrationImpl implements Registration {

        private final String deploymentName;

        private RegistrationImpl(String deploymentName) {
            this.deploymentName = deploymentName;
        }

        @Override
        public void cancel() {
            synchronized(registrations) {
                registrations.remove(this);
            }
        }
    }

    public interface Registration {

        /**
         * Cancel the registration.
         */
        void cancel();
    }

    public final class ApplicationSecurityDomain {

        private final SecurityDomain securityDomain;

        public ApplicationSecurityDomain(final SecurityDomain securityDomain) {
            this.securityDomain = securityDomain;
        }

        public SecurityDomain getSecurityDomain() {
            return securityDomain;
        }

        public Function<String, Registration> getSecurityFunction() {
            return this::registerElytronDeployment;
        }

        private Registration registerElytronDeployment(final String deploymentName) {
            RegistrationImpl registration = new RegistrationImpl(deploymentName);
            synchronized(registrations) {
                registrations.add(registration);
            }
            return registration;
        }
    }
}
