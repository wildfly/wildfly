/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.weld.context.application.startup;

import jakarta.ejb.Stateless;

@Stateless
public class DummySessionBean {
    public void ping() {
    }
}
