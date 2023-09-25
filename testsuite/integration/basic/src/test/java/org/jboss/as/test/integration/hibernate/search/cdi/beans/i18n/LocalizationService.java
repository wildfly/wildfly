/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.hibernate.search.cdi.beans.i18n;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@ApplicationScoped
public class LocalizationService {

    private final Map<Pair<InternationalizedValue, Language>, String> localizationMap = new HashMap<>();

    public LocalizationService() {
        register(InternationalizedValue.HELLO, Language.ENGLISH, "hello");
        register(InternationalizedValue.GOODBYE, Language.ENGLISH, "goodbye");
        register(InternationalizedValue.HELLO, Language.GERMAN, "hallo");
        register(InternationalizedValue.GOODBYE, Language.GERMAN, "auf wiedersehen");
        register(InternationalizedValue.HELLO, Language.FRENCH, "bonjour");
        register(InternationalizedValue.GOODBYE, Language.FRENCH, "au revoir");
    }

    private void register(InternationalizedValue value, Language language, String translation) {
        localizationMap.put(new Pair<>(value, language), translation);
    }

    public String localize(InternationalizedValue value, Language language) {
        return localizationMap.get(new Pair<>(value, language));
    }

    private static class Pair<L, R> {

        private final L left;
        private final R right;

        public Pair(L left, R right) {
            super();
            this.left = left;
            this.right = right;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj != null && obj.getClass() == getClass()) {
                Pair<?, ?> other = (Pair<?, ?>) obj;
                return Objects.equals(left, other.left)
                        && Objects.equals(right, other.right);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(left, right);
        }

    }
}