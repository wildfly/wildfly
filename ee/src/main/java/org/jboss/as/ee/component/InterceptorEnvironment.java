package org.jboss.as.ee.component;

import java.util.ArrayList;
import java.util.List;

/**
 * Class that represents the environment entries of an interceptor, defined in the interceptors section
 * of ejb-jar.xml.
 *
 *
 *
 * @author Stuart Douglas
 */
public class InterceptorEnvironment implements ResourceInjectionTarget {

    private final DeploymentDescriptorEnvironment deploymentDescriptorEnvironment;
    private final List<ResourceInjectionConfiguration> resourceInjections = new ArrayList<ResourceInjectionConfiguration>();
    private final List<BindingConfiguration> bindingConfigurations = new ArrayList<BindingConfiguration>();

    public InterceptorEnvironment(final DeploymentDescriptorEnvironment deploymentDescriptorEnvironment) {
        this.deploymentDescriptorEnvironment = deploymentDescriptorEnvironment;
    }

    public List<BindingConfiguration> getBindingConfigurations() {
        return bindingConfigurations;
    }

    public void addResourceInjection(final ResourceInjectionConfiguration injection) {
        resourceInjections.add(injection);
    }

    public List<ResourceInjectionConfiguration> getResourceInjections() {
        return resourceInjections;
    }

    public DeploymentDescriptorEnvironment getDeploymentDescriptorEnvironment() {
        return deploymentDescriptorEnvironment;
    }
}
