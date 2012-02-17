/*
 * JBoss, Home of Professional Open Source
 * Copyright (c) 2010, JBoss Inc., and individual contributors as indicated
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
package org.jboss.as.ejb3.context;

import javax.ejb.EJBLocalObject;
import javax.ejb.EJBObject;
import javax.ejb.EntityBean;
import javax.transaction.UserTransaction;

import org.jboss.as.ejb3.component.allowedmethods.AllowedMethodsInformation;
import org.jboss.as.ejb3.component.allowedmethods.MethodType;
import org.jboss.as.ejb3.component.entity.EntityBeanComponent;
import org.jboss.as.ejb3.component.entity.EntityBeanComponentInstance;

/**
 * @author Stuart Douglas
 */
public class EntityContextImpl extends EJBContextImpl implements javax.ejb.EntityContext {

    private final EntityBeanComponentInstance instance;

    public EntityContextImpl(EntityBeanComponentInstance componentInstance) {
        super(componentInstance);
        this.instance = componentInstance;
    }

    @Override
    public EntityBeanComponent getComponent() {
        return (EntityBeanComponent) super.getComponent();
    }

    @Override
    public UserTransaction getUserTransaction() throws IllegalStateException {
        return getComponent().getUserTransaction();
    }

    public EJBLocalObject getEJBLocalObject() throws IllegalStateException {
        return instance.getEjbLocalObject();
    }

    public EJBObject getEJBObject() throws IllegalStateException {
        return instance.getEjbObject();
    }

    public Object getPrimaryKey() throws IllegalStateException {
        AllowedMethodsInformation.checkAllowed(MethodType.GET_PRIMARY_KEY);
        return instance.getPrimaryKey();
    }

    public EntityBean getInstance() {
        return instance.getInstance();
    }

    public boolean isRemoved() {
        return instance.isRemoved();
    }
}
