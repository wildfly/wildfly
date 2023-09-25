/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.manualmode.jca.workmanager.distributed;

import jakarta.resource.spi.work.DistributableWork;
import java.io.Serializable;

public class ShortWork implements DistributableWork, Serializable {

    @Override
    public void release() {
        // do nothing
    }

    @Override
    public void run() {
        // just finish quickly
    }
}
