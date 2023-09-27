/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.mdb.dynamic;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.ejb.mdb.dynamic.adapter.TelnetResourceAdapter;
import org.jboss.as.test.integration.ejb.mdb.dynamic.api.TelnetListener;
import org.jboss.as.test.integration.ejb.mdb.dynamic.application.MyMdb;
import org.jboss.as.test.integration.ejb.mdb.dynamic.impl.TelnetInputStream;
import org.jboss.as.test.integration.ejb.mdb.dynamic.impl.TelnetPrintStream;
import org.jboss.as.test.integration.ejb.mdb.dynamic.impl.TelnetServer;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.net.SocketPermission;
import java.util.PropertyPermission;

import static org.jboss.as.test.integration.ejb.mdb.dynamic.impl.TtyCodes.TTY_Bright;
import static org.jboss.as.test.integration.ejb.mdb.dynamic.impl.TtyCodes.TTY_Reset;
import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;
import static org.jboss.shrinkwrap.api.ShrinkWrap.create;
import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class DynamicMessageListenerTestCase {

    @ContainerResource
    private ManagementClient managementClient;

    private static void assertEmptyLine(final DataInputStream in) throws IOException {
        assertEquals("", in.readLine());
    }

    private static void assertReply(final String expected, final String actual) {
        assertEquals(TTY_Reset + TTY_Bright + "pronto> " + TTY_Reset + expected, actual);
    }

    @Deployment
    public static Archive createDeplyoment() {
        final EnterpriseArchive ear = create(EnterpriseArchive.class, "ear-with-rar.ear")
                .addAsModule(create(JavaArchive.class, "telnet-ra.rar")
                        .addAsManifestResource(TelnetResourceAdapter.class.getPackage(), "ra.xml", "ra.xml")
                        .addPackages(true, TelnetResourceAdapter.class.getPackage(), TelnetListener.class.getPackage(), TelnetServer.class.getPackage()))
                .addAsModule(create(JavaArchive.class, "mdb.jar")
                        .addClasses(MyMdb.class));

        ear.addAsManifestResource(createPermissionsXmlAsset(
                // Cmd (TelnetServer package) uses PropertyEditorManager#registerEditor during static initialization
                new PropertyPermission("*", "read,write"),
                // TelnetServer binds socket and accepts connections
                new SocketPermission("*", "accept,listen")),
                "permissions.xml");
        return ear;
    }

    @Test
    public void test1() throws Exception {

        final Socket socket = new Socket(managementClient.getWebUri().getHost(), 2020);
        final OutputStream sockOut = socket.getOutputStream();
        final DataInputStream in = new DataInputStream(new TelnetInputStream(socket.getInputStream(), sockOut));
        final PrintStream out = new TelnetPrintStream(sockOut);

        assertEmptyLine(in);
        assertEquals("type 'help' for a list of commands", in.readLine());

        out.println("set a b");
        out.flush();
        assertReply("set a to b", in.readLine());
        assertEmptyLine(in);

        out.println("get a");
        out.flush();
        assertReply("b", in.readLine());
        assertEmptyLine(in);

        out.println("list");
        out.flush();
        assertReply("a = b", in.readLine());
        assertEmptyLine(in);
    }
}
