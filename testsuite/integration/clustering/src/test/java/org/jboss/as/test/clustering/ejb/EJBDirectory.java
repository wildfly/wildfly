/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.ejb;

import jakarta.ejb.EJBHome;
import jakarta.ejb.SessionBean;
import jakarta.transaction.UserTransaction;

/**
 * Jakarta Enterprise Beans lookup helper
 *
 * @author Paul Ferraro
 */
public interface EJBDirectory extends AutoCloseable {
    <T> T lookupStateful(String beanName, Class<T> beanInterface) throws Exception;

    default <T> T lookupStateful(Class<? extends T> beanClass, Class<T> beanInterface) throws Exception {
        return this.lookupStateful(beanClass.getSimpleName(), beanInterface);
    }

    <T> T lookupStateless(String beanName, Class<T> beanInterface) throws Exception;

    default <T> T lookupStateless(Class<? extends T> beanClass, Class<T> beanInterface) throws Exception {
        return this.lookupStateless(beanClass.getSimpleName(), beanInterface);
    }

    <T> T lookupSingleton(String beanName, Class<T> beanInterface) throws Exception;

    default <T> T lookupSingleton(Class<? extends T> beanClass, Class<T> beanInterface) throws Exception {
        return this.lookupSingleton(beanClass.getSimpleName(), beanInterface);
    }

    <T extends EJBHome> T lookupHome(String beanName, Class<T> homeInterface) throws Exception;

    default <T extends EJBHome> T lookupHome(Class<? extends SessionBean> beanClass, Class<T> homeInterface) throws Exception {
        return this.lookupHome(beanClass.getSimpleName(), homeInterface);
    }

    UserTransaction lookupUserTransaction() throws Exception;

    @Override
    void close() throws Exception;
}
