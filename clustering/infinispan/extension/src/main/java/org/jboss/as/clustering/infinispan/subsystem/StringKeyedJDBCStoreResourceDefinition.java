/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.SimpleAliasEntry;
import org.jboss.as.controller.PathElement;

/**
 * Resource description for the addressable resource and its legacy alias
 *
 * /subsystem=infinispan/cache-container=X/cache=Y/store=string-jdbc
 * /subsystem=infinispan/cache-container=X/cache=Y/string-keyed-jdbc-store=STRING_KEYED_JDBC_STORE
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class StringKeyedJDBCStoreResourceDefinition extends JDBCStoreResourceDefinition {

    static final PathElement STRING_JDBC_PATH = pathElement("string-jdbc");
    static final PathElement PATH = JDBCStoreResourceDefinition.PATH;

    static class ResourceDescriptorConfigurator implements UnaryOperator<ResourceDescriptor> {
        @Override
        public ResourceDescriptor apply(ResourceDescriptor descriptor) {
            return descriptor.addRequiredChildren(StringTableResourceDefinition.PATH);
        }
    }

    StringKeyedJDBCStoreResourceDefinition() {
        super(PATH, InfinispanExtension.SUBSYSTEM_RESOLVER.createChildResolver(PATH, WILDCARD_PATH), new ResourceDescriptorConfigurator());
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = super.register(parent);

        parent.registerAlias(STRING_JDBC_PATH, new SimpleAliasEntry(registration));

        new StringTableResourceDefinition().register(registration);

        return registration;
    }
}
