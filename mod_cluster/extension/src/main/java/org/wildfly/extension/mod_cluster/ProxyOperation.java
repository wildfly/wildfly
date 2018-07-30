/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.extension.mod_cluster;

import static org.wildfly.extension.mod_cluster.ProxyOperationExecutor.CONTEXT;
import static org.wildfly.extension.mod_cluster.ProxyOperationExecutor.HOST;
import static org.wildfly.extension.mod_cluster.ProxyOperationExecutor.PORT;
import static org.wildfly.extension.mod_cluster.ProxyOperationExecutor.VIRTUAL_HOST;
import static org.wildfly.extension.mod_cluster.ProxyOperationExecutor.WAIT_TIME;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.Operation;
import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.modcluster.ModClusterServiceMBean;

/**
 * Enumeration of mod_cluster proxy operations.
 *
 * @author Radoslav Husar
 */
enum ProxyOperation implements Operation<ModClusterServiceMBean>, UnaryOperator<SimpleOperationDefinitionBuilder> {
    ADD_PROXY("add-proxy") {
        @Override
        public SimpleOperationDefinitionBuilder apply(SimpleOperationDefinitionBuilder builder) {
            return builder.setParameters(HOST, PORT).addAccessConstraint(ModClusterExtension.MOD_CLUSTER_PROXIES_DEF);
        }

        @Override
        public ModelNode execute(ExpressionResolver expressionResolver, ModelNode operation, ModClusterServiceMBean service) throws OperationFailedException {
            String host = HOST.resolveModelAttribute(expressionResolver, operation).asString();
            int port = PORT.resolveModelAttribute(expressionResolver, operation).asInt();

            service.addProxy(host, port);
            return null;
        }
    },
    REMOVE_PROXY("remove-proxy") {
        @Override
        public SimpleOperationDefinitionBuilder apply(SimpleOperationDefinitionBuilder builder) {
            return builder.setParameters(HOST, PORT).addAccessConstraint(ModClusterExtension.MOD_CLUSTER_PROXIES_DEF);
        }

        @Override
        public ModelNode execute(ExpressionResolver expressionResolver, ModelNode operation, ModClusterServiceMBean service) throws OperationFailedException {
            String host = HOST.resolveModelAttribute(expressionResolver, operation).asString();
            int port = PORT.resolveModelAttribute(expressionResolver, operation).asInt();

            service.removeProxy(host, port);
            return null;
        }
    },
    READ_PROXIES_INFO("read-proxies-info") {
        @Override
        public SimpleOperationDefinitionBuilder apply(SimpleOperationDefinitionBuilder builder) {
            return builder.setReadOnly().setReplyType(ModelType.LIST).setReplyValueType(ModelType.STRING).addAccessConstraint(ModClusterExtension.MOD_CLUSTER_PROXIES_DEF);
        }

        @Override
        public ModelNode execute(ExpressionResolver expressionResolver, ModelNode operation, ModClusterServiceMBean service) {
            Map<InetSocketAddress, String> map = service.getProxyInfo();

            if (!map.isEmpty()) {
                final ModelNode result = new ModelNode();
                for (Map.Entry<InetSocketAddress, String> entry : map.entrySet()) {
                    result.add(entry.getKey().getHostName() + ":" + entry.getKey().getPort());
                    if (entry.getValue() == null) {
                        result.add();
                    } else {
                        result.add(entry.getValue());
                    }
                }
                return new ModelNode().set(result);
            }

            return null;
        }
    },
    LIST_PROXIES("list-proxies") {
        @Override
        public SimpleOperationDefinitionBuilder apply(SimpleOperationDefinitionBuilder builder) {
            return builder.setReadOnly().setReplyType(ModelType.LIST).setReplyValueType(ModelType.STRING).addAccessConstraint(ModClusterExtension.MOD_CLUSTER_PROXIES_DEF);
        }

        @Override
        public ModelNode execute(ExpressionResolver expressionResolver, ModelNode operation, ModClusterServiceMBean service) {
            Map<InetSocketAddress, String> map = service.getProxyInfo();

            if (!map.isEmpty()) {
                final ModelNode result = new ModelNode();
                InetSocketAddress[] addresses = map.keySet().toArray(new InetSocketAddress[0]);
                for (InetSocketAddress address : addresses) {
                    result.add(address.getHostName() + ":" + address.getPort());
                }
                return new ModelNode().set(result);
            }

            return null;
        }
    },
    READ_PROXIES_CONFIGURATION("read-proxies-configuration") {
        @Override
        public SimpleOperationDefinitionBuilder apply(SimpleOperationDefinitionBuilder builder) {
            return builder.setReadOnly().setReplyType(ModelType.LIST).setReplyValueType(ModelType.STRING).addAccessConstraint(ModClusterExtension.MOD_CLUSTER_PROXIES_DEF);
        }

        @Override
        public ModelNode execute(ExpressionResolver expressionResolver, ModelNode operation, ModClusterServiceMBean service) {
            Map<InetSocketAddress, String> map = service.getProxyConfiguration();

            if (!map.isEmpty()) {
                final ModelNode result = new ModelNode();
                for (Map.Entry<InetSocketAddress, String> entry : map.entrySet()) {
                    result.add(entry.getKey().getHostName() + ":" + entry.getKey().getPort());
                    if (entry.getValue() == null) {
                        result.add();
                    } else {
                        result.add(entry.getValue());
                    }
                }
                return new ModelNode().set(result);
            }

            return null;
        }
    },
    REFRESH("refresh") {
        @Override
        public ModelNode execute(ExpressionResolver expressionResolver, ModelNode operation, ModClusterServiceMBean service) {
            service.refresh();
            return null;
        }
    },
    RESET("reset") {
        @Override
        public ModelNode execute(ExpressionResolver expressionResolver, ModelNode operation, ModClusterServiceMBean service) {
            service.reset();
            return null;
        }
    },
    ENABLE("enable") {
        @Override
        public ModelNode execute(ExpressionResolver expressionResolver, ModelNode operation, ModClusterServiceMBean service) {
            boolean enabled = service.enable();

            return new ModelNode().get(ModelDescriptionConstants.RESULT).set(enabled);
        }
    },
    DISABLE("disable") {
        @Override
        public ModelNode execute(ExpressionResolver expressionResolver, ModelNode operation, ModClusterServiceMBean service) {
            boolean disabled = service.disable();

            return new ModelNode().get(ModelDescriptionConstants.RESULT).set(disabled);
        }
    },
    STOP("stop") {
        @Override
        public SimpleOperationDefinitionBuilder apply(SimpleOperationDefinitionBuilder builder) {
            return builder.setParameters(WAIT_TIME);
        }

        @Override
        public ModelNode execute(ExpressionResolver expressionResolver, ModelNode operation, ModClusterServiceMBean service) throws OperationFailedException {
            int waitTime = WAIT_TIME.resolveModelAttribute(expressionResolver, operation).asInt();

            boolean success = service.stop(waitTime, TimeUnit.SECONDS);
            return new ModelNode().get(ProxyOperationExecutor.SESSION_DRAINING_COMPLETE).set(success);
        }
    },

