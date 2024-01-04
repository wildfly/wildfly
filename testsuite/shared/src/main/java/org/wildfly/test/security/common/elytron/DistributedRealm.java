/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.security.common.elytron;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.dmr.ModelNode;

/**
 * A {@link ConfigurableElement} to define the distributed-realm resource within the Elytron subsystem.
 *
 * @author Ondrej Kotek
 */
public class DistributedRealm implements SecurityRealm {

    private final PathAddress address;
    private final String name;
    private final String[] realms;
    private final Boolean ignoreUnavailableRealms;
    private final Boolean emitEvents;

    DistributedRealm(final String name, final Builder builder) {
        this.name = name;
        this.address = PathAddress.pathAddress(PathElement.pathElement("subsystem", "elytron"), PathElement.pathElement("distributed-realm", name));
        this.realms = builder.realms;
        this.ignoreUnavailableRealms = builder.ignoreUnavailableRealms;
        this.emitEvents = builder.emitEvents;
    }

    @Override
    public String getName() {
        return name;
    }

    public ModelNode getAddOperation() {
        ModelNode addOperation = Util.createAddOperation(address);
        if (realms != null) {
            ModelNode realmsList = addOperation.get("realms");
            for (String realmName : realms) {
                realmsList.add(realmName);
            }
        }
        if (this.ignoreUnavailableRealms != null) {
            addOperation.get("ignore-unavailable-realms").set(this.ignoreUnavailableRealms);
        }
        if (this.emitEvents != null) {
            addOperation.get("emit-events").set(this.emitEvents);
        }

        return addOperation;
    }

    public ModelNode getRemoveOperation() {
        return Util.createRemoveOperation(address);
    }

    @Override
    public void create(ModelControllerClient client, CLIWrapper cli) throws Exception {
        Utils.applyUpdate(getAddOperation(), client);
    }

    @Override
    public void remove(ModelControllerClient client, CLIWrapper cli) throws Exception {
        Utils.applyUpdate(getRemoveOperation(), client);
    }

    public static Builder builder(final String name) {
        return new Builder(name);
    }

    public static class Builder {

        private final String name;
        private String[] realms;
        private Boolean ignoreUnavailableRealms;
        private Boolean emitEvents;

        Builder(final String name) {
            this.name = name;
        }

        public Builder withRealms(final String... realms) {
            this.realms = realms;

            return this;
        }

        public Builder withIgnoreUnavailableRealms(final Boolean ignoreUnavailableRealms) {
            this.ignoreUnavailableRealms = ignoreUnavailableRealms;

            return this;
        }

        public Builder withEmitEvents(final Boolean emitEvents) {
            this.emitEvents = emitEvents;

            return this;
        }

        public SecurityRealm build() {
            return new DistributedRealm(name, this);
        }

    }

}
