/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.controller.persistence.xml;

import java.util.function.Function;

import org.jboss.as.controller.PathElement;

/**
 * Functions resolving the local element name for the {@link PathElement} of a resource.
 * @author Paul Ferraro
 */
public enum ResourceXMLElementLocalName implements Function<PathElement, String> {
    KEY_VALUE() {
        @Override
        public String apply(PathElement path) {
            return String.join("-", path.getKey(), path.getValue());
        }
    },
    VALUE_KEY() {
        @Override
        public String apply(PathElement path) {
            return String.join("-", path.getValue(), path.getKey());
        }
    },
    KEY() {
        @Override
        public String apply(PathElement path) {
            return path.getKey();
        }
    },
    VALUE() {
        @Override
        public String apply(PathElement path) {
            return path.getValue();
        }
    },
    ;
}
