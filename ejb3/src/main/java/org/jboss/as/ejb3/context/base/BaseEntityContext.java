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
package org.jboss.as.ejb3.context.base;

import org.jboss.as.ejb3.component.entity.EntityBeanComponentInstance;
import org.jboss.as.ejb3.context.spi.EntityContext;
import org.jboss.as.ejb3.context.spi.MessageDrivenBeanComponent;

import javax.ejb.EJBLocalObject;
import javax.ejb.EJBObject;

/**
 * @author Stuart Douglas
 */
public class BaseEntityContext extends BaseEJBContext implements EntityContext {

    private final EntityBeanComponentInstance instance;

    public BaseEntityContext(EntityBeanComponentInstance componentInstance) {
        super(componentInstance.getComponent(), componentInstance.getInstance());
        this.instance = componentInstance;
    }

    @Override
    public MessageDrivenBeanComponent getComponent() {
        return (MessageDrivenBeanComponent) super.getComponent();
    }

    @Override
    public EJBLocalObject getEJBLocalObject() throws IllegalStateException {
        return instance.getEjbLocalObject();
    }

    @Override
    public EJBObject getEJBObject() throws IllegalStateException {
        return instance.getEjbObject();
    }

    @Override
    public Object getPrimaryKey() throws IllegalStateException {
        return instance.getPrimaryKey();
    }
}
