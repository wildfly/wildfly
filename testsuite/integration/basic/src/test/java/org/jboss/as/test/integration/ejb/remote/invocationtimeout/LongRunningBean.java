/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.invocationtimeout;

import java.util.concurrent.TimeUnit;
import jakarta.ejb.Stateless;

/**
 * @author Jan Martiska
 */
@Stateless
public class LongRunningBean implements LongRunningBeanRemote {

    @Override
    public void longRunningOperation() {
        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {

        }
    }

}
