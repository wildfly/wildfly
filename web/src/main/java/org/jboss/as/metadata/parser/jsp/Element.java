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

package org.jboss.as.metadata.parser.jsp;

import java.util.HashMap;
import java.util.Map;

/**
 * An enumeration of all the possible XML elements in the TLD schema, by name.
 *
 * @author Remy Maucherat
 */
public enum Element {
    // must be first
    UNKNOWN(null),

    // TLD elements in alpha order
    ATTRIBUTE("attribute"),

    BODY_CONTENT("body-content"),
    BODYCONTENT("bodycontent"),

    DECLARE("declare"),
    DEFERRED_METHOD("deferred-method"),
    DEFERRED_VALUE("deferred-value"),
    DYNAMIC_ATTRIBUTES("dynamic-attributes"),

    EXAMPLE("example"),
    EXTENSION_ELEMENT("extension-element"),

    FRAGMENT("fragment"),
    FUNCTION("function"),
    FUNCTION_CLASS("function-class"),
    FUNCTION_EXTENSION("function-extension"),
    FUNCTION_SIGNATURE("function-signature"),

    INFO("info"),
    INIT_PARAM("init-param"),

    JSP_VERSION("jsp-version"),
    JSPVERSION("jspversion"),

    LARGE_ICON("large-icon"),
    LISTENER("listener"),
    LISTENER_CLASS("listener-class"),

    METHOD_SIGNATURE("method-signature"),

    NAME("name"),
    NAME_FROM_ATTRIBUTE("name-from-attribute"),
    NAME_GIVEN("name-given"),
    NAMESPACE("namespace"),

    PATH("path"),

    REQUIRED("required"),
    RTEXPRVALUE("rtexprvalue"),

    SCOPE("scope"),
    SHORT_NAME("short-name"),
    SHORTNAME("shortname"),
    SMALL_ICON("small-icon"),

    TAG_FILE("tag-file"),
    TAG("tag"),
    TAG_CLASS("tag-class"),
    TAG_EXTENSION("tag-extension"),
    TAGCLASS("tagclass"),
    TAGLIB_EXTENSION("taglib-extension"),
    TEI_CLASS("tei-class"),
    TEICLASS("teiclass"),
    TLIB_VERSION("tlib-version"),
    TLIBVERSION("tlibversion"),
    TYPE("type"),

    URI("uri"),

    VALIDATOR("validator"),
    VALIDATOR_CLASS("validator-class"),
    VARIABLE("variable"),
    VARIABLE_CLASS("variable-class"),
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
