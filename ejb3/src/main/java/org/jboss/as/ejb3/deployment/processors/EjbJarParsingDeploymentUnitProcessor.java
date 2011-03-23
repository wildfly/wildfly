/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.deployment.processors;

import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ejb3.component.MethodIntf;
import org.jboss.as.ejb3.component.session.SessionBeanComponentDescription;
import org.jboss.as.ejb3.component.singleton.SingletonComponentDescription;
import org.jboss.as.ejb3.component.stateful.StatefulComponentDescription;
import org.jboss.as.ejb3.component.stateless.StatelessComponentDescription;
import org.jboss.as.ejb3.deployment.EjbDeploymentAttachmentKeys;
import org.jboss.as.ejb3.deployment.EjbDeploymentMarker;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.logging.Logger;
import org.jboss.metadata.ejb.parser.spec.EjbJarMetaDataParser;
import org.jboss.metadata.ejb.spec.AccessTimeoutMetaData;
import org.jboss.metadata.ejb.spec.BusinessLocalsMetaData;
import org.jboss.metadata.ejb.spec.BusinessRemotesMetaData;
import org.jboss.metadata.ejb.spec.ContainerTransactionMetaData;
import org.jboss.metadata.ejb.spec.ContainerTransactionsMetaData;
import org.jboss.metadata.ejb.spec.EjbJarMetaData;
import org.jboss.metadata.ejb.spec.EnterpriseBeanMetaData;
import org.jboss.metadata.ejb.spec.EnterpriseBeansMetaData;
import org.jboss.metadata.ejb.spec.MethodInterfaceType;
import org.jboss.metadata.ejb.spec.MethodMetaData;
import org.jboss.metadata.ejb.spec.MethodParametersMetaData;
import org.jboss.metadata.ejb.spec.MethodsMetaData;
import org.jboss.metadata.ejb.spec.SessionBean31MetaData;
import org.jboss.metadata.ejb.spec.SessionBeanMetaData;
import org.jboss.metadata.ejb.spec.SessionType;
import org.jboss.metadata.parser.util.MetaDataElementParser;
import org.jboss.vfs.VirtualFile;

import javax.ejb.AccessTimeout;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.LockType;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagementType;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLResolver;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.util.concurrent.TimeUnit;

/**
 * Processes a {@link DeploymentUnit} containing a ejb-jar.xml and creates {@link EjbJarMetaData}
 * for that unit.
 * <p/>
 * This {@link DeploymentUnitProcessor deployment unit processor} looks for ejb-jar.xml in META-INF of a .jar
 * and WEB-INF of a .war file. If it finds the ejb-jar.xml in these locations, it parses that file and creates
 * {@link EjbJarMetaData} out of it. The {@link EjbJarMetaData} is then attached to the {@link DeploymentUnit}
 * with {@link org.jboss.as.ejb3.deployment.EjbDeploymentAttachmentKeys#EJB_JAR_METADATA} as the key.
 * <p/>
 * <p/>
 * Author: Jaikiran Pai
 */
public class EjbJarParsingDeploymentUnitProcessor implements DeploymentUnitProcessor {

    /**
     * Logger
     */
    private static Logger logger = Logger.getLogger(EjbJarParsingDeploymentUnitProcessor.class);

    /**
     * .war file extension
     */
    private static final String WAR_FILE_EXTENSION = ".war";

    /**
     * .jar file extension
     */
    private static final String JAR_FILE_EXTENSION = ".jar";

    /**
     * Location of ejb-jar.xml packaged in a .war
     */
    private static final String EJB_JAR_XML_LOCATION_IN_WAR = "WEB-INF/ejb-jar.xml";

    /**
     * Location of ejb-jar.xml packaged in a .jar
     */
    private static final String EJB_JAR_XML_LOCATION_IN_JAR = "META-INF/ejb-jar.xml";

    /**
     * Finds a ejb-jar.xml (at WEB-INF of a .war or META-INF of a .jar) parses the file and creates
     * metadata out of it. The metadata is then attached to the deployment unit.
     *
     * @param deploymentPhase
     * @throws DeploymentUnitProcessingException
     *
     */
    @Override
    public void deploy(DeploymentPhaseContext deploymentPhase) throws DeploymentUnitProcessingException {

        // get hold of the deployment unit.
        DeploymentUnit deploymentUnit = deploymentPhase.getDeploymentUnit();

        // get the root of the deployment unit
        VirtualFile deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT).getRoot();

        // Locate a ejb-jar.xml
        VirtualFile ejbJarXml = null;
        // EJB 3.1 FR 20.4 Enterprise Beans Packaged in a .war
        // TODO: Is there a better way to do this?
        if (deploymentRoot.getName().toLowerCase().endsWith(WAR_FILE_EXTENSION)) {
            // it's a .war file, so look for the ejb-jar.xml in WEB-INF
            ejbJarXml = deploymentRoot.getChild(EJB_JAR_XML_LOCATION_IN_WAR);
        } else if (deploymentRoot.getName().toLowerCase().endsWith(JAR_FILE_EXTENSION)) {
            ejbJarXml = deploymentRoot.getChild(EJB_JAR_XML_LOCATION_IN_JAR);
        } else {
            // neither a .jar nor a .war. Return
            return;
        }

