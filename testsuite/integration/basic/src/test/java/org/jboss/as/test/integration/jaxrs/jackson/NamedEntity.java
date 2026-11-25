package org.jboss.as.test.integration.jaxrs.jackson;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@XmlRootElement(name = "named-entity")
public class NamedEntity {

    private long id;
    private String name;

    public NamedEntity() {
    }

    public NamedEntity(final long id, final String name) {
        this.id = id;
        this.name = name;
    }

    @JsonProperty("jsonId")
    @XmlElement(name = "xml-id")
    public long getId() {
        return id;
    }

    public void setId(final long id) {
        this.id = id;
    }


    @JsonProperty("jsonName")
    @XmlElement(name = "xml-name")
    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }
}
