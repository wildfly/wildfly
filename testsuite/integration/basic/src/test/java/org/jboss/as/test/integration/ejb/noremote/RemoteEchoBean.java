/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.noremote;

import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;

/**
 * @author Richard Achmatowicz
 */
@Stateless
@Remote(RemoteEcho.class)
public class RemoteEchoBean extends CommonRemoteEcho {
}
