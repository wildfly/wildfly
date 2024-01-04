/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.Set;

import org.jboss.as.clustering.controller.Attribute;
import org.jboss.as.clustering.controller.Capability;
import org.jboss.as.clustering.controller.ResourceCapabilityReference;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.registry.Resource;
import org.wildfly.clustering.service.Requirement;

/**
 * ResourceCapabilityReference that only records tx requirements if the TransactionMode indicates they are necessary.
 * @author Brian Stansberry
 */
public class TransactionResourceCapabilityReference extends ResourceCapabilityReference {

    private final Attribute transactionModeAttriute;
    private final Set<TransactionMode> excludedModes;

    public TransactionResourceCapabilityReference(Capability capability, Requirement requirement, Attribute transactionModeAttribute, Set<TransactionMode> excludedModes) {
        super(capability, requirement);
        this.transactionModeAttriute = transactionModeAttribute;
        this.excludedModes = excludedModes;
    }

    @Override
    public void addCapabilityRequirements(OperationContext context, Resource resource, String attributeName, String... values) {
        if (this.isTransactionalSupportRequired(context, resource)) {
            super.addCapabilityRequirements(context, resource, attributeName, values);
        }
    }

    @Override
    public void removeCapabilityRequirements(OperationContext context, Resource resource, String attributeName, String... values) {
        if (this.isTransactionalSupportRequired(context, resource)) {
            super.removeCapabilityRequirements(context, resource, attributeName, values);
        }
    }

    private boolean isTransactionalSupportRequired(OperationContext context, Resource resource) {
        try {
            TransactionMode mode = TransactionMode.valueOf(this.transactionModeAttriute.resolveModelAttribute(context, resource.getModel()).asString());
            return !this.excludedModes.contains(mode);
        } catch (OperationFailedException | RuntimeException e) {
            // OFE would be due to an expression that can't be resolved right now (OperationContext.Stage.MODEL).
            // Very unlikely an expression is used and that it uses a resolution source not available in MODEL.
            // In any case we add the requirement. Downside is they are forced to configure the tx subsystem when
            // they otherwise wouldn't, but that "otherwise wouldn't" also is a less likely scenario.
            return true;
        }
    }
}