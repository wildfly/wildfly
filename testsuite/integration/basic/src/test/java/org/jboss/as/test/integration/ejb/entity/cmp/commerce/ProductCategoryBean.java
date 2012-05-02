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
package org.jboss.as.test.integration.ejb.entity.cmp.commerce;

import java.util.Collection;
import javax.ejb.CreateException;
import javax.ejb.EntityBean;
import javax.ejb.EntityContext;

public abstract class ProductCategoryBean implements EntityBean {
    transient private EntityContext ctx;

    private static long id = 0;

    public CompositeId ejbCreate() throws CreateException {
        setId(id++);
        setSubId(id++);
        return null;
    }

    public void ejbPostCreate() {
    }

    public abstract long getId();

    public abstract void setId(long id);

    public abstract long getSubId();

    public abstract void setSubId(long id);

    public abstract String getName();

    public abstract void setName(String name);

    public abstract Collection getProducts();

    public abstract void setProducts(Collection girth);

    public abstract ProductCategory getParent();

    public abstract void setParent(ProductCategory parent);

    public abstract Collection getSubcategories();

    public abstract void setSubcategories(Collection subcategories);

    public abstract ProductCategoryType getType();

    public abstract void setType(ProductCategoryType type);

    public CompositeId getPK() {
        return (CompositeId) ctx.getPrimaryKey();
    }

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

    public void ejbHomeResetId() {
        id = 0;
    }
}
