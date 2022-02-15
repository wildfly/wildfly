/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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