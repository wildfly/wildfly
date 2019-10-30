/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.jpa.epcpropagation;

import javax.annotation.Resource;
import javax.ejb.Local;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 */
@Stateless
@Local
public class StatelessBean implements StatelessInterface {
    @PersistenceContext(unitName = "mypc")
    EntityManager em;

    @Resource
    SessionContext sessionContext;

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void createEntity(Integer id, String name) {
        MyEntity entity = em.find(MyEntity.class, id);
        if (entity == null) {
            entity = new MyEntity();
            entity.setId(id);
            em.persist(entity);
        }
        entity.setName(name);
    }

    /**
     * can only be called from a SFSB with an extended persistence context
     *
     * @param id
     * @param name
     */
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public void createEntityNoTx(Integer id, String name) {

    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public String updateEntity(Integer id, String name) {
        MyEntity entity = em.find(MyEntity.class, id);
        String propagatedName = entity.getName();

        entity.setName(name);

        return propagatedName;
    }

}