/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.infinispan.group;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.spi.Formatter;
import org.wildfly.clustering.marshalling.spi.StringExternalizer;

/**
 * Resolver for a {@link LocalNode}.
 * @author Paul Ferraro
 */
@MetaInfServices(Formatter.class)
public class LocalNodeFormatter implements Formatter<LocalNode> {

    @Override
    public Class<LocalNode> getTargetClass() {
        return LocalNode.class;
    }

    @Override
    public LocalNode parse(String name) {
        return new LocalNode(name);
    }

    @Override
    public String format(LocalNode node) {
        return node.getName();
    }

    @MetaInfServices(Externalizer.class)
    public static class LocalNodeExternalizer extends StringExternalizer<LocalNode> {
        public LocalNodeExternalizer() {
            super(new LocalNodeFormatter());
        }
    }
}
