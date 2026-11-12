/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.tracer;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.management.ManagementOperations;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.SnapshotRestoreSetupTask;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ondra Chaloupka <ochaloup@redhat.com>
 */
@ExtendWith(ArquillianExtension.class)
@RunAsClient
@ServerSetup({
    TracerEnableTestCase.TracerEnableSetup.class,
    LogHandlerCreationSetup.class
})
public class TracerEnableTestCase {
    private static final String ARCHIVE_NAME = "tracer-enable";

    @Deployment
    public static JavaArchive deploy() throws Exception {
        return ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar")
            .addClasses(StatelessBean.class, StatelessBeanRemote.class);
    }

    @Test
    public void tracerEnabled() throws NamingException, IOException {
        final Hashtable<String, Object> jndiProps = new Hashtable<String, Object>();
        jndiProps.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        final Context ctx = new InitialContext(jndiProps);
        StatelessBeanRemote bean = (StatelessBeanRemote) ctx.lookup(
            "ejb:/" + ARCHIVE_NAME + "/" + StatelessBean.class.getSimpleName() + "!" + StatelessBeanRemote.class.getName());

        bean.createTable();
        bean.insertToDB();

        File logFile = new File(LogHandlerCreationSetup.SERVER_LOG_DIR_VALUE, LogHandlerCreationSetup.JCA_LOG_FILE_PARAM);
        Assertions.assertTrue(logFile.exists(), "Log file " + logFile + " should exist");
        String logContent = new String(Files.readAllBytes(Paths.get(logFile.getPath())), StandardCharsets.UTF_8);
        Assertions.assertTrue(logContent.contains("org.jboss.jca.core.tracer.Tracer"),
            "Log file " + logFile + " has to contain org.jboss.jca.core.tracer.Tracer");
        Assertions.assertTrue(logContent.contains("IJTRACER-ExampleDS"),
                "Log file " + logFile + " has to contain IJTRACER-ExampleDS");
    }

    static final class TracerEnableSetup extends SnapshotRestoreSetupTask {
        private static final ModelNode TRACER_ADDRESS = new ModelNode()
            .add(SUBSYSTEM, "jca")
            .add("tracer", "tracer");

        @Override
        public void doSetup(ManagementClient managementClient, String containerId) throws Exception {
            ModelNode operation = new ModelNode();
            operation.get(OP).set(ADD);
            operation.get(OP_ADDR).set(TRACER_ADDRESS);
            operation.get("enabled").set("true");
            ManagementOperations.executeOperation(managementClient.getControllerClient(), operation);

            ServerReload.executeReloadAndWaitForCompletion(managementClient, 30_000);
        }
    }
}
