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

import static org.wildfly.extension.vertx.VertxConstants.ATTR_BLOCKED_THREAD_CHECK_INTERVAL;
import static org.wildfly.extension.vertx.VertxConstants.ATTR_BLOCKED_THREAD_CHECK_INTERVAL_UNIT;
import static org.wildfly.extension.vertx.VertxConstants.ATTR_DISABLE_TCCL;
import static org.wildfly.extension.vertx.VertxConstants.ATTR_EVENTLOOP_POOL_SIZE;
import static org.wildfly.extension.vertx.VertxConstants.ATTR_FS_CLASS_PATH_RESOLVING_ENABLED;
import static org.wildfly.extension.vertx.VertxConstants.ATTR_FS_FILE_CACHE_ENABLED;
import static org.wildfly.extension.vertx.VertxConstants.ATTR_HA_ENABLED;
import static org.wildfly.extension.vertx.VertxConstants.ATTR_HA_GROUP;
import static org.wildfly.extension.vertx.VertxConstants.ATTR_INTERNAL_BLOCKING_POOL_SIZE;
import static org.wildfly.extension.vertx.VertxConstants.ATTR_MAX_EVENTLOOP_EXECUTE_TIME;
import static org.wildfly.extension.vertx.VertxConstants.ATTR_MAX_EVENTLOOP_EXECUTE_TIME_UNIT;
import static org.wildfly.extension.vertx.VertxConstants.ATTR_MAX_WORKER_EXECUTE_TIME;
import static org.wildfly.extension.vertx.VertxConstants.ATTR_MAX_WORKER_EXECUTE_TIME_UNIT;
import static org.wildfly.extension.vertx.VertxConstants.ATTR_PREFER_NATIVE_TRANSPORT;
import static org.wildfly.extension.vertx.VertxConstants.ATTR_QUORUM_SIZE;
import static org.wildfly.extension.vertx.VertxConstants.ATTR_WARNING_EXECUTION_TIME;
import static org.wildfly.extension.vertx.VertxConstants.ATTR_WARNING_EXECUTION_TIME_UNIT;
import static org.wildfly.extension.vertx.VertxConstants.ATTR_WORKER_POOL_SIZE;
import static org.wildfly.extension.vertx.VertxConstants.DEFAULT_VERTX_OPTION_NAME;
import static org.wildfly.extension.vertx.VertxConstants.ELEMENT_VERTX_EVENTBUS;
import static org.wildfly.extension.vertx.VertxConstants.ELEMENT_VERTX_OPTION;
import static org.wildfly.extension.vertx.VertxConstants.ELEMENT_VERTX_OPTION_ADDRESS_RESOLVER;
import static org.wildfly.extension.vertx.logging.VertxLogger.VERTX_LOGGER;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

import io.vertx.core.VertxOptions;

/**
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
class VertxOptionsResourceDefinition extends AbstractVertxOptionsResourceDefinition {

  static VertxOptionsResourceDefinition INSTANCE = new VertxOptionsResourceDefinition();

  private static final SimpleOperationDefinition SHOW_VERTX_OPTIONS_INFO = new SimpleOperationDefinitionBuilder("show-info",
    VertxSubsystemExtension.getResourceDescriptionResolver(VertxSubsystemExtension.SUBSYSTEM_NAME, ELEMENT_VERTX_OPTION))
    .setReplyType(ModelType.OBJECT)
    .setReplyValueType(ModelType.OBJECT)
    .build();

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

  @Override
  public void registerOperations(ManagementResourceRegistration resourceRegistration) {
    super.registerOperations(resourceRegistration);
    resourceRegistration.registerOperationHandler(SHOW_VERTX_OPTIONS_INFO, new ShowInfoHandler());
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

      String addressResolverOptionName = null;
      if (operation.hasDefined(ELEMENT_VERTX_OPTION_ADDRESS_RESOLVER)) {
        addressResolverOptionName = VertxOptionsAttributes.ATTR_VERTX_OPTION_ADDRESS_RESOLVER.validateOperation(operation).asString();
      }
      String eventBusName = null;
      if (operation.hasDefined(ELEMENT_VERTX_EVENTBUS)) {
        eventBusName = VertxOptionsAttributes.ATTR_EVENTBUS_OPTION.validateOperation(operation).asString();
      }
      NamedVertxOptionsService.installService(context, namedVertxOptions, addressResolverOptionName, eventBusName, null, null);
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

      // file system options
      if (operation.hasDefined(ATTR_FS_CLASS_PATH_RESOLVING_ENABLED)) {
        vertxOptions.getFileSystemOptions().setClassPathResolvingEnabled(VertxOptionsAttributes.ATTR_FS_CLASS_PATH_RESOLVING_ENABLED.validateOperation(operation).asBoolean());
      }
      if (operation.hasDefined(ATTR_FS_FILE_CACHE_ENABLED)) {
        vertxOptions.getFileSystemOptions().setFileCachingEnabled(VertxOptionsAttributes.ATTR_FS_FILE_CACHE_ENABLED.validateOperation(operation).asBoolean());
      }
      return vertxOptions;
    }

  }

  private static class ShowInfoHandler implements OperationStepHandler {

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
      ModelNode result = new ModelNode();
      final String name = context.getCurrentAddressValue();
      VertxOptions vertxOptions = VertxOptionsRegistry.getInstance().getNamedVertxOptions(name).getVertxOptions();
      result.set(ModelNode.fromJSONString(vertxOptions.toJson().encode()));
      context.getResult().set(result);
    }
  }
}
