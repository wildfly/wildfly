/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.controller.persistence;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import org.jboss.as.controller.persistence.ConfigurationPersister.SnapshotInfo;
import org.jboss.as.protocol.StreamUtils;

/**
 * Encapsulates the configuration file and manages its history
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class ConfigurationFile {

    private static final String LAST = "last";
    private static final String INITIAL = "initial";
    private static final String ORIGINAL = "original";

    private static final int CURRENT_HISTORY_LENGTH = 100;
    private static final int HISTORY_DAYS = 30;
    private static final String TIMESTAMP_STRING = "\\d\\d\\d\\d\\d\\d\\d\\d-\\d\\d\\d\\d\\d\\d\\d\\d\\d";
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile(TIMESTAMP_STRING);
    private static final Pattern VERSION_PATTERN = Pattern.compile("v\\d+");
    private static final Pattern SNAPSHOT_XML = Pattern.compile(TIMESTAMP_STRING + ".xml");


    private final AtomicInteger sequence = new AtomicInteger();
    private final AtomicBoolean doneBootup = new AtomicBoolean();
    private final File configurationDir;
    private final String rawFileName;
    private final String bootFileName;
    private volatile File bootFile;
    private final File mainFile;
    private final File historyRoot;
    private final File currentHistory;
    private final File snapshotsDirectory;


    public ConfigurationFile(final File configurationDir, final String rawName, final String name) {
        if (!configurationDir.exists() || !configurationDir.isDirectory()) {
            throw new IllegalArgumentException("No directory " + configurationDir.getAbsolutePath() + " was found");
        }
        this.rawFileName = rawName;
        this.bootFileName = name != null ? name : rawName;
        this.configurationDir = configurationDir;
        this.mainFile = new File(configurationDir, rawName);
        this.historyRoot = new File(configurationDir, rawName.replace('.', '_'));
        this.currentHistory = new File(historyRoot, "current");
        this.snapshotsDirectory = new File(historyRoot, "snapshot");
    }

    File getBootFile() {
        //System.out.println("----- Boot file " + bootFile.getAbsolutePath());
        if (bootFile == null) {
            synchronized (this) {
                if (bootFile == null) {
                    if (bootFileName.equals(rawFileName)) {
                        bootFile = mainFile;
                    } else {
                        bootFile = determineBootFile(configurationDir, bootFileName);
                    }
                }
            }
        }
        return bootFile;
    }

    private File determineBootFile(final File configurationDir, final String name) {
        if (name.equals(LAST) || name.equals(INITIAL) || name.equals(ORIGINAL)) {
            return addSuffixToFile(mainFile, name);
        } else if (VERSION_PATTERN.matcher(name).matches()) {
            return getVersionedFile(mainFile, name);
        }
        final File snapshot = findSnapshotWithPrefix(name, false);
        if (snapshot != null) {
            return snapshot;
        }
        final File directoryFile = new File(configurationDir, name);
        if (directoryFile.exists()) {
            return directoryFile;
        }
        final File absoluteFile = new File(name);
        if (absoluteFile.exists()) {
            return absoluteFile;
        }
        throw new IllegalArgumentException("Neither " + directoryFile.getAbsolutePath() + " nor " + absoluteFile.getAbsolutePath() + " exist");
    }

    File getMainFile() {
        //System.out.println("----- Using file " + mainFile.getAbsolutePath());
        return mainFile;
    }


    public boolean isMainFile() {
        return mainFile.equals(getBootFile());
    }

    void successfulBoot() throws ConfigurationPersistenceException {
        synchronized (this) {
            if (doneBootup.get()) {
                return;
            }

            try {
                createHistoryDirectory();

                final File last = addSuffixToFile(mainFile, LAST);
                final File original = addSuffixToFile(mainFile, ORIGINAL);
                final File initial = addSuffixToFile(mainFile, INITIAL);

                if (!initial.exists()) {
                    copyFile(mainFile, initial);
                }

                copyFile(mainFile, last);
                copyFile(mainFile, original);
                snapshot();
            } catch (IOException e) {
                // AutoGenerated
                throw new RuntimeException(e);
            }
            doneBootup.set(true);
        }
    }


    void backup() throws ConfigurationPersistenceException {
        if (!doneBootup.get()) {
            return;
        }
        try {
            moveFile(mainFile, getVersionedFile(mainFile));
            int seq = sequence.get();
            if (seq > CURRENT_HISTORY_LENGTH) {
                File delete = getVersionedFile(mainFile, seq - CURRENT_HISTORY_LENGTH);
                if (delete.exists()) {
                    delete.delete();
                }
            }
        } catch (IOException e) {
            throw new ConfigurationPersistenceException("Failed to back up " + mainFile, e);
        }
    }

    void fileWritten() throws ConfigurationPersistenceException {
        if (!doneBootup.get()) {
            return;
        }
        File last = addSuffixToFile(mainFile, LAST);
        try {
            copyFile(mainFile, last);
        } catch (IOException e) {
            throw new ConfigurationPersistenceException("Failed to back up " + mainFile, e);
        }
    }


    private void moveFile(final File file, final File backup) throws IOException {

        if (backup.exists())
            backup.delete();

        if (!file.renameTo(backup) && file.exists()) {
            copyFile(file, backup);
        }
    }

    String snapshot() throws ConfigurationPersistenceException {
        return snapshot(mainFile);
    }

    private String snapshot(File file) throws ConfigurationPersistenceException {
        String name = getTimeStamp(new Date()) + ".xml";
        File snapshot = new File(snapshotsDirectory, name);
        try {
            copyFile(mainFile, snapshot);
        } catch (IOException e) {
            throw new ConfigurationPersistenceException("Failed to take a snapshot of " + mainFile + " to " + snapshot, e);
        }
        return snapshot.toString();
    }

    SnapshotInfo listSnapshots() {
        return new BackupSnapshotInfo();
    }

    void deleteSnapshot(final String prefix) {
        findSnapshotWithPrefix(prefix, true).delete();
    }

    private File findSnapshotWithPrefix(final String prefix, boolean errorIfNoFiles) {
        List<String> names = new ArrayList<String>();
        for (String curr : snapshotsDirectory.list()) {
            if (curr.startsWith(prefix)) {
                names.add(curr);
            }
        }
        if (names.size() == 0 && errorIfNoFiles) {
            throw new IllegalArgumentException("No files beginning with '" + prefix + "' found in " + snapshotsDirectory.getAbsolutePath());
        }
        if (names.size() > 1) {
            throw new IllegalArgumentException("Ambiguous name '" + prefix + "' in " + snapshotsDirectory.getAbsolutePath() + ": " + names.toString());
        }

        return names.size() > 0 ? new File(snapshotsDirectory, names.get(0)) : null;
    }


    private void copyFile(final File file, final File backup) throws IOException {
        final FileInputStream fis = new FileInputStream(file);
        try {
            final FileOutputStream fos = new FileOutputStream(backup);
            try {
                StreamUtils.copyStream(fis, fos);
                fos.close();
            } finally {
                StreamUtils.safeClose(fos);
            }
        } finally {
            StreamUtils.safeClose(fis);
        }
    }

    private void createHistoryDirectory() throws IOException {
        mkdir(this.historyRoot);
        mkdir(this.snapshotsDirectory);
        if (currentHistory.exists()) {
            if (!currentHistory.isDirectory()) {
                throw new IllegalStateException(currentHistory.getAbsolutePath() + " is not a directory");
            }

            if (!bootFile.equals(mainFile)) {
                copyFile(bootFile, mainFile);
            }

            //Copy any existing history directory to a timestamped backup directory
            final Date date = new Date();
            final String backupName = getTimeStamp(date);
            final File old = new File(historyRoot, backupName);
            if (!new File(currentHistory.getAbsolutePath()).renameTo(old)) {
                throw new IllegalStateException("Could not rename " + currentHistory.getAbsolutePath() + " to " + old.getAbsolutePath());
            }

            //Delete any old history directories
            final String cutoffFileName = getTimeStamp(subtractDays(date, HISTORY_DAYS));
            for (String name : historyRoot.list()) {
                if (name.length() == cutoffFileName.length() && TIMESTAMP_PATTERN.matcher(name).matches() && name.compareTo(cutoffFileName) < 0) {
                    deleteRecursive(new File(historyRoot, name));
                }
            }
        }

        //Create the history directory
        currentHistory.mkdir();
        if (!currentHistory.exists()) {
            throw new IllegalStateException("Could not create " + currentHistory.getAbsolutePath());
        }
    }

    private void deleteRecursive(final File file) {
        if (file.isDirectory()) {
            for (String name : file.list()) {
                deleteRecursive(new File(file, name));
            }
        }
        if (!file.delete()) {
            throw new IllegalStateException("Could not delete " + file);
        }
    }

    private File getVersionedFile(final File file) {
        return getVersionedFile(file, sequence.incrementAndGet());
    }

    private File getVersionedFile(final File file, int i) {
        return addSuffixToFile(new File(currentHistory, file.getName()), "v" + i);
    }

    private File getVersionedFile(final File file, String versionString) {
        return addSuffixToFile(new File(currentHistory, file.getName()), versionString);
    }

    private File addSuffixToFile(final File file, final String suffix) {
        final String path = file.getAbsolutePath();
        int index = path.lastIndexOf(".");
        if (index == -1) {
            return new File(file.getAbsolutePath() + "." + suffix);
        }
        StringBuilder sb = new StringBuilder();
        sb.append(path.substring(0, index));
        sb.append(".");
        sb.append(suffix);
        sb.append(path.substring(index));
        return new File(sb.toString());
    }

    private Date subtractDays(final Date date, final int days) {
        final Calendar calendar = new GregorianCalendar();
        calendar.setTime(date);
        final int doy = calendar.get(Calendar.DAY_OF_YEAR);
        calendar.set(Calendar.DAY_OF_YEAR, doy - days);
        return calendar.getTime();
    }

    private static String getTimeStamp(final Date date) {
        final SimpleDateFormat sfd = new SimpleDateFormat("yyyyMMdd-HHmmssSSS");
        return sfd.format(date);
    }

    private File mkdir(final File dir) {
        if (!dir.exists()) {
            if (!dir.mkdir()) {
                throw new IllegalStateException("Could not create " + historyRoot.getAbsolutePath());
            }
        } else if (!dir.isDirectory()) {
            throw new IllegalStateException(dir.getAbsolutePath() + " is not a directory");
        }
        return dir;
    }

    private class BackupSnapshotInfo implements SnapshotInfo {
        final ArrayList<String> names = new ArrayList<String>();

        public BackupSnapshotInfo() {
            for (String name : snapshotsDirectory.list(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return SNAPSHOT_XML.matcher(name).matches();
                }
            })) {
                names.add(name);
            }
        }

        @Override
        public String getSnapshotDirectory() {
            return snapshotsDirectory.getAbsolutePath();
        }

        @Override
        public List<String> names() {
            return names;
        }
    }


}
