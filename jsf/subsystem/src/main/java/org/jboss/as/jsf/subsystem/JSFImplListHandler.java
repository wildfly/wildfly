/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jsf.subsystem;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.jsf.deployment.JSFModuleIdFactory;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Defines and handles the list-active-jsf-impls operation.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2012 Red Hat Inc.
 */
public class JSFImplListHandler implements OperationStepHandler {
    public static final String OPERATION_NAME = "list-active-jsf-impls";

    public static final OperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(OPERATION_NAME, JSFExtension.SUBSYSTEM_RESOLVER)
            .setRuntimeOnly()
            .setReadOnly()
            .setReplyType(ModelType.LIST)
            .setReplyValueType(ModelType.STRING)
            .build();

    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        ModelNode result = context.getResult();
        result.setEmptyList();
        for (String impl : JSFModuleIdFactory.getInstance().getActiveJSFVersions()) {
            result.add(impl);
        }
        context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
    }

}
