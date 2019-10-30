package org.jboss.as.test.integration.jsf.undertow.deployment;

public class PersonalDetails {

    private String name;

    public PersonalDetails(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
