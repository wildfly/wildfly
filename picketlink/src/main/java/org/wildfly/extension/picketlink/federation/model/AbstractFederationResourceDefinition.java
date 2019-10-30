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

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.picketlink.common.model.AbstractResourceDefinition;
import org.wildfly.extension.picketlink.common.model.ModelElement;
import org.wildfly.extension.picketlink.common.model.validator.AlternativeAttributeValidationStepHandler;
import org.wildfly.extension.picketlink.federation.FederationExtension;

import java.util.List;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 * @since Mar 18, 2012
 */
public abstract class AbstractFederationResourceDefinition extends AbstractResourceDefinition {

    protected AbstractFederationResourceDefinition(ModelElement modelElement, OperationStepHandler addHandler, OperationStepHandler removeHandler, SimpleAttributeDefinition... attributes) {
        super(modelElement, addHandler, removeHandler, FederationExtension.getResourceDescriptionResolver(modelElement.getName()), attributes);
    }

    protected AbstractFederationResourceDefinition(ModelElement modelElement, String name, OperationStepHandler addHandler, OperationStepHandler removeHandler, SimpleAttributeDefinition... attributes) {
        super(modelElement, name, addHandler, removeHandler, FederationExtension.getResourceDescriptionResolver(modelElement.getName()), attributes);
    }

    @Override
    protected OperationStepHandler createAttributeWriterHandler() {
        List<SimpleAttributeDefinition> attributes = getAttributes();
        final List<AttributeDefinition> alternativeAttributes = getAlternativesAttributes();

        return new ReloadRequiredWriteAttributeHandler(attributes.toArray(new AttributeDefinition[attributes.size()])) {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                if (!alternativeAttributes.isEmpty()) {
                    context.addStep(new AlternativeAttributeValidationStepHandler(alternativeAttributes
                            .toArray(new AttributeDefinition[alternativeAttributes.size()])),
                        OperationContext.Stage.MODEL);
                }

                doRegisterModelWriteAttributeHandler(context, operation);

                super.execute(context, operation);
            }
        };
    }

    protected void doRegisterModelWriteAttributeHandler(OperationContext context, ModelNode operation) {

    }
}
