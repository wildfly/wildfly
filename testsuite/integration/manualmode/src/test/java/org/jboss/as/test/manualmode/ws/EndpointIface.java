/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.manualmode.ws;

import jakarta.jws.WebService;

/**
 *
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a>
 */
@WebService
public interface EndpointIface {

    String helloString(String input);
}
