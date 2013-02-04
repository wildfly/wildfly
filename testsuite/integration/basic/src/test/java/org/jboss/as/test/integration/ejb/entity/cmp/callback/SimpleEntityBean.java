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
package org.jboss.as.test.integration.ejb.entity.cmp.callback;

import javax.ejb.CreateException;
import javax.ejb.EntityBean;
import javax.ejb.EntityContext;
import javax.ejb.RemoveException;

import org.jboss.logging.Logger;

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
    private static final Logger LOG = Logger.getLogger(SimpleEntityBean.class);

    private EntityContext ctx;

    private SingletonTestResults callbackLog;

    public void ejbActivate() {
        logCallbackInvocation(getId().toString(), "ejbActivate");
    }

    public void ejbLoad() {
        logCallbackInvocation(getId().toString(), "ejbLoad");
    }

    public void ejbPassivate() {
        logCallbackInvocation(getId().toString(), "ejbPassivate");
    }

    public void ejbRemove() throws RemoveException {
        logCallbackInvocation(getId().toString(), "ejbRemove");
    }

    public void ejbStore() {
        logCallbackInvocation(getId().toString(), "ejbStore");
    }

    public void setEntityContext(EntityContext context) {
        this.ctx = context;
        this.callbackLog = (SingletonTestResults)this.ctx.lookup("java:module/" + SingletonTestResultsBean.class.getSimpleName());
        logCallbackInvocation("", "setEntityContext");
    }

    public void unsetEntityContext() {
        logCallbackInvocation("", "unsetEntityContext");
        this.ctx = null;
    }

    public Long ejbCreate() throws CreateException {
        logCallbackInvocation("", "ejbCreate");
        return (null);
    }

    public void ejbPostCreate() {
        logCallbackInvocation("", "ejbPostCreate");
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
        logCallbackInvocation(String.valueOf(id), "ejbCreate");
        setId(id);
        setName(name);
        return (null);
    }

    public void ejbPostCreate(Long id, String name) {
        logCallbackInvocation(getId().toString(), "ejbPostCreate");
    }

    private void logCallbackInvocation(String pKey, String callbackName) {
        LOG.info("Callback '"+callbackName+"' executed for " +this.getClass().getSimpleName()+" pKey="+pKey);
        this.callbackLog.setMethodExecuted("SimpleEntity", pKey, callbackName);
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
}
