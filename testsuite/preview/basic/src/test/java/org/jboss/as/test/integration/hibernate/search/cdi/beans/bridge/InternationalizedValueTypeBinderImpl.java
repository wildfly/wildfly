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
package org.jboss.as.test.integration.hibernate.search.cdi.beans.bridge;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext;
import org.jboss.as.test.integration.hibernate.search.cdi.beans.i18n.InternationalizedValue;
import org.jboss.as.test.integration.hibernate.search.cdi.beans.i18n.Language;
import org.jboss.as.test.integration.hibernate.search.cdi.beans.i18n.LocalizationService;
import org.jboss.as.test.integration.hibernate.search.cdi.beans.model.EntityWithCDIAwareBridges;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

@Dependent
public class InternationalizedValueTypeBinderImpl implements InternationalizedValueTypeBinder {

    @Inject
    private LocalizationService localizationService;

    @Override
    public void bind(TypeBindingContext context) {
        context.dependencies().useRootOnly();

        String fieldName = (String) context.param("fieldName");
        IndexFieldReference<String> fieldRef = context.indexSchemaElement()
                .field(fieldName, f -> f.asString())
                .multiValued()
                .toReference();
        context.bridge( EntityWithCDIAwareBridges.class, new Bridge( localizationService, fieldRef ) );
    }

    private static class Bridge implements TypeBridge<EntityWithCDIAwareBridges> {
        private final LocalizationService localizationService;

        private final IndexFieldReference<String> fieldRef;

        private Bridge(LocalizationService localizationService, IndexFieldReference<String> fieldRef) {
            this.localizationService = localizationService;
            this.fieldRef = fieldRef;
        }

        @Override
        public void write(DocumentElement target, EntityWithCDIAwareBridges source, TypeBridgeWriteContext context) {
            InternationalizedValue internationalizedValue = source.getInternationalizedValue();
            for (Language language : Language.values()) {
                String localizedValue = localizationService.localize(internationalizedValue, language);
                target.addValue(fieldRef, localizedValue);
            }
        }
    }

}