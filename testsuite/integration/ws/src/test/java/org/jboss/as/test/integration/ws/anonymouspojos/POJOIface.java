/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ws.anonymouspojos;

import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
@WebService
@SOAPBinding
public interface POJOIface {

    String echo(String s);

}
