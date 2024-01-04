/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.ejbnamespace;

import jakarta.ejb.Stateless;

/**
 *
 */
@Stateless
public class StatelessRemoteBean  implements RemoteInterface {


    @Override
    public String hello() {
        return "hello";
    }
}
