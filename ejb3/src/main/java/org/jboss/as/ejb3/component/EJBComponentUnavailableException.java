package org.jboss.as.ejb3.component;

import javax.ejb.EJBException;

/**
 * An exception which can be used to indicate that a particular Jakarta Enterprise Beans component is (no longer) available for handling invocations.
 * This typically is thrown when an Jakarta Enterprise Beans are invoked
 * after the Jakarta Enterprise Beans component has been marked for shutdown.
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
