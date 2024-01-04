/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.health;

import org.jboss.dmr.ModelNode;

public interface ServerProbe {
    Outcome getOutcome();

    String getName();

    static class Outcome {
        final ModelNode data;
        final boolean success;

        Outcome(boolean success, ModelNode data) {
            this.data = data;
            this.success = success;
        }

        private Outcome(boolean success) {
            this(success, new ModelNode());
        }

        public ModelNode getData() {
            return data;
        }

        public boolean isSuccess() {
            return success;
        }

        static Outcome SUCCESS = new Outcome(true);
        static Outcome FAILURE = new Outcome(false);

    }
}
