/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.web.deployment.jsf;

import com.sun.faces.spi.AnnotationProvider;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletContext;

/**
 * {@link }AnnotationProvider} implementation which provides the JSF annations which we parsed from from a
 * Jandex index.
 *
 * @author John Bailey
 */
public class JandexAnnotationProvider extends AnnotationProvider {
    static final String FACES_ANNOTATIONS = "FACES_ANNOTATIONS";

    private final Map<Class<? extends Annotation>, Set<Class<?>>> annotations;

    @SuppressWarnings("unchecked")
    public JandexAnnotationProvider(final ServletContext servletContext) {
        super(servletContext);
        annotations = (Map<Class<? extends Annotation>, Set<Class<?>>>) servletContext.getAttribute(FACES_ANNOTATIONS);
    }

    public Map<Class<? extends Annotation>, Set<Class<?>>> getAnnotatedClasses(final Set<URL> urls) {
        return annotations; // TODO:  Should this be limited by URL
    }
}
