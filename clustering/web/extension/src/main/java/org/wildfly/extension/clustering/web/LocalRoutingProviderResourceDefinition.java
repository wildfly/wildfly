/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web;

import java.util.function.UnaryOperator;

import org.jboss.as.controller.PathElement;

/**
 * Definition of the /subsystem=distributable-web/routing=local resource.
 * @author Paul Ferraro
 */
public class LocalRoutingProviderResourceDefinition extends RoutingProviderResourceDefinition {

    static final PathElement PATH = pathElement("local");

    LocalRoutingProviderResourceDefinition() {
        super(PATH, UnaryOperator.identity(), LocalRoutingProviderServiceConfigurator::new);
    }
}
