package org.jboss.as.jaxr;

import javax.xml.registry.JAXRException;

import org.jboss.logging.Message;
import org.jboss.logging.MessageBundle;
import org.jboss.logging.Messages;

/**
 * This module is using message IDs in the range 14000-14099. This file is using the subset 14080-14099 for host
 * controller logger messages. See http://community.jboss.org/docs/DOC-16810 for the full list of currently reserved
 * JBAS message id blocks.
 *
 * Date: 31.1.2012
 *
 * @author Kurt T Stam
 */
@MessageBundle(projectCode = "JBAS")
public interface JAXRMessages {

    /**
     * The default messages.
     */
    JAXRMessages MESSAGES = Messages.getBundle(JAXRMessages.class);

    /**
     * Creates an exception indicating it could not instantiate.
     *
     * @param factoryName    JAXR ConnectionFactory implementation class.
     *
     * @return a {@link JAXRException} for the error.
     */
    @Message(id = 14080, value = "Failed to create instance of %s")
    JAXRException couldNotInstantiateJAXRFactory(String factoryName);

}
