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

import org.hornetq.api.core.SimpleString;
import org.hornetq.core.settings.impl.AddressFullMessagePolicy;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.model.UpdateResultHandler;

/**
 * Update adding {@code AddressSettings}.
 *
 * @author Emanuel Muckenhuber
 */
public class AddressSettingsAdd extends AbstractMessagingSubsystemUpdate<Void> {
    private static final long serialVersionUID = -1629954652094107618L;
    private final String name;
    private SimpleString deadLetterAddress;
    private SimpleString expiryAddress;
    private long redeliveryDelay;
    private int messageCounterHistoryDayLimit;
    private AddressFullMessagePolicy addressFullMessagePolicy;
    private boolean lastValueQueue;
    private int maxDeliveryAttempts;
    private long redistributionDelay;
    private boolean sendToDLAOnNoRoute;
    private long maxSizeBytes;
    private long pageSizeBytes;

    public AddressSettingsAdd(final String name) {
        this.name = name;
    }

    public AbstractMessagingSubsystemUpdate<?> getCompensatingUpdate(MessagingSubsystemElement element) {
        return new AddressSettingsRemove(name);
    }

    protected void applyUpdate(final MessagingSubsystemElement element) throws UpdateFailedException {
        final AddressSettingsElement addressSettingsElement = element.addAddressSettings(name);
        addressSettingsElement.setAddressFullMessagePolicy(getAddressFullMessagePolicy());
        addressSettingsElement.setDeadLetterAddress(getDeadLetterAddress());
        addressSettingsElement.setExpiryAddress(getExpiryAddress());
        addressSettingsElement.setLastValueQueue(isLastValueQueue());
        addressSettingsElement.setMaxDeliveryAttempts(getMaxDeliveryAttempts());
        addressSettingsElement.setMaxSizeBytes(getMaxSizeBytes());
        addressSettingsElement.setMessageCounterHistoryDayLimit(getMessageCounterHistoryDayLimit());
        addressSettingsElement.setPageSizeBytes(getPageSizeBytes());
        addressSettingsElement.setRedeliveryDelay(getRedeliveryDelay());
        addressSettingsElement.setRedistributionDelay(getRedistributionDelay());
        addressSettingsElement.setSendToDLAOnNoRoute(isSendToDLAOnNoRoute());
    }

    /** {@inheritDoc} */
    protected <P> void applyUpdate(UpdateContext updateContext, UpdateResultHandler<? super Void, P> resultHandler, P param) {
        // TODO Auto-generated method stub

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

    public long getRedeliveryDelay() {
        return redeliveryDelay;
    }

    public void setRedeliveryDelay(long redeliveryDelay) {
        this.redeliveryDelay = redeliveryDelay;
    }

    public int getMessageCounterHistoryDayLimit() {
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

    public boolean isLastValueQueue() {
        return lastValueQueue;
    }

    public void setLastValueQueue(boolean lastValueQueue) {
        this.lastValueQueue = lastValueQueue;
    }

    public int getMaxDeliveryAttempts() {
        return maxDeliveryAttempts;
    }

    public void setMaxDeliveryAttempts(int maxDeliveryAttempts) {
        this.maxDeliveryAttempts = maxDeliveryAttempts;
    }

    public long getRedistributionDelay() {
        return redistributionDelay;
    }

    public void setRedistributionDelay(long redistributionDelay) {
        this.redistributionDelay = redistributionDelay;
    }

    public boolean isSendToDLAOnNoRoute() {
        return sendToDLAOnNoRoute;
    }

    public void setSendToDLAOnNoRoute(boolean sendToDLAOnNoRoute) {
        this.sendToDLAOnNoRoute = sendToDLAOnNoRoute;
    }

    public long getMaxSizeBytes() {
        return maxSizeBytes;
    }

    public void setMaxSizeBytes(long maxSizeBytes) {
        this.maxSizeBytes = maxSizeBytes;
    }

    public long getPageSizeBytes() {
        return pageSizeBytes;
    }

    public void setPageSizeBytes(long pageSizeBytes) {
        this.pageSizeBytes = pageSizeBytes;
    }
}
