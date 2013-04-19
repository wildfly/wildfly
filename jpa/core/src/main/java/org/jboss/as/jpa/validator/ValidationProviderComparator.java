/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.jpa.validator;

import java.util.Comparator;

import javax.validation.spi.ValidationProvider;

import org.hibernate.validator.HibernateValidator;

/**
 * A {@link Comparator} for sorting {@link ValidationProvider}s. Providers are sorted alphabetically by their class name with
 * the exception of {@link HibernateValidator}, which always comes first.
 * <p/>
 * Note: This class is a copy of {@code org.jboss.as.ee.beanvalidation.ValidationProviderComparator}.
 *
 * @author Gunnar Morling
 */
public class ValidationProviderComparator implements Comparator<ValidationProvider<?>> {

    @Override
    public int compare(ValidationProvider<?> o1, ValidationProvider<?> o2) {
        String name1 = o1.getClass().getName();
        String name2 = o2.getClass().getName();

        if (name1.equals(HibernateValidator.class.getName())) {
            return -1;
        } else if (name2.equals(HibernateValidator.class.getName())) {
            return 1;
        } else {
            return name1.compareTo(name2);
        }
    }
}
