/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.pojo.support;

import static org.wildfly.common.Assert.checkNotNullParam;

import java.net.URL;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class TcclChecker {
    public void start() {
        URL url = checkNotNullParam("tccl.txt", Thread.currentThread().getContextClassLoader().getResource("tccl.txt"));
    }
}
