/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

import org.jboss.as.clustering.controller.Operation;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.dmr.ModelNode;

/**
 * Backup site operations.
 * @author Paul Ferraro
 */
public enum BackupSiteOperation implements Operation<BackupSiteOperationContext> {

    BRING_SITE_ONLINE(ModelKeys.BRING_SITE_ONLINE, false) {
        @Override
        public ModelNode getValue(BackupSiteOperationContext context) {
            return new ModelNode(context.getOperations().bringSiteOnline(context.getSite()));
        }
    },
    TAKE_SITE_OFFLINE(ModelKeys.TAKE_SITE_OFFLINE, false) {
        @Override
        public ModelNode getValue(BackupSiteOperationContext context) {
            return new ModelNode(context.getOperations().takeSiteOffline(context.getSite()));
        }
    },
    SITE_STATUS(ModelKeys.SITE_STATUS, true) {
        @Override
        public ModelNode getValue(BackupSiteOperationContext context) {
            return new ModelNode(context.getOperations().siteStatus(context.getSite()));
        }
    },
    ;

    private final OperationDefinition definition;

    private BackupSiteOperation(String name, boolean readOnly) {
        SimpleOperationDefinitionBuilder builder = new SimpleOperationDefinitionBuilder(name, InfinispanExtension.getResourceDescriptionResolver("backup.ops"));
        if (readOnly) {
            builder.setReadOnly();
        }
        this.definition = builder.setRuntimeOnly().build();
    }

    @Override
    public OperationDefinition getDefinition() {
        return this.definition;
    }
}
