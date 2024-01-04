/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ws.injection.ejb.basic.shared;

import jakarta.ejb.Stateless;

/**
 * The EJB3 implementation
 *
 * @author <a href="mailto:rsvoboda@redhat.com">Rostislav Svoboda</a>
 */
@Stateless
public class BeanImpl implements BeanIface {
    public String printString() {
        return "Injected hello message";
    }
}
