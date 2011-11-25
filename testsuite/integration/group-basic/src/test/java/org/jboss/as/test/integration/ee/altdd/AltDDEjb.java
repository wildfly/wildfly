package org.jboss.as.test.integration.ee.altdd;

import javax.ejb.Stateless;

/**
 * @author Stuart Douglas
 */
@Stateless
public class AltDDEjb {

    private String value;

    public String getValue() {
        return value;
    }

}
