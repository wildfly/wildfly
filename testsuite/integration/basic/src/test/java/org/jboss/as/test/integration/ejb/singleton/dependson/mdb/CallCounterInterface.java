/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.singleton.dependson.mdb;

import jakarta.ejb.Local;

/**
 * @author baranowb
 */
@Local
public interface CallCounterInterface {
    boolean isPostConstruct();

    void setPostConstruct();

    boolean isPreDestroy();

    void setPreDestroy();

    void setMessage();

    boolean isMessage();
}
