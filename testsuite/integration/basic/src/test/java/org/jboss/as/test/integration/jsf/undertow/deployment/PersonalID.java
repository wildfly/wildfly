package org.jboss.as.test.integration.jsf.undertow.deployment;

public enum PersonalID {
    ID1(1100),
    ID2(1111);

    private final int specificId;

    PersonalID(final int specificId) {
        this.specificId = specificId;
    }

    public int getSpecificId() {
        return specificId;
    }
}
