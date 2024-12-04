/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.vertx;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.LongRangeValidator;
import org.jboss.dmr.ModelType;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
public abstract class VertxOptionsAttributes implements VertxConstants {

  public static final SimpleAttributeDefinition ATTR_EVENTLOOP_POOL_SIZE = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_EVENTLOOP_POOL_SIZE, ModelType.INT)
    .setRequired(false)
    .setAllowExpression(true)
    .setValidator(new IntRangeValidator(1,  true))
    .build();

  public static final SimpleAttributeDefinition ATTR_WORKER_POOL_SIZE = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_WORKER_POOL_SIZE, ModelType.INT)
    .setRequired(false)
    .setAllowExpression(true)
    .setValidator(new IntRangeValidator(1,  true))
    .build();

  public static final SimpleAttributeDefinition ATTR_INTERNAL_BLOCKING_POOL_SIZE = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_INTERNAL_BLOCKING_POOL_SIZE, ModelType.INT)
    .setRequired(false)
    .setAllowExpression(true)
    .setValidator(new IntRangeValidator(1,  true))
    .build();

  public static final SimpleAttributeDefinition ATTR_PREFER_NATIVE_TRANSPORT = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_PREFER_NATIVE_TRANSPORT, ModelType.BOOLEAN)
    .setRequired(false)
    .setAllowExpression(true)
    .build();

  public static final SimpleAttributeDefinition ATTR_BLOCKED_THREAD_CHECK_INTERVAL = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_BLOCKED_THREAD_CHECK_INTERVAL, ModelType.LONG)
    .setRequired(false)
    .setAllowExpression(true)
    .setValidator(new LongRangeValidator(1,  true))
    .build();

  public static final SimpleAttributeDefinition ATTR_BLOCKED_THREAD_CHECK_INTERVAL_UNIT = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_BLOCKED_THREAD_CHECK_INTERVAL_UNIT, ModelType.STRING)
    .setRequired(false)
    .setAllowExpression(true)
    .setAllowedValues(TIME_UNITS)
    .build();

  public static final SimpleAttributeDefinition ATTR_MAX_EVENTLOOP_EXECUTE_TIME = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_MAX_EVENTLOOP_EXECUTE_TIME, ModelType.LONG)
    .setRequired(false)
    .setAllowExpression(true)
    .setValidator(new LongRangeValidator(1,  true))
    .build();

  public static final SimpleAttributeDefinition ATTR_MAX_EVENTLOOP_EXECUTE_TIME_UNIT = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_MAX_EVENTLOOP_EXECUTE_TIME_UNIT, ModelType.STRING)
    .setRequired(false)
    .setAllowExpression(true)
    .setAllowedValues(TIME_UNITS)
    .build();

  public static final SimpleAttributeDefinition ATTR_MAX_WORKER_EXECUTE_TIME = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_MAX_WORKER_EXECUTE_TIME, ModelType.LONG)
    .setRequired(false)
    .setAllowExpression(true)
    .setValidator(new LongRangeValidator(1,  true))
    .build();

  public static final SimpleAttributeDefinition ATTR_MAX_WORKER_EXECUTE_TIME_UNIT = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_MAX_WORKER_EXECUTE_TIME_UNIT, ModelType.STRING)
    .setRequired(false)
    .setAllowExpression(true)
    .setAllowedValues(TIME_UNITS)
    .build();

  public static final SimpleAttributeDefinition ATTR_WARNING_EXECUTION_TIME = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_WARNING_EXECUTION_TIME, ModelType.LONG)
    .setRequired(false)
    .setAllowExpression(true)
    .setValidator(new LongRangeValidator(1,  true))
    .build();

  public static final SimpleAttributeDefinition ATTR_WARNING_EXECUTION_TIME_UNIT = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_WARNING_EXECUTION_TIME_UNIT, ModelType.STRING)
    .setRequired(false)
    .setAllowExpression(true)
    .setAllowedValues(TIME_UNITS)
    .build();

  public static final SimpleAttributeDefinition ATTR_FS_CLASS_PATH_RESOLVING_ENABLED = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_FS_CLASS_PATH_RESOLVING_ENABLED, ModelType.BOOLEAN)
    .setRequired(false)
    .setAllowExpression(true)
    .build();

  public static final SimpleAttributeDefinition ATTR_FS_FILE_CACHE_ENABLED = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_FS_FILE_CACHE_ENABLED, ModelType.BOOLEAN)
    .setRequired(false)
    .setAllowExpression(true)
    .build();

  public static final SimpleAttributeDefinition ATTR_FS_FILE_CACHE_DIR = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_FS_FILE_CACHE_DIR, ModelType.STRING)
          .setRequired(false)
          .setAllowExpression(true)
          .build();

  // address-resolver-option
  public static final SimpleAttributeDefinition ATTR_VERTX_OPTION_ADDRESS_RESOLVER = new SimpleAttributeDefinitionBuilder(VertxConstants.ELEMENT_VERTX_OPTION_ADDRESS_RESOLVER, ModelType.STRING)
    .setRequired(false)
    .setAllowExpression(true)
    .build();

  private static final List<AttributeDefinition> VERTX_OPTIONS_ATTRS = new ArrayList<>();
  static {
    VERTX_OPTIONS_ATTRS.add(ATTR_EVENTLOOP_POOL_SIZE);
    VERTX_OPTIONS_ATTRS.add(ATTR_WORKER_POOL_SIZE);
    VERTX_OPTIONS_ATTRS.add(ATTR_INTERNAL_BLOCKING_POOL_SIZE);
    VERTX_OPTIONS_ATTRS.add(ATTR_PREFER_NATIVE_TRANSPORT);
    VERTX_OPTIONS_ATTRS.add(ATTR_BLOCKED_THREAD_CHECK_INTERVAL);
    VERTX_OPTIONS_ATTRS.add(ATTR_BLOCKED_THREAD_CHECK_INTERVAL_UNIT);
    VERTX_OPTIONS_ATTRS.add(ATTR_MAX_EVENTLOOP_EXECUTE_TIME);
    VERTX_OPTIONS_ATTRS.add(ATTR_MAX_EVENTLOOP_EXECUTE_TIME_UNIT);
    VERTX_OPTIONS_ATTRS.add(ATTR_MAX_WORKER_EXECUTE_TIME);
    VERTX_OPTIONS_ATTRS.add(ATTR_MAX_WORKER_EXECUTE_TIME_UNIT);
    VERTX_OPTIONS_ATTRS.add(ATTR_WARNING_EXECUTION_TIME);
    VERTX_OPTIONS_ATTRS.add(ATTR_WARNING_EXECUTION_TIME_UNIT);

    // file system options
    VERTX_OPTIONS_ATTRS.add(ATTR_FS_CLASS_PATH_RESOLVING_ENABLED);
    VERTX_OPTIONS_ATTRS.add(ATTR_FS_FILE_CACHE_ENABLED);
    VERTX_OPTIONS_ATTRS.add(ATTR_FS_FILE_CACHE_DIR);

    // address-resolver-option
    VERTX_OPTIONS_ATTRS.add(ATTR_VERTX_OPTION_ADDRESS_RESOLVER);
  }

  /**
   * @return Attributes used in element like: /subsystem=vertx/vertx-option=vo
   */
  static List<AttributeDefinition> getVertxOptionsAttributes() {
    return VERTX_OPTIONS_ATTRS;
  }

}
