/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.webservices;

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.FATAL;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.TRACE;
import static org.jboss.logging.Logger.Level.WARN;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Cause;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Logger;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;
import org.jboss.vfs.VirtualFile;
import org.jboss.wsf.spi.deployment.DeploymentAspect;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
@MessageLogger(projectCode = "JBAS")
public interface WSLogger extends BasicLogger {

    WSLogger ROOT_LOGGER = Logger.getMessageLogger(WSLogger.class, "org.jboss.as.webservices");

    @LogMessage(level = TRACE)
    @Message(id = 15503, value = "Creating new unified WS deployment model for %s")
    void creatingUnifiedWebservicesDeploymentModel(DeploymentUnit unit);

    @LogMessage(level = WARN)
    @Message(id = 15504, value = "Cannot load metadata files for deployment root %s")
    void cannotLoadMetaDataFiles(@Cause Throwable cause, VirtualFile root);

    @LogMessage(level = WARN)
    @Message(id = 15506, value = "Cannot load WS deployment aspects from %s")
    void cannotLoadDeploymentAspectsDefinitionFile(String resourcePath);

    @LogMessage(level = INFO)
    @Message(id = 15537, value = "Activating WebServices Extension")
    void activatingWebservicesExtension();

    @LogMessage(level = TRACE)
    @Message(id = 15538, value = "Installing aspect %s")
    void installingAspect(String aspectName);

    @LogMessage(level = INFO)
    @Message(id = 15539, value = "Starting %s")
    void starting(Object object);

    @LogMessage(level = INFO)
    @Message(id = 15540, value = "Stopping %s")
    void stopping(Object object);

    @LogMessage(level = TRACE)
    @Message(id = 15541, value = "Creating %s %s endpoints meta data model")
    void creatingEndpointsMetaDataModel(String specType, String endpointType);

    @LogMessage(level = TRACE)
    @Message(id = 15542, value = "EJB name: %s")
    void ejbName(String name);

    @LogMessage(level = TRACE)
    @Message(id = 15543, value = "EJB class: %s")
    void ejbClass(String name);

    @LogMessage(level = TRACE)
    @Message(id = 15544, value = "POJO name: %s")
    void pojoName(String name);

    @LogMessage(level = TRACE)
    @Message(id = 15545, value = "POJO class: %s")
    void pojoClass(String name);

    @LogMessage(level = TRACE)
    @Message(id = 15546, value = "JMS name: %s")
    void jmsName(String name);

    @LogMessage(level = TRACE)
    @Message(id = 15547, value = "JMS class: %s")
    void jmsClass(String name);

    @LogMessage(level = TRACE)
    @Message(id = 15548, value = "JMS address: %s")
    void jmsAddress(String name);

    @LogMessage(level = TRACE)
    @Message(id = 15549, value = "Detected %s %s deployment")
    void detectedDeployment(String specType, String endpointType);

    @LogMessage(level = TRACE)
    @Message(id = 15550, value = "Creating JBoss agnostic %s %s meta data for deployment: %s")
    void creatingDeployment(String specType, String endpointType, String unitName);

    @LogMessage(level = TRACE)
    @Message(id = 15551, value = "Building JBoss agnostic meta data for EJB webservice deployment: %s")
    void creatingEjbDeployment(String unitName);

    @LogMessage(level = TRACE)
    @Message(id = 15552, value = "Setting context root: %s")
    void settingContextRoot(String unitName);

    @LogMessage(level = TRACE)
    @Message(id = 15553, value = "Setting config name: %s")
    void settingConfigName(String unitName);

    @LogMessage(level = TRACE)
    @Message(id = 15554, value = "Setting config file: %s")
    void settingConfigFile(String unitName);

    @LogMessage(level = TRACE)
    @Message(id = 15555, value = "Creating JBoss agnostic meta data for POJO webservice deployment: %s")
    void creatingPojoDeployment(String unitName);

    @LogMessage(level = TRACE)
    @Message(id = 15556, value = "Creating web.xml descriptor")
    void creatingWebXmlDescriptor();

    @LogMessage(level = TRACE)
    @Message(id = 15557, value = "Creating jboss-web.xml descriptor")
    void creatingJBossWebXmlDescriptor();

    @LogMessage(level = TRACE)
    @Message(id = 15558, value = "Setting security domain: %s")
    void settingSecurityDomain(String domainName);

    @LogMessage(level = TRACE)
    @Message(id = 15559, value = "Setting virtual host: %s")
    void settingVirtualHost(String virtualHost);

