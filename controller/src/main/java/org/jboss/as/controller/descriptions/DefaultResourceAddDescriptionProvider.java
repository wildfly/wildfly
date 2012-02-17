/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller.descriptions;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.dmr.ModelNode;

/**
 * Uses an analysis of registry metadata to provide a default description of an operation that adds a resource.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DefaultResourceAddDescriptionProvider implements DescriptionProvider {

    private final ImmutableManagementResourceRegistration registration;
    final ResourceDescriptionResolver descriptionResolver;

    public DefaultResourceAddDescriptionProvider(final ImmutableManagementResourceRegistration registration,
                                                 final ResourceDescriptionResolver descriptionResolver) {
        this.registration = registration;
        this.descriptionResolver = descriptionResolver;
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        ModelNode result = new ModelNode();

        final ResourceBundle bundle = descriptionResolver.getResourceBundle(locale);
        result.get(OPERATION_NAME).set(ADD);
        result.get(DESCRIPTION).set(descriptionResolver.getOperationDescription(ADD, locale, bundle));

        final ModelNode params = result.get(REQUEST_PROPERTIES).setEmptyObject();

        Set<String> attributeNames = registration.getAttributeNames(PathAddress.EMPTY_ADDRESS);
        for (String attr : attributeNames)  {
            AttributeAccess attributeAccess = registration.getAttributeAccess(PathAddress.EMPTY_ADDRESS, attr);
            if (attributeAccess.getStorageType() == AttributeAccess.Storage.CONFIGURATION) {
                AttributeDefinition def = attributeAccess.getAttributeDefinition();
                if (def != null) {
                    def.addOperationParameterDescription(result, ADD, descriptionResolver, locale, bundle);
                } else {
                    // Just stick in a placeholder;
                    params.get(attr);
                }
            }
        }

        result.get(REPLY_PROPERTIES).setEmptyObject();

        return result;
    }
}
