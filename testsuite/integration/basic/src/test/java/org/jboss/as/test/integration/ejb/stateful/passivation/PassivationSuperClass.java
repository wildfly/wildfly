package org.jboss.as.test.integration.ejb.stateful.passivation;

import javax.annotation.PostConstruct;

/**
 * @author Stuart Douglas
 */
public class PassivationSuperClass {

    private Employee superEmployee;

    @PostConstruct
    private void postConstruct() {
        superEmployee = new Employee();
        superEmployee.setName("Super");
    }

    public Employee getSuperEmployee() {
        return superEmployee;
    }
}
