/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.multinode.clientinterceptor;

import org.jboss.logging.Logger;

import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;

/**
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */

@Stateless
@Remote(StatelessRemote.class)
public class StatelessBean implements StatelessRemote {
    private static final Logger log = Logger.getLogger(StatelessBean.class);

    private static int methodCount = 0;

    public int method() throws Exception {
        ++methodCount;
        log.trace("Method called " + methodCount);
        return methodCount;
    }
}
