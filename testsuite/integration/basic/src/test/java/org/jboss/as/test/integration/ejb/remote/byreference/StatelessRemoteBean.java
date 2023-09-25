/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.byreference;

import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;

/**
 * @author Jaikiran Pai
 */
@Stateless
@Remote(RemoteInterface.class)
public class StatelessRemoteBean implements RemoteInterface {


    @Override
    public void modifyFirstElementOfArray(String[] array, String newValue) {
        array[0] = newValue;
    }
}
