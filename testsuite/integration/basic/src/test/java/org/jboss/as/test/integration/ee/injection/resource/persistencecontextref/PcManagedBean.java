/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.test.integration.ee.injection.resource.persistencecontextref;

import javax.annotation.ManagedBean;
import javax.ejb.EJB;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * Managed bean with persistence unit definitions.
 * @author Stuart Douglas
 */
@ManagedBean("pcManagedBean")
public class PcManagedBean {

    @PersistenceContext(unitName = "mypc")
    private EntityManager mypc;

    //this one will be overridden via deployment descriptor to be otherpu
    @PersistenceContext(unitName = "mypc", name = "otherPcBinding")
    private EntityManager otherpc;

    @EJB
    SFSB sfsb;


    //this one is injected via deployment descriptor
    private EntityManager mypc2;

    public EntityManager getMypc2() {
        return mypc2;
    }

    public EntityManager getMypc() {
        return mypc;
    }

    public EntityManager getOtherpc() {
        return otherpc;
    }

    public boolean unsynchronizedIsNotJoinedToTX() {
        return sfsb.unsynchronizedIsNotJoinedToTX();
    }

    public boolean synchronizedIsJoinedToTX() {
        return sfsb.synchronizedIsJoinedToTX();
    }

}
