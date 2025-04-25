/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.mod_cluster.undertow;

import org.jboss.as.controller.PathAddress;
import org.wildfly.common.function.Functions;
import org.wildfly.extension.undertow.Constants;
import org.wildfly.extension.undertow.HttpsListenerService;
import org.xnio.OptionMap;

/**
 * @author Paul Ferraro
 */
class TestListener extends HttpsListenerService {

    TestListener(TestServer server) {
        super(Functions.discardingConsumer(), PathAddress.pathAddress(Constants.HTTPS_LISTENER, "default"), "foo", OptionMap.EMPTY, null, OptionMap.EMPTY, false);
        server.registerListener(this);
    }
}
