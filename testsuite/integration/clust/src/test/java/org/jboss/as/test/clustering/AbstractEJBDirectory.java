/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.test.clustering;

import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;


/**
 * @author Paul Ferraro
 *
 */
public abstract class AbstractEJBDirectory implements EJBDirectory {
    private final Context context;

    protected enum Type {
        STATEFUL, STATELESS, SINGLETON
    };
    
    protected AbstractEJBDirectory(Properties env) throws NamingException {
        this.context = new InitialContext(env);
    }

    public <T> T lookupStateful(Class<? extends T> beanClass, Class<T> beanInterface) throws NamingException {
        return this.lookup(beanClass, beanInterface, Type.STATEFUL);
    }
    
    public <T> T lookupStateless(Class<? extends T> beanClass, Class<T> beanInterface) throws NamingException {
        return this.lookup(beanClass, beanInterface, Type.STATELESS);
    }
    
    public <T> T lookupSingleton(Class<? extends T> beanClass, Class<T> beanInterface) throws NamingException {
        return this.lookup(beanClass, beanInterface, Type.SINGLETON);
    }

    protected <T> T lookup(Class<? extends T> beanClass, Class<T> beanInterface, Type type) throws NamingException {
        return this.lookup(this.createJndiName(beanClass, beanInterface, type), beanInterface);
    }

    protected abstract <T> String createJndiName(Class<? extends T> beanClass, Class<T> beanInterface, Type type);

    protected <T> T lookup(String name, Class<T> beanInterface) throws NamingException {
        return beanInterface.cast(this.context.lookup(name));
    }
}
