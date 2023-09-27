/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jca.rar;

import org.jboss.logging.Logger;
import jakarta.resource.spi.work.Work;

public class MultipleWork implements Work {

    private static Logger log = Logger.getLogger("MultipleWork");

    @Override
    public void run() {

        log.trace("Work is started");
    }

    @Override
    public void release() {

        log.trace("Work is done");
    }

}
