package org.jboss.as.test.integration.jpa.jaxb;

import java.util.Objects;

import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "data")
public class XmlData {
    private String name;
    private String address;
    private SerializationSpy serializationSpy = new SerializationSpy();

    public XmlData() {
    }

    public XmlData(String name, String address) {
        this.name = name;
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public SerializationSpy getSerializationSpy() {
        return serializationSpy;
    }

    public void setSerializationSpy(SerializationSpy serializationSpy) {
        this.serializationSpy = serializationSpy;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        XmlData data = (XmlData) o;
        return Objects.equals(name, data.name) && Objects.equals(address, data.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, address);
    }
}