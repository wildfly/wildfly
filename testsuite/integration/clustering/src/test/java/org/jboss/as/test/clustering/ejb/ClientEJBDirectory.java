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

import javax.ejb.EJBHome;
import javax.transaction.UserTransaction;

import org.jboss.ejb.client.EJBClient;
import org.jboss.ejb.client.EJBHomeLocator;
import org.jboss.ejb.client.StatelessEJBLocator;
import org.wildfly.transaction.client.RemoteTransactionContext;

/**
 * {@link EJBDirectory} that uses the EJB client API.
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
