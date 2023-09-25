/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.remote.ejbnamespace;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author Stuart Douglas
 */
@Stateless
public class SimpleEjb {

    @EJB(lookup="ejb:/RemoteInvocationTest//StatelessRemoteBean!org.jboss.as.test.integration.ejb.remote.ejbnamespace.RemoteInterface")
    RemoteInterface ejb;


    public String hello() {
        return ejb.hello();
    }


}
