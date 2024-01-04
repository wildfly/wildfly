/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.remotelookup;

import jakarta.ejb.Stateless;

@Stateless
public class StatelessBean {
    public void hello() {
        // hi, father
    }
}
