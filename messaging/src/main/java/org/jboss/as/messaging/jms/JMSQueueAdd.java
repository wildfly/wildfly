/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.messaging.jms;

import java.util.Set;

import org.hornetq.jms.server.JMSServerManager;
import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceController.Mode;

/**
 * Update adding a {@code JMSQueueElement} to the {@code JMSSubsystemElement}. The
 * runtime action, will create the {@code JMSQueueService}.
 *
 * @author Emanuel Muckenhuber
 */
public class JMSQueueAdd extends AbstractJMSSubsystemUpdate<Void> {

    private static final long serialVersionUID = 5900849533546313463L;

    private final String name;
    private Set<String> bindings;
    private String selector;
    private Boolean durable;

    static JMSQueueAdd create(final JMSQueueElement queue) {
        final JMSQueueAdd action = new JMSQueueAdd(queue.getName());
        action.bindings = queue.getBindings();
        action.selector = queue.getSelector();
        action.durable = queue.getDurable();
        return action;
    }

    public JMSQueueAdd(String name) {
        if(name == null) {
            throw new IllegalArgumentException("null name");
        }
        this.name = name;
    }

    /** {@inheritDoc} */
    protected void applyUpdate(JMSSubsystemElement element) throws UpdateFailedException {
        final JMSQueueElement queue = element.addQueue(name);
        if(queue == null) {
            throw new UpdateFailedException("duplicate queue " + name);
        }
        queue.setBindings(bindings);
        queue.setSelector(selector);
        queue.setDurable(durable);
    }

    /** {@inheritDoc} */
    protected <P> void applyUpdate(UpdateContext context, UpdateResultHandler<? super Void, P> handler, P param) {
        final JMSQueueService service = new JMSQueueService(name, selector, durableDefault(), jndiBindings());
        final ServiceName serviceName = JMSSubsystemElement.JMS_QUEUE_BASE.append(name);
        context.getBatchBuilder().addService(serviceName, service)
                .addDependency(JMSSubsystemElement.JMS_MANAGER, JMSServerManager.class, service.getJmsServer())
                .addListener(new UpdateResultHandler.ServiceStartListener<P>(handler, param))
                .setInitialMode(Mode.IMMEDIATE);
    }

    /** {@inheritDoc} */
    public AbstractSubsystemUpdate<JMSSubsystemElement, ?> getCompensatingUpdate(JMSSubsystemElement original) {
        return new JMSQueueRemove(name);
    }

    public Set<String> getBindings() {
        return bindings;
    }

    public void setBindings(Set<String> bindings) {
        this.bindings = bindings;
    }

    public String getSelector() {
        return selector;
    }

    public void setSelector(String selector) {
        this.selector = selector;
    }

    public Boolean getDurable() {
        return durable;
    }

    public void setDurable(Boolean durable) {
        this.durable = durable;
    }

    public String getName() {
        return name;
    }

    private boolean durableDefault() {
        if(durable != null) {
            return durable;
        }
        // JMSServerDeployer.DEFAULT_QUEUE_DURABILITY
        return true;
    }

    private String[] jndiBindings() {
        if(bindings != null && ! bindings.isEmpty()) {
            return bindings.toArray(new String[bindings.size()]);
        }
        return new String[0];
    }
}
