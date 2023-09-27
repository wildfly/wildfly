/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.ejb;

import java.util.Properties;

import jakarta.ejb.EJBHome;
import jakarta.ejb.SessionBean;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.transaction.UserTransaction;


/**
 * Abstract JNDI-based {@link EJBDirectory}.
 * @author Paul Ferraro
 */
public abstract class NamingEJBDirectory implements EJBDirectory {
    private final Context context;
    private final String prefix;
    private final String moduleName;
    private final String txContextName;

    protected enum Type {
        STATEFUL, STATELESS, SINGLETON, HOME
    }

    protected NamingEJBDirectory(Properties env, String prefix, String moduleName, String txContextName) throws NamingException {
        this(new InitialContext(env), prefix, moduleName, txContextName);
    }

    protected NamingEJBDirectory(InitialContext context, String prefix, String moduleName, String txContextName) {
        this.context = context;
        this.prefix = prefix;
        this.moduleName = moduleName;
        this.txContextName = txContextName;
    }

    @Override
    public void close() throws NamingException {
        this.context.close();
    }

    @Override
    public <T> T lookupStateful(Class<? extends T> beanClass, Class<T> beanInterface) throws NamingException {
        return this.lookupStateful(beanClass.getSimpleName(), beanInterface);
    }

    @Override
    public <T> T lookupStateless(Class<? extends T> beanClass, Class<T> beanInterface) throws NamingException {
        return this.lookupStateless(beanClass.getSimpleName(), beanInterface);
    }

    @Override
    public <T> T lookupSingleton(Class<? extends T> beanClass, Class<T> beanInterface) throws NamingException {
        return this.lookupSingleton(beanClass.getSimpleName(), beanInterface);
    }

    @Override
    public <T extends EJBHome> T lookupHome(Class<? extends SessionBean> beanClass, Class<T> homeInterface) throws NamingException {
        return this.lookupHome(beanClass.getSimpleName(), homeInterface);
    }

    @Override
    public <T> T lookupStateful(String beanName, Class<T> beanInterface) throws NamingException {
        return this.lookup(beanName, beanInterface, Type.STATEFUL);
    }

    @Override
    public <T> T lookupStateless(String beanName, Class<T> beanInterface) throws NamingException {
        return this.lookup(beanName, beanInterface, Type.STATELESS);
    }

    @Override
    public <T> T lookupSingleton(String beanName, Class<T> beanInterface) throws NamingException {
        return this.lookup(beanName, beanInterface, Type.SINGLETON);
    }

    @Override
    public <T extends EJBHome> T lookupHome(String beanName, Class<T> homeInterface) throws NamingException {
        return this.lookup(beanName, homeInterface, Type.HOME);
    }

    @Override
    public UserTransaction lookupUserTransaction() throws NamingException {
        return this.lookup(this.txContextName, UserTransaction.class);
    }

    protected <T> T lookup(String beanName, Class<T> beanInterface, Type type) throws NamingException {
        return this.lookup(this.createJndiName(beanName, beanInterface, type), beanInterface);
    }

    protected String createJndiName(String beanName, Class<?> beanInterface, Type type) {
        return String.format("%s/%s/%s!%s", this.prefix, this.moduleName, beanName, beanInterface.getName());
    }

    protected <T> T lookup(String name, Class<T> targetClass) throws NamingException {
        return targetClass.cast(this.context.lookup(name));
    }
}
