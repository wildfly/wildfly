/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.servlet.async;

import jakarta.ejb.Remote;

@Remote
public interface AsyncLeash {
    void init(long l);

    void onTimeout(Throwable throwable);

    void onComplete();

    void onError(Throwable throwable);

    void onStartAsync();

    boolean isTimeout();

    String detail();

    long timeoutTStamp();

    long initTStamp();

    boolean initialized();

    long getExpectedTDiff();
}
