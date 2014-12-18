/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.connector.subsystems.jca;

import org.jboss.as.connector.services.workmanager.DistributedWorkManagerService;
import org.jboss.as.connector.services.workmanager.NamedDistributedWorkManager;
import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.connector.util.Injection;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PropertiesAttributeDefinition;
import org.jboss.as.threads.ThreadsServices;
import org.jboss.as.txn.service.TxnServices;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.core.api.workmanager.DistributedWorkManager;
import org.jboss.jca.core.workmanager.policy.Always;
import org.jboss.jca.core.workmanager.policy.Never;
import org.jboss.jca.core.workmanager.policy.WaterMark;
import org.jboss.jca.core.workmanager.selector.FirstAvailable;
import org.jboss.jca.core.workmanager.selector.MaxFreeThreads;
import org.jboss.jca.core.workmanager.selector.PingTime;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.tm.JBossXATerminator;
import org.wildfly.clustering.jgroups.ChannelFactory;
import org.wildfly.clustering.jgroups.spi.service.ProtocolStackServiceName;

import java.util.Map;
import java.util.concurrent.Executor;

import static org.jboss.as.connector.logging.ConnectorLogger.ROOT_LOGGER;
import static org.jboss.as.connector.subsystems.jca.Constants.WORKMANAGER_LONG_RUNNING;
import static org.jboss.as.connector.subsystems.jca.Constants.WORKMANAGER_SHORT_RUNNING;

/**
 * @author <a href="jesper.pedersen@jboss.org">Jesper Pedersen</a>
 * @author <a href="stefano.maestri@redhat.com">Stefano Maestri</a>
 */
public class DistributedWorkManagerAdd extends AbstractAddStepHandler {

    public static final DistributedWorkManagerAdd INSTANCE = new DistributedWorkManagerAdd();

    @Override
    protected void populateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
        for (JcaDistributedWorkManagerDefinition.DWmParameters parameter : JcaDistributedWorkManagerDefinition.DWmParameters.values()) {
            parameter.getAttribute().validateAndSet(operation, model);
        }
    }

    @Override
    protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model) throws OperationFailedException {

        String name = JcaDistributedWorkManagerDefinition.DWmParameters.NAME.getAttribute().resolveModelAttribute(context, model).asString();

        String policy = JcaDistributedWorkManagerDefinition.DWmParameters.POLICY.getAttribute().resolveModelAttribute(context, model).asString();
        String selector = JcaDistributedWorkManagerDefinition.DWmParameters.SELECTOR.getAttribute().resolveModelAttribute(context, model).asString();

        ServiceTarget serviceTarget = context.getServiceTarget();
        NamedDistributedWorkManager namedDistributedWorkManager = new NamedDistributedWorkManager(name);

        if (policy != null && !policy.trim().isEmpty()) {
            switch (JcaDistributedWorkManagerDefinition.PolicyValue.valueOf(policy)) {
                case NEVER: {
                    namedDistributedWorkManager.setPolicy(new Never());
                    break;
                }
                case ALWAYS: {
                    namedDistributedWorkManager.setPolicy(new Always());
                    break;
                }
                case WATERMARK: {
                    namedDistributedWorkManager.setPolicy(new WaterMark());
                    break;
                }
                default:
                    throw ROOT_LOGGER.unsupportedPolicy(policy);

            }
            Injection injector = new Injection();
            for (Map.Entry<String, String> entry : ((PropertiesAttributeDefinition) JcaDistributedWorkManagerDefinition.DWmParameters.POLICY_OPTIONS.getAttribute()).unwrap(context, model).entrySet()) {
                try {
                    injector.inject(namedDistributedWorkManager.getPolicy(), entry.getKey(), entry.getValue());
                } catch (Exception e) {
                    ROOT_LOGGER.unsupportedPolicyOption(entry.getKey());
                }
            }
        } else {
            namedDistributedWorkManager.setPolicy(new WaterMark());
        }

        if (selector != null && !selector.trim().isEmpty()) {
            switch (JcaDistributedWorkManagerDefinition.SelectorValue.valueOf(selector)) {
                case FIRST_AVAILABLE: {
                    namedDistributedWorkManager.setSelector(new FirstAvailable());
                    break;
                }
                case MAX_FREE_THREADS: {
                    namedDistributedWorkManager.setSelector(new MaxFreeThreads());
                    break;
                }
                case PING_TIME: {
                    namedDistributedWorkManager.setSelector(new PingTime());
                    break;
                }
                default:
                    throw ROOT_LOGGER.unsupportedSelector(selector);
            }
            Injection injector = new Injection();
            for (Map.Entry<String, String> entry : ((PropertiesAttributeDefinition) JcaDistributedWorkManagerDefinition.DWmParameters.SELECTOR_OPTIONS.getAttribute()).unwrap(context, model).entrySet()) {
                try {
                    injector.inject(namedDistributedWorkManager.getSelector(), entry.getKey(), entry.getValue());
                } catch (Exception e) {
                    ROOT_LOGGER.unsupportedSelectorOption(entry.getKey());
                }
            }
        } else {
            namedDistributedWorkManager.setSelector(new PingTime());
        }

        String jgroupsStack = model.hasDefined(JcaDistributedWorkManagerDefinition.DWmParameters.TRANSPORT_JGROPUS_STACK.getAttribute().getName()) ?
                JcaDistributedWorkManagerDefinition.DWmParameters.TRANSPORT_JGROPUS_STACK.getAttribute().resolveModelAttribute(context, model).asString() :
                "udp";
        String channelName = JcaDistributedWorkManagerDefinition.DWmParameters.TRANSPORT_JGROPUS_CLUSTER.getAttribute().resolveModelAttribute(context, model).asString();
        Long requestTimeout = JcaDistributedWorkManagerDefinition.DWmParameters.TRANSPORT_REQUEST_TIMEOUT.getAttribute().resolveModelAttribute(context, model).asLong();

        DistributedWorkManagerService wmService = new DistributedWorkManagerService(namedDistributedWorkManager, channelName, requestTimeout);
        ServiceBuilder<DistributedWorkManager> builder = serviceTarget
                .addService(ConnectorServices.WORKMANAGER_SERVICE.append(name), wmService);
        builder.addDependency(ProtocolStackServiceName.CHANNEL_FACTORY.getServiceName(jgroupsStack), ChannelFactory.class, wmService.getJGroupsChannelFactoryInjector());


        builder.addDependency(ServiceBuilder.DependencyType.OPTIONAL, ThreadsServices.EXECUTOR.append(WORKMANAGER_LONG_RUNNING).append(name), Executor.class, wmService.getExecutorLongInjector());
        builder.addDependency(ThreadsServices.EXECUTOR.append(WORKMANAGER_SHORT_RUNNING).append(name), Executor.class, wmService.getExecutorShortInjector());

        builder.addDependency(TxnServices.JBOSS_TXN_XA_TERMINATOR, JBossXATerminator.class, wmService.getXaTerminatorInjector())
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install();
    }
}
