package org.jboss.as.test.integration.ejb.descriptor.ejbnamewildcard;

import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;

/**
 * @author Jan Martiska
 */
@Stateless
@LocalBean
public class BeanTwo {

    public void wildcardRestrictedMethod() {

    }

    public void wildcardExcludedMethod() {

    }

    public void localRestrictedMethod() {

    }

    public void localExcludedMethod() {

    }

    public void unRestrictedMethod() {

    }

    public void notExcludedMethod() {

    }
}
