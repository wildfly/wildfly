/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.management.deploy.runtime.ejb.singleton.timer;

import jakarta.ejb.Remote;

/**
 * @author baranowb
 *
 */
@Remote
public interface PointlessInterface {

    void triggerTimer() throws Exception;

    int getTimerCount();
}
