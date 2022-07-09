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

import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBindingContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;
import org.jboss.as.test.integration.hibernate.search.cdi.beans.i18n.InternationalizedValue;
import org.jboss.as.test.integration.hibernate.search.cdi.beans.i18n.Language;
import org.jboss.as.test.integration.hibernate.search.cdi.beans.i18n.LocalizationService;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

@Dependent
public class InternationalizedValueBridgeImpl implements InternationalizedValueBinder {

    @Inject
    private LocalizationService localizationService;

    @Override
    public void bind(ValueBindingContext<?> context) {
        Language targetLanguage = Language.valueOf((String) context.param("language"));
        context.bridge(InternationalizedValue.class, new Bridge(localizationService, targetLanguage));
    }

    private static class Bridge implements ValueBridge<InternationalizedValue, String> {
        private final LocalizationService localizationService;
        private final Language targetLanguage;

        private Bridge(LocalizationService localizationService, Language targetLanguage) {
            this.localizationService = localizationService;
            this.targetLanguage = targetLanguage;
        }

        @Override
        public String toIndexedValue(InternationalizedValue value, ValueBridgeToIndexedValueContext context) {
            return localizationService.localize(value, targetLanguage);
        }
    }

}