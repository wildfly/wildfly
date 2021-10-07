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

package org.wildfly.extension.picketlink.federation.model.saml;

import org.jboss.as.controller.ModelOnlyAddStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.picketlink.common.model.ModelElement;
import org.wildfly.extension.picketlink.federation.model.AbstractFederationResourceDefinition;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 * @since Mar 16, 2012
 */
public class SAMLResourceDefinition extends AbstractFederationResourceDefinition {

    public static final SimpleAttributeDefinition TOKEN_TIMEOUT = new SimpleAttributeDefinitionBuilder(ModelElement.SAML_TOKEN_TIMEOUT.getName(), ModelType.INT, true)
        .setDefaultValue(new ModelNode(5000))
        .setAllowExpression(true)
        .build();
    public static final SimpleAttributeDefinition CLOCK_SKEW = new SimpleAttributeDefinitionBuilder(ModelElement.SAML_CLOCK_SKEW.getName(), ModelType.INT, true)
        .setDefaultValue(ModelNode.ZERO)
        .setAllowExpression(true)
        .build();

    public static final SAMLResourceDefinition INSTANCE = new SAMLResourceDefinition();

    private SAMLResourceDefinition() {
        super(ModelElement.SAML, ModelElement.SAML.getName(), new ModelOnlyAddStepHandler(TOKEN_TIMEOUT, CLOCK_SKEW), TOKEN_TIMEOUT, CLOCK_SKEW);
    }


}
