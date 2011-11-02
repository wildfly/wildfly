package org.jboss.as.clustering.subsystem;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.net.URL;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.OperationContext;
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

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return new AdditionalInitialization(){
            @Override
            protected OperationContext.Type getType() {
                return OperationContext.Type.MANAGEMENT;
            }

            @Override
            protected ValidationConfiguration getModelValidationConfiguration() {
                return ClusteringSubsystemTest.this.getModelValidationConfiguration();
            }
        };
    }

    protected abstract ValidationConfiguration getModelValidationConfiguration();
}
