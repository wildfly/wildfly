/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.jaxrs;

import org.jboss.jandex.DotName;

/**
 * Class that stores the {@link DotName}s of Jakarta RESTful Web Services annotations
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
    PROVIDER(Constants.JAVAX_WS_EXT,"Provider"),
    APPLICATION_PATH("ApplicationPath");

    private final String simpleName;
    private final DotName dotName;

    JaxrsAnnotations(String simpleName) {
        this.simpleName = simpleName;
        this.dotName = DotName.createComponentized(Constants.JAVAX_WS_RS, simpleName);
    }
    JaxrsAnnotations(DotName prefix, String simpleName) {
        this.simpleName = simpleName;
        this.dotName = DotName.createComponentized(prefix, simpleName);
    }
    // this can't go on the enum itself
    private static class Constants {
        public static final DotName JAVAX;

        static {
            // The odd split here of the namespace prefix is due to a custom rule in Batavia for this type
            String p1 = "ja";
            String p2;
            try {
                Class.forName("jakarta.ws.rs.ApplicationPath", false, JaxrsAnnotations.class.getClassLoader());
                p2 = "karta";
            } catch (ClassNotFoundException ignore) {
                p2 = "vax";
            }
            JAVAX = DotName.createComponentized(null, p1 + p2);
        }

        public static final DotName JAVAX_WS = DotName.createComponentized(JAVAX, "ws");
        public static final DotName JAVAX_WS_RS = DotName.createComponentized(JAVAX_WS, "rs");
        public static final DotName JAVAX_WS_CORE = DotName.createComponentized(JAVAX_WS_RS, "core");
        public static final DotName JAVAX_WS_EXT = DotName.createComponentized(JAVAX_WS_RS, "ext");
    }

    public DotName getDotName() {
        return dotName;
    }

    public String getSimpleName() {
        return simpleName;
    }

}
