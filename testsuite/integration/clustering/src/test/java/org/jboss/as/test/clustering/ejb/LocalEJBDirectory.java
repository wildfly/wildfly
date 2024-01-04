/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.ejb;

import java.util.Properties;

import javax.naming.NamingException;

/**
 * {@link EJBDirectory} that uses local JNDI.
 * @author Paul Ferraro
 */
public class LocalEJBDirectory extends NamingEJBDirectory {

    public LocalEJBDirectory(String module) throws NamingException {
        this(module, new Properties());
    }

    public LocalEJBDirectory(String module, Properties properties) throws NamingException {
        super(properties, "java:app", module, "java:comp/UserTransaction");
    }

    public <T> T lookupStateful(Class<T> beanClass) throws Exception {
        return this.lookupStateful(beanClass, beanClass);
    }

    public <T> T lookupStateless(Class<T> beanClass) throws Exception {
        return this.lookupStateless(beanClass, beanClass);
    }

    public <T> T lookupSingleton(Class<T> beanClass) throws Exception {
        return this.lookupSingleton(beanClass, beanClass);
    }
}
