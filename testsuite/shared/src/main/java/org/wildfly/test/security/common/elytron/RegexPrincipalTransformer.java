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
 * A {@link ConfigurableElement} to define a regex principal transformer.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class RegexPrincipalTransformer implements ConfigurableElement {

    private final PathAddress address;
    private final String name;
    private final String pattern;
    private final String replacement;
    private final boolean replaceAll;


    RegexPrincipalTransformer(String name, String pattern, String replacement, boolean replaceAll) {
        this.name = name;
        this.address = PathAddress.pathAddress(PathElement.pathElement("subsystem", "elytron"), PathElement.pathElement("regex-principal-transformer", name));
        this.pattern = pattern;
        this.replacement = replacement;
        this.replaceAll = replaceAll;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void create(ModelControllerClient client, CLIWrapper cli) throws Exception {
        ModelNode addOperation = Util.createAddOperation(address);
        addOperation.get("pattern").set(pattern);
        addOperation.get("replacement").set(replacement);
        addOperation.get("replace-all").set(replaceAll);
        Utils.applyUpdate(addOperation, client);
    }

    @Override
    public void remove(ModelControllerClient client, CLIWrapper cli) throws Exception {
        ModelNode removeOperation = Util.createRemoveOperation(address);
        Utils.applyUpdate(removeOperation, client);
    }

    public static Builder builder(final String name) {
        return new Builder(name);
    }

    public static class Builder {

        private final String name;
        private String pattern;
        private String replacement;
        private boolean replaceAll;

        Builder(final String name) {
            this.name = name;
        }

        public Builder withPattern(final String pattern) {
            this.pattern = pattern;

            return this;
        }

        public Builder withReplacement(final String replacement) {
            this.replacement = replacement;

            return this;
        }

        public Builder replaceAll(final boolean replaceAll) {
            this.replaceAll = replaceAll;

            return this;
        }

        public RegexPrincipalTransformer build() {
            return new RegexPrincipalTransformer(name, pattern, replacement, replaceAll);
        }

    }

}
