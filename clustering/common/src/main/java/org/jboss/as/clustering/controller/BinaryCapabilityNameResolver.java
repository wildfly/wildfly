/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import java.util.function.Function;

import org.jboss.as.controller.PathAddress;

/**
 * Dynamic name mapper implementations for binary capability names.
 * @author Paul Ferraro
 */
public enum BinaryCapabilityNameResolver implements Function<PathAddress, String[]> {
    PARENT_CHILD() {
        @Override
        public String[] apply(PathAddress address) {
            return new String[] { address.getParent().getLastElement().getValue(), address.getLastElement().getValue() };
        }
    },
    GRANDPARENT_PARENT() {
        @Override
        public String[] apply(PathAddress address) {
            PathAddress parent = address.getParent();
            return new String[] { parent.getParent().getLastElement().getValue(), parent.getLastElement().getValue() };
        }
    },
    GRANDPARENT_CHILD() {
        @Override
        public String[] apply(PathAddress address) {
            return new String[] { address.getParent().getParent().getLastElement().getValue(), address.getLastElement().getValue() };
        }
    },
}
