package org.jboss.as.ee.managedbean.component;

import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.msc.service.ServiceName;

/**
 * Component descriptor for {@link javax.annotation.ManagedBean} managed beans.
 *
 * This is only here so that other interested processors can tell if a given component is a managed bean,
 * it does not add anything to the component description.
 *
 * @author Stuart Douglas
 */
public class ManagedBeanComponentDescription extends ComponentDescription {
    /**
     * Construct a new instance.
     *
     * @param componentName             the component name
     * @param componentClassName        the component instance class name
     * @param moduleDescription         the EE module description
     * @param deploymentUnitServiceName the service name of the DU containing this component
     */
    public ManagedBeanComponentDescription(final String componentName, final String componentClassName, final EEModuleDescription moduleDescription, final ServiceName deploymentUnitServiceName) {
        super(componentName, componentClassName, moduleDescription, deploymentUnitServiceName);
    }
}
