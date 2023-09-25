/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.timerservice;

import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.timerservice.spi.TimedObjectInvoker;
import org.jboss.as.ejb3.timerservice.spi.TimedObjectInvokerFactory;
import org.jboss.modules.Module;

/**
 * @author Paul Ferraro
 */
public class TimedObjectInvokerFactoryImpl implements TimedObjectInvokerFactory {

    private final Module module;
    private final String deploymentName;

    public TimedObjectInvokerFactoryImpl(Module module, String deploymentName) {
        this.module = module;
        this.deploymentName = deploymentName;
    }

    @Override
    public TimedObjectInvoker createInvoker(EJBComponent component) {
        return new TimedObjectInvokerImpl(this.module, this.deploymentName, component);
    }
}
