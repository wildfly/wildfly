/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.security.picketlink;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.directory.server.kerberos.shared.crypto.encryption.KerberosKeyFactory;
import org.apache.directory.server.kerberos.shared.keytab.Keytab;
import org.apache.directory.server.kerberos.shared.keytab.KeytabEntry;
import org.apache.directory.shared.kerberos.KerberosTime;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.kerberos.components.EncryptionKey;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;

/**
 * Class which sets up the Kerberos keytab file for HTTP service principal. It also sets system properties
 */
public class KerberosKeyTabSetup implements ServerSetupTask {

    private static final String KEYTAB_FILENAME = "keytab.krb";
    private static final File KEYTAB_FILE = new File(KEYTAB_FILENAME);

    public static final String HTTP_SERVICE_PASSWORD = "httppwd";

    /**
     * Creates a keytab file for given principal.
     *
     * @param principalName
     * @param passPhrase
     * @param keytabFile
     * @throws IOException
     *
     * @author Josef Cacek
     */
    public static void createKeytab(final String principalName, final String passPhrase, final File keytabFile)
            throws IOException {
        final KerberosTime timeStamp = new KerberosTime();
        final long principalType = 1L; // KRB5_NT_PRINCIPAL

        final Keytab keytab = Keytab.getInstance();
        final List<KeytabEntry> entries = new ArrayList<KeytabEntry>();
        for (Map.Entry<EncryptionType, EncryptionKey> keyEntry : KerberosKeyFactory.getKerberosKeys(principalName, passPhrase)
                .entrySet()) {
            final EncryptionKey key = keyEntry.getValue();
            final byte keyVersion = (byte) key.getKeyVersion();
            entries.add(new KeytabEntry(principalName, principalType, timeStamp, keyVersion, key));
        }
        keytab.setEntries(entries);
        keytab.write(keytabFile);
    }

    public static File getKeyTab() {
        return KEYTAB_FILE;
    }

    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        createKeytab(KerberosServerSetupTask.getHttpServicePrincipal(managementClient), HTTP_SERVICE_PASSWORD, KEYTAB_FILE);
    }

    public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        KEYTAB_FILE.delete();
    }

}