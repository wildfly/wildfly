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

package org.jboss.as.test.integration.ejb.remote.entity.cmp.commerce;

import java.util.Collection;
import javax.ejb.EJBLocalObject;

public interface ProductLocal extends EJBLocalObject {
    Long getId();

    void setId(Long id);

    String getName();

    void setName(String name);

    String getType();

    void setType(String type);

    String getUnit();

    void setUnit(String unit);

    double getCostPerUnit();

    void setCostPerUnit(double cost);

    double getWeight();

    void setWeight(double weight);

    double getLength();

    void setLength(double length);

    double getGirth();

    void setGirth(double girth);

    Collection getProductCategories();

    void setProductCategories(Collection productCategories);
}
