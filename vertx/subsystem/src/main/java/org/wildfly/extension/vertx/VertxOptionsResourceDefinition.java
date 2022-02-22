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
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.dns.AddressResolverOptions;
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

      // file system options
      if (operation.hasDefined(ATTR_FS_CLASS_PATH_RESOLVING_ENABLED)) {
        vertxOptions.getFileSystemOptions().setClassPathResolvingEnabled(VertxOptionsAttributes.ATTR_FS_CLASS_PATH_RESOLVING_ENABLED.validateOperation(operation).asBoolean());
      }
      if (operation.hasDefined(ATTR_FS_FILE_CACHE_ENABLED)) {
        vertxOptions.getFileSystemOptions().setFileCachingEnabled(VertxOptionsAttributes.ATTR_FS_FILE_CACHE_ENABLED.validateOperation(operation).asBoolean());
      }

      // address resolver options
      if (operation.hasDefined(ATTR_ADDRESS_RESOLVER)) {
        AddressResolverOptions addressResolverOptions = parseAddressResolverOptions(VertxOptionsAttributes.ATTR_ADDRESS_RESOLVER.validateOperation(operation));
        vertxOptions.setAddressResolverOptions(addressResolverOptions);
      }

      return vertxOptions;
    }

    private AddressResolverOptions parseAddressResolverOptions(ModelNode operation) throws OperationFailedException {
      AddressResolverOptions addressResolverOptions = new AddressResolverOptions();
      if (operation.hasDefined(ATTR_HOSTS_PATH)) {
        addressResolverOptions.setHostsPath(VertxOptionsAttributes.ATTR_HOSTS_PATH.validateOperation(operation).asString());
      }
      if (operation.hasDefined(ATTR_HOSTS_VALUE)) {
        addressResolverOptions.setHostsValue(Buffer.buffer(VertxOptionsAttributes.ATTR_HOSTS_VALUE.validateOperation(operation).asString()));
      }
      if (operation.hasDefined(ATTR_SERVERS)) {
        List<ModelNode> list = VertxOptionsAttributes.ATTR_SERVERS.validateOperation(operation).asList();
        addressResolverOptions.setServers(list.stream().map(ModelNode::asString).collect(Collectors.toList()));
      }
      if (operation.hasDefined(ATTR_OPT_RES_ENABLED)) {
        addressResolverOptions.setOptResourceEnabled(VertxOptionsAttributes.ATTR_OPT_RES_ENABLED.validateOperation(operation).asBoolean());
      }
      if (operation.hasDefined(ATTR_CACHE_MIN_TTL)) {
        addressResolverOptions.setCacheMinTimeToLive(VertxOptionsAttributes.ATTR_CACHE_MIN_TTL.validateOperation(operation).asInt());
      }
      if (operation.hasDefined(ATTR_MAX_TTL)) {
        addressResolverOptions.setCacheMaxTimeToLive(VertxOptionsAttributes.ATTR_MAX_TTL.validateOperation(operation).asInt());
      }
      if (operation.hasDefined(ATTR_NEGATIVE_TTL)) {
        addressResolverOptions.setCacheNegativeTimeToLive(VertxOptionsAttributes.ATTR_NEGATIVE_TTL.validateOperation(operation).asInt());
      }
      if (operation.hasDefined(ATTR_QUERY_TIMEOUT)) {
        addressResolverOptions.setQueryTimeout(VertxOptionsAttributes.ATTR_QUERY_TIMEOUT.validateOperation(operation).asLong());
      }
      if (operation.hasDefined(ATTR_MAX_QUERIES)) {
        addressResolverOptions.setMaxQueries(VertxOptionsAttributes.ATTR_MAX_QUERIES.validateOperation(operation).asInt());
      }
      if (operation.hasDefined(ATTR_RD_FLAG)) {
        addressResolverOptions.setRdFlag(VertxOptionsAttributes.ATTR_RD_FLAG.validateOperation(operation).asBoolean());
      }
      if (operation.hasDefined(ATTR_SEARCH_DOMAIN)) {
        List<ModelNode> list = VertxOptionsAttributes.ATTR_SEARCH_DOMAIN.validateOperation(operation).asList();
        addressResolverOptions.setSearchDomains(list.stream().map(ModelNode::asString).collect(Collectors.toList()));
      }
      if (operation.hasDefined(ATTR_N_DOTS)) {
        addressResolverOptions.setNdots(VertxOptionsAttributes.ATTR_N_DOTS.validateOperation(operation).asInt());
      }
      if (operation.hasDefined(ATTR_ROTATE_SERVERS)) {
        addressResolverOptions.setRotateServers(VertxOptionsAttributes.ATTR_ROTATE_SERVERS.validateOperation(operation).asBoolean());
      }
      if (operation.hasDefined(ATTR_ROUND_ROBIN_INET_ADDRESS)) {
        addressResolverOptions.setRoundRobinInetAddress(VertxOptionsAttributes.ATTR_ROUND_ROBIN_INET_ADDRESS.validateOperation(operation).asBoolean());
      }
      return addressResolverOptions;
    }

  }



}
