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

import static org.wildfly.extension.mod_cluster.ModClusterLogger.ROOT_LOGGER;
import static org.wildfly.extension.mod_cluster.ProxyOperationExecutor.CONTEXT;
import static org.wildfly.extension.mod_cluster.ProxyOperationExecutor.HOST;
import static org.wildfly.extension.mod_cluster.ProxyOperationExecutor.PORT;
import static org.wildfly.extension.mod_cluster.ProxyOperationExecutor.VIRTUAL_HOST;
import static org.wildfly.extension.mod_cluster.ProxyOperationExecutor.WAIT_TIME;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.jboss.as.clustering.controller.Definable;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.modcluster.ModClusterServiceMBean;

/**
 * These are legacy operations suffering from multiple issues such as WFLY-10442, WFLY-10445, WFLY-10444, WFLY-10441, etc.
 * Operations are registered to support legacy workflow, thus, they can only be used when a single proxy configuration is
 * defined.
 * The class in its entirety is designed to be removed (plus couple of line for registration) once deprecated long enough.
 * Deprecated since model version 6.0.0.
 *
 * @author Radoslav Husar
 */
@Deprecated
public enum LegacyProxyOperation implements Definable<OperationDefinition> {
    ADD_PROXY {
        @Override
        public OperationDefinition getDefinition() {
            return new SimpleOperationDefinitionBuilder(ProxyOperation.ADD_PROXY.getName(), ModClusterExtension.SUBSYSTEM_RESOLVER)
                    .addParameter(HOST)
                    .addParameter(PORT)
                    .setRuntimeOnly()
                    .addAccessConstraint(ModClusterExtension.MOD_CLUSTER_PROXIES_DEF)
                    .setDeprecated(ModClusterModel.VERSION_6_0_0.getVersion())
                    .build();
        }

        @Override
        void execute(OperationContext context, ModelNode operation, ModClusterServiceMBean service) throws OperationFailedException {
            ROOT_LOGGER.debugf("add-proxy: %s", operation);

            String host = HOST.resolveModelAttribute(context, operation).asString();
            int port = PORT.resolveModelAttribute(context, operation).asInt();

            // Keeping this test here to maintain same behavior as previous versions.
            try {
                InetAddress.getByName(host);
            } catch (UnknownHostException e) {
                throw new OperationFailedException(ROOT_LOGGER.couldNotResolveProxyIpAddress(), e);
            }

            service.addProxy(host, port);

            context.completeStep(new OperationContext.RollbackHandler() {
                @Override
                public void handleRollback(OperationContext context, ModelNode operation) {
                    service.removeProxy(host, port);
                }
            });
        }
    },
    DISABLE {
        @Override
        public OperationDefinition getDefinition() {
            return new SimpleOperationDefinitionBuilder(ProxyOperation.DISABLE.getName(), ModClusterExtension.SUBSYSTEM_RESOLVER)
                    .setRuntimeOnly()
                    .setDeprecated(ModClusterModel.VERSION_6_0_0.getVersion())
                    .build();
        }

        @Override
        void execute(OperationContext context, ModelNode operation, ModClusterServiceMBean service) throws OperationFailedException {
            service.disable();

            context.completeStep(new OperationContext.RollbackHandler() {
                @Override
                public void handleRollback(OperationContext context, ModelNode operation) {
                    service.enable();
                }
            });
        }
    },
    DISABLE_CONTEXT {
        @Override
        public OperationDefinition getDefinition() {
            return new SimpleOperationDefinitionBuilder(ProxyOperation.DISABLE_CONTEXT.getName(), ModClusterExtension.SUBSYSTEM_RESOLVER)
                    .addParameter(VIRTUAL_HOST)
                    .addParameter(CONTEXT)
                    .setRuntimeOnly()
                    .setDeprecated(ModClusterModel.VERSION_6_0_0.getVersion())
                    .build();
        }

        @Override
        void execute(OperationContext context, ModelNode operation, ModClusterServiceMBean service) throws OperationFailedException {
            ROOT_LOGGER.debugf("disable-context: %s", operation);

            String webHost = VIRTUAL_HOST.resolveModelAttribute(context, operation).asString();
            String webContext = CONTEXT.resolveModelAttribute(context, operation).asString();

            try {
                service.disableContext(webHost, webContext);
            } catch (IllegalArgumentException e) {
                throw new OperationFailedException(ModClusterLogger.ROOT_LOGGER.contextOrHostNotFound(webHost, webContext));
            }

            context.completeStep(new OperationContext.RollbackHandler() {
                @Override
                public void handleRollback(OperationContext context, ModelNode operation) {
                    service.enableContext(webHost, webContext);
                }
            });
        }
    },
    ENABLE {
        @Override
        public OperationDefinition getDefinition() {
            return new SimpleOperationDefinitionBuilder(ProxyOperation.ENABLE.getName(), ModClusterExtension.SUBSYSTEM_RESOLVER)
                    .setRuntimeOnly()
                    .setDeprecated(ModClusterModel.VERSION_6_0_0.getVersion())
                    .build();
        }

        @Override
        void execute(OperationContext context, ModelNode operation, ModClusterServiceMBean service) throws OperationFailedException {
            service.enable();

            context.completeStep(new OperationContext.RollbackHandler() {
                @Override
                public void handleRollback(OperationContext context, ModelNode operation) {
                    service.disable();
                }
            });
        }
    },
    ENABLE_CONTEXT {
        @Override
        public OperationDefinition getDefinition() {
            return new SimpleOperationDefinitionBuilder(ProxyOperation.ENABLE_CONTEXT.getName(), ModClusterExtension.SUBSYSTEM_RESOLVER)
                    .addParameter(VIRTUAL_HOST)
                    .addParameter(CONTEXT)
                    .setRuntimeOnly()
                    .setDeprecated(ModClusterModel.VERSION_6_0_0.getVersion())
                    .build();
        }

        @Override
        void execute(OperationContext context, ModelNode operation, ModClusterServiceMBean service) throws OperationFailedException {
            ROOT_LOGGER.debugf("enable-context: %s", operation);

            String webHost = VIRTUAL_HOST.resolveModelAttribute(context, operation).asString();
            String webContext = CONTEXT.resolveModelAttribute(context, operation).asString();

            try {
                service.enableContext(webHost, webContext);
            } catch (IllegalArgumentException e) {
                throw new OperationFailedException(ModClusterLogger.ROOT_LOGGER.contextOrHostNotFound(webHost, webContext));
            }

            context.completeStep(new OperationContext.RollbackHandler() {
                @Override
                public void handleRollback(OperationContext context, ModelNode operation) {
                    service.disableContext(webHost, webContext);
                }
            });
        }
    },
    LIST_PROXIES {
        @Override
        public OperationDefinition getDefinition() {
            return new SimpleOperationDefinitionBuilder(ProxyOperation.LIST_PROXIES.getName(), ModClusterExtension.SUBSYSTEM_RESOLVER)
                    .setReadOnly()
                    .setRuntimeOnly()
                    .setReplyType(ModelType.LIST)
                    .setReplyValueType(ModelType.STRING)
                    .addAccessConstraint(ModClusterExtension.MOD_CLUSTER_PROXIES_DEF)
                    .setDeprecated(ModClusterModel.VERSION_6_0_0.getVersion())
                    .build();
        }

        @Override
        void execute(OperationContext context, ModelNode operation, ModClusterServiceMBean service) throws OperationFailedException {
            Map<InetSocketAddress, String> map = service.getProxyInfo();
            ROOT_LOGGER.debugf("Mod_cluster ListProxies %s", map);
            if (!map.isEmpty()) {
                ModelNode result = new ModelNode();
                InetSocketAddress[] addr = map.keySet().toArray(new InetSocketAddress[map.size()]);
                for (InetSocketAddress address : addr) {
                    result.add(address.getHostName() + ":" + address.getPort());
                }
                context.getResult().set(result);
            }
        }
    },
    READ_PROXIES_CONFIGURATION {
        @Override
        public OperationDefinition getDefinition() {
            return new SimpleOperationDefinitionBuilder(ProxyOperation.READ_PROXIES_CONFIGURATION.getName(), ModClusterExtension.SUBSYSTEM_RESOLVER)
                    .setReadOnly()
                    .setRuntimeOnly()
                    .setReplyType(ModelType.LIST)
                    .setReplyValueType(ModelType.STRING)
                    .addAccessConstraint(ModClusterExtension.MOD_CLUSTER_PROXIES_DEF)
                    .setDeprecated(ModClusterModel.VERSION_6_0_0.getVersion())
                    .build();
        }

        @Override
        void execute(OperationContext context, ModelNode operation, ModClusterServiceMBean service) throws OperationFailedException {
            Map<InetSocketAddress, String> map = service.getProxyConfiguration();
            ROOT_LOGGER.debugf("Mod_cluster ProxyConfiguration %s", map);
            if (!map.isEmpty()) {
                ModelNode result = new ModelNode();
                for (Map.Entry<InetSocketAddress, String> entry : map.entrySet()) {
                    result.add(entry.getKey().getHostName() + ":" + entry.getKey().getPort());
                    if (entry.getValue() == null) {
                        result.add();
                    } else {
                        result.add(entry.getValue());
                    }
                }
                context.getResult().set(result);
            }
        }
    },
    READ_PROXIES_INFO {
        @Override
        public OperationDefinition getDefinition() {
            return new SimpleOperationDefinitionBuilder(ProxyOperation.READ_PROXIES_INFO.getName(), ModClusterExtension.SUBSYSTEM_RESOLVER)
                    .setReadOnly()
                    .setRuntimeOnly()
                    .setReplyType(ModelType.LIST)
                    .setReplyValueType(ModelType.STRING)
                    .addAccessConstraint(ModClusterExtension.MOD_CLUSTER_PROXIES_DEF)
                    .setDeprecated(ModClusterModel.VERSION_6_0_0.getVersion())
                    .build();
        }

        @Override
        void execute(OperationContext context, ModelNode operation, ModClusterServiceMBean service) throws OperationFailedException {
            Map<InetSocketAddress, String> map = service.getProxyInfo();
            ROOT_LOGGER.debugf("Mod_cluster ProxyInfo %s", map);
            if (!map.isEmpty()) {
                ModelNode result = new ModelNode();
                for (Map.Entry<InetSocketAddress, String> entry : map.entrySet()) {
                    result.add(entry.getKey().getHostName() + ":" + entry.getKey().getPort());
                    if (entry.getValue() == null) {
                        result.add();
                    } else {
                        result.add(entry.getValue());
                    }
                }
                context.getResult().set(result);
            }
        }
    },
    REFRESH {
        @Override
        public OperationDefinition getDefinition() {
            return new SimpleOperationDefinitionBuilder(ProxyOperation.REFRESH.getName(), ModClusterExtension.SUBSYSTEM_RESOLVER)
                    .setRuntimeOnly()
                    .setDeprecated(ModClusterModel.VERSION_6_0_0.getVersion())
                    .build();
        }

        @Override
        void execute(OperationContext context, ModelNode operation, ModClusterServiceMBean service) throws OperationFailedException {
            service.refresh();
            context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
        }
    },
    REMOVE_PROXY {
        @Override
        public OperationDefinition getDefinition() {
            return new SimpleOperationDefinitionBuilder(ProxyOperation.REMOVE_PROXY.getName(), ModClusterExtension.SUBSYSTEM_RESOLVER)
                    .addParameter(HOST)
                    .addParameter(PORT)
                    .setRuntimeOnly()
                    .addAccessConstraint(ModClusterExtension.MOD_CLUSTER_PROXIES_DEF)
                    .setDeprecated(ModClusterModel.VERSION_6_0_0.getVersion())
                    .build();
        }

        @Override
        void execute(OperationContext context, ModelNode operation, ModClusterServiceMBean service) throws OperationFailedException {
            ROOT_LOGGER.debugf("remove-proxy: %s", operation);

            String host = HOST.resolveModelAttribute(context, operation).asString();
            int port = PORT.resolveModelAttribute(context, operation).asInt();

            // Keeping this test here to maintain same behavior as previous versions.
            try {
                InetAddress.getByName(host);
            } catch (UnknownHostException e) {
                throw new OperationFailedException(ModClusterLogger.ROOT_LOGGER.couldNotResolveProxyIpAddress(), e);
            }

            service.removeProxy(host, port);

            context.completeStep(new OperationContext.RollbackHandler() {
                @Override
                public void handleRollback(OperationContext context, ModelNode operation) {
                    // TODO What if mod_cluster was never aware of this proxy?
                    service.addProxy(host, port);
                }
            });
        }
    },
    RESET {
        @Override
        public OperationDefinition getDefinition() {
            return new SimpleOperationDefinitionBuilder(ProxyOperation.RESET.getName(), ModClusterExtension.SUBSYSTEM_RESOLVER)
                    .setRuntimeOnly()
                    .setDeprecated(ModClusterModel.VERSION_6_0_0.getVersion())
                    .build();
        }

        @Override
        void execute(OperationContext context, ModelNode operation, ModClusterServiceMBean service) throws OperationFailedException {
            service.reset();
            context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
        }
    },
    STOP {
        @Override
        public OperationDefinition getDefinition() {
            return new SimpleOperationDefinitionBuilder(ProxyOperation.STOP.getName(), ModClusterExtension.SUBSYSTEM_RESOLVER)
                    .addParameter(WAIT_TIME)
                    .setRuntimeOnly()
                    .setDeprecated(ModClusterModel.VERSION_6_0_0.getVersion())
                    .build();
        }

        @Override
        void execute(OperationContext context, ModelNode operation, ModClusterServiceMBean service) throws OperationFailedException {
            ROOT_LOGGER.debugf("stop: %s", operation);

            int waitTime = WAIT_TIME.resolveModelAttribute(context, operation).asInt();

            boolean success = service.stop(waitTime, TimeUnit.SECONDS);
            context.getResult().get(SESSION_DRAINING_COMPLETE).set(success);

            context.completeStep(new OperationContext.RollbackHandler() {
                @Override
                public void handleRollback(OperationContext context, ModelNode operation) {
                    // TODO We're assuming that the all contexts were previously enabled, but they could have been disabled
                    service.enable();
                }
            });
        }
    },
    STOP_CONTEXT {
        @Override
        public OperationDefinition getDefinition() {
            return new SimpleOperationDefinitionBuilder(ProxyOperation.STOP_CONTEXT.getName(), ModClusterExtension.SUBSYSTEM_RESOLVER)
                    .addParameter(VIRTUAL_HOST)
                    .addParameter(CONTEXT)
                    .addParameter(WAIT_TIME)
                    .setRuntimeOnly()
                    .setDeprecated(ModClusterModel.VERSION_6_0_0.getVersion())
                    .build();
        }

        @Override
        void execute(OperationContext context, ModelNode operation, ModClusterServiceMBean service) throws OperationFailedException {
            ROOT_LOGGER.debugf("stop-context: %s", operation);

            String webHost = VIRTUAL_HOST.resolveModelAttribute(context, operation).asString();
            String webContext = CONTEXT.resolveModelAttribute(context, operation).asString();
            int waitTime = WAIT_TIME.resolveModelAttribute(context, operation).asInt();

            try {
                boolean success = service.stopContext(webHost, webContext, waitTime, TimeUnit.SECONDS);
                context.getResult().get(SESSION_DRAINING_COMPLETE).set(success);
            } catch (IllegalArgumentException e) {
                throw new OperationFailedException(ModClusterLogger.ROOT_LOGGER.contextOrHostNotFound(webHost, webContext));
            }

            context.completeStep(new OperationContext.RollbackHandler() {
                @Override
                public void handleRollback(OperationContext context, ModelNode operation) {
                    // TODO We're assuming that the context was previously enabled, but it could have been disabled
                    service.enableContext(webHost, webContext);
                }
            });
        }
    },
    ;

    static final String SESSION_DRAINING_COMPLETE = "session-draining-complete";

    abstract void execute(OperationContext context, ModelNode operation, ModClusterServiceMBean service) throws OperationFailedException;
}
