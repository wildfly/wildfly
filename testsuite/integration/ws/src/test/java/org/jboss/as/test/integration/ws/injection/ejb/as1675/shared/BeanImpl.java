/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ws.injection.ejb.as1675.shared;

import jakarta.ejb.Stateless;

/**
 * Shared EJB3 implementation.
 *
 * @author <a href="mailto:richard.opalka@jboss.org">Richard Opalka</a>
 */
@Stateless
public class BeanImpl implements BeanIface {
    public String printString() {
        return "Injected hello message";
    }
}
