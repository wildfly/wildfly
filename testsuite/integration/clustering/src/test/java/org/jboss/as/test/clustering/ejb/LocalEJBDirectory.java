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

package org.jboss.as.test.clustering.ejb;

import java.util.Properties;
import javax.naming.InitialContext;

import javax.naming.NamingException;

/**
 * @author Paul Ferraro
 */
public class LocalEJBDirectory extends AbstractEJBDirectory {
    private final String module;

    public LocalEJBDirectory(String module) throws NamingException {
        super(new Properties());
        this.module = module;
    }

    public LocalEJBDirectory(String module, InitialContext context) {
        super(context);
        this.module = module;
    }

    public <T> T lookupStateful(Class<T> beanClass) throws NamingException {
        return this.lookupStateful(beanClass, beanClass);
    }

    public <T> T lookupStateless(Class<T> beanClass) throws NamingException {
        return this.lookupStateless(beanClass, beanClass);
    }

    public <T> T lookupSingleton(Class<T> beanClass) throws NamingException {
        return this.lookupSingleton(beanClass, beanClass);
    }

    @Override
    protected <T> String createJndiName(String beanName, Class<T> beanInterface, Type type) {
        return String.format("java:app/%s/%s!%s", this.module, beanName, beanInterface.getName());
    }
}
