package org.jboss.as.messaging.deployment;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.messaging.MessagingLogger;
import org.jboss.as.messaging.MessagingMessages;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.staxmapper.XMLMapper;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;

/**
 * Processor that handles the messaging subsystems deployable XML
 *
 * @author Stuart Douglas
 */
public class MessagingXmlParsingDeploymentUnitProcessor implements DeploymentUnitProcessor {

    private static final XMLInputFactory INPUT_FACTORY = XMLInputFactory.newInstance();

    private static final String[] LOCATIONS = {"WEB-INF", "META-INF"};

    private static final QName ROOT_1_0 = new QName(Namespace.MESSAGING_DEPLOYMENT_1_0.getUriString(), "messaging-deployment");
    private static final QName ROOT_NO_NAMESPACE = new QName("messaging-deployment");

    private final XMLMapper mapper;

    public MessagingXmlParsingDeploymentUnitProcessor() {
        mapper = XMLMapper.Factory.create();
        mapper.registerRootElement(ROOT_1_0, MessagingDeploymentParser_1_0.INSTANCE);
        mapper.registerRootElement(ROOT_NO_NAMESPACE, MessagingDeploymentParser_1_0.INSTANCE);
    }

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final Set<VirtualFile> files = messageDestinations(deploymentUnit);

        for (final VirtualFile file : files) {
            InputStream xmlStream = null;

            try {
                final File f = file.getPhysicalFile();
                xmlStream = new FileInputStream(f);
                try {

                    final XMLInputFactory inputFactory = INPUT_FACTORY;
                    setIfSupported(inputFactory, XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
                    setIfSupported(inputFactory, XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
                    final XMLStreamReader streamReader = inputFactory.createXMLStreamReader(xmlStream);
                    final ParseResult result = new ParseResult();
                    try {
                        mapper.parseDocument(result, streamReader);
                        deploymentUnit.addToAttachmentList(MessagingAttachments.PARSE_RESULT, result);
                    } finally {
                        safeClose(streamReader, f.getAbsolutePath());
                    }
                } catch (XMLStreamException e) {
                    throw MessagingMessages.MESSAGES.couldNotParseDeployment(f.getPath(), e);
                }
            } catch (Exception e) {
                throw new DeploymentUnitProcessingException(e.getMessage(), e);
            } finally {
                VFSUtils.safeClose(xmlStream);
            }
        }
    }

    private void setIfSupported(final XMLInputFactory inputFactory, final String property, final Object value) {
        if (inputFactory.isPropertySupported(property)) {
            inputFactory.setProperty(property, value);
        }
    }

    @Override
    public void undeploy(final DeploymentUnit context) {

    }


    private Set<VirtualFile> messageDestinations(final DeploymentUnit deploymentUnit) {
        final VirtualFile deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT).getRoot();
        if (deploymentRoot == null || !deploymentRoot.exists()) {
            return Collections.emptySet();
        }

        final String deploymentRootName = deploymentRoot.getLowerCaseName();

        if (deploymentRootName.endsWith("-jms.xml")) {
            return Collections.singleton(deploymentRoot);
        }
        final Set<VirtualFile> ret = new HashSet<VirtualFile>();
        for (String location : LOCATIONS) {
            final VirtualFile loc = deploymentRoot.getChild(location);
            if (loc.exists()) {
                for (final VirtualFile file : loc.getChildren()) {
                    if (file.getName().endsWith("-jms.xml")) {
                        ret.add(file);
                    }
                }
            }
        }
        return ret;
    }

    private static void safeClose(final XMLStreamReader closeable, final String file) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (XMLStreamException e) {
                MessagingLogger.ROOT_LOGGER.couldNotCloseFile(file, e);
            }
        }
    }
}
