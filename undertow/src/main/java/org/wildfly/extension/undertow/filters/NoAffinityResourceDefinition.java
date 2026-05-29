/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.filters;

import java.util.function.UnaryOperator;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceRegistration;
import org.wildfly.extension.undertow.Constants;

/**
 * Affinity resource configuring no affinity handling.
 *
 * @author Radoslav Husar
 */
public class NoAffinityResourceDefinition extends AffinityResourceDefinition {
    public static final ResourceRegistration REGISTRATION = ResourceRegistration.of(pathElement(Constants.NONE));
    public static final PathElement PATH = REGISTRATION.getPathElement();

    public NoAffinityResourceDefinition() {
        super(REGISTRATION, UnaryOperator.identity());
    }
}
