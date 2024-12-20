/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.picketlink.federation.model;

import java.util.List;

import org.jboss.as.controller.ModelOnlyAddStepHandler;
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

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 * @since Mar 16, 2012
 */
public class FederationResourceDefinition extends AbstractFederationResourceDefinition {

    private static final List<AccessConstraintDefinition> CONSTRAINTS = new SensitiveTargetAccessConstraintDefinition(
        new SensitivityClassification(FederationExtension.SUBSYSTEM_NAME, "federation", false, true, true)
    ).wrapAsList();

    public FederationResourceDefinition() {
        super(ModelElement.FEDERATION, new ModelOnlyAddStepHandler());
        setDeprecated(FederationExtension.DEPRECATED_SINCE);
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        addChildResourceDefinition(KeyStoreProviderResourceDefinition.INSTANCE, resourceRegistration);
        addChildResourceDefinition(new IdentityProviderResourceDefinition(), resourceRegistration);
        addChildResourceDefinition(new ServiceProviderResourceDefinition(), resourceRegistration);
        addChildResourceDefinition(SAMLResourceDefinition.INSTANCE, resourceRegistration);
    }

    @Override
    public List<AccessConstraintDefinition> getAccessConstraints() {
        return CONSTRAINTS;
    }
}
