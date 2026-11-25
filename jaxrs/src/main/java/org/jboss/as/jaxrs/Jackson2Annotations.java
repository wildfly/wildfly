/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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

    Jackson2Annotations(String simpleName) {
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
