package org.jboss.as.test.integration.jsf.undertow.deployment;

import java.util.HashMap;
import java.util.Map;

public class SpecificOrder extends Order {

    private Map<PersonalID, PersonalOrder> personInformation = new HashMap<PersonalID, PersonalOrder>();

    public SpecificOrder(String name, PersonalID personalID) {
        PersonalOrder personalOrder = new PersonalOrder(name);
        personInformation.put(personalID, personalOrder);
    }

    @Override
    public PersonalOrder getPersonalDetails(PersonalID id) {
        return personInformation.get(id);
    }
}
