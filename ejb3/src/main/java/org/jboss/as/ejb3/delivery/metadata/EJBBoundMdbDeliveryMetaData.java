/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.delivery.metadata;

import org.jboss.metadata.ejb.parser.jboss.ejb3.AbstractEJBBoundMetaData;

/**
 * Metadata for delivery active property of message-driven beans
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc
 * @author Flavia Rainone.
 */
public class EJBBoundMdbDeliveryMetaData extends AbstractEJBBoundMetaData {

    private boolean deliveryActive;
    private String[] deliveryGroups;

    public boolean isDeliveryActive() {
        return deliveryActive;
    }

    public void setDeliveryActive(boolean deliveryActive) {
        this.deliveryActive = deliveryActive;
    }

    public String[] getDeliveryGroups() {
        return deliveryGroups;
    }

    public void setDeliveryGroups(String... deliveryGroups) {
        this.deliveryGroups = deliveryGroups;
    }
}
