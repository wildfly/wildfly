package org.jboss.as.clustering.subsystem;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.net.URL;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.ModelDescriptionValidator.ValidationConfiguration;

public abstract class ClusteringSubsystemTest extends AbstractSubsystemBaseTest {
    private final String path;

    protected ClusteringSubsystemTest(String name, Extension extension, String path) {
        super(name, extension);
        this.path = path;
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        URL url = Thread.currentThread().getContextClassLoader().getResource(this.path);
        if (url == null) {
            throw new IllegalStateException(String.format("Failed to locate %s", this.path));
        }
        try {
            BufferedReader reader = new BufferedReader(new FileReader(new File(url.toURI())));
            StringWriter writer = new StringWriter();
            try {
                String line = reader.readLine();
                while (line != null) {
                    writer.write(line);
                    line = reader.readLine();
                }
            } finally {
                reader.close();
            }
            return writer.toString();
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Need to override the XML comparison in the case where the input xsd and the output xsd differ.
     *
     * @param configId   the id of the xml configuration
     * @param original   the original subsystem xml
     * @param marshalled the marshalled subsystem xml
     * @throws Exception
     */
    @Override
    protected void compareXml(String configId, final String original, final String marshalled) throws Exception {

        final XMLStreamReader originalReader = XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(original));
        final XMLStreamReader marshalledReader = XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(marshalled));

        String originalNS = null;
        if (originalReader.next() == XMLStreamReader.START_ELEMENT) {
            originalNS = originalReader.getNamespaceURI();
        }
        String marshalledNS = null;
        if (marshalledReader.next() == XMLStreamReader.START_ELEMENT) {
            marshalledNS = marshalledReader.getNamespaceURI();
        }

        // only compare if namespace URIs are the same
        if (originalNS.equals(marshalledNS)) {
            compareXml(configId, original, marshalled, true);
        }
    }


    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return new AdditionalInitialization() {
            @Override
            protected RunningMode getRunningMode() {
                return RunningMode.ADMIN_ONLY;
            }

            @Override
            protected ValidationConfiguration getModelValidationConfiguration() {
                return ClusteringSubsystemTest.this.getModelValidationConfiguration();
            }
        };
    }

    protected abstract ValidationConfiguration getModelValidationConfiguration();
}
