/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.manualmode.ee.globaldirectory;

public class EchoUtility {
    public String echo(String msg) {
        return "echo-library-1 " + msg;
    }
}
