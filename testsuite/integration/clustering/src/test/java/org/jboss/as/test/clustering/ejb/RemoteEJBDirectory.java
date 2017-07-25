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

import org.jboss.ejb.client.Affinity;
import org.jboss.ejb.client.ClusterAffinity;
import org.jboss.ejb.client.EJBClient;

/**
 * @author Paul Ferraro
 */
public class RemoteEJBDirectory extends AbstractEJBDirectory {

    private static Properties createEnvironment() {
        Properties env = new Properties();
        env.setProperty(Context.INITIAL_CONTEXT_FACTORY, org.wildfly.naming.client.WildFlyInitialContextFactory.class.getName());
        return env;
    }

    private final String module;

    public RemoteEJBDirectory(String module) throws NamingException {
        super(createEnvironment());
        this.module = module;
    }

    @Override
    protected <T> String createJndiName(String beanName, Class<T> beanInterface, Type type) {
        return String.format("ejb:/%s/%s!%s%s", this.module, beanName, beanInterface.getName(), (type == Type.STATEFUL) ? "?stateful" : "");
    }

    @Override
    protected <T> T lookup(String beanName, Class<T> beanInterface, Type type) throws NamingException {
        T bean = super.lookup(beanName, beanInterface, type);
        Affinity affinity = new ClusterAffinity("ejb");
        switch (type) {
            case STATEFUL: {
                EJBClient.setStrongAffinity(bean, affinity);
                break;
            }
            case STATELESS: {
                EJBClient.setWeakAffinity(bean, affinity);
                break;
            }
            default: {
                // No need to set initial affinity
            }
        }
        return bean;
    }
}
