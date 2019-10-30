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

package org.jboss.as.jsf.injection;

import com.sun.faces.spi.AnnotationProvider;
import java.lang.annotation.Annotation;

import javax.servlet.ServletContext;
import java.util.Map;
import java.util.Set;

/**
 * {@link }AnnotationProvider} implementation which provides the JSF annotations which we parsed from from a
 * Jandex index.
 *
 * @author John Bailey
 * @author Stan Silvert ssilvert@redhat.com (C) 2012 Red Hat Inc.
 */
public class JandexAnnotationProvider extends AnnotationProvider {
    private final Map<Class<? extends Annotation>, Set<Class<?>>> annotations;

    @SuppressWarnings("unchecked")
    public JandexAnnotationProvider(final ServletContext servletContext) {
        super(servletContext);
        annotations = AnnotationMap.get(servletContext);
    }

    // Note: The Mojarra 2.0 SPI specifies that this method takes Set<URL> as its argument.  The Mojarra 2.1 SPI
    // makes a slight change and specifies Set<URI> as its argument.  Since we aren't using this anyway, we can just
    // use a plain Set and it should work for both versions.
    @Override
    public Map<Class<? extends Annotation>, Set<Class<?>>> getAnnotatedClasses(final Set uris) {
        return annotations; // TODO:  Should this be limited by URI
    }
}
