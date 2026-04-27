/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.noremote;

import jakarta.ejb.Remote;

/**
 * @author Richard Achmatowicz
 */
@Remote
public interface RemoteEcho {

    String echo(String message);
}
