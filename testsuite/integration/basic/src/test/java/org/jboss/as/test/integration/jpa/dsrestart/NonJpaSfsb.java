/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jpa.dsrestart;

import jakarta.ejb.Stateless;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
@Stateless
public class NonJpaSfsb {

    public String echo(String s) {
        return s;
    }
}
