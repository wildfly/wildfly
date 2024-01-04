/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.naming.remote.multiple;

import jakarta.ejb.Remote;

@Remote
public interface MyEjb {
    String doIt();
}
