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

package org.jboss.as.domain.controller.transformers;

import java.util.Locale;

import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.ResourceTransformationContext;
import org.jboss.as.controller.transform.ResourceTransformer;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.TransformersSubRegistration;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescription;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.jboss.as.domain.management.CoreManagementResourceDefinition;
import org.jboss.as.domain.management.access.AccessAuthorizationResourceDefinition;
import org.jboss.dmr.ModelNode;

/**
 * Transformers for the domain-wide management configuration.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
class ManagementTransformers {

    /**
     * Transformation for versions prior to support for RBAC (WFLY-490)
     *
     * @param domain the domain level registration
     */
    static void registerTransformersPreRBAC(TransformersSubRegistration domain) {
        // Discard the domain level core-service=management resource and its children unless RBAC is enabled
        // Configuring rbac details is OK (i.e. discarable), so long as the provider is not enabled
        ResourceTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createInstance(CoreManagementResourceDefinition.PATH_ELEMENT);
        builder.setCustomResourceTransformer(new ResourceTransformer() {
            @Override
            public void transformResource(ResourceTransformationContext context, PathAddress address, Resource resource) throws OperationFailedException {
                Resource accessAuth = resource.getChild(AccessAuthorizationResourceDefinition.PATH_ELEMENT);
                if (accessAuth != null) {
                    ModelNode model = accessAuth.getModel();
                    String providerName = AccessAuthorizationResourceDefinition.PROVIDER.resolveModelAttribute(ExpressionResolver.REJECTING, model).asString();
                    AccessAuthorizationResourceDefinition.Provider provider = AccessAuthorizationResourceDefinition.Provider.valueOf(providerName.toUpperCase(Locale.ENGLISH));
                    if (provider != AccessAuthorizationResourceDefinition.Provider.SIMPLE) {
                        // Continue to lower levels where this will be rejected
                        final ResourceTransformationContext childContext = context.addTransformedResource(PathAddress.EMPTY_ADDRESS, resource);
                        childContext.processChildren(resource);
                    }
                }

                // else do nothing, i.e. acts as a discard
            }
        });
        ResourceTransformationDescriptionBuilder accessBuilder = builder.addChildResource(AccessAuthorizationResourceDefinition.PATH_ELEMENT)
            .getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.DEFINED, AccessAuthorizationResourceDefinition.PROVIDER)
                .setDiscard(new DiscardAttributeChecker.DefaultDiscardAttributeChecker() {
                    @Override
                    protected boolean isValueDiscardable(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context) {
                        return !attributeValue.isDefined()
                                || AccessAuthorizationResourceDefinition.Provider.SIMPLE.toString().equalsIgnoreCase(attributeValue.asString());
                    }
                }, AccessAuthorizationResourceDefinition.PROVIDER)
                .setDiscard(DiscardAttributeChecker.ALWAYS, AccessAuthorizationResourceDefinition.PERMISSION_COMBINATION_POLICY)
                .end();
        accessBuilder.discardChildResource(PathElement.pathElement(ModelDescriptionConstants.CONSTRAINT));
        accessBuilder.discardChildResource(PathElement.pathElement(ModelDescriptionConstants.ROLE_MAPPING));
        accessBuilder.discardChildResource(PathElement.pathElement(ModelDescriptionConstants.SERVER_GROUP_SCOPED_ROLE));
        accessBuilder.discardChildResource(PathElement.pathElement(ModelDescriptionConstants.HOST_SCOPED_ROLE));

        TransformationDescription.Tools.register(builder.build(), domain);
    }

    private ManagementTransformers() {
        // prevent instantiation
    }
}
