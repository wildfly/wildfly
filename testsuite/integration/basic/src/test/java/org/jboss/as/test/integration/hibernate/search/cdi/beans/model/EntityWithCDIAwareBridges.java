/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
