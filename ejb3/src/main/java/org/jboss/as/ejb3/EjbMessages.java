
package org.jboss.as.ejb3;

import org.jboss.ejb.client.SessionID;
import org.jboss.logging.Cause;
import org.jboss.logging.Logger;
import org.jboss.logging.Message;
import org.jboss.logging.MessageBundle;
import org.jboss.logging.Messages;

import javax.ejb.NoSuchEJBException;

/**
 * Date: 19.10.2011
 *
 * @author <a href="mailto:Flemming.Harms@gmail.com">Flemming Harms</a>
 */
@MessageBundle(projectCode = "JBAS")
public interface EjbMessages {

    /**
     * The default messages.
     */
    EjbMessages MESSAGES = Messages.getBundle(EjbMessages.class);

     /**
     * Creates an exception indicating it could not find the EJB with specific id
     *
     *
      * @param sessionId  the name of the integration.
      *
      * @return a {@link NoSuchEJBException} for the error.
     */
    @Message(id = 14300, value = "Could not find EJB with id %s")
    NoSuchEJBException couldNotFindEjb(SessionID sessionId);


}
