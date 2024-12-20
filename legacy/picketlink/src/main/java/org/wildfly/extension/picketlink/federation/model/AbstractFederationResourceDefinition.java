/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.picketlink.federation.model;

import org.jboss.as.controller.ModelOnlyAddStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.wildfly.extension.picketlink.common.model.AbstractResourceDefinition;
import org.wildfly.extension.picketlink.common.model.ModelElement;
import org.wildfly.extension.picketlink.federation.FederationExtension;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 * @since Mar 18, 2012
 */
public abstract class AbstractFederationResourceDefinition extends AbstractResourceDefinition {

    protected AbstractFederationResourceDefinition(ModelElement modelElement, ModelOnlyAddStepHandler addHandler, SimpleAttributeDefinition... attributes) {
        super(modelElement, addHandler, FederationExtension.getResourceDescriptionResolver(modelElement.getName()), attributes);
    }
    protected AbstractFederationResourceDefinition(ModelElement modelElement, String name, ModelOnlyAddStepHandler addHandler, SimpleAttributeDefinition... attributes) {
        super(modelElement, name, addHandler, FederationExtension.getResourceDescriptionResolver(modelElement.getName()), attributes);
    }
}
