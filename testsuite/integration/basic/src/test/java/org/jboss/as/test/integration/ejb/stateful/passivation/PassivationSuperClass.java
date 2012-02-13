package org.jboss.as.test.integration.ejb.stateful.passivation;

import javax.annotation.PostConstruct;

/**
 * AS7-3716
 *
 * We also need to make sure that a beans super class is still serialized even when it is not
 * marked serializable
 *
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
