/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.picketlink.federation.model.idp;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.picketlink.common.model.ModelElement;
import org.wildfly.extension.picketlink.federation.Namespace;
import org.wildfly.extension.picketlink.federation.model.AbstractFederationResourceDefinition;

import java.util.List;

import static org.wildfly.extension.picketlink.logging.PicketLinkLogger.ROOT_LOGGER;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 * @since Mar 16, 2012
 */
public class TrustDomainResourceDefinition extends AbstractFederationResourceDefinition {

    public static final SimpleAttributeDefinition CERT_ALIAS = new SimpleAttributeDefinitionBuilder(ModelElement.IDENTITY_PROVIDER_TRUST_DOMAIN_CERT_ALIAS.getName(), ModelType.STRING, true)
        .setAllowExpression(true)
        .setDeprecated(Namespace.PICKETLINK_FEDERATION_1_1.getModelVersion())
        .build();

    public static final TrustDomainResourceDefinition INSTANCE = new TrustDomainResourceDefinition();

    private TrustDomainResourceDefinition() {
        super(ModelElement.IDENTITY_PROVIDER_TRUST_DOMAIN, TrustDomainAddHandler.INSTANCE, TrustDomainRemoveHandler.INSTANCE, CERT_ALIAS);
    }

    @Override
    protected OperationStepHandler createAttributeWriterHandler() {
        List<SimpleAttributeDefinition> attributes = getAttributes();
        return new ReloadRequiredWriteAttributeHandler(attributes.toArray(new AttributeDefinition[attributes.size()])) {
            @Override
            protected void validateUpdatedModel(OperationContext context, Resource model) throws OperationFailedException {
                validateModelInOperation(context, model.getModel());
                super.validateUpdatedModel(context, model);
            }
        };
    }



    public static void validateModelInOperation(OperationContext context, ModelNode model) throws OperationFailedException {
        String certAliasAttribName = ModelElement.IDENTITY_PROVIDER_TRUST_DOMAIN_CERT_ALIAS.getName();

        if (context.getProcessType().isServer() && model.hasDefined(certAliasAttribName)) {
            throw ROOT_LOGGER.attributeNoLongerSupported(certAliasAttribName);
        }
    }
}
