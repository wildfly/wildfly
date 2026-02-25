/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.timeout;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;

public class HttpListenerTimeoutTestCase extends ListenerTimeoutTestCaseBase{
    private static final PathAddress ADDRESS_HTTP_LISTENER = PathAddress.pathAddress(
            PathElement.pathElement(SUBSYSTEM, "undertow"), PathElement.pathElement(SERVER, "default-server"),
            PathElement.pathElement("http-listener", "default"));
    @Override
    protected PathAddress getListenerAddress() {
        return ADDRESS_HTTP_LISTENER;
    }

}
