/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.security.loginmodules.negotiation;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.directory.server.kerberos.shared.crypto.encryption.KerberosKeyFactory;
import org.apache.directory.server.kerberos.shared.keytab.Keytab;
import org.apache.directory.shared.kerberos.KerberosTime;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.kerberos.components.EncryptionKey;
import org.apache.log4j.Logger;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.network.NetworkUtils;
import org.jboss.as.test.integration.security.common.Utils;

/**
 * This server setup task creates a krb5.conf file and generates KeyTab files for the HTTP server and users hnelson and jduke.
 * The task also sets system properties
 * <ul>
 * <li>"java.security.krb5.conf" - path to the newly created krb5.conf is set</li>
 * <li>"sun.security.krb5.debug" - true is set (Kerberos debugging for Oracle Java)</li>
 * </ul>
 * 
 * @author Josef Cacek
 */
public class Krb5ConfServerSetupTask implements ServerSetupTask {
    private static Logger LOGGER = Logger.getLogger(Krb5ConfServerSetupTask.class);

    private static final File WORK_DIR = new File("SPNEGO-workdir");
    private static final String KRB5_CONF = "krb5.conf";
    private static final File KRB5_CONF_FILE = new File(WORK_DIR, KRB5_CONF);

    public static final File HTTP_KEYTAB_FILE = new File(WORK_DIR, "http.keytab");
    public static final File HNELSON_KEYTAB_FILE = new File(WORK_DIR, "hnelson.keytab");
    public static final File JDUKE_KEYTAB_FILE = new File(WORK_DIR, "jduke.keytab");

    private String origKrb5Conf;
    private String origKrbDebug;

    // Public methods --------------------------------------------------------

    /**
     * 
     * @param managementClient
     * @param containerId
     * @throws Exception
     * @see org.jboss.as.arquillian.api.ServerSetupTask#setup(org.jboss.as.arquillian.container.ManagementClient,
     *      java.lang.String)
     */
    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        LOGGER.info("(Re)Creating workdir: " + WORK_DIR.getAbsolutePath());
        FileUtils.deleteDirectory(WORK_DIR);
        WORK_DIR.mkdirs();
        final String canonicalHost = NetworkUtils.formatPossibleIpv6Address(Utils.getCannonicalHost(managementClient));
        final Map<String, String> map = new HashMap<String, String>();
        map.put("hostname", canonicalHost);
        FileUtils.write(KRB5_CONF_FILE,
                StrSubstitutor.replace(IOUtils.toString(getClass().getResourceAsStream(KRB5_CONF), "UTF-8"), map), "UTF-8");

        createKeytab("HTTP/" + canonicalHost + "@JBOSS.ORG", "httppwd", HTTP_KEYTAB_FILE);
        createKeytab("hnelson@JBOSS.ORG", "secret", HNELSON_KEYTAB_FILE);
        createKeytab("jduke@JBOSS.ORG", "theduke", JDUKE_KEYTAB_FILE);

        LOGGER.info("Setting Kerberos configuration: " + KRB5_CONF_FILE);
        origKrb5Conf = Utils.setSystemProperty("java.security.krb5.conf", KRB5_CONF_FILE.getAbsolutePath());
        origKrbDebug = Utils.setSystemProperty("sun.security.krb5.debug", "true");

    }

    /**
     * Removes working directory with Kerberos related generated files.
     * 
     * @param managementClient
     * @param containerId
     * @throws Exception
     * @see org.jboss.as.arquillian.api.ServerSetupTask#tearDown(org.jboss.as.arquillian.container.ManagementClient,
     *      java.lang.String)
     */
    public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        FileUtils.deleteDirectory(WORK_DIR);
        Utils.setSystemProperty("java.security.krb5.conf", origKrb5Conf);
        Utils.setSystemProperty("sun.security.krb5.debug", origKrbDebug);
    }

    /**
     * Returns an absolute path to krb5.conf file.
     * 
     * @return
     */
    public static final String getKrb5ConfFullPath() {
        return KRB5_CONF_FILE.getAbsolutePath();
    }

    /**
     * Returns an absolute path to a keytab with JBoss AS credentials (HTTP/host@JBOSS.ORG).
     * 
     * @return
     */
    public static final String getKeyTabFullPath() {
        return HTTP_KEYTAB_FILE.getAbsolutePath();
    }

    // Private methods -------------------------------------------------------

    /**
     * Creates a keytab file for given principal.
     * 
     * @param principalName
     * @param passPhrase
     * @param keytabFile
     * @throws IOException
     */
    public static void createKeytab(final String principalName, final String passPhrase, final File keytabFile) throws IOException {
        LOGGER.info("Principal name: " + principalName);
        final KerberosTime timeStamp = new KerberosTime();

        DataOutputStream dos = null;
        try {
            dos = new DataOutputStream(new FileOutputStream(keytabFile));
            dos.write(Keytab.VERSION_0X502_BYTES);

            for (Map.Entry<EncryptionType, EncryptionKey> keyEntry : KerberosKeyFactory.getKerberosKeys(principalName,
                    passPhrase).entrySet()) {
                final EncryptionKey key = keyEntry.getValue();
                final byte keyVersion = (byte) key.getKeyVersion();
                // entries.add(new KeytabEntry(principalName, principalType, timeStamp, keyVersion, key));

                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream entryDos = new DataOutputStream(baos);
                // handle principal name
                String[] spnSplit = principalName.split("@");
                String nameComponent = spnSplit[0];
                String realm = spnSplit[1];

                String[] nameComponents = nameComponent.split("/");
                try {
                    // increment for v1
                    entryDos.writeShort((short) nameComponents.length);
                    entryDos.writeUTF(realm);
                    // write components
                    for (String component : nameComponents) {
                        entryDos.writeUTF(component);
                    }

                    entryDos.writeInt(1); // principal type: KRB5_NT_PRINCIPAL
                    entryDos.writeInt((int) (timeStamp.getTime() / 1000));
                    entryDos.write(keyVersion);

                    entryDos.writeShort((short) key.getKeyType().getValue());

                    byte[] data = key.getKeyValue();
                    entryDos.writeShort((short) data.length);
                    entryDos.write(data);
                } finally {
                    IOUtils.closeQuietly(entryDos);
                }
                final byte[] entryBytes = baos.toByteArray();
                dos.writeInt(entryBytes.length);
                dos.write(entryBytes);
            }
            // } catch (IOException ioe) {
        } finally {
            IOUtils.closeQuietly(dos);
        }
    }
}
