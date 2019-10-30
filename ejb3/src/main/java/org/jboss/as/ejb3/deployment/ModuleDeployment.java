/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import org.jboss.as.ee.component.deployers.StartupCountdown;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import java.util.Collections;
import java.util.Map;

/**
 * Represents a deployed module on AS7
 *
 * @author Stuart Douglas
 */
public class ModuleDeployment implements Service<ModuleDeployment> {

    public static final ServiceName SERVICE_NAME = ServiceName.of("moduleDeploymentRuntimeInformation");
    public static final ServiceName START_SERVICE_NAME = ServiceName.of("moduleDeploymentRuntimeInformationStart");

    private final DeploymentModuleIdentifier identifier;
    private final InjectedValue<DeploymentRepository> deploymentRepository = new InjectedValue<DeploymentRepository>();
    private final Map<String, EjbDeploymentInformation> ejbs;

    public ModuleDeployment(DeploymentModuleIdentifier identifier, Map<String, EjbDeploymentInformation> ejbs) {
        this.identifier = identifier;
        this.ejbs = Collections.unmodifiableMap(ejbs);
    }


    public DeploymentModuleIdentifier getIdentifier() {
        return identifier;
    }

    public Map<String, EjbDeploymentInformation> getEjbs() {
        return ejbs;
    }

    public InjectedValue<DeploymentRepository> getDeploymentRepository() {
        return deploymentRepository;
    }

    @Override
    public void start(StartContext context) throws StartException {
        deploymentRepository.getValue().add(identifier, ModuleDeployment.this);
    }

    @Override
    public void stop(StopContext context) {
        deploymentRepository.getValue().remove(identifier);
    }

    @Override
    public ModuleDeployment getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    /**
     * service that marks a module a started
     */
    public static final class ModuleDeploymentStartService implements Service<Void> {

        private final DeploymentModuleIdentifier identifier;
        private final InjectedValue<DeploymentRepository> deploymentRepository = new InjectedValue<DeploymentRepository>();
        private final StartupCountdown countdown;

        public ModuleDeploymentStartService(DeploymentModuleIdentifier identifier, StartupCountdown countdown) {
            this.identifier = identifier;
            this.countdown = countdown;
        }

        @Override
        public void start(StartContext startContext) throws StartException {
            Runnable action = new Runnable() {
                @Override
                public void run() {
                    deploymentRepository.getValue().startDeployment(identifier);
                }
            };
            if (countdown == null) action.run();
            else countdown.addCallback(action);
        }

        @Override
        public void stop(StopContext stopContext) {
        }

        @Override
        public Void getValue() throws IllegalStateException, IllegalArgumentException {
            return null;
        }

        public InjectedValue<DeploymentRepository> getDeploymentRepository() {
            return deploymentRepository;
        }
    }

}
