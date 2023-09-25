/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.security.common.elytron;

/**
 * @author Jan Martiska
 */
public interface PermissionMapper extends ConfigurableElement {

    enum MappingMode {
        AND("and"),
        FIRST("first"),
        OR("or"),
        UNLESS("unless"),
        XOR("xor");

        MappingMode(String string) {
            this.string = string;
        }

        private String string;

        @Override
        public String toString() {
            return string;
        }
    }

}
