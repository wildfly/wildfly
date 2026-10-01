/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.noremote;

import jakarta.ejb.Local;

/**
 * @author Richard Achmatowicz
 */
@Local
public interface LocalEcho {
    String echo(String message);
}
