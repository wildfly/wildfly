/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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