/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.hibernate.search.cdi.beans.model;

import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.TypeBinderRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBinderRef;
import org.hibernate.search.mapper.pojo.common.annotation.Param;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.TypeBinding;
import org.jboss.as.test.integration.hibernate.search.cdi.beans.bridge.InternationalizedValueBinder;
import org.jboss.as.test.integration.hibernate.search.cdi.beans.bridge.InternationalizedValueTypeBinder;
import org.jboss.as.test.integration.hibernate.search.cdi.beans.i18n.InternationalizedValue;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
@Indexed
@TypeBinding(binder = @TypeBinderRef(type = InternationalizedValueTypeBinder.class,
        params = @Param(name = "fieldName", value = EntityWithCDIAwareBridges.TYPE_BRIDGE_FIELD_NAME)))
public class EntityWithCDIAwareBridges {

    public static final String TYPE_BRIDGE_FIELD_NAME = "typeBridge_internationalizedValue";

    @Id
    @GeneratedValue
    private Long id;

    @FullTextField(name = "value_fr", valueBinder = @ValueBinderRef(type = InternationalizedValueBinder.class,
            params = @Param(name = "language", value = "FRENCH")))
    @FullTextField(name = "value_de", valueBinder = @ValueBinderRef(type = InternationalizedValueBinder.class,
            params = @Param(name = "language", value = "GERMAN")))
    @FullTextField(name = "value_en", valueBinder = @ValueBinderRef(type = InternationalizedValueBinder.class,
            params = @Param(name = "language", value = "ENGLISH")))
    private InternationalizedValue internationalizedValue;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public InternationalizedValue getInternationalizedValue() {
        return internationalizedValue;
    }

    public void setInternationalizedValue(InternationalizedValue internationalizedValue) {
        this.internationalizedValue = internationalizedValue;
    }

}
