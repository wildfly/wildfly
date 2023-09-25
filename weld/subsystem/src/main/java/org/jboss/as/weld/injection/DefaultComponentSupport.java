/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld.injection;

import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.web.common.WebComponentDescription;
import org.jboss.as.weld.spi.ComponentSupport;

/**
 *
 * @author Martin Kouba
 */
public class DefaultComponentSupport implements ComponentSupport {

    @Override
    public boolean isProcessing(ComponentDescription componentDescription) {
        return componentDescription instanceof WebComponentDescription;
    }

}
