/*
 * Copyright (C) 2020 RedHat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.extension.vertx;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.msc.Service;
import org.jboss.msc.service.LifecycleEvent;
import org.jboss.msc.service.LifecycleListener;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.vertx.logging.VertxLogger;

class VertxProxyService implements Service {
    private final VertxProxy vertxProxy;
    private static final ServiceName serviceNameBase = ServiceName.JBOSS.append(VertxSubsystemExtension.SUBSYSTEM_NAME);

    static void installService(OperationContext context, VertxProxy vertxProxy) {
        VertxProxyService service = new VertxProxyService(vertxProxy);
        ServiceName serviceName = serviceNameBase.append(vertxProxy.getName());

        context.getServiceTarget().addService(serviceName)
                .setInstance(service)
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install();

        final String jndiName = vertxProxy.getJndiName();
        final ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(jndiName);
        final BinderService binderService = new BinderService(bindInfo.getBindName());
        VertxManagementReferenceFactory valueManagedReferenceFactory = new VertxManagementReferenceFactory(service);
        binderService.getManagedObjectInjector().inject(valueManagedReferenceFactory);
        final ServiceBuilder<?> builder = context.getServiceTarget().addService(bindInfo.getBinderServiceName());
          builder.addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, binderService.getNamingStoreInjector());
        builder.setInstance(binderService)
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .addListener(new LifecycleListener() {
                    private volatile boolean bound;
                    @Override
                    public void handleEvent(ServiceController<?> controller, LifecycleEvent event) {
                        switch (event) {
                            case UP: {
                                VertxLogger.VERTX_LOGGER.vertxStarted(vertxProxy.getName(), jndiName);
                                bound = true;
                                break;
                            }
                            case DOWN: {
                                if (bound) {
                                    VertxLogger.VERTX_LOGGER.vertxStopped(vertxProxy.getName(), jndiName);
                                    bound = false;
                                }
                                break;
                            }
                            case REMOVED: {
                                VertxLogger.VERTX_LOGGER.vertxRemoved(vertxProxy.getName(), jndiName);
                                break;
                            }
                        }
                    }
                })
                .install();
    }

    VertxProxy getValue() {
        return this.vertxProxy;
    }

    private VertxProxyService(VertxProxy vertxProxy) {
        this.vertxProxy = vertxProxy;
    }

    @Override
    public void start(StartContext context) throws StartException {
        VertxRegistry.INSTANCE.registerVertx(vertxProxy);
    }

    @Override
    public void stop(StopContext context) {
        VertxRegistry.INSTANCE.unRegister(vertxProxy.getName());
        vertxProxy.getVertx().closeInternal();
    }

}