        if (ejbJarXml == null || !ejbJarXml.exists()) {
            // no ejb-jar.xml found, nothing to do!
            return;
        }
        // Mark it as a EJB deployment
        EjbDeploymentMarker.mark(deploymentUnit);

        // get the XMLStreamReader and parse the ejb-jar.xml
        MetaDataElementParser.DTDInfo dtdInfo = new MetaDataElementParser.DTDInfo();
        InputStream stream = null;
        try {
            stream = ejbJarXml.openStream();

            XMLStreamReader reader = this.getXMLStreamReader(stream, ejbJarXml, dtdInfo);

            EjbJarMetaData ejbJarMetaData = EjbJarMetaDataParser.parse(reader, dtdInfo);
            EnterpriseBeansMetaData ejbs = ejbJarMetaData.getEnterpriseBeans();
            if (ejbs == null || ejbs.isEmpty()) {
                return;
            }
            for (EnterpriseBeanMetaData ejb : ejbs) {
                if (!ejb.isSession()) {
                    // TODO: Implement processing of non-session EJBs
                    logger.info("Only sessions beans currently supported! Skipping EJB description processing for bean: " + ejb.getName() + " in deployment unit: " + deploymentUnit);
                    return;
                }
                SessionBeanMetaData sessionBean = (SessionBeanMetaData) ejb;
                this.processSessionBean(sessionBean, deploymentUnit);
            }

        } catch (XMLStreamException xmlse) {
            throw new DeploymentUnitProcessingException("Exception while parsing ejb-jar.xml: " + ejbJarXml.getPathName(), xmlse);
        } catch (IOException ioe) {
            throw new DeploymentUnitProcessingException("Failed to create reader for ejb-jar.xml: " + ejbJarXml.getPathName(), ioe);
        } finally {
            try {
                if (stream != null) {
                    stream.close();
                }
            } catch (IOException ioe) {
                logger.debug("Ignoring exception while closing the InputStream ", ioe);
            }
        }

    }

    /**
     * @param unit
     */
    @Override
    public void undeploy(DeploymentUnit unit) {

    }

    /**
     * Creates and returns a {@link XMLStreamReader} for the passed {@link VirtualFile ejb-jar.xml}
     *
     * @param stream    The input stream
     * @param ejbJarXml
     * @return
     * @throws DeploymentUnitProcessingException
     *
     */
    private XMLStreamReader getXMLStreamReader(InputStream stream, VirtualFile ejbJarXml, XMLResolver resolver) throws DeploymentUnitProcessingException {
        try {
            final XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            inputFactory.setXMLResolver(resolver);
            XMLStreamReader xmlReader = inputFactory.createXMLStreamReader(stream);
            return xmlReader;
        } catch (XMLStreamException xmlse) {
            throw new DeploymentUnitProcessingException("Failed to create reader for ejb-jar.xml: " + ejbJarXml.getPathName(), xmlse);
        }
    }

    /**
     * Processes the passed {@link org.jboss.metadata.ejb.spec.SessionBeanMetaData} and creates appropriate {@link org.jboss.as.ejb3.component.session.SessionBeanComponentDescription} out of it.
     * The {@link org.jboss.as.ejb3.component.session.SessionBeanComponentDescription} is then added to the {@link org.jboss.as.ee.component.EEModuleDescription module description} available
     * in the passed deployment unit.
     *
     * @param sessionBean
     * @param deploymentUnit
     * @throws DeploymentUnitProcessingException
     *
     */
    private void processSessionBean(SessionBeanMetaData sessionBean, DeploymentUnit deploymentUnit) throws DeploymentUnitProcessingException {
        // get the module description
        final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);
        final String applicationName = moduleDescription.getAppName();

        SessionType sessionType = sessionBean.getSessionType();
        if (sessionType == null) {
            throw new DeploymentUnitProcessingException("Unknown session-type for session bean: " + sessionBean.getName() + " in deployment unit: " + deploymentUnit);
        }
        String beanName = sessionBean.getName();
        String beanClassName = sessionBean.getEjbClass();
        SessionBeanComponentDescription sessionBeanDescription = null;
        switch (sessionType) {
            case Stateless:
                sessionBeanDescription = new StatelessComponentDescription(beanName, beanClassName, moduleDescription.getModuleName(), applicationName);
                break;
            case Stateful:
                sessionBeanDescription = new StatefulComponentDescription(beanName, beanClassName, moduleDescription.getModuleName(), applicationName);
                break;
            case Singleton:
                sessionBeanDescription = new SingletonComponentDescription(beanName, beanClassName, moduleDescription.getModuleName(), applicationName);
                break;
            default:
                throw new IllegalArgumentException("Unknown session bean type: " + sessionType);
        }
        // mapped-name
        sessionBeanDescription.setMappedName(sessionBean.getMappedName());
        // local business interface views
        BusinessLocalsMetaData businessLocals = sessionBean.getBusinessLocals();
        if (businessLocals != null && !businessLocals.isEmpty()) {
            sessionBeanDescription.addLocalBusinessInterfaceViews(businessLocals);
        }
        // remote business interface views
        BusinessRemotesMetaData businessRemotes = sessionBean.getBusinessRemotes();
        if (businessRemotes != null && !businessRemotes.isEmpty()) {
            logger.debug("Remote business interface processing isn't yet implemented");
            // TODO: Process remote business interfaces
        }
        // tx management type
        if (sessionBean.getTransactionType() != null) {
            sessionBeanDescription.setTransactionManagementType(sessionBean.getTransactionType());
        }
        // CMT Tx attributes
        if (sessionBean.getTransactionType() != TransactionManagementType.BEAN) {
            ContainerTransactionsMetaData containerTransactions = sessionBean.getContainerTransactions();
            if (containerTransactions != null && !containerTransactions.isEmpty()) {
                for (ContainerTransactionMetaData containerTx : containerTransactions) {
                    TransactionAttributeType txAttr = containerTx.getTransAttribute();
                    MethodsMetaData methods = containerTx.getMethods();
                    for (MethodMetaData method : methods) {
                        String methodName = method.getMethodName();
                        MethodIntf methodIntf = this.getMethodIntf(method.getMethodIntf());
                        if (methodName.equals("*")) {
                            sessionBeanDescription.setTransactionAttribute(methodIntf, txAttr);
                        } else {

                            MethodParametersMetaData methodParams = method.getMethodParams();
                            // update the session bean description with the tx attribute info
                            sessionBeanDescription.setTransactionAttribute(methodIntf, txAttr, methodName, this.getMethodParams(methodParams));
                        }
                    }
                }
            }
        }

        // process EJB3.1 specific session bean description
        if (sessionBean instanceof SessionBean31MetaData) {
            this.processSessionBean31((SessionBean31MetaData) sessionBean, sessionBeanDescription);
        }

        // Add this component description to the module description
        moduleDescription.addComponent(sessionBeanDescription);

    }

    private void processSessionBean31(SessionBean31MetaData sessionBean31MetaData, SessionBeanComponentDescription sessionBeanComponentDescription) {
        // no-interface view
        if (sessionBean31MetaData.isNoInterfaceBean()) {
            sessionBeanComponentDescription.addNoInterfaceView();
        }
        // process singleton bean specific description
        if (sessionBean31MetaData.isSingleton() && sessionBeanComponentDescription instanceof SingletonComponentDescription) {
            this.processSingletonBean(sessionBean31MetaData, (SingletonComponentDescription) sessionBeanComponentDescription);
        }
    }

    private void processSingletonBean(SessionBean31MetaData singletonBeanMetaData, SingletonComponentDescription singletonComponentDescription) {
        if (singletonBeanMetaData.isInitOnStartup()) {
            singletonComponentDescription.initOnStartup();
        }
        // bean level lock-type
        LockType lockType = singletonBeanMetaData.getLockType();
        singletonComponentDescription.setBeanLevelLockType(lockType);
        // TODO: Add method level lock type to the description

        // concurrency management type
        ConcurrencyManagementType concurrencyManagementType = singletonBeanMetaData.getConcurrencyManagementType();
        if (concurrencyManagementType == ConcurrencyManagementType.BEAN) {
            singletonComponentDescription.beanManagedConcurrency();
        } else {
            singletonComponentDescription.containerManagedConcurrency();
        }

        // bean level access timeout
        // TODO: This should apply to other bean types too (JBoss specific feature) and not just singleton beans
        AccessTimeoutMetaData accessTimeoutMetaData = singletonBeanMetaData.getAccessTimeout();
        if (accessTimeoutMetaData != null) {
            final long timeout = accessTimeoutMetaData.getTimeout();
            final TimeUnit unit = accessTimeoutMetaData.getUnit();
            AccessTimeout accessTimeout = new AccessTimeout() {
                @Override
                public long value() {
                    return timeout;
                }

                @Override
                public TimeUnit unit() {
                    return unit;
                }

                @Override
                public Class<? extends Annotation> annotationType() {
                    return AccessTimeout.class;
                }
            };
            singletonComponentDescription.setBeanLevelAccessTimeout(accessTimeout);
        }
    }

    private MethodIntf getMethodIntf(MethodInterfaceType viewType) {
        if (viewType == null) {
            return MethodIntf.BEAN;
        }
        switch (viewType) {
            case Home:
                return MethodIntf.HOME;
            case LocalHome:
                return MethodIntf.LOCAL_HOME;
            case ServiceEndpoint:
                return MethodIntf.SERVICE_ENDPOINT;
            case Local:
                return MethodIntf.LOCAL;
            case Remote:
                return MethodIntf.REMOTE;
            // TODO: Need to handle more recent ones (like timer, mdb)
        }
        return MethodIntf.BEAN;
    }

    private String[] getMethodParams(MethodParametersMetaData methodParametersMetaData) {
        if (methodParametersMetaData == null) {
            return null;
        }
        return methodParametersMetaData.toArray(new String[0]);
    }
}
