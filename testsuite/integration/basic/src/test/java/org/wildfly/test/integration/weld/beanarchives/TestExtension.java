/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.weld.beanarchives;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.util.TypeLiteral;

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

        //@Override Not part of Bean interface in CDI 4
        public boolean isNullable() {
            return false;
        }

    }

}
