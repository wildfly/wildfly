/*
 * Copyright 2017 Red Hat, Inc.
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

package org.wildfly.test.security.common.elytron;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.dmr.ModelNode;

/**
 * Elytron configurator for trusted-domains attribute in a security-domain.
 *
 * @author Josef Cacek
 */
public class TrustedDomainsConfigurator extends AbstractConfigurableElement {

    private final String[] trustedSecurityDomains;

    private ModelNode originalDomains;

    private TrustedDomainsConfigurator(Builder builder) {
        super(builder);
        this.trustedSecurityDomains = builder.trustedSecurityDomains;
    }

    @Override
    public void create(ModelControllerClient client, CLIWrapper cli) throws Exception {
        final PathAddress domainAddress = PathAddress.pathAddress().append("subsystem", "elytron").append("security-domain",
                name);
        ModelNode op = Util.createEmptyOperation("read-attribute", domainAddress);
        op.get("name").set("trusted-security-domains");
        ModelNode result = client.execute(op);
        if (Operations.isSuccessfulOutcome(result)) {
            result = Operations.readResult(result);
            originalDomains = result.isDefined() ? result : null;
        } else {
            throw new RuntimeException("Reading existing value of trusted-security-domains attribute failed: "
                    + Operations.getFailureDescription(result));
        }

        op = Util.createEmptyOperation("write-attribute", domainAddress);
        op.get("name").set("trusted-security-domains");
        for (String domain : trustedSecurityDomains) {
            op.get("value").add(domain);
        }
        Utils.applyUpdate(op, client);
    }

    @Override
    public void remove(ModelControllerClient client, CLIWrapper cli) throws Exception {
        final PathAddress domainAddress = PathAddress.pathAddress().append("subsystem", "elytron").append("security-domain",
                name);
        ModelNode op = Util.createEmptyOperation("write-attribute", domainAddress);
        op.get("name").set("trusted-security-domains");
        if (originalDomains != null) {
            op.get("value").set(originalDomains);
        }
        Utils.applyUpdate(op, client);
        originalDomains = null;
    }

    /**
     * Creates builder to build {@link TrustedDomainsConfigurator}.
     *
     * @return created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder to build {@link TrustedDomainsConfigurator}.
     */
    public static final class Builder extends AbstractConfigurableElement.Builder<Builder> {
        private String[] trustedSecurityDomains;

        private Builder() {
        }

        public Builder withTrustedSecurityDomains(String... trustedSecurityDomains) {
            this.trustedSecurityDomains = trustedSecurityDomains;
            return this;
        }

        public TrustedDomainsConfigurator build() {
            return new TrustedDomainsConfigurator(this);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }
}
