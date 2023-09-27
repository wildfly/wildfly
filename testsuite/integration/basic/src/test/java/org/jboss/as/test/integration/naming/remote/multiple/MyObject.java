/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.naming.remote.multiple;

import java.io.Serializable;

public class MyObject implements Serializable {
    public String doIt(String s) {
        return s;
    }
}
