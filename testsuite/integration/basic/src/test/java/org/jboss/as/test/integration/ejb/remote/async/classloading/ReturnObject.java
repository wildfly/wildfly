/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.async.classloading;

import java.io.Serializable;

/**
 * @author baranowb
 *
 */
public class ReturnObject implements Serializable {
    private final String value;
    private static int count = 0;

    /**
     * @param value
     */
    public ReturnObject(String value) {
        super();
        this.value = value;
        ReturnObject.count++;
    }

    /**
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * @return the count
     */
    public static int getCount() {
        return count;
    }

}
