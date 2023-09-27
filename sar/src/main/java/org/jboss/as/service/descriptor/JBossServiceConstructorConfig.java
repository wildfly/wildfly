/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.service.descriptor;

import java.io.Serializable;

/**
 * Configuration object for a JBoss service constructor.
 *
 * @author John E. Bailey
 */
public class JBossServiceConstructorConfig  implements Serializable {
    private static final long serialVersionUID = -4307592928958905408L;

    public static final Argument[] EMPTY_ARGS = {};
    private Argument[] arguments = EMPTY_ARGS;

    public Argument[] getArguments() {
        return arguments;
    }

    public void setArguments(Argument[] arguments) {
        this.arguments = arguments;
    }

    public static class Argument implements Serializable {
        private static final long serialVersionUID = 7644229980407045584L;

        private final String value;
        private final String type;

        public Argument(final String type, final String value) {
            this.value = value;
            this.type = type;
        }

        public String getType() {
            return type;
        }

        public String getValue() {
            return value;
        }
    }
}
