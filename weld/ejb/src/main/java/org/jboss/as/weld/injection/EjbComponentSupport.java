/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld.injection;

import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.component.messagedriven.MessageDrivenComponentDescription;
import org.jboss.as.weld.spi.ComponentSupport;

/**
 *
 * @author Martin Kouba
 */
public class EjbComponentSupport implements ComponentSupport {

    @Override
    public boolean isProcessing(ComponentDescription componentDescription) {
        return componentDescription instanceof MessageDrivenComponentDescription;
    }

    @Override
    public boolean isDiscoveredExternalType(ComponentDescription componentDescription) {
        return !(componentDescription instanceof EJBComponentDescription);
    }

}
