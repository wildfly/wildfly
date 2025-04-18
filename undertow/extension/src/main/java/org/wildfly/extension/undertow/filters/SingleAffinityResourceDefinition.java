/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.filters;

import java.util.function.UnaryOperator;

import org.jboss.as.controller.PathElement;
import org.wildfly.extension.undertow.Constants;

/**
 * Affinity resource configuring default - single - routing behavior.
 *
 * @author Radoslav Husar
 */
public class SingleAffinityResourceDefinition extends AffinityResourceDefinition {

    public static final PathElement PATH = pathElement(Constants.SINGLE);

    public SingleAffinityResourceDefinition() {
        super(PATH, UnaryOperator.identity());
    }
}
