/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.injection.resource.basic;

import jakarta.ejb.Remote;

/**
 * @author carlo
 */
@Remote
public interface CheckORBRemote {
    void checkForInjectedORB();

    void checkForORBInEnvironment();
}
