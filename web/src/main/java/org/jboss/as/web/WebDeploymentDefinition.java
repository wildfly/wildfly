package org.jboss.as.web;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleResourceDefinition;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

/**
 * @author Tomaz Cerar
 * @created 23.2.12 18:32
 */
public class WebDeploymentDefinition extends SimpleResourceDefinition {
    public static final WebDeploymentDefinition INSTANCE = new WebDeploymentDefinition();


    private WebDeploymentDefinition() {
        super(PathElement.pathElement(SUBSYSTEM,WebExtension.SUBSYSTEM_NAME),
                WebExtension.getResourceDescriptionResolver("deployment"));
    }

}
