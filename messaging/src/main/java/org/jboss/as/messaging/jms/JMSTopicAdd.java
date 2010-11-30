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
 * Update adding a {@code JMSTopicElement} to the {@code JMSSubsystemElement}. The
 * runtime action, will create the {@code JMSTopicService}.
 *
 * @author Emanuel Muckenhuber
 */
public class JMSTopicAdd extends AbstractJMSSubsystemUpdate<Void> {

    private static final long serialVersionUID = 3528505591156292231L;
    private final String name;
    private Set<String> bindings;

    static JMSTopicAdd create(final JMSTopicElement topic) {
        final JMSTopicAdd action = new JMSTopicAdd(topic.getName());
        action.bindings = topic.getBindings();
        return action;
    }

    public JMSTopicAdd(final String name) {
        if(name == null) {
            throw new IllegalArgumentException("null name");
        }
        this.name = name;
    }

    /** {@inheritDoc} */
    protected void applyUpdate(JMSSubsystemElement element) throws UpdateFailedException {
        final JMSTopicElement topic = element.addTopic(name);
        if(topic == null) {
            throw new UpdateFailedException("duplicate topic " + name);
        }
        topic.setBindings(bindings);
    }

    /** {@inheritDoc} */
    protected <P> void applyUpdate(UpdateContext context, UpdateResultHandler<? super Void, P> handler, P param) {
        final JMSTopicService service = new JMSTopicService(name, jndiBindings());
        final ServiceName serviceName = JMSSubsystemElement.JMS_TOPIC_BASE.append(name);
        context.getBatchBuilder().addService(serviceName, service)
                .addDependency(JMSSubsystemElement.JMS_MANAGER, JMSServerManager.class, service.getJmsServer())
                .addListener(new UpdateResultHandler.ServiceStartListener<P>(handler, param))
                .setInitialMode(Mode.ACTIVE)
                .install();
    }

    /** {@inheritDoc} */
    public AbstractSubsystemUpdate<JMSSubsystemElement, ?> getCompensatingUpdate(JMSSubsystemElement original) {
        return new JMSTopicRemove(name);
    }

    public Set<String> getBindings() {
        return bindings;
    }

    public void setBindings(Set<String> bindings) {
        this.bindings = bindings;
    }

    public String getName() {
        return name;
    }

    private String[] jndiBindings() {
        if(bindings != null && ! bindings.isEmpty()) {
            return bindings.toArray(new String[bindings.size()]);
        }
        return new String[0];
    }
}
