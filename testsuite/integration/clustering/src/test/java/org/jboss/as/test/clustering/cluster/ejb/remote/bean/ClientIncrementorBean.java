/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.clustering.cluster.ejb.remote.bean;

import javax.annotation.PostConstruct;
import javax.ejb.Remote;
import javax.ejb.Stateful;
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
