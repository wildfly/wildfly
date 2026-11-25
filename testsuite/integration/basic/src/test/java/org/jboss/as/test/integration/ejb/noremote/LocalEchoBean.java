/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.noremote;

import jakarta.ejb.Local;
import jakarta.ejb.Stateless;

/**
 * @author Richard Achmatowicz
 */
@Stateless
@Local(LocalEcho.class)
public class LocalEchoBean extends CommonLocalEcho {
}
