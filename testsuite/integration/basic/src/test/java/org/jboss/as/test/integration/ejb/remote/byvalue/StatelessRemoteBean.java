/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.byvalue;

import jakarta.ejb.Stateless;

/**
 *
 */
@Stateless
public class StatelessRemoteBean  implements RemoteInterface , LocalInterface{


    public void modifyArray(final String[] array) {
        array[0] = "goodbye";
    }
}
