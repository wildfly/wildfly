/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.component.messagedriven;

import java.util.function.Supplier;

import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;

/**
 * Service that controls delivery for a specific MDB.
 *
 * When started, delivery to a mdb is enabled, when stopped, it is disabled.
 *
 * @author Flavia Rainone
 */
public class MdbDeliveryControllerService implements Service {

    private final Supplier<MessageDrivenComponent> mdbComponent;

    public MdbDeliveryControllerService(Supplier<MessageDrivenComponent> mdbComponent) {
        this.mdbComponent = mdbComponent;
    }

    @Override
    public void start(StartContext context) {
        this.mdbComponent.get().startDelivery();
    }

    @Override
    public void stop(StopContext context) {
        mdbComponent.get().stopDelivery();
    }
}
