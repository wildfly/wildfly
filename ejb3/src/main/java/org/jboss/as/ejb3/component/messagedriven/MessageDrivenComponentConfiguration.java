/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ejb3.component.messagedriven;

import org.jboss.as.ejb3.component.EJBComponentConfiguration;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;

import javax.resource.spi.ResourceAdapter;

import static org.jboss.as.ejb3.component.pool.PooledInstanceInterceptor.pooled;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class MessageDrivenComponentConfiguration extends EJBComponentConfiguration {
    private Class<?> messageListenerInterface;
    private final String resourceAdapterName;
    private final ServiceName raServiceName;

    /**
     * Construct a new instance.
     *
     * @param description the original component description
     */
    public MessageDrivenComponentConfiguration(final MessageDrivenComponentDescription description) {
        super(description);

        this.resourceAdapterName = description.getResourceAdapterName();
        if (this.resourceAdapterName == null)
            throw new IllegalArgumentException("No resource adapter name set in " + description);

        // See RaDeploymentParsingProcessor
        String deploymentName = resourceAdapterName.substring(0, resourceAdapterName.indexOf(".rar"));
        // See ResourceAdapterDeploymentService
        this.raServiceName = ServiceName.of(deploymentName);
        description.addDependency(raServiceName, ServiceBuilder.DependencyType.REQUIRED);

        addComponentSystemInterceptorFactory(pooled());
    }

    @Override
    protected void addCurrentInvocationContextInterceptorFactory() {
        addComponentSystemInterceptorFactory(new ImmediateInterceptorFactory(MessageDrivenInvocationContextInterceptor.INSTANCE));
    }

    @Override
    public MessageDrivenComponent constructComponent() {
        MessageDrivenComponent component = new MessageDrivenComponent(this);
        component.setResourceAdapter(getInjectionValue(raServiceName, ResourceAdapter.class));
        return component;
    }

    Class<?> getMessageListenerInterface() {
        return messageListenerInterface;
    }

    void setMessageListenerInterface(Class<?> messsageListenerInterface) {
        this.messageListenerInterface = messsageListenerInterface;
    }
}
