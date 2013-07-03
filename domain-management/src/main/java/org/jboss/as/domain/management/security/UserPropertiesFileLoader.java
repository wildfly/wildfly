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

package org.jboss.as.domain.management.security;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;

/**
 * An extension of {@link PropertiesFileLoader} that is realm aware.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class UserPropertiesFileLoader extends PropertiesFileLoader {

    private static final String REALM_COMMENT_PREFIX = "$REALM_NAME=";
    private static final String REALM_COMMENT_SUFFIX = "$";
    private static final String REALM_COMMENT_COMMENT = " This line is used by the add-user utility to identify the realm name already used in this file.";

    private String realmName;

    /*
     * State maintained during persistence.
     */
    private boolean realmWritten = false;

    /*
     * End of state maintained during persistence.
     */

    public UserPropertiesFileLoader(final String path) {
        super(path);
    }

    public String getRealmName() throws IOException {
        loadAsRequired();

        return realmName;
    }

    public void setRealmName(final String realmName) {
        this.realmName = realmName;
    }

    @Override
    protected void load() throws IOException {
        super.load();

        String realmName = null;
        BufferedReader br = new BufferedReader(new FileReader(propertiesFile));
        try {
            String currentLine = null;
            while (realmName == null && (currentLine = br.readLine()) != null) {
                String trimmed = currentLine.trim();
                if (trimmed.startsWith(COMMENT_PREFIX) && trimmed.contains(REALM_COMMENT_PREFIX)) {
                    int start = trimmed.indexOf(REALM_COMMENT_PREFIX) + REALM_COMMENT_PREFIX.length();
                    int end = trimmed.indexOf(REALM_COMMENT_SUFFIX, start);
                    if (end > -1) {
                        realmName = trimmed.substring(start, end);
                    }
                }
            }
        } finally {
            safeClose(br);
        }
        this.realmName = realmName;
    }

    @Override
    protected void beginPersistence() throws IOException {
        super.beginPersistence();

        realmWritten = false;
    }

    @Override
    protected void write(BufferedWriter writer, String line, boolean newLine) throws IOException {
        if (realmWritten == false) {
            // Once we know it has been written we can skip subsequent checks.
            String trimmed = line.trim();
            if (trimmed.startsWith(COMMENT_PREFIX) && trimmed.contains(REALM_COMMENT_PREFIX)) {
                realmWritten = true;
            }
        }
        // We currently do not support replacing the realm name as that would involve new passwords for all current users.

        super.write(writer, line, newLine);
    }

    @Override
    protected void endPersistence(BufferedWriter writer) throws IOException {
        // Allow super class to write any remaining users first.
        super.endPersistence(writer);

        if (realmWritten == false) {
            writeRealm(writer, realmName);
        }
    }

    private void writeRealm(final BufferedWriter bw, final String realmName) throws IOException {
        bw.append(COMMENT_PREFIX);
        bw.newLine();
        bw.append(COMMENT_PREFIX);
        bw.append(REALM_COMMENT_PREFIX);
        bw.append(realmName);
        bw.append(REALM_COMMENT_SUFFIX);
        bw.append(REALM_COMMENT_COMMENT);
        bw.newLine();
        bw.append(COMMENT_PREFIX);
        bw.newLine();
    }

}
