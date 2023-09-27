/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.injection.ejbref.lookup;

import jakarta.ejb.Remote;

@Remote
public interface RemoteInterface {

    String ping();

}
