package org.jboss.as.ejb3.util;

public class EjbWithPrivateFinalMethod {

    private final void privateFinalMethodIsOk() {

    }

    private static void privateStaticFinalMehtodIsAlsoOK() {

    }

    public void method() {
        this.privateFinalMethodIsOk();
        EjbWithPrivateFinalMethod.privateStaticFinalMehtodIsAlsoOK();
    }
}
