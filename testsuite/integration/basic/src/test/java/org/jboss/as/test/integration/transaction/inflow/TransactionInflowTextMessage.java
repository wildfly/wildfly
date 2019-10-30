/*
 * JBoss, Home of Professional Open Source
 * Copyright 2016, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

package org.jboss.as.test.integration.transaction.inflow;

import java.util.Enumeration;
import java.util.Random;
import javax.jms.Destination;
import javax.jms.JMSException;

/**
 * Test message used for testing in jca inflow transaction rar.
 *
 * @author Ondrej Chaloupka <ochaloup@redhat.com>
 */
public class TransactionInflowTextMessage implements javax.jms.TextMessage {
    private int messageId = new Random().nextInt();
    private String text;

    public TransactionInflowTextMessage(String text) {
        this.text = text;
    }

    public String getJMSMessageID() throws JMSException {
        return messageId + "-" + text;
    }

    public void setText(String string) throws JMSException {
        this.text = string;
    }

    public String getText() throws JMSException {
        return this.text;
    }

    public String getTextCleaned() {
        return this.text;
    }

    public void setJMSMessageID(String id) throws JMSException {
        throw new UnsupportedOperationException();
    }

    public long getJMSTimestamp() throws JMSException {
        throw new UnsupportedOperationException();
    }

    public void setJMSTimestamp(long timestamp) throws JMSException {
        throw new UnsupportedOperationException();
    }

    public byte[] getJMSCorrelationIDAsBytes() throws JMSException {
        throw new UnsupportedOperationException();
    }

    public void setJMSCorrelationIDAsBytes(byte[] correlationID) throws JMSException {
        throw new UnsupportedOperationException();
    }

    public void setJMSCorrelationID(String correlationID) throws JMSException {
        throw new UnsupportedOperationException();
    }

    public String getJMSCorrelationID() throws JMSException {
        throw new UnsupportedOperationException();
    }

    public Destination getJMSReplyTo() throws JMSException {
        throw new UnsupportedOperationException();
    }

    public void setJMSReplyTo(Destination replyTo) throws JMSException {
        throw new UnsupportedOperationException();
    }

    public Destination getJMSDestination() throws JMSException {
        throw new UnsupportedOperationException();
    }

    public void setJMSDestination(Destination destination) throws JMSException {
        throw new UnsupportedOperationException();
    }

    public int getJMSDeliveryMode() throws JMSException {
        throw new UnsupportedOperationException();
    }

    public void setJMSDeliveryMode(int deliveryMode) throws JMSException {
        throw new UnsupportedOperationException();
    }

    public boolean getJMSRedelivered() throws JMSException {
        throw new UnsupportedOperationException();
    }

    public void setJMSRedelivered(boolean redelivered) throws JMSException {
        throw new UnsupportedOperationException();
    }

    public String getJMSType() throws JMSException {
        throw new UnsupportedOperationException();
    }

    public void setJMSType(String type) throws JMSException {
        throw new UnsupportedOperationException();
    }

    public long getJMSExpiration() throws JMSException {
        throw new UnsupportedOperationException();
    }

    public void setJMSExpiration(long expiration) throws JMSException {
        throw new UnsupportedOperationException();
    }

    public long getJMSDeliveryTime() throws JMSException {
        throw new UnsupportedOperationException();
    }

    public void setJMSDeliveryTime(long deliveryTime) throws JMSException {
        throw new UnsupportedOperationException();
    }

    public int getJMSPriority() throws JMSException {
        throw new UnsupportedOperationException();
    }

    public void setJMSPriority(int priority) throws JMSException {
        throw new UnsupportedOperationException();
    }

    public void clearProperties() throws JMSException {
        throw new UnsupportedOperationException();
    }

    public boolean propertyExists(String name) throws JMSException {
        throw new UnsupportedOperationException();
    }

    public boolean getBooleanProperty(String name) throws JMSException {
        throw new UnsupportedOperationException();
    }

    public byte getByteProperty(String name) throws JMSException {
        throw new UnsupportedOperationException();
    }

    public short getShortProperty(String name) throws JMSException {
        throw new UnsupportedOperationException();
    }

    public int getIntProperty(String name) throws JMSException {
        throw new UnsupportedOperationException();
    }

    public long getLongProperty(String name) throws JMSException {
        throw new UnsupportedOperationException();
    }

    public float getFloatProperty(String name) throws JMSException {
        throw new UnsupportedOperationException();
    }

    public double getDoubleProperty(String name) throws JMSException {
        throw new UnsupportedOperationException();
    }

    public String getStringProperty(String name) throws JMSException {
        throw new UnsupportedOperationException();
    }

    public Object getObjectProperty(String name) throws JMSException {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("rawtypes")
    public Enumeration getPropertyNames() throws JMSException {
        throw new UnsupportedOperationException();
    }

    public void setBooleanProperty(String name, boolean value) throws JMSException {
        throw new UnsupportedOperationException();
    }

    public void setByteProperty(String name, byte value) throws JMSException {
        throw new UnsupportedOperationException();
    }

    public void setShortProperty(String name, short value) throws JMSException {
        throw new UnsupportedOperationException();
    }

    public void setIntProperty(String name, int value) throws JMSException {
        throw new UnsupportedOperationException();
    }

    public void setLongProperty(String name, long value) throws JMSException {
        throw new UnsupportedOperationException();
    }

    public void setFloatProperty(String name, float value) throws JMSException {
        throw new UnsupportedOperationException();
    }

    public void setDoubleProperty(String name, double value) throws JMSException {
        throw new UnsupportedOperationException();
    }

    public void setStringProperty(String name, String value) throws JMSException {
        throw new UnsupportedOperationException();
    }

    public void setObjectProperty(String name, Object value) throws JMSException {
        throw new UnsupportedOperationException();
    }

    public void acknowledge() throws JMSException {
        throw new UnsupportedOperationException();
    }

    public void clearBody() throws JMSException {
        throw new UnsupportedOperationException();
    }

    public <T> T getBody(Class<T> c) throws JMSException {
        throw new UnsupportedOperationException();
    }

    public boolean isBodyAssignableTo(@SuppressWarnings("rawtypes") Class c) throws JMSException {
        throw new UnsupportedOperationException();
    }
}

