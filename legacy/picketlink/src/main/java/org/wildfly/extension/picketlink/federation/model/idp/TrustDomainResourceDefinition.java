/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.picketlink.federation.model.idp;

import org.jboss.as.controller.ModelOnlyAddStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.picketlink.common.model.ModelElement;
import org.wildfly.extension.picketlink.federation.Namespace;
import org.wildfly.extension.picketlink.federation.model.AbstractFederationResourceDefinition;

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
        super(ModelElement.IDENTITY_PROVIDER_TRUST_DOMAIN, new ModelOnlyAddStepHandler(CERT_ALIAS), CERT_ALIAS);
    }
}
