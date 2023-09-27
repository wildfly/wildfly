/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.ejb;

import jakarta.ejb.EJBHome;
import jakarta.transaction.UserTransaction;

import org.jboss.ejb.client.EJBClient;
import org.jboss.ejb.client.EJBHomeLocator;
import org.jboss.ejb.client.StatelessEJBLocator;
import org.wildfly.transaction.client.RemoteTransactionContext;

/**
 * {@link EJBDirectory} that uses the Jakarta Enterprise Beans client API.
 * @author Paul Ferraro
 */
public class ClientEJBDirectory implements EJBDirectory {

    private final String appName;
    private final String moduleName;

    public ClientEJBDirectory(String moduleName) {
        this("", moduleName);
    }

    public ClientEJBDirectory(String appName, String moduleName) {
        this.appName = appName;
        this.moduleName = moduleName;
    }

    @Override
    public <T> T lookupStateful(String beanName, Class<T> beanInterface) throws Exception {
        return EJBClient.createSessionProxy(this.createStatelessLocator(beanName, beanInterface));
    }

    @Override
    public <T> T lookupStateless(String beanName, Class<T> beanInterface) {
        return EJBClient.createProxy(this.createStatelessLocator(beanName, beanInterface));
    }

    @Override
    public <T> T lookupSingleton(String beanName, Class<T> beanInterface) {
        return EJBClient.createProxy(this.createStatelessLocator(beanName, beanInterface));
    }

    private <T> StatelessEJBLocator<T> createStatelessLocator(String beanName, Class<T> beanInterface) {
        return new StatelessEJBLocator<>(beanInterface, this.appName, this.moduleName, beanName);
    }

    @Override
    public <T extends EJBHome> T lookupHome(String beanName, Class<T> homeInterface) {
        return EJBClient.createProxy(new EJBHomeLocator<>(homeInterface, this.appName, this.moduleName, beanName));
    }

    @Override
    public UserTransaction lookupUserTransaction() {
        return RemoteTransactionContext.getInstance().getUserTransaction();
    }

    @Override
    public void close() {
        // Do nothing
    }
}
