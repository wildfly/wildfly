/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.remote.async;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author Stuart Douglas
 */
@Stateless
public class StatelessRunningBean {

    @EJB
    private RemoteInterface remoteInterface;

    public void modifyArray(final String[] array) {
        remoteInterface.modifyArray(array);
    }

}
