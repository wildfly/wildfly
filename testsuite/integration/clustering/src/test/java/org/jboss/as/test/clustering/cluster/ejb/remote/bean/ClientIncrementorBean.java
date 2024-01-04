/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.ejb.remote.bean;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Remote;
import jakarta.ejb.Stateful;
import javax.naming.NamingException;

import org.jboss.as.test.clustering.ejb.RemoteEJBDirectory;

@Stateful(passivationCapable = false)
@Remote(Incrementor.class)
public class ClientIncrementorBean implements Incrementor {
    public static final String MODULE = "remote";

    private Incrementor locator;

    @PostConstruct
    public void postConstruct() {
        try (RemoteEJBDirectory directory = new RemoteEJBDirectory(MODULE)) {
            this.locator = directory.lookupStateful(StatefulIncrementorBean.class, Incrementor.class);
        } catch (NamingException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Result<Integer> increment() {
        return this.locator.increment();
    }
}
