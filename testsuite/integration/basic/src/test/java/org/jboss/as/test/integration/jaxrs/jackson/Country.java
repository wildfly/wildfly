/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jaxrs.jackson;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;
import java.io.Serializable;


@XmlRootElement(name = "Country")
@XmlType(name = "Country", propOrder = {"name", "awesomeness"})
@JsonPropertyOrder({"name", "temperature"})
public class Country implements Serializable {

    private Integer id;
    private String name;
    private String temperature;

    public Country(final Integer id, final String name, final String temperature) {
        this.id = id;
        this.name = name;
        this.temperature = temperature;
    }


    @XmlTransient
    @JsonIgnore
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @XmlElement(name = "name", required = true)
    @JsonProperty
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setTemperature(final String temperature) {
        this.temperature = temperature;
    }
    @XmlElement(name = "temperature", required = true)
    @JsonProperty
    public String getTemperature() {
        return temperature;
    }
}
