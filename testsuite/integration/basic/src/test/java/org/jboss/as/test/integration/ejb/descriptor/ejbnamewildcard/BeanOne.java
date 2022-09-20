package org.jboss.as.test.integration.ejb.descriptor.ejbnamewildcard;

import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;

/**
 * @author Jan Martiska
 */
@Stateless
@LocalBean
public class BeanOne {

    public void wildcardRestrictedMethod() {

    }

    public void wildcardExcludedMethod() {

    }

}
