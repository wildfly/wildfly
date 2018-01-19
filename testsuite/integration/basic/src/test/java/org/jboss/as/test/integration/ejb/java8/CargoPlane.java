package org.jboss.as.test.integration.ejb.java8;

public interface CargoPlane extends Airplane {

    default String getPlaneType() {
        return "Cargo";
    }
}
