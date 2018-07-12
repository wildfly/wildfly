/*
 * JBoss, Home of Professional Open Source
 * Copyright 2018, Red Hat Inc., and individual contributors as indicated
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
 * Class that stores the {@link DotName}s of Jackson2 annotations
 *
 * @author Alessio Soldano
 *
 */
public enum Jackson2Annotations {

    JacksonAnnotation("JacksonAnnotation"),
    JacksonInject("JacksonInject"),
    JsonAnyGetter("JsonAnyGetter"),
    JsonAnySetter("JsonAnySetter"),
    JsonAutoDetect("JsonAutoDetect"),
    JsonBackReference("JsonBackReference"),
    JsonClassDescription("JsonClassDescription"),
    JsonCreator("JsonCreator"),
    JsonEnumDefaultValue("JsonEnumDefaultValue"),
    JsonFilter("JsonFilter"),
    JsonFormat("JsonFormat"),
    JsonGetter("JsonGetter"),
    JsonIdentityInfo("JsonIdentityInfo"),
    JsonIdentityReference("JsonIdentityReference"),
    JsonIgnore("JsonIgnore"),
    JsonIgnoreProperties("JsonIgnoreProperties"),
    JsonIgnoreType("JsonIgnoreType"),
    JsonInclude("JsonInclude"),
    JsonManagedReference("JsonManagedReference"),
    JsonProperty("JsonProperty"),
    JsonPropertyDescription("JsonPropertyDescription"),
    JsonPropertyOrder("JsonPropertyOrder"),
    JsonRawValue("JsonRawValue"),
    JsonRootName("JsonRootName"),
    JsonSetter("JsonSetter"),
    JsonSubTypes("JsonSubTypes"),
    JsonTypeId("JsonTypeId"),
    JsonTypeInfo("JsonTypeInfo"),
    JsonTypeName("JsonTypeName"),
    JsonUnwrapped("JsonUnwrapped"),
    JsonValue("JsonValue"),
    JsonView("JsonView");

    private final String simpleName;
    private final DotName dotName;

    private Jackson2Annotations(String simpleName) {
        this.simpleName = simpleName;
        this.dotName = DotName.createComponentized(Constants.ANNOTATION, simpleName);
    }
    // this can't go on the enum itself
    private static class Constants {
        public static final DotName COM = DotName.createComponentized(null, "com");
        public static final DotName FASTERXML = DotName.createComponentized(COM, "fasterxml");
        public static final DotName JACKSON = DotName.createComponentized(FASTERXML, "jackson");
        public static final DotName ANNOTATION = DotName.createComponentized(JACKSON, "annotation");
    }

    public DotName getDotName() {
        return dotName;
    }

    public String getSimpleName() {
        return simpleName;
    }

}
