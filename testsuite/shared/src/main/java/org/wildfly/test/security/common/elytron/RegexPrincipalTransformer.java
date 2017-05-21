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

import static org.wildfly.test.security.common.ModelNodeUtil.setIfNotNull;

import java.util.Objects;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.dmr.ModelNode;

/**
 * Elytron 'regex-principal-transformer' configuration.
 *
 * @author Josef Cacek
 */
public class RegexPrincipalTransformer extends AbstractConfigurableElement {

    private final String pattern;
    private final String replacement;
    private final Boolean replaceAll;

    private RegexPrincipalTransformer(Builder builder) {
        super(builder);
        this.pattern = Objects.requireNonNull(builder.pattern, "Pattern attribute has to be provided");
        this.replacement = Objects.requireNonNull(builder.replacement, "Replacement attribute has to be provided");
        this.replaceAll = builder.replaceAll;
    }

    @Override
    public void create(ModelControllerClient client, CLIWrapper cli) throws Exception {
        ModelNode op = Util.createAddOperation(
                PathAddress.pathAddress().append("subsystem", "elytron").append("regex-principal-transformer", name));
        setIfNotNull(op, "pattern", pattern);
        setIfNotNull(op, "replacement", replacement);
        setIfNotNull(op, "replace-all", replaceAll);
        Utils.applyUpdate(op, client);
    }

    @Override
    public void remove(ModelControllerClient client, CLIWrapper cli) throws Exception {
        Utils.applyUpdate(
                Util.createRemoveOperation(
                        PathAddress.pathAddress().append("subsystem", "elytron").append("regex-principal-transformer", name)),
                client);
    }

    /**
     * Creates builder to build {@link RegexPrincipalTransformer}.
     *
     * @return created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder to build {@link RegexPrincipalTransformer}.
     */
    public static final class Builder extends AbstractConfigurableElement.Builder<Builder> {

        private String pattern;
        private String replacement;
        private Boolean replaceAll;

        private Builder() {
        }

        public Builder withPattern(String pattern) {
            this.pattern = pattern;
            return this;
        }

        public Builder withReplacement(String replacement) {
            this.replacement = replacement;
            return this;
        }

        public Builder withReplaceAll(Boolean replaceAll) {
            this.replaceAll = replaceAll;
            return this;
        }

        public RegexPrincipalTransformer build() {
            return new RegexPrincipalTransformer(this);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }
}
