/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.singleton;

import java.util.function.UnaryOperator;

import org.jboss.as.controller.PathElement;

/**
 * @author Paul Ferraro
 */
public class RandomElectionPolicyResourceDefinition extends ElectionPolicyResourceDefinition {

    static final String PATH_VALUE = "random";
    static final PathElement PATH = pathElement(PATH_VALUE);

    RandomElectionPolicyResourceDefinition() {
        super(PATH, SingletonExtension.SUBSYSTEM_RESOLVER.createChildResolver(PATH, WILDCARD_PATH), UnaryOperator.identity(), RandomElectionPolicyServiceConfigurator::new);
    }
}
