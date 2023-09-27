/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.util.Map;

import org.infinispan.xsite.XSiteAdminOperations;
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
public enum BackupOperation implements Operation<Map.Entry<String, XSiteAdminOperations>> {

    BRING_SITE_ONLINE("bring-site-online", false) {
        @Override
        public ModelNode execute(ExpressionResolver expressionResolver, ModelNode operation, Map.Entry<String, XSiteAdminOperations> context) {
            return new ModelNode(context.getValue().bringSiteOnline(context.getKey()));
        }
    },
    TAKE_SITE_OFFLINE("take-site-offline", false) {
        @Override
        public ModelNode execute(ExpressionResolver expressionResolver, ModelNode operation, Map.Entry<String, XSiteAdminOperations> context) {
            return new ModelNode(context.getValue().takeSiteOffline(context.getKey()));
        }
    },
    SITE_STATUS("site-status", true) {
        @Override
        public ModelNode execute(ExpressionResolver expressionResolver, ModelNode operation, Map.Entry<String, XSiteAdminOperations> context) {
            return new ModelNode(context.getValue().siteStatus(context.getKey()));
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
