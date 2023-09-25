/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.remote.stateful;

import jakarta.ejb.Remote;
import jakarta.ejb.Remove;

/**
 * @author Stuart Douglas
 */
@Remote
public interface RemoteInterface {

    void add(int i);

    int get();
    ValueWrapper getValue();

    @Remove
    void remove();
}
