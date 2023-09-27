/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jaxrs.rsources;

public class SimpleFromValueProvider {

    public String fromValue(String s) {
        throw new RuntimeException("Force error");
    }
}
