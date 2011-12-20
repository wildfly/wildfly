package org.jboss.as.jaxr.service;

import java.io.IOException;
import java.util.Properties;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.juddi.v3.client.config.UDDIClerkManager;
import org.apache.juddi.v3.client.config.UDDIClientContainer;
import org.jboss.as.jaxr.JAXRConstants;

/**
 * Starting a juddi client manager to handle connections from scout to
 * any UDDI v3 compliant registry.
 *
 * @author Kurt Stam
 *
 */
public class UDDIv3Client {

    private static final String MANAGER_NAME = "org.apache.juddi.v3.client.manager.name";
    private static final String NODE_NAME = "org.apache.juddi.v3.client.node.name";

    UDDIClerkManager manager;
    Properties properties;

    /**
     * Reads the configuration and registers it with the UDDIContainer.
     *
     * @throws ConfigurationException
     * @throws IOException
     */
    public void start() throws ConfigurationException, IOException {
        // Starting the manager, which reads the config
        UDDIClerkManager manager = new UDDIClerkManager("META-INF/jaxr-uddi.xml", properties);
        manager.start();
    }
    /**
     * Shutdown the manager and release resource.
     *
     * @throws ConfigurationException
     */
    public static void stop() throws ConfigurationException {
        UDDIClerkManager manager = UDDIClientContainer.getUDDIClerkManager("jaxr");
        if (manager!=null) manager.stop();
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    /**
     * When using the UDDIv3Client the following properties will connect Scout to
     * the juddi-client.
     * @param config
     */
    public void overrideJAXRProperties(Properties properties) {

        properties.setProperty(JAXRConstants.QUERYMANAGER,
                "org.apache.juddi.v3.client.transport.wrapper.UDDIInquiryService#inquire");
        properties.setProperty(JAXRConstants.LIFECYCLEMANAGER,
                "org.apache.juddi.v3.client.transport.wrapper.UDDIPublicationService#publish");
        properties.setProperty(JAXRConstants.SECURITYMANAGER,
                "org.apache.juddi.v3.client.transport.wrapper.UDDISecurityService#secure");
        if (!properties.containsKey(JAXRConstants.UDDI_NAMESPACE_PROPERTY_NAME))
            properties.setProperty(JAXRConstants.UDDI_NAMESPACE_PROPERTY_NAME,
                "urn:uddi-org:api_v3");
        properties.setProperty(JAXRConstants.JAXR_FACTORY_IMPLEMENTATION,
                "org.apache.ws.scout.transport.LocalTransport");
        // The juddi client wrapper will look for the following System properties
        SecurityActions.setSystemProperty(MANAGER_NAME, "jaxr");
        SecurityActions.setSystemProperty(NODE_NAME, "jaxr-client");
    }
}
