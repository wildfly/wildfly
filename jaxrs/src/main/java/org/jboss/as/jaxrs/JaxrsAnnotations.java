/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.as.jaxrs;

import org.jboss.jandex.DotName;

/**
 * Class that stores the {@link DotName}s of Jax-RS annotations
 *
 * @author Stuart Douglas
 *
 */
public enum JaxrsAnnotations {

    CONSUMES("Consumes"),
    COOKIE_PARAM("CookieParam"),
    DEFAULT_VALUE("DefaultValue"),
    DELETE("DELETE"),
    ENCODED("Encoded"),
    FORM_PARAM("FormParam"),
    GET("GET"),
    HEAD("HEAD"),
    HEADER_PARAM("HeaderParam"),
    HTTP_METHOD("HttpMethod"),
    MATRIX_PARAM("MatrixParam"),
    PATH("Path"),
    PATH_PARAM("PathParam"),
    POST("POST"),
    PRODUCES("Produces"),
    PUT("PUT"),
    QUERY_PARAM("QueryParam"),
    CONTEXT(Constants.JAVAX_WS_CORE,"Context"),
    PROVIDER(Constants.JAVAX_WS_EXT,"Provider");

    private final String simpleName;
    private final DotName dotName;

    private JaxrsAnnotations(String simpleName) {
        this.simpleName = simpleName;
        this.dotName = DotName.createComponentized(Constants.JAVAX_WS_JAXRS, simpleName);
    }
    private JaxrsAnnotations(DotName prefix,String simpleName) {
        this.simpleName = simpleName;
        this.dotName = DotName.createComponentized(prefix, simpleName);
    }
    // this can't go on the enum itself
    private static class Constants {
        public static final DotName JAVAX_WS_JAXRS = DotName.createSimple("javax.ws.rs");
        public static final DotName JAVAX_WS_CORE = DotName.createSimple("javax.ws.rs.core");
        public static final DotName JAVAX_WS_EXT = DotName.createSimple("javax.ws.rs.ext");
    }

    public DotName getDotName() {
        return dotName;
    }

    public String getSimpleName() {
        return simpleName;
    }

}
