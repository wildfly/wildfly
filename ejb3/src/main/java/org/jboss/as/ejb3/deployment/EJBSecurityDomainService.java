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

package org.jboss.as.ejb3.deployment;

import static java.security.AccessController.doPrivileged;
import static org.wildfly.security.auth.server.SecurityDomain.unregisterClassLoader;
import static org.wildfly.security.manager.WildFlySecurityManager.isChecking;

import java.security.PrivilegedAction;
import java.util.function.BiFunction;

import org.jboss.as.ejb3.subsystem.ApplicationSecurityDomainService.ApplicationSecurityDomain;
import org.jboss.as.ejb3.subsystem.ApplicationSecurityDomainService.Registration;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.modules.Module;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * A service that sets up the security domain mapping for an EJB deployment.
 *
 * This service is reponsible for deployment level actions such as registering a deployment with the resolved
 * application-security-domain mapping or associating the {@code SecurityDomain} with the deployment's {@code ClassLoader}.
 * Defined {@code ComponentConfigurator} instances will then provide the component specific installation.
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah</a>
 */
public class EJBSecurityDomainService implements Service<Void> {
    public static final ServiceName SERVICE_NAME = ServiceName.of("ejb3", "security-domain");

    private final InjectedValue<ApplicationSecurityDomain> applicationSecurityDomain = new InjectedValue<>();
    private final InjectedValue<SecurityDomain> securityDomain = new InjectedValue<>();

    private final DeploymentUnit deploymentUnit;

    private volatile Runnable cleanUpTask;

    public EJBSecurityDomainService(final DeploymentUnit deploymentUnit) {
        this.deploymentUnit = deploymentUnit;
    }

    @Override
    public synchronized void start(StartContext context) throws StartException {
        final String deploymentName = deploymentUnit.getParent() == null ? deploymentUnit.getName()
                : deploymentUnit.getParent().getName() + "." + deploymentUnit.getName();
        final Module module = deploymentUnit.getAttachment(Attachments.MODULE);
        final ClassLoader classLoader = module.getClassLoader();

        ApplicationSecurityDomain applicationSecurityDomain = this.applicationSecurityDomain.getOptionalValue();
        if (applicationSecurityDomain != null) {
            BiFunction<String, ClassLoader, Registration> securityFunction = applicationSecurityDomain.getSecurityFunction();
            if (securityFunction != null) {
                Registration registration = securityFunction.apply(deploymentName, classLoader);
                cleanUpTask = registration::cancel;
            }
        } else {
            final SecurityDomain securityDomain = this.securityDomain.getValue(); // At least one must be injected.

            if (isChecking()) {
                doPrivileged((PrivilegedAction<Void>) () -> {
                    securityDomain.registerWithClassLoader(classLoader);
                    return null;
                });
            } else {
                securityDomain.registerWithClassLoader(classLoader);
            }

            cleanUpTask = new Runnable() {

                @Override
                public void run() {
                    if (WildFlySecurityManager.isChecking()) {
                        doPrivileged((PrivilegedAction<Void>) () -> {
                            unregisterClassLoader(classLoader);
                            return null;
                        });
                    } else {
                        unregisterClassLoader(classLoader);
                    }
                }

            };

        }
    }

    @Override
    public synchronized void stop(StopContext context) {
        if (cleanUpTask != null) {
            cleanUpTask.run();
        }
    }

    @Override
    public Void getValue() throws IllegalStateException, IllegalArgumentException {
        return null;
    }

    public Injector<ApplicationSecurityDomain> getApplicationSecurityDomainInjector() {
        return applicationSecurityDomain;
    }

    public Injector<SecurityDomain> getSecurityDomainInjector() {
        return securityDomain;
    }

}
