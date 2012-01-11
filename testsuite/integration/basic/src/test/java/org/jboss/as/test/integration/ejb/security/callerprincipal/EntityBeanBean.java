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

package org.jboss.as.test.integration.ejb.security.callerprincipal;

import java.security.Principal;

import javax.ejb.CreateException;
import javax.ejb.EntityBean;
import javax.ejb.EntityContext;
import org.jboss.logging.Logger;



public abstract class  EntityBeanBean implements EntityBean {
    final static long serialVersionUID = 1L;
    
    private static final Logger log = Logger.getLogger(EntityBeanBean.class);
       
    private transient EntityContext ctx;
    
    private ITestResultsSingleton getSingleton() {
        try {
            return (ITestResultsSingleton) ctx.lookup("java:global/single/" + TestResultsSingleton.class.getSimpleName());
        } catch (Exception e) {
            return null;
        }
    }

    public EntityBeanBean() {
    }

    public String ejbCreate(String id) throws CreateException {
        setId(id);
        return id;
    }

    public void ejbPostCreate(String id) {
    }
       
    public void ejbActivate() {
        ITestResultsSingleton results = this.getSingleton();
        log.info(EntityBeanBean.class.getSimpleName() + " method: ejbActivate");
        
        Principal princ = null;
        try {
            princ = ctx.getCallerPrincipal();
        } catch (IllegalStateException e) {
            results.setEb("ejbactivate", "OK");
            return;
        }
        results.setEb("ejbactivate", "Method getCallerPrincipal was called from ejbActivate with result: " + princ);
        
    }

    //TODO: test ejbPassivate
    public void ejbPassivate() {
        log.info(EntityBeanBean.class.getSimpleName() + " method: ejbPassivate");
    }

    public void ejbLoad() {
    }

    public void ejbStore() {
    }

    public void ejbRemove() {
    }
   
    public void setEntityContext(EntityContext ctx) {
        this.ctx = ctx;
    }

    public void unsetEntityContext() {
        this.ctx = null;
    }

    
    public abstract String getId();
    
    public abstract void setId(String id);
}
