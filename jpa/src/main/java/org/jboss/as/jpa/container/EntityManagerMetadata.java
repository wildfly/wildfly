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

package org.jboss.as.jpa.container;

import java.io.Serializable;

/**
 * Identifies a subset of information about an EntityManager
 *
 * @author Scott Marlow
 */
public class EntityManagerMetadata implements Serializable {
    private static final long serialVersionUID = -54354321L;
    private String puName;
    private String puScopedName;
    private boolean isTransactionScopedEntityManager;

    /**
     * set the scoped persistence unit name
     */
    public void setScopedPuName(String name) {
        this.puScopedName = name;
        int index = name.indexOf("#");
        if (index == -1) {
            throw new RuntimeException("scoped persistence name should be \"APPLICATION_SCOPE#PU-NAME\" but was " + name);
        }
        this.puName = name.substring(index + 1);
    }

    /**
     * return the scoped persistence unit name
     */
    public String getScopedPuName() {
        return puScopedName;
    }

    /**
     * return the short Persistence Unit name.
     *
     * @return
     */
    public String getPuName() {
        return puName;
    }

    public void setTransactionScopedEntityManager(boolean isTransactionScopedEntityManager) {
        this.isTransactionScopedEntityManager = isTransactionScopedEntityManager;
    }

    /**
     * Returns true if this is a transactional scoped entity manager
     *
     * @return
     */
    public boolean isTransactionScopedEntityManager() {
        return this.isTransactionScopedEntityManager;
    }

    /**
     * Returns true if this is an extended persistence context (entity manager)
     */
    public boolean isExtendedPersistenceContext() {
        return !this.isTransactionScopedEntityManager();
    }

}
