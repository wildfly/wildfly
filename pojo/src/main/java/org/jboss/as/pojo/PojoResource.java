/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.pojo;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;

/**
 * @author Tomaz Cerar
 * @created 7.2.12 14:41
 */
public class PojoResource extends SimpleResourceDefinition {
    static final PathElement PATH = PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, PojoExtension.SUBSYSTEM_NAME);

    PojoResource() {
        super(PATH, PojoExtension.SUBSYSTEM_RESOLVER, PojoSubsystemAdd.INSTANCE, ReloadRequiredRemoveStepHandler.INSTANCE);
    }
}
