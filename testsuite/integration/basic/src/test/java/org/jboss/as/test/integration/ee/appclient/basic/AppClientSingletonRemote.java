/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.appclient.basic;

import jakarta.ejb.Remote;

/**
 * @author Stuart Douglas
 */
@Remote
public interface AppClientSingletonRemote {

    void reset();

    void makeAppClientCall(final String value);

    String awaitAppClientCall();

    String echo(String toEcho);
}
