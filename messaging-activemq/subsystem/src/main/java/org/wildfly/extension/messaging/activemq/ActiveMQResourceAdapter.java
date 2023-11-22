/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.messaging.activemq;

import java.security.AccessController;
import jakarta.resource.spi.work.ExecutionContext;
import jakarta.resource.spi.work.Work;
import jakarta.resource.spi.work.WorkException;
import jakarta.resource.spi.work.WorkListener;
import jakarta.resource.spi.work.WorkManager;

import org.apache.activemq.artemis.api.core.BroadcastEndpointFactory;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.activemq.artemis.ra.ConnectionFactoryProperties;
import org.jboss.as.naming.context.NamespaceContextSelector;
import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.msc.service.ServiceContainer;
import org.wildfly.extension.messaging.activemq.broadcast.CommandDispatcherBroadcastEndpointFactory;
import org.wildfly.extension.messaging.activemq.jms.ExternalPooledConnectionFactoryService;
import org.wildfly.extension.messaging.activemq.jms.JMSServices;

/**
 * Custom resource adapter that returns an appropriate BroadcastEndpointFactory if discovery is configured using
 * JGroups.
 *
 * @author Paul Ferraro
 */
public class ActiveMQResourceAdapter extends org.apache.activemq.artemis.ra.ActiveMQResourceAdapter {

    private static final long serialVersionUID = 170278234232275756L;

    public ActiveMQResourceAdapter() {
        super();
        this.setEnable1xPrefixes(true);
        this.getProperties().setEnable1xPrefixes(true);
    }

    @Override
    protected BroadcastEndpointFactory createBroadcastEndpointFactory(ConnectionFactoryProperties overrideProperties) {
        String clusterName = overrideProperties.getJgroupsChannelName() != null ? overrideProperties.getJgroupsChannelName() : getJgroupsChannelName();
        if (clusterName != null) {
            String channelRefName = this.getProperties().getJgroupsChannelRefName();

            String[] split = channelRefName.split("/");
            String serverName = split[0];
            String key = split[1];
            String pcf = null;
            if (key.indexOf(':') >= 0) {
                split = key.split(":");
                pcf = split[0];
                key = split[1];
            }

            if (serverName != null && !serverName.isEmpty()) {
                ActiveMQServerService service = (ActiveMQServerService) currentServiceContainer().getService(MessagingServices.getActiveMQServiceName(serverName)).getService();
                return new CommandDispatcherBroadcastEndpointFactory(service.getCommandDispatcherFactory(key), clusterName);
            }
            assert pcf != null;
            ExternalPooledConnectionFactoryService service = (ExternalPooledConnectionFactoryService) currentServiceContainer().getService(JMSServices.getPooledConnectionFactoryBaseServiceName(MessagingServices.getActiveMQServiceName()).append(pcf)).getService();
            return new CommandDispatcherBroadcastEndpointFactory(service.getCommandDispatcherFactory(key), clusterName);
        }
        return super.createBroadcastEndpointFactory(overrideProperties);
    }

    private static ServiceContainer currentServiceContainer() {
        return (System.getSecurityManager() == null) ? CurrentServiceContainer.getServiceContainer() : AccessController.doPrivileged(CurrentServiceContainer.GET_ACTION);
    }

    /**
     * Workaround for WFLY-18756 until https://issues.apache.org/jira/browse/ARTEMIS-4508 is merged and released.
     */
    @Override
    public ActiveMQConnectionFactory createRecoveryActiveMQConnectionFactory(final ConnectionFactoryProperties overrideProperties) {
        ActiveMQConnectionFactory cf = super.createRecoveryActiveMQConnectionFactory(overrideProperties);
        cf.setUseTopologyForLoadBalancing(this.isUseTopologyForLoadBalancing());
        return cf;
    }

    @Override
    public WorkManager getWorkManager() {
        return new NamingWorkManager(super.getWorkManager());
    }

    private class NamingWorkManager implements WorkManager {

        private final WorkManager delegate;

        private NamingWorkManager(WorkManager delegate) {
            this.delegate = delegate;
        }

        @Override
        public void doWork(Work work) throws WorkException {
            delegate.doWork(wrapWork(work));
        }

        @Override
        public void doWork(Work work, long startTimeout, ExecutionContext execContext, WorkListener workListener) throws WorkException {
            delegate.doWork(wrapWork(work), startTimeout, execContext, workListener);
        }

        @Override
        public long startWork(Work work) throws WorkException {
            return delegate.startWork(wrapWork(work));
        }

        @Override
        public long startWork(Work work, long startTimeout, ExecutionContext execContext, WorkListener workListener) throws WorkException {
            return delegate.startWork(wrapWork(work), startTimeout, execContext, workListener);
        }

        @Override
        public void scheduleWork(Work work) throws WorkException {
            delegate.scheduleWork(wrapWork(work));
        }

        @Override
        public void scheduleWork(Work work, long startTimeout, ExecutionContext execContext, WorkListener workListener) throws WorkException {
            delegate.scheduleWork(wrapWork(work), startTimeout, execContext, workListener);
        }

        private Work wrapWork(Work work) {
            return new WorkWrapper(NamespaceContextSelector.getCurrentSelector(), work);
        }
    }

    private class WorkWrapper implements Work {

        private final Work delegate;
        private final NamespaceContextSelector selector;

        private WorkWrapper(NamespaceContextSelector selector, Work work) {
            this.selector = selector;
            this.delegate = work;
        }

        @Override
        public void release() {
            NamespaceContextSelector.pushCurrentSelector(selector);
            try {
                delegate.release();
            } finally {
                NamespaceContextSelector.popCurrentSelector();
            }
        }

        @Override
        public void run() {
            NamespaceContextSelector.pushCurrentSelector(selector);
            try {
                delegate.run();
            } finally {
                NamespaceContextSelector.popCurrentSelector();
            }
        }
    }
}
