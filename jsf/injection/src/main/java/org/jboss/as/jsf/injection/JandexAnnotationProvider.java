/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jsf.injection;

import com.sun.faces.spi.AnnotationProvider;
import java.lang.annotation.Annotation;

import jakarta.servlet.ServletContext;
import java.util.Map;
import java.util.Set;

/**
 * {@link }AnnotationProvider} implementation which provides the Jakarta Server Faces annotations which we parsed from from a
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
