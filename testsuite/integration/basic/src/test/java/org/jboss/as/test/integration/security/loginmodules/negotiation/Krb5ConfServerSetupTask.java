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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.directory.server.kerberos.shared.crypto.encryption.KerberosKeyFactory;
import org.apache.directory.server.kerberos.shared.keytab.Keytab;
import org.apache.directory.server.kerberos.shared.keytab.KeytabEntry;
import org.apache.directory.shared.kerberos.KerberosTime;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.kerberos.components.EncryptionKey;
import org.apache.log4j.Logger;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.security.common.Utils;

/**
 * A Krb5ConfServerSetupTask.
 * 
 * @author Josef Cacek
 */
public class Krb5ConfServerSetupTask implements ServerSetupTask {
    private static Logger LOGGER = Logger.getLogger(Krb5ConfServerSetupTask.class);

    private static final File WORK_DIR = new File("SPNEGO-workdir");
    private static final String KRB5_CONF = "krb5.conf";
    private static final File KRB5_CONF_FILE = new File(WORK_DIR, KRB5_CONF);
    private static final String HTTP_KEYTAB = "http.keytab";
    private static final File HTTP_KEYTAB_FILE = new File(WORK_DIR, HTTP_KEYTAB);

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
        final String cannonicalHost = Utils.getCannonicalHost(managementClient);
        final Map<String, String> map = new HashMap<String, String>();
        map.put("hostname", cannonicalHost);
        FileUtils.write(KRB5_CONF_FILE,
                StrSubstitutor.replace(IOUtils.toString(getClass().getResourceAsStream(KRB5_CONF), "UTF-8"), map), "UTF-8");

        final String principalName = "HTTP/" + cannonicalHost + "@JBOSS.ORG";
        LOGGER.info("Principal name: " + principalName);
        final KerberosTime timeStamp = new KerberosTime();
        final long principalType = 1L; //KRB5_NT_PRINCIPAL

        final Keytab keytab = Keytab.getInstance();
        final List<KeytabEntry> entries = new ArrayList<KeytabEntry>();
        for (Map.Entry<EncryptionType, EncryptionKey> keyEntry : KerberosKeyFactory.getKerberosKeys(principalName, "httppwd")
                .entrySet()) {
            final EncryptionKey key = keyEntry.getValue();
            final byte keyVersion = (byte) key.getKeyVersion();
            entries.add(new KeytabEntry(principalName, principalType, timeStamp, keyVersion, key));
        }
        keytab.setEntries(entries);
        keytab.write(HTTP_KEYTAB_FILE);
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

    /**
     * Returns File which denotes a path to a keytab with JBoss AS credentials (HTTP/host@JBOSS.ORG).
     * 
     * @return
     */
    public static final File getKeyTabFile() {
        return HTTP_KEYTAB_FILE;
    }

}
