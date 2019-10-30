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

import org.infinispan.commons.api.BasicCacheContainer;
import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.SimpleResourceRegistration;
import org.jboss.as.clustering.controller.SimpleAliasEntry;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Definition of the backup-for resource of a cache.
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 * @author Paul Ferraro
 */
@Deprecated
public class BackupForResourceDefinition extends ComponentResourceDefinition {

    static final PathElement PATH = pathElement("backup-for");
    static final PathElement LEGACY_PATH = PathElement.pathElement(PATH.getValue(), "BACKUP_FOR");

    enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        CACHE("remote-cache", ModelType.STRING, new ModelNode(BasicCacheContainer.DEFAULT_CACHE_NAME)),
        SITE("remote-site", ModelType.STRING, null),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, ModelNode defaultValue) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setRequired(false)
                    .setDefaultValue(defaultValue)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .setDeprecated(InfinispanModel.VERSION_6_0_0.getVersion())
                    .build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent) {
        if (InfinispanModel.VERSION_4_0_0.requiresTransformation(version)) {
            parent.addChildRedirection(PATH, LEGACY_PATH);
        }
    }

    BackupForResourceDefinition() {
        super(PATH);
        this.setDeprecated(InfinispanModel.VERSION_6_0_0.getVersion());
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubModel(this);
        parent.registerAlias(LEGACY_PATH, new SimpleAliasEntry(registration));

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver()).addAttributes(Attribute.class);
        new SimpleResourceRegistration(descriptor, null).register(registration);

        return registration;
    }
}
