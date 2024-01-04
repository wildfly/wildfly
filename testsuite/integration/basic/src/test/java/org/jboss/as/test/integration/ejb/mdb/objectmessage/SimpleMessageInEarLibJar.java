/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.mdb.objectmessage;

import static org.wildfly.common.Assert.checkNotNullParam;

import java.io.Serializable;

/**
 * User: jpai
 */
public class SimpleMessageInEarLibJar implements Serializable {

    private final String msg;

    public SimpleMessageInEarLibJar(String msg) {
        this.msg = checkNotNullParam("msg", msg);
    }

    public String getMessage() {
        return this.msg;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SimpleMessageInEarLibJar that = (SimpleMessageInEarLibJar) o;

        return msg.equals(that.msg);

    }

    @Override
    public int hashCode() {
        return msg.hashCode();
    }
}
