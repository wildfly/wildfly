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
package org.jboss.as.test.integration.ejb.entity.cmp.findbypkey;

import javax.ejb.CreateException;
import javax.ejb.EntityBean;
import javax.ejb.EntityContext;
import javax.ejb.RemoveException;

import org.jboss.logging.Logger;

/**
 *
 * EJB CMP bean simple implementation.
 *
 *
 * @author @author <a href="mailto:wfink@redhat.com">Wolf-Dieter Fink</a>
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
    private static final Logger LOG = Logger.getLogger(SimpleEntityBean.class);

    private int ejbStoreCall = 0;

    public void ejbActivate() {
    }

    public void ejbLoad() {
        LOG.info("LOAD pkey="+getId());
    }

    public void ejbPassivate() {
    }

    public void ejbRemove() throws RemoveException {
        LOG.info("REMOVE pkey="+getId());
    }

    public void ejbStore() {
        ejbStoreCall++;
        LOG.info("STORE pkey="+getId()+" calls="+ejbStoreCall);
    }

    public void setEntityContext(EntityContext arg0) {
    }

    public void unsetEntityContext() {
    }

    public Long ejbCreate() throws CreateException {
        return (null);
    }

    public void ejbPostCreate() {
    }

    /**
     * Invoked by the container when the create method is called on the home interface.
     *
     * @param id The value of the primary key property.
     * @param name The given name of the object
     * @param timestamp The creation time, if not set the current time is used
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

    public int getEjbStoreCounter() {
        return this.ejbStoreCall;
    }
}
