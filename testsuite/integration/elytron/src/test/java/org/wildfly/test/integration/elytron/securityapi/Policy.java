/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.test.integration.elytron.securityapi;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.wildfly.test.security.common.elytron.AbstractConfigurableElement;
import org.wildfly.test.security.common.elytron.ConfigurableElement;

/**
 * A {@link ConfigurableElement} to add a policy definition to the Elytron subsystem.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
class Policy extends AbstractConfigurableElement {

    private static final Logger LOGGER = Logger.getLogger(Utils.class);

    private final PathAddress address;
    private final boolean jaccPolicy;

    Policy(Builder builder) {
        super(builder);
        this.address = PathAddress.pathAddress().append("subsystem", "elytron").append("policy", name);
        this.jaccPolicy = builder.jaccPolicy;
    }

    @Override
    public void create(ModelControllerClient client, CLIWrapper cli) throws Exception {
        ModelNode add = Util.createAddOperation(address);
        if (jaccPolicy) {
            add.get("jacc-policy").setEmptyObject();
        }
        LOGGER.tracef("Adding policy (%s)", add.toString());

        Utils.applyUpdate(add, client);
    }

    @Override
    public void remove(ModelControllerClient client, CLIWrapper cli) throws Exception {
        Utils.applyUpdate(Util.createRemoveOperation(address), client);
    }

    /**
     * Create a new {@link Builder} to build a policy definition.
     *
     * @return a new {@link Builder} to build a policy definition.
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * Builder to build a policy resource in the elytron subsystem.
     */
    public static final class Builder extends AbstractConfigurableElement.Builder<Builder> {

        private boolean jaccPolicy;

        /**
         * With an empty JACC policy.
         *
         * @return this {@link Builder} to allow method chaining.
         */
        public Builder withJaccPolicy() {
            jaccPolicy = true;

            return this;
        }

        /**
         * Build a new Policy {@link ConfigurableElement}
         *
         * @return a new Policy {@link ConfigurableElement}
         */
        public Policy build() {
            return new Policy(this);
        }

        @Override
        protected Builder self() {
            return this;
        }

    }

}
