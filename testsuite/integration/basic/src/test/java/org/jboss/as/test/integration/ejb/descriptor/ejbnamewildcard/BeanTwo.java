package org.jboss.as.test.integration.ejb.descriptor.ejbnamewildcard;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;

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
