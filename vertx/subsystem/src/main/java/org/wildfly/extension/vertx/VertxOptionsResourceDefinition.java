/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.vertx;

import io.vertx.core.VertxOptions;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

import java.util.concurrent.TimeUnit;

import static org.wildfly.extension.vertx.VertxConstants.ATTR_BLOCKED_THREAD_CHECK_INTERVAL;
import static org.wildfly.extension.vertx.VertxConstants.ATTR_BLOCKED_THREAD_CHECK_INTERVAL_UNIT;
import static org.wildfly.extension.vertx.VertxConstants.ATTR_EVENT_LOOP_POOL_SIZE;
import static org.wildfly.extension.vertx.VertxConstants.ATTR_FS_CLASS_PATH_RESOLVING_ENABLED;
import static org.wildfly.extension.vertx.VertxConstants.ATTR_FS_FILE_CACHE_DIR;
import static org.wildfly.extension.vertx.VertxConstants.ATTR_FS_FILE_CACHE_ENABLED;
import static org.wildfly.extension.vertx.VertxConstants.ATTR_INTERNAL_BLOCKING_POOL_SIZE;
import static org.wildfly.extension.vertx.VertxConstants.ATTR_MAX_EVENT_LOOP_EXECUTE_TIME;
import static org.wildfly.extension.vertx.VertxConstants.ATTR_MAX_EVENT_LOOP_EXECUTE_TIME_UNIT;
import static org.wildfly.extension.vertx.VertxConstants.ATTR_MAX_WORKER_EXECUTE_TIME;
import static org.wildfly.extension.vertx.VertxConstants.ATTR_MAX_WORKER_EXECUTE_TIME_UNIT;
import static org.wildfly.extension.vertx.VertxConstants.ATTR_PREFER_NATIVE_TRANSPORT;
import static org.wildfly.extension.vertx.VertxConstants.ATTR_WARNING_EXECUTION_TIME;
import static org.wildfly.extension.vertx.VertxConstants.ATTR_WARNING_EXECUTION_TIME_UNIT;
import static org.wildfly.extension.vertx.VertxConstants.ATTR_WORKER_POOL_SIZE;
import static org.wildfly.extension.vertx.VertxConstants.ELEMENT_VERTX_OPTION;

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
      .setRemoveHandler(new VertxOptionRemoveHandler())
      .setCapabilities(VERTX_OPTIONS_CAPABILITY)
    );
  }

  @Override
  public void registerOperations(ManagementResourceRegistration resourceRegistration) {
    super.registerOperations(resourceRegistration);
    resourceRegistration.registerOperationHandler(SHOW_VERTX_OPTIONS_INFO, new ShowInfoHandler());
  }

  @Override
  public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
    super.registerAttributes(resourceRegistration);
    AttrWriteHandler handler = new AttrWriteHandler();
    for (AttributeDefinition attr : VertxOptionsAttributes.getVertxOptionsAttributes()) {
      resourceRegistration.registerReadWriteAttribute(attr, null, handler);
    }
  }

  static class VertxOptionAddHandler extends AbstractAddStepHandler {

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
      NamedVertxOptionsService.installVertxOptionsService(context, operation);
    }
  }

  static VertxOptions parseOptions(ModelNode operation) throws OperationFailedException {
    VertxOptions vertxOptions = new VertxOptions();
    if (operation.hasDefined(ATTR_EVENT_LOOP_POOL_SIZE)) {
      vertxOptions.setEventLoopPoolSize(VertxOptionsAttributes.ATTR_EVENT_LOOP_POOL_SIZE.validateOperation(operation).asInt());
    }
    if (operation.hasDefined(ATTR_WORKER_POOL_SIZE)) {
      vertxOptions.setWorkerPoolSize(VertxOptionsAttributes.ATTR_WORKER_POOL_SIZE.validateOperation(operation).asInt());
    }
    if (operation.hasDefined(ATTR_INTERNAL_BLOCKING_POOL_SIZE)) {
      vertxOptions.setInternalBlockingPoolSize(VertxOptionsAttributes.ATTR_INTERNAL_BLOCKING_POOL_SIZE.validateOperation(operation).asInt());
    }
    if (operation.hasDefined(ATTR_PREFER_NATIVE_TRANSPORT)) {
      vertxOptions.setPreferNativeTransport(VertxOptionsAttributes.ATTR_PREFER_NATIVE_TRANSPORT.validateOperation(operation).asBoolean());
    }
    if (operation.hasDefined(ATTR_BLOCKED_THREAD_CHECK_INTERVAL)) {
      vertxOptions.setBlockedThreadCheckInterval(VertxOptionsAttributes.ATTR_BLOCKED_THREAD_CHECK_INTERVAL.validateOperation(operation).asLong());
    }
    if (operation.hasDefined(ATTR_BLOCKED_THREAD_CHECK_INTERVAL_UNIT)) {
      vertxOptions.setBlockedThreadCheckIntervalUnit(TimeUnit.valueOf(VertxOptionsAttributes.ATTR_BLOCKED_THREAD_CHECK_INTERVAL_UNIT.validateOperation(operation).asString()));
    }
    if (operation.hasDefined(ATTR_MAX_EVENT_LOOP_EXECUTE_TIME)) {
      vertxOptions.setMaxEventLoopExecuteTime(VertxOptionsAttributes.ATTR_MAX_EVENT_LOOP_EXECUTE_TIME.validateOperation(operation).asLong());
    }
    if (operation.hasDefined(ATTR_MAX_EVENT_LOOP_EXECUTE_TIME_UNIT)) {
      vertxOptions.setMaxEventLoopExecuteTimeUnit(TimeUnit.valueOf(VertxOptionsAttributes.ATTR_MAX_EVENT_LOOP_EXECUTE_TIME_UNIT.validateOperation(operation).asString()));
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
    if (operation.hasDefined(ATTR_FS_FILE_CACHE_DIR)) {
      vertxOptions.getFileSystemOptions().setFileCacheDir(VertxOptionsAttributes.ATTR_FS_FILE_CACHE_DIR.validateOperation(operation).asString());
    }
    return vertxOptions;
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
