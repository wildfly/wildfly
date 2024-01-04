/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.multinode.clientinterceptor.secured;

public interface Secured {

    String permitAll(String message);
    void denyAll(String message);

    String roleEcho(String message);
    String role2Echo(String message);

}
