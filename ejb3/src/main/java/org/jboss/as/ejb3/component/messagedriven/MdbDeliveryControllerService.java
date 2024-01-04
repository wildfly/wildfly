/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.component.messagedriven;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Service that controls delivery for a specific MDB.
 *
 * When started, delivery to a mdb is enabled, when stopped, it is disabled.
 *
 * @author Flavia Rainone
 */
public class MdbDeliveryControllerService implements Service<MdbDeliveryControllerService> {

    private final InjectedValue<MessageDrivenComponent> mdbComponent = new InjectedValue<MessageDrivenComponent>();

    public MdbDeliveryControllerService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public InjectedValue<MessageDrivenComponent> getMdbComponent() {
        return mdbComponent;
    }

    public void start(final StartContext context) throws StartException {
        mdbComponent.getValue().startDelivery();
    }

    public void stop(final StopContext context) {
        mdbComponent.getValue().stopDelivery();
    }
}
