/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.messaging.activemq;

import static org.wildfly.extension.messaging.activemq.MessagingServices.JBOSS_MESSAGING_ACTIVEMQ;

import java.security.AccessController;
import javax.resource.spi.work.ExecutionContext;
import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkListener;
import javax.resource.spi.work.WorkManager;

import org.apache.activemq.artemis.api.core.BroadcastEndpointFactory;
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
            ExternalPooledConnectionFactoryService service = (ExternalPooledConnectionFactoryService) currentServiceContainer().getService(JMSServices.getPooledConnectionFactoryBaseServiceName(JBOSS_MESSAGING_ACTIVEMQ).append(pcf)).getService();
            return new CommandDispatcherBroadcastEndpointFactory(service.getCommandDispatcherFactory(key), clusterName);
        }
        return super.createBroadcastEndpointFactory(overrideProperties);
    }

    private static ServiceContainer currentServiceContainer() {
        return (System.getSecurityManager() == null) ? CurrentServiceContainer.getServiceContainer() : AccessController.doPrivileged(CurrentServiceContainer.GET_ACTION);
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
