package org.jboss.as.host.controller.model.host;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.host.controller.HostModelUtil;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
public class CoreServiceResourceDefinition extends SimpleResourceDefinition {
    public static CoreServiceResourceDefinition INSTANCE = new CoreServiceResourceDefinition();

    private CoreServiceResourceDefinition() {
        super(PathElement.pathElement(CORE_SERVICE, MANAGEMENT), HostModelUtil.getResourceDescriptionResolver("core", "management"));
    }


    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
    }
    /*
    see ManagementDescription.getManagementDescription
     */
}
