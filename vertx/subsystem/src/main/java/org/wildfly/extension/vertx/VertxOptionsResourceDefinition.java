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

import static org.wildfly.extension.vertx.VertxConstants.*;
import static org.wildfly.extension.vertx.logging.VertxLogger.*;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

import io.vertx.core.VertxOptions;

/**
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
class VertxOptionsResourceDefinition extends AbstractVertxOptionsResourceDefinition {

  static VertxOptionsResourceDefinition INSTANCE = new VertxOptionsResourceDefinition();

  VertxOptionsResourceDefinition() {
    super(new SimpleResourceDefinition.Parameters(PathElement.pathElement(ELEMENT_VERTX_OPTION),
      VertxSubsystemExtension.getResourceDescriptionResolver(VertxSubsystemExtension.SUBSYSTEM_NAME, ELEMENT_VERTX_OPTION))
      .setAddHandler(new VertxOptionAddHandler())
      .setRemoveHandler(ReloadRequiredRemoveStepHandler.INSTANCE)
      .setCapabilities(VERTX_OPTIONS_CAPABILITY)
    );
  }

  @Override
  public Collection<AttributeDefinition> getAttributes() {
    return VertxOptionsAttributes.getVertxOptionsAttributes();
  }

  static class VertxOptionAddHandler extends AbstractAddStepHandler {

    VertxOptionAddHandler() {
        super(new Parameters().addAttribute(VertxOptionsAttributes.getVertxOptionsAttributes()));
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
      final String name = context.getCurrentAddressValue();
      if (DEFAULT_VERTX_OPTION_NAME.equals(name)) {
        throw VERTX_LOGGER.optionNameIsReserved(name);
      }
      VertxOptions vertxOptions = parseOptions(operation);
      NamedVertxOptions namedVertxOptions = new NamedVertxOptions(name, vertxOptions);
      NamedVertxOptionsService.installService(context, namedVertxOptions);
    }

    VertxOptions parseOptions(ModelNode operation) throws OperationFailedException {
      VertxOptions vertxOptions = new VertxOptions();
      if (operation.hasDefined(ATTR_EVENTLOOP_POOL_SIZE)) {
        vertxOptions.setEventLoopPoolSize(VertxOptionsAttributes.ATTR_EVENTLOOP_POOL_SIZE.validateOperation(operation).asInt());
      }
      if (operation.hasDefined(ATTR_WORKER_POOL_SIZE)) {
        vertxOptions.setWorkerPoolSize(VertxOptionsAttributes.ATTR_WORKER_POOL_SIZE.validateOperation(operation).asInt());
      }
      if (operation.hasDefined(ATTR_INTERNAL_BLOCKING_POOL_SIZE)) {
        vertxOptions.setInternalBlockingPoolSize(VertxOptionsAttributes.ATTR_INTERNAL_BLOCKING_POOL_SIZE.validateOperation(operation).asInt());
      }
      if (operation.hasDefined(ATTR_HA_ENABLED)) {
        vertxOptions.setHAEnabled(VertxOptionsAttributes.ATTR_HA_ENABLED.validateOperation(operation).asBoolean());
      }
      if (operation.hasDefined(ATTR_HA_GROUP)) {
        vertxOptions.setHAGroup(VertxOptionsAttributes.ATTR_HA_GROUP.validateOperation(operation).asString());
      }
      if (operation.hasDefined(ATTR_QUORUM_SIZE)) {
        vertxOptions.setQuorumSize(VertxOptionsAttributes.ATTR_QUORUM_SIZE.validateOperation(operation).asInt());
      }
      if (operation.hasDefined(ATTR_PREFER_NATIVE_TRANSPORT)) {
        vertxOptions.setPreferNativeTransport(VertxOptionsAttributes.ATTR_PREFER_NATIVE_TRANSPORT.validateOperation(operation).asBoolean());
      }
      if (operation.hasDefined(ATTR_DISABLE_TCCL)) {
        vertxOptions.setDisableTCCL(VertxOptionsAttributes.ATTR_DISABLE_TCCL.validateOperation(operation).asBoolean());
      }
      if (operation.hasDefined(ATTR_BLOCKED_THREAD_CHECK_INTERVAL)) {
        vertxOptions.setBlockedThreadCheckInterval(VertxOptionsAttributes.ATTR_BLOCKED_THREAD_CHECK_INTERVAL.validateOperation(operation).asLong());
      }
      if (operation.hasDefined(ATTR_BLOCKED_THREAD_CHECK_INTERVAL_UNIT)) {
        vertxOptions.setBlockedThreadCheckIntervalUnit(TimeUnit.valueOf(VertxOptionsAttributes.ATTR_BLOCKED_THREAD_CHECK_INTERVAL_UNIT.validateOperation(operation).asString()));
      }
      if (operation.hasDefined(ATTR_MAX_EVENTLOOP_EXECUTE_TIME)) {
        vertxOptions.setMaxEventLoopExecuteTime(VertxOptionsAttributes.ATTR_MAX_EVENTLOOP_EXECUTE_TIME.validateOperation(operation).asLong());
      }
      if (operation.hasDefined(ATTR_MAX_EVENTLOOP_EXECUTE_TIME_UNIT)) {
        vertxOptions.setMaxEventLoopExecuteTimeUnit(TimeUnit.valueOf(VertxOptionsAttributes.ATTR_MAX_EVENTLOOP_EXECUTE_TIME_UNIT.validateOperation(operation).asString()));
      }
      if (operation.hasDefined(ATTR_MAX_WORKER_EXECUTE_TIME)) {
        vertxOptions.setMaxWorkerExecuteTime(VertxOptionsAttributes.ATTR_MAX_WORKER_EXECUTE_TIME.validateOperation(operation).asLong());
      }
      if (operation.hasDefined(ATTR_MAX_WORKER_EXECUTE_TIME_UNIT)) {
        vertxOptions.setMaxWorkerExecuteTimeUnit(TimeUnit.valueOf(VertxOptionsAttributes.ATTR_MAX_WORKER_EXECUTE_TIME_UNIT.validateOperation(operation).asString()));
      }
      if (operation.hasDefined(ATTR_WARNING_EXECUTION_TIME)) {
        vertxOptions.setWarningExceptionTime(VertxOptionsAttributes.ATTR_WARNING_EXECUTION_TIME.validateOperation(operation).asLong());
      }
      if (operation.hasDefined(ATTR_WARNING_EXECUTION_TIME_UNIT)) {
        vertxOptions.setWarningExceptionTimeUnit(TimeUnit.valueOf(VertxOptionsAttributes.ATTR_WARNING_EXECUTION_TIME_UNIT.validateOperation(operation).asString()));
      }

      return vertxOptions;
    }

  }



}
