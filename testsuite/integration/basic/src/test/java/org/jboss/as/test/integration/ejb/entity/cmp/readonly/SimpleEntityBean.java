/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.ejb.entity.cmp.readonly;

import javax.ejb.CreateException;
import javax.ejb.EntityBean;
import javax.ejb.EntityContext;

/**
 *
 * EJB2 CMP bean, simple implementation for container test.
 *
 *
 * @author <a href="mailto:wfink@redhat.com">Wolf-Dieter Fink</a>
 *
 * @ejb.bean name="SimpleEntity" display-name="SimpleEntity" local-jndi-name="local/SimpleEntity" view-type="local" type="CMP"
 *           cmp-version="2.x" primkey-field="id"
 * @ejb.home local-class="de.wfink.ejb21.cmp.SimpleEntityLocalHome"
 * @ejb.interface local-class="org.jboss.as.test.integration.ejb.entity.cmp.findbypkey.SimpleEntityLocal"
 * @ejb.transaction type="Required"
 *
 * @jboss.create-table create="true"
 * @jboss.remove-table remove="false"
 *
 * @ejb.finder signature="org.jboss.as.test.integration.ejb.entity.cmp.findbypkey.SimpleEntityLocal findById(java.lang.Long id)"
 *             query="SELECT OBJECT(o) FROM SimpleEntity o WHERE o.id = ?1"
 */
public abstract class SimpleEntityBean implements EntityBean {
    private transient EntityContext ctx;

    public void setEntityContext(EntityContext ctx) {
        this.ctx = ctx;
    }

    public void unsetEntityContext() {
        this.ctx = null;
    }

    public void ejbActivate() {
    }

    public void ejbPassivate() {
    }

    public void ejbLoad() {
    }

    public void ejbStore() {
    }

    public void ejbRemove() {
    }

    /**
     * Invoked by the container when the create method is called on the home interface.
     *
     * @param id The value of the primary key property.
     * @param name The given name of the object
     * @throws javax.ejb.CreateException
     *
     * @ejb.create-method view-type="local"
     */
    public Long ejbCreate(Long id, String name) throws CreateException {
        setId(id);
        setName(name);
        return (null);
    }

    public void ejbPostCreate(Long id, String name) {
    }

    /**
     * @ejb.persistence
     * @ejb.interface-method
     */
    public abstract Long getId();

    /**
     * @ejb.interface-method
     */
    public abstract void setId(Long id);

    /**
     * @ejb.persistence
     * @ejb.interface-method
     */
    public abstract String getName();

    /**
     * @ejb.interface-method
     */
    public abstract void setName(String name);

    /**
     * @ejb.persistence
     * @ejb.interface-method
     */
    public abstract String getLastName();

    /**
     * @ejb.interface-method
     */
    public abstract void setLastName(String lastName);
}
