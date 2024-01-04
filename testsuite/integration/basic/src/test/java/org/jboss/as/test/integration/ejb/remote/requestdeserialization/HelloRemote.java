/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.requestdeserialization;

import jakarta.ejb.Remote;

@Remote
public interface HelloRemote {

    Response sayHello(Request request);

}
