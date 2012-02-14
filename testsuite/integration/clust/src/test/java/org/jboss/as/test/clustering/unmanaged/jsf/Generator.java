package org.jboss.as.test.clustering.unmanaged.jsf;


import java.io.Serializable;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

@ApplicationScoped
public class Generator implements Serializable {
    private static final long serialVersionUID = -7213673465118041882L;


    private int maxNumber = 100;

    @Produces
    @Random
    int next() {
        //random number, chosen by a completely fair dice roll
        return 3;
    }

    @Produces
    @MaxNumber
    int getMaxNumber() {
        return maxNumber;
    }
}
