package org.jboss.as.ejb3.deployment;

import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.msc.value.InjectedValue;

import java.util.Collection;
import java.util.Map;

/**
 * Runtime information about an EJB in a module
 *
 * @author Stuart Douglas
 */
public class EjbDeploymentInformation {

    private final String ejbName;

    private final ClassLoader deploymentClassLoader;

    private final InjectedValue<EJBComponent> ejbComponent;

    private final Map<String, InjectedValue<ComponentView>> componentViews;

    public EjbDeploymentInformation(String ejbName, InjectedValue<EJBComponent> ejbComponent, Map<String, InjectedValue<ComponentView>> componentViews, final ClassLoader deploymentClassLoader) {
        this.ejbName = ejbName;
        this.ejbComponent = ejbComponent;
        this.componentViews = componentViews;
        this.deploymentClassLoader = deploymentClassLoader;
    }

    public String getEjbName() {
        return ejbName;
    }

    public EJBComponent getEjbComponent() {
        return ejbComponent.getValue();
    }

    public Collection<String> getViewNames() {
        return componentViews.keySet();
    }

    public ComponentView getView(String name) {
        final InjectedValue<ComponentView> value = componentViews.get(name);
        if(value == null) {
            throw new IllegalArgumentException("View " + name + " was not found");
        }
        return value.getValue();
    }

    public ClassLoader getDeploymentClassLoader() {
        return deploymentClassLoader;
    }
}
