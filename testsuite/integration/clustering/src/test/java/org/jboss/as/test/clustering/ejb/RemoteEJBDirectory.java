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

import javax.naming.Context;
import javax.naming.NamingException;

import org.wildfly.naming.client.WildFlyInitialContextFactory;

/**
 * @author Paul Ferraro
 */
public class RemoteEJBDirectory extends AbstractEJBDirectory {
    private static final Properties env = new Properties();

    static {
        env.setProperty(Context.INITIAL_CONTEXT_FACTORY, WildFlyInitialContextFactory.class.getName());
    }

    private final String module;

    public RemoteEJBDirectory(String module) throws NamingException {
        super(env);
        this.module = module;
    }

    @Override
    protected <T> String createJndiName(String beanName, Class<T> beanInterface, Type type) {
        return String.format("ejb:/%s/%s!%s%s", this.module, beanName, beanInterface.getName(), (type == Type.STATEFUL) ? "?stateful" : "");
    }
}
