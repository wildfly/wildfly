/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.mc.descriptor;

import org.jboss.as.mc.BeanState;
import org.jboss.msc.service.ServiceName;

import java.io.Serializable;

/**
 * The legacy bean meta data.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class BeanMetaDataConfig implements Serializable, ConfigVisitorNode {
    private static final long serialVersionUID = 1L;

    /** Name prefix of all MC-style beans. */
    public static final ServiceName JBOSS_MC_POJO = ServiceName.JBOSS.append("mc", "pojo");

    private String name;
    private String beanClass;
    private ConstructorConfig constructor;

    @Override
    public void visit(ConfigVisitor visitor) {
        if (constructor != null && visitor.getState() == BeanState.DESCRIBED)
            constructor.visit(visitor);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBeanClass() {
        return beanClass;
    }

    public void setBeanClass(String beanClass) {
        this.beanClass = beanClass;
    }

    public ConstructorConfig getConstructor() {
        return constructor;
    }

    public void setConstructor(ConstructorConfig constructor) {
        this.constructor = constructor;
    }
}