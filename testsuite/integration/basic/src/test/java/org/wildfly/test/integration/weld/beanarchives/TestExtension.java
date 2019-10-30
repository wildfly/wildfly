/*
 * JBoss, Home of Professional Open Source
 * Copyright 2017, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.wildfly.test.integration.weld.beanarchives;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.util.TypeLiteral;

/**
 * @author Martin Kouba
 */
public class TestExtension implements Extension {

    public void beforeBeanDiscovery(@Observes AfterBeanDiscovery event) {
        Map<String, String> alphaMap = new HashMap<>();
        alphaMap.put("foo", "bar");
        event.addBean(new MapBean(alphaMap, Alpha.Literal.INSTANCE));
        Map<String, String> bravoMap = new HashMap<>();
        bravoMap.put("foo", "bar");
        event.addBean(new MapBean(bravoMap, Bravo.Literal.INSTANCE));
    }

    private static class MapBean implements Bean<Map<String, String>> {

        private final Map<String, String> instance;

        private final Set<Annotation> qualifiers;

        MapBean(Map<String, String> instance, Annotation... qualifiers) {
            this.instance = instance;
            this.qualifiers = new HashSet<>();
            Collections.addAll(this.qualifiers, qualifiers);
        }

        @Override
        public Map<String, String> create(CreationalContext<Map<String, String>> creationalContext) {
            return instance;
        }

        @Override
        public void destroy(Map<String, String> instance, CreationalContext<Map<String, String>> creationalContext) {
            instance.clear();
        }

        @SuppressWarnings("serial")
        @Override
        public Set<Type> getTypes() {
            return Collections.singleton(new TypeLiteral<Map<String, String>>() {
            }.getType());
        }

        @Override
        public Set<Annotation> getQualifiers() {
            return qualifiers;
        }

        @Override
        public Class<? extends Annotation> getScope() {
            return Dependent.class;
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public Set<Class<? extends Annotation>> getStereotypes() {
            return Collections.emptySet();
        }

        @Override
        public boolean isAlternative() {
            return false;
        }

        @Override
        public Class<?> getBeanClass() {
            return Map.class;
        }

        @Override
        public Set<InjectionPoint> getInjectionPoints() {
            return Collections.emptySet();
        }

        @Override
        public boolean isNullable() {
            return false;
        }

    }

}
