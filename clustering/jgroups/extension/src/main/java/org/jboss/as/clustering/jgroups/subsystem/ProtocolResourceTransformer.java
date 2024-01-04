/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;

/**
 * Transformer for protocol resources.
 * @author Paul Ferraro
 */
public class ProtocolResourceTransformer extends AbstractProtocolResourceTransformer {

    ProtocolResourceTransformer(ResourceTransformationDescriptionBuilder builder) {
        super(builder);
    }
}
