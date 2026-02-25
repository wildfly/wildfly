/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.timeout;

import java.util.List;

import jakarta.ejb.Remote;

@Remote
public interface TimeoutLog {

    void receivedMessage();

    void sentResponse();

    void failedIO();

    List<ActionSequence> getTestResult();

}
