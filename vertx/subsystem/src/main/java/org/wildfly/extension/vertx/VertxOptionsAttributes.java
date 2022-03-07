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

import static org.wildfly.extension.vertx.AddressResolverResourceDefinition.VERTX_OPTIONS_ADDRESS_RESOLVER_CAPABILITY;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.LongRangeValidator;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
public abstract class VertxOptionsAttributes implements VertxConstants {

  public static final SimpleAttributeDefinition VERTX_OPTION_FILE_PATH = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_PATH, ModelType.STRING)
    .setRequired(true)
    .setAllowExpression(true)
    .setRestartAllServices()
    .build();

  private static final List<AttributeDefinition> VERTX_OPTIONS_FILE_ATTRS = new ArrayList<>();
  static {
    VERTX_OPTIONS_FILE_ATTRS.add(VERTX_OPTION_FILE_PATH);
  }

  /**
   * @return Attributes used in element like: /subsystem=vertx/vertx-options-file=vof
   */
  static List<AttributeDefinition> getVertxOptionsFileAttributes() {
    return VERTX_OPTIONS_FILE_ATTRS;
  }

  public static final SimpleAttributeDefinition ATTR_EVENTLOOP_POOL_SIZE = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_EVENTLOOP_POOL_SIZE, ModelType.INT)
    .setRequired(false)
    .setAllowExpression(true)
    .setValidator(new IntRangeValidator(1,  true))
    .setRestartAllServices()
    .build();

  public static final SimpleAttributeDefinition ATTR_WORKER_POOL_SIZE = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_WORKER_POOL_SIZE, ModelType.INT)
    .setRequired(false)
    .setAllowExpression(true)
    .setValidator(new IntRangeValidator(1,  true))
    .setRestartAllServices()
    .build();

  public static final SimpleAttributeDefinition ATTR_INTERNAL_BLOCKING_POOL_SIZE = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_INTERNAL_BLOCKING_POOL_SIZE, ModelType.INT)
    .setRequired(false)
    .setAllowExpression(true)
    .setValidator(new IntRangeValidator(1,  true))
    .setRestartAllServices()
    .build();

  public static final SimpleAttributeDefinition ATTR_HA_ENABLED = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_HA_ENABLED, ModelType.BOOLEAN)
    .setRequired(false)
    .setAllowExpression(true)
    .setRestartAllServices()
    .build();

  public static final SimpleAttributeDefinition ATTR_HA_GROUP = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_HA_GROUP, ModelType.STRING)
    .setRequired(false)
    .setAllowExpression(true)
    .setRestartAllServices()
    .build();

  public static final SimpleAttributeDefinition ATTR_QUORUM_SIZE = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_QUORUM_SIZE, ModelType.INT)
    .setRequired(false)
    .setAllowExpression(true)
    .setValidator(new IntRangeValidator(1,  true))
    .setRestartAllServices()
    .build();

  public static final SimpleAttributeDefinition ATTR_PREFER_NATIVE_TRANSPORT = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_PREFER_NATIVE_TRANSPORT, ModelType.BOOLEAN)
    .setRequired(false)
    .setAllowExpression(true)
    .setRestartAllServices()
    .build();

  public static final SimpleAttributeDefinition ATTR_DISABLE_TCCL = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_DISABLE_TCCL, ModelType.BOOLEAN)
    .setRequired(false)
    .setAllowExpression(true)
    .setRestartAllServices()
    .build();

  public static final SimpleAttributeDefinition ATTR_BLOCKED_THREAD_CHECK_INTERVAL = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_BLOCKED_THREAD_CHECK_INTERVAL, ModelType.LONG)
    .setRequired(false)
    .setAllowExpression(true)
    .setValidator(new LongRangeValidator(1,  true))
    .setRestartAllServices()
    .build();

  public static final SimpleAttributeDefinition ATTR_BLOCKED_THREAD_CHECK_INTERVAL_UNIT = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_BLOCKED_THREAD_CHECK_INTERVAL_UNIT, ModelType.STRING)
    .setRequired(false)
    .setAllowExpression(true)
    .setAllowedValues(TIME_UNITS)
    .setRestartAllServices()
    .build();

  public static final SimpleAttributeDefinition ATTR_MAX_EVENTLOOP_EXECUTE_TIME = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_MAX_EVENTLOOP_EXECUTE_TIME, ModelType.LONG)
    .setRequired(false)
    .setAllowExpression(true)
    .setValidator(new LongRangeValidator(1,  true))
    .setRestartAllServices()
    .build();

  public static final SimpleAttributeDefinition ATTR_MAX_EVENTLOOP_EXECUTE_TIME_UNIT = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_MAX_EVENTLOOP_EXECUTE_TIME_UNIT, ModelType.STRING)
    .setRequired(false)
    .setAllowExpression(true)
    .setAllowedValues(TIME_UNITS)
    .setRestartAllServices()
    .build();

  public static final SimpleAttributeDefinition ATTR_MAX_WORKER_EXECUTE_TIME = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_MAX_WORKER_EXECUTE_TIME, ModelType.LONG)
    .setRequired(false)
    .setAllowExpression(true)
    .setValidator(new LongRangeValidator(1,  true))
    .setRestartAllServices()
    .build();

  public static final SimpleAttributeDefinition ATTR_MAX_WORKER_EXECUTE_TIME_UNIT = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_MAX_WORKER_EXECUTE_TIME_UNIT, ModelType.STRING)
    .setRequired(false)
    .setAllowExpression(true)
    .setAllowedValues(TIME_UNITS)
    .setRestartAllServices()
    .build();

  public static final SimpleAttributeDefinition ATTR_WARNING_EXECUTION_TIME = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_WARNING_EXECUTION_TIME, ModelType.LONG)
    .setRequired(false)
    .setAllowExpression(true)
    .setValidator(new LongRangeValidator(1,  true))
    .setRestartAllServices()
    .build();

  public static final SimpleAttributeDefinition ATTR_WARNING_EXECUTION_TIME_UNIT = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_WARNING_EXECUTION_TIME_UNIT, ModelType.STRING)
    .setRequired(false)
    .setAllowExpression(true)
    .setAllowedValues(TIME_UNITS)
    .setRestartAllServices()
    .build();

  public static final SimpleAttributeDefinition ATTR_FS_CLASS_PATH_RESOLVING_ENABLED = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_FS_CLASS_PATH_RESOLVING_ENABLED, ModelType.BOOLEAN)
    .setRequired(false)
    .setAllowExpression(true)
    .setRestartAllServices()
    .build();

  public static final SimpleAttributeDefinition ATTR_FS_FILE_CACHE_ENABLED = new SimpleAttributeDefinitionBuilder(VertxConstants.ATTR_FS_FILE_CACHE_ENABLED, ModelType.BOOLEAN)
    .setRequired(false)
    .setAllowExpression(true)
    .setRestartAllServices()
    .build();

  // address-resolver-option
  public static final SimpleAttributeDefinition ATTR_VERTX_OPTION_ADDRESS_RESOLVER = new SimpleAttributeDefinitionBuilder(VertxConstants.ELEMENT_VERTX_OPTION_ADDRESS_RESOLVER, ModelType.STRING)
    .setRequired(false)
    .setAllowExpression(true)
    .setCapabilityReference(VERTX_OPTIONS_ADDRESS_RESOLVER_CAPABILITY.getName())
    .setRestartAllServices()
    .build();

  // eventbus-option
  public static final SimpleAttributeDefinition ATTR_EVENTBUS_OPTION = new SimpleAttributeDefinitionBuilder(VertxConstants.ELEMENT_VERTX_EVENTBUS, ModelType.STRING)
    .setRequired(false)
    .setAllowExpression(true)
    .setCapabilityReference(EventBusResourceDefinition.VERTX_EVENT_BUS_OPTIONS_CAPABILITY.getName())
    .setRestartAllServices()
    .build();

  private static final List<AttributeDefinition> VERTX_OPTIONS_ATTRS = new ArrayList<>();
  static {
    VERTX_OPTIONS_ATTRS.add(ATTR_EVENTLOOP_POOL_SIZE);
    VERTX_OPTIONS_ATTRS.add(ATTR_WORKER_POOL_SIZE);
    VERTX_OPTIONS_ATTRS.add(ATTR_INTERNAL_BLOCKING_POOL_SIZE);
    VERTX_OPTIONS_ATTRS.add(ATTR_HA_ENABLED);
    VERTX_OPTIONS_ATTRS.add(ATTR_HA_GROUP);
    VERTX_OPTIONS_ATTRS.add(ATTR_QUORUM_SIZE);
    VERTX_OPTIONS_ATTRS.add(ATTR_PREFER_NATIVE_TRANSPORT);
    VERTX_OPTIONS_ATTRS.add(ATTR_DISABLE_TCCL);
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

    // address-resolver-option
    VERTX_OPTIONS_ATTRS.add(ATTR_VERTX_OPTION_ADDRESS_RESOLVER);

    // evnetbus-option
    VERTX_OPTIONS_ATTRS.add(ATTR_EVENTBUS_OPTION);

  }

  /**
   * @return Attributes used in element like: /subsystem=vertx/vertx-option=vo
   */
  static List<AttributeDefinition> getVertxOptionsAttributes() {
    return VERTX_OPTIONS_ATTRS;
  }

}
