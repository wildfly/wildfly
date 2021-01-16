package org.wildfly.test.integration.elytron.util;

import org.apache.commons.io.FileUtils;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.wildfly.test.security.common.AbstractElytronSetupTask;
import org.wildfly.test.security.common.elytron.AbstractConfigurableElement;
import org.wildfly.test.security.common.elytron.ConfigurableElement;

import java.io.File;
import java.io.FileWriter;

import static org.jboss.as.test.integration.security.common.Utils.createTemporaryFolder;
import static org.jboss.as.test.shared.CliUtils.escapePath;

/**
 * Installs mock welcome-content for Galleon slimmed installation testing where that
 * is not provided. TODO having tests rely on welcome-content instead of a deployment
 * is odd, unless the point is to validate handling of the way welcome-content
 * is installed.
 */
public class WelcomeContent extends AbstractConfigurableElement {

    private final boolean layersTest = Boolean.getBoolean("ts.layers") || Boolean.getBoolean("ts.bootable");

    private WelcomeContent(Builder builder) {
        super(builder);
    }
    private File tempFolder;

    @Override
    public void create(CLIWrapper cli) throws Exception {
        if (layersTest){
            this.tempFolder = createTemporaryFolder("ely-welcome-content" + name);
            try (FileWriter writer = new FileWriter(new File(tempFolder, "index.html"))) {
                // Tests check for 'Welcome to ' in the entity returned by reading the root resource. So include that.
                writer.write("Welcome to AMeaninglessValueThatShouldNotBeAsserted");
            }
            cli.sendLine(String.format("/subsystem=undertow/configuration=handler/file=welcome-content:add(path=%s)",
                    escapePath(tempFolder.getAbsolutePath())));
            cli.sendLine("/subsystem=undertow/server=default-server/host=default-host/location=\"/\":add(handler=welcome-content)");
        } // else the server is already configured with the standard welcome content
    }

    @Override
    public void remove(CLIWrapper cli) throws Exception {
        if (layersTest){
            cli.sendLine("/subsystem=undertow/server=default-server/host=default-host/location=\"/\":remove");
            cli.sendLine("/subsystem=undertow/configuration=handler/file=welcome-content:remove");
            FileUtils.deleteQuietly(tempFolder);
            tempFolder = null;
        }
    }

    public static class Builder extends AbstractConfigurableElement.Builder<Builder> {

        public WelcomeContent build() {
            return new WelcomeContent(this);
        }


        @Override
        protected Builder self() {
            return this;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class SetupTask extends AbstractElytronSetupTask {
        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            super.setup(managementClient, containerId);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            super.tearDown(managementClient, containerId);
        }

        @Override
        protected ConfigurableElement[] getConfigurableElements() {
            return new ConfigurableElement[]{WelcomeContent.builder().withName(getName()).build()};
        }

        public String getName() {
            return "WelcomeContent";
        }
    }
}
