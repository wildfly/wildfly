package org.jboss.as.controller.descriptions.common;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleResourceDefinition;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
public class CoreManagementDefinition extends SimpleResourceDefinition {
    public static final CoreManagementDefinition INSTANCE = new CoreManagementDefinition();

    private CoreManagementDefinition() {
        super(PathElement.pathElement(CORE_SERVICE, MANAGEMENT),
                ControllerResolver.getResolver("core", MANAGEMENT));
    }
}
