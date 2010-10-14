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

import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.messaging.MessagingSubsystemElement;
import org.jboss.as.model.AbstractSubsystemElement;
import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * The JMS subsystem configuration.
 *
 * @author Emanuel Muckenhuber
 */
public class JMSSubsystemElement extends AbstractSubsystemElement<JMSSubsystemElement> {

    private static final long serialVersionUID = 3225118788089921849L;

    public static final ServiceName JMS = MessagingSubsystemElement.JBOSS_MESSAGING.append("jms");
    public static final ServiceName JMS_MANAGER = JMS.append("manager");
    public static final ServiceName JMS_QUEUE_BASE = JMS.append("queue");
    public static final ServiceName JMS_TOPIC_BASE = JMS.append("topic");
    public static final ServiceName JMS_CF_BASE = JMS.append("connection-factory");

    private final NavigableMap<String, JMSQueueElement> queues = new TreeMap<String, JMSQueueElement>();
    private final NavigableMap<String, JMSTopicElement> topics = new TreeMap<String, JMSTopicElement>();
    private final NavigableMap<String, ConnectionFactoryElement> connectionFactories = new TreeMap<String, ConnectionFactoryElement>();

    protected JMSSubsystemElement() {
        super(Namespace.CURRENT.getUriString());
    }

    /** {@inheritDoc} */
    protected void getUpdates(List<? super AbstractSubsystemUpdate<JMSSubsystemElement, ?>> list) {
        for(final ConnectionFactoryElement cf : connectionFactories.values()) {
            list.add(new ConnectionFactoryAdd(cf));
        }
        for(final JMSQueueElement queue : queues.values()) {
            list.add(JMSQueueAdd.create(queue));
        }
        for(final JMSTopicElement topic : topics.values()) {
            list.add(JMSTopicAdd.create(topic));
        }
    }

    /** {@inheritDoc} */
    protected boolean isEmpty() {
        return queues.isEmpty() && topics.isEmpty() && connectionFactories.isEmpty();
    }

    /** {@inheritDoc} */
    protected JMSSubsystemAdd getAdd() {
        return new JMSSubsystemAdd();
    }

    /** {@inheritDoc} */
    protected <P> void applyRemove(UpdateContext context, UpdateResultHandler<? super Void, P> resultHandler, P param) {
        final ServiceController<?> service = context.getServiceContainer().getService(JMS_MANAGER);
        if(service == null) {
            resultHandler.handleSuccess(null, param);
        } else {
            service.addListener(new UpdateResultHandler.ServiceRemoveListener<P>(resultHandler, param));
        }
    }

    boolean addConnectionFactory(final ConnectionFactoryElement cf) {
        if(connectionFactories.containsKey(cf.getName())) {
            return false;
        }
        connectionFactories.put(cf.getName(), cf);
        return true;
    }

    public ConnectionFactoryElement getConnectionFactory(final String name) {
        return connectionFactories.get(name);
    }

    boolean removeConnectionFactory(final String name) {
        return connectionFactories.remove(name) != null;
    }

    JMSQueueElement addQueue(final String name) {
        if(queues.containsKey(name)) {
            return null;
        }
        final JMSQueueElement queue = new JMSQueueElement(name);
        queues.put(name, queue);
        return queue;
    }

    public JMSQueueElement getQueue(final String name) {
        return queues.get(name);
    }

    boolean removeQueue(final String name) {
        return queues.remove(name) != null;
    }

    JMSTopicElement addTopic(final String name) {
        if(topics.containsKey(name)) {
            return null;
        }
        final JMSTopicElement topic = new JMSTopicElement(name);
        topics.put(name, topic);
        return topic;
    }

    public JMSTopicElement getTopic(final String name) {
        return topics.get(name);
    }

    boolean removeTopic(final String name) {
        return topics.remove(name) != null;
    }

    /** {@inheritDoc} */
    protected Class<JMSSubsystemElement> getElementClass() {
        return JMSSubsystemElement.class;
    }

    /** {@inheritDoc} */
    public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        if(! connectionFactories.isEmpty()) {
            for(final ConnectionFactoryElement cf : connectionFactories.values()) {
                streamWriter.writeStartElement(Element.CONNECTION_FACTORY.getLocalName());
                cf.writeContent(streamWriter);
            }
        }
        if(! queues.isEmpty()) {
            for(final JMSQueueElement queue : queues.values()) {
                streamWriter.writeStartElement(Element.QUEUE.getLocalName());
                queue.writeContent(streamWriter);
            }
        }
        if(! topics.isEmpty()) {
            for(final JMSTopicElement topic : topics.values()) {
                streamWriter.writeStartElement(Element.TOPIC.getLocalName());
                topic.writeContent(streamWriter);
            }
        }
    }

}
