/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.jndi;

import java.io.Serializable;

/**
 *
 */
public class EchoMessage implements Serializable {

    private String message;

    public void setMessage(final String msg) {
        this.message = msg;
    }

    public String getMessage() {
        return this.message;
    }

}
