/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.jaxrs;

import org.jboss.jandex.DotName;

/**
 * Class that stores the {@link DotName}s of Jackson annotations
 *
 * @author Alessio Soldano
 *
 */
public enum JacksonAnnotations {

    JacksonAnnotation("JacksonAnnotation"),
    JsonAnyGetter("JsonAnyGetter"),
    JsonAnySetter("JsonAnySetter"),
    JsonAutoDetect("JsonAutoDetect"),
    JsonBackReference("JsonBackReference"),
    JsonCreator("JsonCreator"),
    JsonGetter("JsonGetter"),
    JsonIgnore("JsonIgnore"),
    JsonIgnoreProperties("JsonIgnoreProperties"),
    JsonIgnoreType("JsonIgnoreType"),
    JsonManagedReference("JsonManagedReference"),
    JsonProperty("JsonProperty"),
    JsonPropertyOrder("JsonPropertyOrder"),
    JsonRawValue("JsonRawValue"),
    JsonSetter("JsonSetter"),
    JsonSubTypes("JsonSubTypes"),
    JsonTypeInfo("JsonTypeInfo"),
    JsonTypeName("JsonTypeName"),
    JsonUnwrapped("JsonUnwrapped"),
    JsonValue("JsonValue"),
    JsonWriteNullProperties("JsonWriteNullProperties");

    private final String simpleName;
    private final DotName dotName;

    private JacksonAnnotations(String simpleName) {
        this.simpleName = simpleName;
        this.dotName = DotName.createComponentized(Constants.ANNOTATE, simpleName);
    }
    // this can't go on the enum itself
    private static class Constants {
        //org.codehaus.jackson.annotate
        public static final DotName ORG = DotName.createComponentized(null, "org");
        public static final DotName CODEHAUS = DotName.createComponentized(ORG, "codehaus");
        public static final DotName JACKSON = DotName.createComponentized(CODEHAUS, "jackson");
        public static final DotName ANNOTATE = DotName.createComponentized(JACKSON, "annotate");
    }

    public DotName getDotName() {
        return dotName;
    }

    public String getSimpleName() {
        return simpleName;
    }

}
