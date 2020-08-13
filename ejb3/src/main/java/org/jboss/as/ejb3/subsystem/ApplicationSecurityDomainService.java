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

import static java.security.AccessController.doPrivileged;

import java.security.PrivilegedAction;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * A {@link Service} that manages a security domain mapping.
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
public class ApplicationSecurityDomainService implements Service {

    private final Supplier<SecurityDomain> securityDomainSupplier;
    private final Consumer<ApplicationSecurityDomainService.ApplicationSecurityDomain> applicationSecurityDomainConsumer;
    private final Consumer<SecurityDomain> securityDomainConsumer;
    private final Set<RegistrationImpl> registrations = new HashSet<>();
    private final boolean enableJacc;

    public ApplicationSecurityDomainService(boolean enableJacc,
            Supplier<SecurityDomain> securityDomainSupplier, Consumer<ApplicationSecurityDomainService.ApplicationSecurityDomain> applicationSecurityDomainConsumer,
            Consumer<SecurityDomain> securityDomainConsumer) {
        this.enableJacc = enableJacc;
        this.securityDomainSupplier = securityDomainSupplier;
        this.applicationSecurityDomainConsumer = applicationSecurityDomainConsumer;
        this.securityDomainConsumer = securityDomainConsumer;
    }

    @Override
    public void start(StartContext context) throws StartException {
        SecurityDomain securityDomain = securityDomainSupplier.get();
        applicationSecurityDomainConsumer.accept(new ApplicationSecurityDomain(securityDomain, enableJacc));
        securityDomainConsumer.accept(securityDomain);
    }

    @Override
    public void stop(StopContext context) {}

    public String[] getDeployments() {
        synchronized(registrations) {
            Set<String> deploymentNames = new HashSet<>();
            for (RegistrationImpl r : registrations) {
                String deploymentName = r.deploymentName;
                deploymentNames.add(deploymentName);
            }
            return deploymentNames.toArray(new String[deploymentNames.size()]);
        }
    }

    private class RegistrationImpl implements Registration {

        private final String deploymentName;
        private final ClassLoader classLoader;

        private RegistrationImpl(String deploymentName, ClassLoader classLoader) {
            this.deploymentName = deploymentName;
            this.classLoader = classLoader;
        }

        @Override
        public void cancel() {
            if (WildFlySecurityManager.isChecking()) {
                doPrivileged((PrivilegedAction<Void>) () -> {
                    SecurityDomain.unregisterClassLoader(classLoader);
                    return null;
                });
            } else {
                SecurityDomain.unregisterClassLoader(classLoader);
            }

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
        private final boolean enableJacc;

        public ApplicationSecurityDomain(final SecurityDomain securityDomain, boolean enableJacc) {
            this.securityDomain = securityDomain;
            this.enableJacc = enableJacc;
        }

        public SecurityDomain getSecurityDomain() {
            return securityDomain;
        }

        public boolean isEnableJacc() {
            return enableJacc;
        }

        public BiFunction<String, ClassLoader, Registration> getSecurityFunction() {
            return this::registerElytronDeployment;
        }

        private Registration registerElytronDeployment(final String deploymentName, final ClassLoader classLoader) {
            if (WildFlySecurityManager.isChecking()) {
                doPrivileged((PrivilegedAction<Void>) () -> {
                    securityDomain.registerWithClassLoader(classLoader);
                    return null;
                });
            } else {
                securityDomain.registerWithClassLoader(classLoader);
            }

            RegistrationImpl registration = new RegistrationImpl(deploymentName, classLoader);
            synchronized(registrations) {
                registrations.add(registration);
            }
            return registration;
        }
    }
}
