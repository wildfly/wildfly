/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.ejb.remote.bean;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;

import org.jboss.ejb3.annotation.SecurityDomain;

@Stateless
@SecurityDomain("other")
@Remote(Incrementor.class)
public class SecureStatelessIncrementorBean extends IncrementorBean {

    @RolesAllowed({"Role1", "Role2", "Users"})
    @Override
    public Result<Integer> increment() {
        return super.increment();
    }
}
