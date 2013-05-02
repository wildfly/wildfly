package org.jboss.as.ejb3.component;

import javax.ejb.EJBException;

/**
 * An exception which can be used to indicate that a particular EJB component is (no longer) available for handling invocations. This typically is thrown when an EJB is invoked
 * after the EJB component has been marked for shutdown.
 *
 * @author: Jaikiran Pai
 */
public class EJBComponentUnavailableException extends EJBException {

    public EJBComponentUnavailableException() {

    }

    public EJBComponentUnavailableException(final String msg) {
        super(msg);
    }

    public EJBComponentUnavailableException(final String msg, final Exception e) {
        super(msg, e);
    }

    public EJBComponentUnavailableException(final Exception e) {
        super(e);
    }
}
