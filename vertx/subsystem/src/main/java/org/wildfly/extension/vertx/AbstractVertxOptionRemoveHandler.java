/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.vertx;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;

/**
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
public abstract class AbstractVertxOptionRemoveHandler extends AbstractRemoveStepHandler {

  protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
    boolean reloadRequired = isOptionUsedInRuntime(context);
    doPerform(context, operation, model);
    if (reloadRequired) {
      context.reloadRequired();
    }
  }

  protected abstract boolean isOptionUsedInRuntime(OperationContext context);

  protected void doPerform(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {}

}
