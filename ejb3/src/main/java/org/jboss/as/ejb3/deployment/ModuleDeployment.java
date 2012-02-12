package org.jboss.as.ejb3.deployment;

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
        deploymentRepository.getValue().add(identifier, this);
    }

    @Override
    public void stop(StopContext context) {
        deploymentRepository.getValue().remove(identifier);
    }

    @Override
    public ModuleDeployment getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

}
