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

package org.jboss.as.domain.management.security;

import static org.jboss.as.domain.management.DomainManagementLogger.ROOT_LOGGER;
import static org.jboss.as.domain.management.DomainManagementMessages.MESSAGES;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * The base class for services depending on loading a properties file, loads the properties on
 * start up and re-loads as required where updates to the file are detected.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public abstract class PropertiesFileLoader {

    private final String path;
    private final InjectedValue<String> relativeTo = new InjectedValue<String>();

    private File propertiesFile;
    private volatile long fileUpdated = -1;
    private volatile Properties properties = null;

    protected PropertiesFileLoader(final String path) {
        this.path = path;
    }

    public InjectedValue<String> getRelativeToInjector() {
        return relativeTo;
    }

    public void start(StartContext context) throws StartException {
        String relativeTo = this.relativeTo.getOptionalValue();
        String file = relativeTo == null ? path : relativeTo + "/" + path;

        propertiesFile = new File(file);
        try {
            getProperties();
        } catch (IOException ioe) {
            throw MESSAGES.unableToLoadProperties(ioe);
        }
    }

    public void stop(StopContext context) {
        properties.clear();
        properties = null;
        propertiesFile = null;
    }

    protected Properties getProperties() throws IOException {
        /*
         * This method does attempt to minimise the effect of race conditions, however this is not overly critical as if you
         * have users attempting to authenticate at the exact point their details are added to the file there is also a chance
         * of a race.
         */

        boolean loadRequired = properties == null || fileUpdated != propertiesFile.lastModified();

        if (loadRequired) {
            synchronized (this) {
                // Cache the value as there is still a chance of further modification.
                long fileLastModified = propertiesFile.lastModified();
                boolean loadReallyRequired = properties == null || fileUpdated != fileLastModified;
                if (loadReallyRequired) {
                    ROOT_LOGGER.debugf("Reloading properties file '%s'", propertiesFile.getAbsolutePath());
                    Properties props = new Properties();
                    InputStream is = new FileInputStream(propertiesFile);
                    try {
                        props.load(is);
                    } finally {
                        is.close();
                    }
                    verifyProperties(props);

                    properties = props;
                    // Update this last otherwise the check outside the synchronized block could return true before the file is
                    // set.
                    fileUpdated = fileLastModified;
                }
            }
        }

        return properties;
    }

    private synchronized void persistProperties() throws IOException {
        Properties toSave = (Properties) properties.clone();

        File backup = new File(propertiesFile.getCanonicalPath() + ".bak");
        if (backup.exists()) {
            if (backup.delete() == false) {
                throw new IllegalStateException("Unable to delete backup properties file.");
            }
        }

        if (propertiesFile.renameTo(backup) == false) {
            throw new IllegalStateException("Unable to backup properties file.");
        }

        FileReader fr = new FileReader(backup);
        BufferedReader br = new BufferedReader(fr);

        FileWriter fw = new FileWriter(propertiesFile);
        BufferedWriter bw = new BufferedWriter(fw);

        try {
            String line;
            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.startsWith("#")) {
                    bw.append(line);
                    bw.newLine();
                } else if (trimmed.length() == 0) {
                    bw.newLine();
                } else {
                    int equals = trimmed.indexOf('=');
                    if (equals > 0) {
                        String userName = trimmed.substring(0, equals);
                        if (toSave.contains(userName)) {
                            bw.append(userName + "=" + toSave.getProperty(userName));
                            bw.newLine();
                            toSave.remove(userName);
                        }
                    }
                }
            }

            // Append any additional users to the end of the file.
            for (Object currentKey : toSave.keySet()) {
                bw.append(currentKey + "=" + toSave.getProperty((String) currentKey));
                bw.newLine();
            }
            bw.newLine();
        } finally {
            safeClose(bw);
            safeClose(fw);
            safeClose(br);
            safeClose(fr);
        }
    }

    private void safeClose(final Closeable c) {
        try {
            c.close();
        } catch (IOException ignored) {
        }
    }

    /**
     * Provides the base class with an opportunity to verify the contents of the properties before they are used.
     *
     * @param properties - The Properties instance to verify.
     */
    protected void verifyProperties(Properties properties) throws IOException {
    };

}
