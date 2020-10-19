/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.validator;

import org.hibernate.validator.cdi.spi.BeanNames;
import org.hibernate.validator.metadata.BeanMetaDataClassNormalizer;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.literal.NamedLiteral;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.PassivationCapable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;

/**
 *
 * This class is used to provide bean validation with a interface instead of EJB proxy. This is necessary as EJB proxy
 * does not contain generics data.
 * @see <a href="https://issues.redhat.com/browse/WFLY-11566">WFLY-11566</a>
 *
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */

public class EjbProxyBeanMetaDataClassNormalizer implements BeanMetaDataClassNormalizer, Bean<BeanMetaDataClassNormalizer>, PassivationCapable {

    private static final Set<Type> TYPES = Collections.singleton(BeanMetaDataClassNormalizer.class);
    private static final Set<Annotation> QUALIFIERS = Collections.singleton(NamedLiteral.of(BeanNames.BEAN_META_DATA_CLASS_NORMALIZER));


    @Override
    public <T> Class<? super T> normalize(Class<T> clazz) {
        if (EjbProxy.class.isAssignableFrom(clazz) && clazz.getSuperclass() != Object.class) {
            return clazz.getSuperclass();
        }
        return clazz;
    }

    @Override
    public Class<?> getBeanClass() {
        return BeanMetaDataClassNormalizer.class;
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return Collections.emptySet();
    }

    @Override
    public boolean isNullable() {
        return false;
    }

    @Override
    public BeanMetaDataClassNormalizer create(CreationalContext<BeanMetaDataClassNormalizer> creationalContext) {
        return new EjbProxyBeanMetaDataClassNormalizer();
    }

    @Override
    public void destroy(BeanMetaDataClassNormalizer beanMetaDataClassNormalizer, CreationalContext<BeanMetaDataClassNormalizer> creationalContext) {

    }

    @Override
    public Set<Type> getTypes() {
        return TYPES;
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return QUALIFIERS;
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return ApplicationScoped.class;
    }

    @Override
    public String getName() {
        return BeanNames.BEAN_META_DATA_CLASS_NORMALIZER;
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
    public String getId() {
        return EjbProxyBeanMetaDataClassNormalizer.class.getName();
    }
}
