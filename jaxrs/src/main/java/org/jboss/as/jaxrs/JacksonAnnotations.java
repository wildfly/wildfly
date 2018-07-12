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
