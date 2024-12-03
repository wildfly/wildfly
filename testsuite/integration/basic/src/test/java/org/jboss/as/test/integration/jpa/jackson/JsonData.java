package org.jboss.as.test.integration.jpa.jackson;

import java.util.Objects;

public class JsonData {
    private String name;
    private String address;
    private SerializationSpy serializationSpy = new SerializationSpy();

    public JsonData() {
    }

    public JsonData(String name, String address) {
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
        JsonData data = (JsonData) o;
        return Objects.equals(name, data.name) && Objects.equals(address, data.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, address);
    }
}