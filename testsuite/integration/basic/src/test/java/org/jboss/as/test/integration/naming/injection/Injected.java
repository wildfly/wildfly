/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.naming.injection;

import jakarta.ejb.Remote;

/**
 * @author Eduardo Martins
 */
@Remote
public interface Injected {

    String getInjectedResource();

}
