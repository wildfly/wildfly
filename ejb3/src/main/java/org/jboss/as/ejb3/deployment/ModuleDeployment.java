/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.deployment;

import org.jboss.as.ee.component.deployers.StartupCountdown;
import org.jboss.ejb.client.EJBModuleIdentifier;
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

    private final EJBModuleIdentifier moduleId;
    private final InjectedValue<DeploymentRepository> deploymentRepository = new InjectedValue<DeploymentRepository>();
    private final Map<String, EjbDeploymentInformation> ejbs;

    public ModuleDeployment(EJBModuleIdentifier moduleId, Map<String, EjbDeploymentInformation> ejbs) {
        this.moduleId = moduleId;
        this.ejbs = Collections.unmodifiableMap(ejbs);
    }


    public EJBModuleIdentifier getIdentifier() {
        return moduleId;
    }

    public Map<String, EjbDeploymentInformation> getEjbs() {
        return ejbs;
    }

    public InjectedValue<DeploymentRepository> getDeploymentRepository() {
        return deploymentRepository;
    }

    @Override
    public void start(StartContext context) throws StartException {
        deploymentRepository.getValue().add(moduleId, ModuleDeployment.this);
    }

    @Override
    public void stop(StopContext context) {
        deploymentRepository.getValue().remove(moduleId);
    }

    @Override
    public ModuleDeployment getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    /**
     * service that marks a module a started
     */
    public static final class ModuleDeploymentStartService implements Service<Void> {

        private final EJBModuleIdentifier moduleId;
        private final InjectedValue<DeploymentRepository> deploymentRepository = new InjectedValue<DeploymentRepository>();
        private final StartupCountdown countdown;

        public ModuleDeploymentStartService(EJBModuleIdentifier moduleId, StartupCountdown countdown) {
            this.moduleId = moduleId;
            this.countdown = countdown;
        }

        @Override
        public void start(StartContext startContext) throws StartException {
            Runnable action = new Runnable() {
                @Override
                public void run() {
                    deploymentRepository.getValue().startDeployment(moduleId);
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
