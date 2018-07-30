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
import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Backup site operations.
 * @author Paul Ferraro
 */
public enum BackupOperation implements Operation<BackupOperationContext> {

    BRING_SITE_ONLINE("bring-site-online", false) {
        @Override
        public ModelNode execute(ExpressionResolver expressionResolver, ModelNode operation, BackupOperationContext context) {
            return new ModelNode(context.getOperations().bringSiteOnline(context.getSite()));
        }
    },
    TAKE_SITE_OFFLINE("take-site-offline", false) {
        @Override
        public ModelNode execute(ExpressionResolver expressionResolver, ModelNode operation, BackupOperationContext context) {
            return new ModelNode(context.getOperations().takeSiteOffline(context.getSite()));
        }
    },
    SITE_STATUS("site-status", true) {
        @Override
        public ModelNode execute(ExpressionResolver expressionResolver, ModelNode operation, BackupOperationContext context) {
            return new ModelNode(context.getOperations().siteStatus(context.getSite()));
        }
    },
    ;
    private final OperationDefinition definition;

    BackupOperation(String name, boolean readOnly) {
        SimpleOperationDefinitionBuilder builder = new SimpleOperationDefinitionBuilder(name, InfinispanExtension.SUBSYSTEM_RESOLVER.createChildResolver(BackupResourceDefinition.WILDCARD_PATH));
        if (readOnly) {
            builder.setReadOnly();
        }
        this.definition = builder.setReplyType(ModelType.STRING).setRuntimeOnly().build();
    }

    @Override
    public OperationDefinition getDefinition() {
        return this.definition;
    }
}
