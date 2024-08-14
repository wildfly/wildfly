/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;

/**
 * Transformer for plain auth token resources.
 * @author Paul Ferraro
 */
public class PlainAuthTokenResourceTransformer extends AuthTokenResourceTransformer {

    PlainAuthTokenResourceTransformer(ResourceTransformationDescriptionBuilder parent) {
        super(parent.addChildResource(AuthTokenResourceDefinitionRegistrar.Token.PLAIN.getPathElement()));
    }
}
