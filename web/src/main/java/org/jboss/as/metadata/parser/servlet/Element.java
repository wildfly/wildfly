/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.metadata.parser.servlet;

import java.util.HashMap;
import java.util.Map;

/**
 * An enumeration of all the possible XML elements in the web-app schema, by name.
 *
 * @author Remy Maucherat
 */
public enum Element {
    // must be first
    UNKNOWN(null),

    // web-app 3.0 elements in alpha order
    ABSOLUTE_ORDERING("absolute-ordering"),
    AFTER("after"),
    ASYNC_SUPPORTED("async-supported"),
    AUTH_CONSTRAINT("auth-constraint"),
    AUTH_METHOD("auth-method"),

    BEFORE("before"),
    BUFFER("buffer"),

    COMMENT("comment"),
    CONTEXT_PARAM("context-param"),
    COOKIE_CONFIG("cookie-config"),

    DEFAULT_CONTENT_TYPE("default-content-type"),
    DEFERRED_SYNTAX_ALLOWED_AS_LITERAL("deferred-syntax-allowed-as-literal"),
    DISPATCHER("dispatcher"),
    DISPLAY_NAME("display-name"),
    DISTRIBUTABLE("distributable"),
    DOMAIN("domain"),

    EL_IGNORED("el-ignored"),
    ENABLED("enabled"),
    ENCODING("encoding"),
    ERROR_CODE("error-code"),
    ERROR_ON_UNDECLARED_NAMESPACE("error-on-undeclared-namespace"),
    ERROR_PAGE("error-page"),
    EXCEPTION_TYPE("exception-type"),
    EXTENSION("extension"),

    FILE_SIZE_THRESHOLD("file-size-threshold"),
    FILTER("filter"),
    FILTER_CLASS("filter-class"),
    FILTER_NAME("filter-name"),
    FILTER_MAPPING("filter-mapping"),
    FORM_ERROR_PAGE("form-error-page"),
    FORM_LOGIN_CONFIG("form-login-config"),
    FORM_LOGIN_PAGE("form-login-page"),

    HTTP_METHOD("http-method"),
    HTTP_METHOD_OMISSION("http-method-omission"),
    HTTP_ONLY("http-only"),

    INCLUDE_CODA("include-coda"),
    INCLUDE_PRELUDE("include-prelude"),
    INIT_PARAM("init-param"),
    IS_XML("is-xml"),

    JSP_CONFIG("jsp-config"),
    JSP_FILE("jsp-file"),
    JSP_PROPERTY_GROUP("jsp-property-group"),

    LISTENER("listener"),
    LISTENER_CLASS("listener-class"),
    LOAD_ON_STARTUP("load-on-startup"),
    LOCALE_ENCODING_MAPPING("locale-encoding-mapping"),
    LOCALE_ENCODING_MAPPING_LIST("locale-encoding-mapping-list"),
    LOCALE("locale"),
    LOCATION("location"),
    LOGIN_CONFIG("login-config"),

    MAX_AGE("max-age"),
    MAX_FILE_SIZE("max-file-size"),
    MAX_REQUEST_SIZE("max-request-size"),
    MESSAGE_DESTINATION("message-destination"),
    MIME_MAPPING("mime-mapping"),
    MIME_TYPE("mime-type"),
    MODULE_NAME("module-name"),
    MULTIPART_CONFIG("multipart-config"),

    NAME("name"),

    ORDERING("ordering"),
    OTHERS("others"),

    PAGE_ENCODING("page-encoding"),
    PATH("path"),

    REALM_NAME("realm-name"),
    ROLE_NAME("role-name"),
    RUN_AS("run-as"),

    SCRIPTING_INVALID("scripting-invalid"),
    SECURE("secure"),
    SECURITY_CONSTRAINT("security-constraint"),
    SECURITY_ROLE("security-role"),
    SECURITY_ROLE_REF("security-role-ref"),
    SERVLET("servlet"),
    SERVLET_CLASS("servlet-class"),
    SERVLET_MAPPING("servlet-mapping"),
    SERVLET_NAME("servlet-name"),
    SESSION_CONFIG("session-config"),
    SESSION_TIMEOUT("session-timeout"),

    TAGLIB("taglib"),
    TAGLIB_LOCATION("taglib-location"),
    TAGLIB_URI("taglib-uri"),
    TRACKING_MODE("tracking-mode"),
    TRANSPORT_GUARANTEE("transport-guarantee"),
    TRIM_DIRECTIVE_WHITESPACES("trim-directive-whitespaces"),

    URL_PATTERN("url-pattern"),
    USER_DATA_CONSTRAINT("user-data-constraint"),

    WEB_RESOURCE_COLLECTION("web-resource-collection"),
    WEB_RESOURCE_NAME("web-resource-name"),
    WELCOME_FILE("welcome-file"),
    WELCOME_FILE_LIST("welcome-file-list"),
    ;

    private final String name;

    Element(final String name) {
        this.name = name;
    }

    /**
     * Get the local name of this element.
     *
     * @return the local name
     */
    public String getLocalName() {
        return name;
    }

    private static final Map<String, Element> MAP;

    static {
        final Map<String, Element> map = new HashMap<String, Element>();
        for (Element element : values()) {
            final String name = element.getLocalName();
            if (name != null) map.put(name, element);
        }
        MAP = map;
    }

    public static Element forName(String localName) {
        final Element element = MAP.get(localName);
        return element == null ? UNKNOWN : element;
    }
}
