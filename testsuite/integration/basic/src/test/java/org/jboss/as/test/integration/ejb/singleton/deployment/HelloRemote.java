/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.singleton.deployment;

import jakarta.ejb.Remote;

/**
 * @author Bartosz Spyrko-Smietanko
 */
@Remote
public interface HelloRemote {
    String hello();
}
