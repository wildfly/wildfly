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