/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.client.api;

import java.util.concurrent.Future;

/**
 * User: jpai
 */
public interface EchoRemote {

    String echo(String message);

    Future<String> asyncEcho(String message, long delayInMilliSec);

    EchoRemote getBusinessObject();

    boolean testRequestScopeActive();

    ValueWrapper getValue();
}
