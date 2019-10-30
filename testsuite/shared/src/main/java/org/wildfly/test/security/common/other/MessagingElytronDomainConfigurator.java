/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.test.security.common.other;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.dmr.ModelNode;
import org.wildfly.test.security.common.elytron.ConfigurableElement;

/**
 * Elytron configurator for elytron-domain attribute in a messaging subsystem (
 * {@code /subsystem=messaging-activemq/server=default:write-attribute(name=elytron-domain, value=...)}). If the server name is
 * not provided "default" value is used.
 *
 * @author Josef Cacek
 */
public class MessagingElytronDomainConfigurator implements ConfigurableElement {

    private final String server;
    private final String elytronDomain;

    private final PathAddress messagingServerAddress;
    private String originalDomain;

    private MessagingElytronDomainConfigurator(Builder builder) {
        this.server = builder.server != null ? builder.server : "default";
        this.elytronDomain = builder.elytronDomain;
        this.messagingServerAddress = PathAddress.pathAddress().append("subsystem", "messaging-activemq").append("server",
                server);
    }

    @Override
    public void create(ModelControllerClient client, CLIWrapper cli) throws Exception {
        originalDomain = setElytronDomain(client, elytronDomain);
    }

    @Override
    public void remove(ModelControllerClient client, CLIWrapper cli) throws Exception {
        setElytronDomain(client, originalDomain);
        originalDomain = null;
    }

    @Override
    public String getName() {
        return "/subsystem=messaging-activemq/server=" + server + " elytronDomain=" + elytronDomain;
    }

    private String setElytronDomain(ModelControllerClient client, String domainToSet) throws Exception {
        String origDomainValue = null;
        ModelNode op = Util.createEmptyOperation("read-attribute", messagingServerAddress);
        op.get("name").set("elytron-domain");
        ModelNode result = client.execute(op);
        boolean isConfigured = Operations.isSuccessfulOutcome(result);
        op = null;
        if (isConfigured) {
            result = Operations.readResult(result);
            origDomainValue = result.isDefined() ? result.asString() : null;
        }
        op = Util.createEmptyOperation("write-attribute", messagingServerAddress);
        op.get("name").set("elytron-domain");
        op.get("value").set(domainToSet);
        Utils.applyUpdate(op, client);

        return origDomainValue;
    }

    /**
     * Creates builder to build {@link MessagingElytronDomainConfigurator}.
     *
     * @return created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder to build {@link MessagingElytronDomainConfigurator}.
     */
    public static final class Builder {
        private String server;
        private String elytronDomain;

        private Builder() {
        }

        public Builder withServer(String server) {
            this.server = server;
            return this;
        }

        public Builder withElytronDomain(String elytronDomain) {
            this.elytronDomain = elytronDomain;
            return this;
        }

        public MessagingElytronDomainConfigurator build() {
            return new MessagingElytronDomainConfigurator(this);
        }
    }

}