    // Context operations
    ENABLE_CONTEXT("enable-context") {
        @Override
        public SimpleOperationDefinitionBuilder apply(SimpleOperationDefinitionBuilder builder) {
            return builder.setParameters(VIRTUAL_HOST, CONTEXT);
        }

        @Override
        public ModelNode execute(ExpressionResolver expressionResolver, ModelNode operation, ModClusterServiceMBean service) throws OperationFailedException {
            String virtualHost = VIRTUAL_HOST.resolveModelAttribute(expressionResolver, operation).asString();
            String webContext = CONTEXT.resolveModelAttribute(expressionResolver, operation).asString();

            try {
                service.enableContext(virtualHost, webContext);
            } catch (IllegalArgumentException e) {
                throw new OperationFailedException(ModClusterLogger.ROOT_LOGGER.contextOrHostNotFound(virtualHost, webContext));
            }

            return null;
        }
    },
    DISABLE_CONTEXT("disable-context") {
        @Override
        public SimpleOperationDefinitionBuilder apply(SimpleOperationDefinitionBuilder builder) {
            return builder.setParameters(VIRTUAL_HOST, CONTEXT);
        }

        @Override
        public ModelNode execute(ExpressionResolver expressionResolver, ModelNode operation, ModClusterServiceMBean service) throws OperationFailedException {
            String virtualHost = VIRTUAL_HOST.resolveModelAttribute(expressionResolver, operation).asString();
            String webContext = CONTEXT.resolveModelAttribute(expressionResolver, operation).asString();

            try {
                boolean disabled = service.disableContext(virtualHost, webContext);
                return new ModelNode().get(ModelDescriptionConstants.RESULT).set(disabled);
            } catch (IllegalArgumentException e) {
                throw new OperationFailedException(ModClusterLogger.ROOT_LOGGER.contextOrHostNotFound(virtualHost, webContext));
            }
        }
    },
    STOP_CONTEXT("stop-context") {
        @Override
        public SimpleOperationDefinitionBuilder apply(SimpleOperationDefinitionBuilder builder) {
            return builder.setParameters(VIRTUAL_HOST, CONTEXT, WAIT_TIME);
        }

        @Override
        public ModelNode execute(ExpressionResolver expressionResolver, ModelNode operation, ModClusterServiceMBean service) throws OperationFailedException {
            String virtualHost = VIRTUAL_HOST.resolveModelAttribute(expressionResolver, operation).asString();
            String webContext = CONTEXT.resolveModelAttribute(expressionResolver, operation).asString();
            int waitTime = WAIT_TIME.resolveModelAttribute(expressionResolver, operation).asInt();

            try {
                boolean success = service.stopContext(virtualHost, webContext, waitTime, TimeUnit.SECONDS);
                return new ModelNode().get(ProxyOperationExecutor.SESSION_DRAINING_COMPLETE).set(success);
            } catch (IllegalArgumentException e) {
                throw new OperationFailedException(ModClusterLogger.ROOT_LOGGER.contextOrHostNotFound(virtualHost, webContext));
            }
        }
    },
    ;

    private final OperationDefinition definition;

    ProxyOperation(String name) {
        this.definition = this.apply(new SimpleOperationDefinitionBuilder(name, ModClusterExtension.SUBSYSTEM_RESOLVER)).setRuntimeOnly().build();
    }

    @Override
    public OperationDefinition getDefinition() {
        return this.definition;
    }

    @Override
    public SimpleOperationDefinitionBuilder apply(SimpleOperationDefinitionBuilder builder) {
        return builder;
    }
}
