/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.server.group;

import java.util.function.Function;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.infinispan.spi.persistence.KeyFormat;
import org.wildfly.clustering.infinispan.spi.persistence.SimpleKeyFormat;
import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.spi.StringExternalizer;

/**
 * Resolver for a {@link LocalNode}.
 * @author Paul Ferraro
 */
public enum LocalNodeResolver implements Function<String, LocalNode> {
    INSTANCE;

    @Override
    public LocalNode apply(String name) {
        return new LocalNode(name);
    }

    static final Function<LocalNode, String> PARSER = LocalNode::getName;

    @MetaInfServices(Externalizer.class)
    public static class LocalNodeExternalizer extends StringExternalizer<LocalNode> {
        public LocalNodeExternalizer() {
            super(LocalNode.class, INSTANCE, PARSER);
        }
    }

    @MetaInfServices(KeyFormat.class)
    public static class LocalNodeKeyFormat extends SimpleKeyFormat<LocalNode> {
        public LocalNodeKeyFormat() {
            super(LocalNode.class, INSTANCE, PARSER);
        }
    }
}
