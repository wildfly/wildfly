/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.util.function.UnaryOperator;

import org.jboss.as.clustering.jgroups.auth.BinaryAuthToken;
import org.jboss.as.controller.PathElement;

/**
 * @author Paul Ferraro
 */
public class PlainAuthTokenResourceDefinition extends AuthTokenResourceDefinition<BinaryAuthToken> {

    static final PathElement PATH = pathElement("plain");

    PlainAuthTokenResourceDefinition() {
        super(PATH, UnaryOperator.identity(), PlainAuthTokenServiceConfigurator::new);
    }
}
