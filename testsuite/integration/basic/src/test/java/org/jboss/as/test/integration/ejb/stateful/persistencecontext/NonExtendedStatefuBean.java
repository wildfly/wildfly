/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.stateful.persistencecontext;

import java.io.Serializable;
import javax.ejb.Stateful;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.jboss.ejb3.annotation.Cache;

/**
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 */
@Stateful
@Cache("passivating")
public class NonExtendedStatefuBean implements Serializable, StatefulRemote {
    private static final long serialVersionUID = 1L;

    @PersistenceContext
    EntityManager manager;

    public int doit() {
        Customer cust = new Customer();
        cust.setName("Bill");
        manager.persist(cust);
        return cust.getId();
    }

    public void find(int id) {
        if (manager.find(Customer.class, id) == null)
            throw new RuntimeException("not found");
    }
}
