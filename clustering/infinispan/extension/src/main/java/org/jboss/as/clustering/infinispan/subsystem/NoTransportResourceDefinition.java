/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.function.UnaryOperator;

import org.jboss.as.controller.PathElement;

/**
 * @author Paul Ferraro
 */
public class NoTransportResourceDefinition extends TransportResourceDefinition {
    static final PathElement PATH = pathElement("none");

    NoTransportResourceDefinition() {
        super(PATH, UnaryOperator.identity(), new NoTransportServiceHandler());
    }
}
