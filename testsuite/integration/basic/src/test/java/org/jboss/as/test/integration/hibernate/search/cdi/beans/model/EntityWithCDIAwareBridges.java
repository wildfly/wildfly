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

import org.hibernate.search.annotations.ClassBridge;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Indexed;
import org.jboss.as.test.integration.hibernate.search.cdi.beans.bridge.InternationalizedValueBridge;
import org.jboss.as.test.integration.hibernate.search.cdi.beans.bridge.InternationalizedValueClassBridge;
import org.jboss.as.test.integration.hibernate.search.cdi.beans.i18n.InternationalizedValue;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
@Indexed
@ClassBridge(name = EntityWithCDIAwareBridges.CLASS_BRIDGE_FIELD_NAME, impl = InternationalizedValueClassBridge.class)
public class EntityWithCDIAwareBridges {

    public static final String CLASS_BRIDGE_FIELD_NAME = "classBridge.internationalizedValue";

    @Id
    @GeneratedValue
    private Long id;

    @Field(bridge = @FieldBridge(impl = InternationalizedValueBridge.class))
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
