/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
