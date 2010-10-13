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

package org.jboss.as.messaging;

import javax.xml.stream.XMLStreamException;

import org.hornetq.api.core.SimpleString;
import org.hornetq.core.settings.impl.AddressFullMessagePolicy;
import org.jboss.as.model.AbstractModelElement;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author John Bailey
 */
public class AddressSettingsElement extends AbstractModelElement<AddressSettingsElement> {
    private static final long serialVersionUID = -9199912333707812250L;
    private final String match;
    private SimpleString deadLetterAddress;
    private SimpleString expiryAddress;
    private Long redeliveryDelay;
    private Integer messageCounterHistoryDayLimit;
    private AddressFullMessagePolicy addressFullMessagePolicy;
    private Boolean lastValueQueue;
    private Integer maxDeliveryAttempts;
    private Long redistributionDelay;
    private Boolean sendToDLAOnNoRoute;
    private Long maxSizeBytes;
    private Long pageSizeBytes;

    public AddressSettingsElement(final String match) {
        this.match = match;
    }

    public String getMatch() {
        return match;
    }

    public SimpleString getDeadLetterAddress() {
        return deadLetterAddress;
    }

    public void setDeadLetterAddress(SimpleString deadLetterAddress) {
        this.deadLetterAddress = deadLetterAddress;
    }

    public SimpleString getExpiryAddress() {
        return expiryAddress;
    }

    public void setExpiryAddress(SimpleString expiryAddress) {
        this.expiryAddress = expiryAddress;
    }

    public Long getRedeliveryDelay() {
        return redeliveryDelay;
    }

    public void setRedeliveryDelay(long redeliveryDelay) {
        this.redeliveryDelay = redeliveryDelay;
    }

    public Integer getMessageCounterHistoryDayLimit() {
        return messageCounterHistoryDayLimit;
    }

    public void setMessageCounterHistoryDayLimit(int messageCounterHistoryDayLimit) {
        this.messageCounterHistoryDayLimit = messageCounterHistoryDayLimit;
    }

    public AddressFullMessagePolicy getAddressFullMessagePolicy() {
        return addressFullMessagePolicy;
    }

    public void setAddressFullMessagePolicy(AddressFullMessagePolicy addressFullMessagePolicy) {
        this.addressFullMessagePolicy = addressFullMessagePolicy;
    }

    public Boolean isLastValueQueue() {
        return lastValueQueue;
    }

    public void setLastValueQueue(boolean lastValueQueue) {
        this.lastValueQueue = lastValueQueue;
    }

    public Integer getMaxDeliveryAttempts() {
        return maxDeliveryAttempts;
    }

    public void setMaxDeliveryAttempts(int maxDeliveryAttempts) {
        this.maxDeliveryAttempts = maxDeliveryAttempts;
    }

    public Long getRedistributionDelay() {
        return redistributionDelay;
    }

    public void setRedistributionDelay(long redistributionDelay) {
        this.redistributionDelay = redistributionDelay;
    }

    public Boolean isSendToDLAOnNoRoute() {
        return sendToDLAOnNoRoute;
    }

    public void setSendToDLAOnNoRoute(boolean sendToDLAOnNoRoute) {
        this.sendToDLAOnNoRoute = sendToDLAOnNoRoute;
    }

    public Long getMaxSizeBytes() {
        return maxSizeBytes;
    }

    public void setMaxSizeBytes(long maxSizeBytes) {
        this.maxSizeBytes = maxSizeBytes;
    }

    public Long getPageSizeBytes() {
        return pageSizeBytes;
    }

    public void setPageSizeBytes(long pageSizeBytes) {
        this.pageSizeBytes = pageSizeBytes;
    }

    /** {@inheritDoc} */
    protected Class<AddressSettingsElement> getElementClass() {
        return AddressSettingsElement.class;
    }

    public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        // TODO move this detail out of this class
        streamWriter.writeAttribute(Attribute.MATCH.getLocalName(), match);
        ElementUtils.writeSimpleElement(Element.DEAD_LETTER_ADDRESS_NODE_NAME, getDeadLetterAddress(), streamWriter);
        ElementUtils.writeSimpleElement(Element.EXPIRY_ADDRESS_NODE_NAME, getExpiryAddress(), streamWriter);
        if(getRedeliveryDelay() != null) {
            ElementUtils.writeSimpleElement(Element.REDELIVERY_DELAY_NODE_NAME, String.valueOf(getRedeliveryDelay()), streamWriter);
        }
        if(getMaxSizeBytes() != null) {
            ElementUtils.writeSimpleElement(Element.MAX_SIZE_BYTES_NODE_NAME, String.valueOf(getMaxSizeBytes()), streamWriter);
        }
        if(getPageSizeBytes() != null) {
            ElementUtils.writeSimpleElement(Element.PAGE_SIZE_BYTES_NODE_NAME, String.valueOf(getPageSizeBytes()), streamWriter);
        }
        if(getMessageCounterHistoryDayLimit() != null) {
            ElementUtils.writeSimpleElement(Element.MESSAGE_COUNTER_HISTORY_DAY_LIMIT_NODE_NAME, String.valueOf(getMessageCounterHistoryDayLimit()), streamWriter);
        }
        AddressFullMessagePolicy policy = getAddressFullMessagePolicy();
        if (policy != null) {
            ElementUtils.writeSimpleElement(Element.ADDRESS_FULL_MESSAGE_POLICY_NODE_NAME, policy.toString(), streamWriter);
        }
        if(isLastValueQueue() != null) {
            ElementUtils.writeSimpleElement(Element.LVQ_NODE_NAME, String.valueOf(isLastValueQueue()), streamWriter);
        }
        if(getMaxDeliveryAttempts() != null) {
            ElementUtils.writeSimpleElement(Element.MAX_DELIVERY_ATTEMPTS, String.valueOf(getMaxDeliveryAttempts()), streamWriter);
        }
        if(getRedistributionDelay() != null) {
            ElementUtils.writeSimpleElement(Element.REDISTRIBUTION_DELAY_NODE_NAME, String.valueOf(getRedistributionDelay()), streamWriter);
        }
        if(isSendToDLAOnNoRoute() != null) {
            ElementUtils.writeSimpleElement(Element.SEND_TO_DLA_ON_NO_ROUTE, String.valueOf(isSendToDLAOnNoRoute()), streamWriter);
        }

        streamWriter.writeEndElement();
     }

}