    @LogMessage(level = TRACE)
    @Message(id = 15560, value = "Creating servlets")
    void creatingServlets();

    @LogMessage(level = TRACE)
    @Message(id = 15561, value = "Servlet name: %s, class: %s")
    void creatingServlet(String servletName, String servletClass);

    @LogMessage(level = TRACE)
    @Message(id = 15562, value = "Creating servlet mappings")
    void creatingServletMappings();

    @LogMessage(level = TRACE)
    @Message(id = 15563, value = "Servlet name: %s, URL patterns: %s")
    void creatingServletMapping(String servletName, Object urlPatterns);

    @LogMessage(level = TRACE)
    @Message(id = 15564, value = "Creating security constraints")
    void creatingSecurityConstraints();

    @LogMessage(level = TRACE)
    @Message(id = 15565, value = "Creating web resource collection for endpoint: %s, URL pattern: %s")
    void creatingWebResourceCollection(String endpointName, String urlPattern);

    @LogMessage(level = TRACE)
    @Message(id = 15566, value = "Creating auth constraint for endpoint: %s")
    void creatingAuthConstraint(String endpointName);

    @LogMessage(level = TRACE)
    @Message(id = 15567, value = "Creating new user data constraint for endpoint: %s, transport guarantee: %s")
    void creatingUserDataConstraint(String endpointName, String transportGuarantee);

    @LogMessage(level = TRACE)
    @Message(id = 15568, value = "Creating new login config: %s, auth method: %s")
    void creatingLoginConfig(String realName, String authMethod);

    @LogMessage(level = TRACE)
    @Message(id = 15569, value = "Setting security roles")
    void creatingSecurityRoles();

    @LogMessage(level = TRACE)
    @Message(id = 15570, value = "Modifying servlets")
    void modifyingServlets();

    @LogMessage(level = TRACE)
    @Message(id = 15571, value = "Setting transport class: %s for endpoint: %s")
    void settingTransportClass(String transportClass, String endpointClass);

    @LogMessage(level = TRACE)
    @Message(id = 15572, value = "Setting context root: %s for deployment: %s")
    void settingContextRoot(String contextRoot, String unitName);

    @LogMessage(level = TRACE)
    @Message(id = 15573, value = "%s start: %s")
    void aspectStart(DeploymentAspect aspect, String unitName);

    @LogMessage(level = TRACE)
    @Message(id = 15574, value = "%s stop: %s")
    void aspectStop(DeploymentAspect aspect, String unitName);

    @LogMessage(level = TRACE)
    @Message(id = 15575, value = "Creating web meta data for EJB webservice deployment: %s")
    void creatingWebMetaData(String unitName);

    @LogMessage(level = TRACE)
    @Message(id = 15576, value = "Modifying web meta data for webservice deployment: %s")
    void modifyingWebMetaData(String unitName);

    @LogMessage(level = FATAL)
    @Message(id = 15577, value = "Error while creating configuration service")
    void configServiceCreationFailed();

    @LogMessage(level = ERROR)
    @Message(id = 15578, value = "Error while destroying configuration service")
    void configServiceDestroyFailed();

    @LogMessage(level = TRACE)
    @Message(id = 15579, value = "Skipping WS annotation processing since no composite annotation index found in unit: %s")
    void skippingAnnotationProcessing(String unitName);

    @LogMessage(level = WARN)
    @Message(id = 15580, value = "Could not read WSDL from: %s")
    void cannotReadWsdl(String wsdlLocation);

    @LogMessage(level = WARN)
    @Message(id = 15581, value = "[JAXWS 2.2 spec, section 7.7] The @WebService and @WebServiceProvider annotations are mutually exclusive - %s won't be considered as a webservice endpoint, since it doesn't meet that requirement")
    void mutuallyExclusiveAnnotations(String className);

    @LogMessage(level = WARN)
    @Message(id = 15582, value = "WebService endpoint class cannot be final - %s won't be considered as a webservice endpoint")
    void finalEndpointClassDetected(String className);

    @LogMessage(level = WARN)
    @Message(id = 15583, value = "Ignoring <port-component-ref> without <service-endpoint-interface> and <port-qname>: %s")
    void ignoringPortComponentRef(Object o);

    @LogMessage(level = ERROR)
    @Message(id = 15591, value = "Cannot register record processor with JMX server")
    void cannotRegisterRecordProcessor();

    @LogMessage(level = ERROR)
    @Message(id = 15592, value = "Cannot unregister record processor with JMX server")
    void cannotUnregisterRecordProcessor();
}
