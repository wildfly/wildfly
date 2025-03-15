/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;

/**
 * Transformer for digest auth token resources.
 * @author Paul Ferraro
 */
public class DigestAuthTokenResourceTransformer extends AuthTokenResourceTransformer {

    DigestAuthTokenResourceTransformer(ResourceTransformationDescriptionBuilder parent) {
        super(parent.addChildResource(AuthTokenResourceDefinitionRegistrar.Token.DIGEST.getPathElement()));
    }
}
