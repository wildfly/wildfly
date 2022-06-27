/*
 *  Copyright (c) 2022 The original author or authors
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of Apache License v2.0 which
 *  accompanies this distribution.
 *
 *       The Apache License v2.0 is available at
 *       http://www.opensource.org/licenses/apache2.0.php
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
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
