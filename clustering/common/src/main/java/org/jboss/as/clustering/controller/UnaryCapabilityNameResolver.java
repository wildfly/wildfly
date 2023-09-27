/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import java.util.function.Function;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;

/**
 * Dynamic name mapper implementations for unary capability names.
 * @author Paul Ferraro
 */
public enum UnaryCapabilityNameResolver implements Function<PathAddress, String[]> {
    DEFAULT() {
        @Override
        public String[] apply(PathAddress address) {
            return new String[] { address.getLastElement().getValue() };
        }
    },
    PARENT() {
        @Override
        public String[] apply(PathAddress address) {
            return new String[] { address.getParent().getLastElement().getValue() };
        }
    },
    GRANDPARENT() {
        @Override
        public String[] apply(PathAddress address) {
            return new String[] { address.getParent().getParent().getLastElement().getValue() };
        }
    },
    LOCAL() {
        @Override
        public String[] apply(PathAddress address) {
            return new String[] { ModelDescriptionConstants.LOCAL };
        }
    },
}
