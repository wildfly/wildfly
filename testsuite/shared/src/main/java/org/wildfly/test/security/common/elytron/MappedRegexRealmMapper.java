/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.security.common.elytron;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.dmr.ModelNode;

/**
 * A {@link ConfigurableElement} to create a mapped regex realm mapper resource.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class MappedRegexRealmMapper implements ConfigurableElement {

    private final PathAddress address;
    private final String name;
    private final String pattern;
    private final String delegateRealmMapper;
    private final Map<String, String> realmMapping;

    MappedRegexRealmMapper(final String name, final String pattern, final String delegateRealmMapper, final Map<String, String> realmMapping) {
        this.name = name;
        this.address = PathAddress.pathAddress(PathElement.pathElement("subsystem", "elytron"), PathElement.pathElement("mapped-regex-realm-mapper", name));
        this.pattern = pattern;
        this.delegateRealmMapper = delegateRealmMapper;
        this.realmMapping = realmMapping;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void create(ModelControllerClient client, CLIWrapper cli) throws Exception {
        ModelNode addOperation = Util.createAddOperation(address);
        addOperation.get("pattern").set(pattern);
        if (delegateRealmMapper != null) {
            addOperation.get("delegate-realm-mapper").set(delegateRealmMapper);
        }
        if (realmMapping.size() > 0) {
            ModelNode realmMapping = new ModelNode();
            for (Entry<String, String> entry : this.realmMapping.entrySet()) {
                realmMapping.get(entry.getKey()).set(entry.getValue());
            }
            addOperation.get("realm-map").set(realmMapping);
        }
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
        private String delegateRealmMapper;
        private Map<String, String> realmMapping = new HashMap<>();

        Builder(final String name) {
            this.name = name;
        }

        public Builder withPattern(final String pattern) {
            this.pattern = pattern;

            return this;
        }

        public Builder withDelegateRealmMapper(final String delegateRealmMapper) {
            this.delegateRealmMapper = delegateRealmMapper;

            return this;
        }

        public Builder withRealmMapping(final String from, final String to) {
            realmMapping.put(from, to);

            return this;
        }

        public MappedRegexRealmMapper build() {
            return new MappedRegexRealmMapper(name, pattern, delegateRealmMapper, realmMapping);
        }
    }
}
