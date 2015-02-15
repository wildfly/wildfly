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

package org.wildfly.extension.picketlink.federation.model;

import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.access.constraint.SensitivityClassification;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.wildfly.extension.picketlink.common.model.ModelElement;
import org.wildfly.extension.picketlink.federation.FederationExtension;
import org.wildfly.extension.picketlink.federation.model.idp.IdentityProviderResourceDefinition;
import org.wildfly.extension.picketlink.federation.model.keystore.KeyStoreProviderResourceDefinition;
import org.wildfly.extension.picketlink.federation.model.saml.SAMLResourceDefinition;
import org.wildfly.extension.picketlink.federation.model.sp.ServiceProviderResourceDefinition;

import java.util.List;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 * @since Mar 16, 2012
 */
public class FederationResourceDefinition extends AbstractFederationResourceDefinition {

    private static final List<AccessConstraintDefinition> CONSTRAINTS = new SensitiveTargetAccessConstraintDefinition(
        new SensitivityClassification(FederationExtension.SUBSYSTEM_NAME, "federation", false, true, true)
    ).wrapAsList();

    public static final SimpleAttributeDefinition[] ATTRIBUTE_DEFINITIONS = new SimpleAttributeDefinition[0];

    private final ExtensionContext extensionContext;

    public FederationResourceDefinition(ExtensionContext extensionContext) {
        super(ModelElement.FEDERATION, FederationAddHandler.INSTANCE, FederationRemoveHandler.INSTANCE);
        this.extensionContext = extensionContext;
        setDeprecated(FederationExtension.DEPRECATED_SINCE);
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        addChildResourceDefinition(KeyStoreProviderResourceDefinition.INSTANCE, resourceRegistration);
        addChildResourceDefinition(new IdentityProviderResourceDefinition(this.extensionContext), resourceRegistration);
        addChildResourceDefinition(new ServiceProviderResourceDefinition(this.extensionContext), resourceRegistration);
        addChildResourceDefinition(SAMLResourceDefinition.INSTANCE, resourceRegistration);
    }

    @Override
    public List<AccessConstraintDefinition> getAccessConstraints() {
        return CONSTRAINTS;
    }
}
