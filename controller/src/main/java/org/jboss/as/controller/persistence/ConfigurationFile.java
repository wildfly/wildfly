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

import static org.jboss.as.controller.ControllerMessages.MESSAGES;

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
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author Brian Stansberry
 */
public class ConfigurationFile {

    private static final String LAST = "last";
    private static final String INITIAL = "initial";
    private static final String BOOT = "boot";

    private static final String LAST_SUFFIX = LAST + ".xml";
    private static final String INITIAL_SUFFIX = INITIAL + ".xml";
    private static final String ORIGINAL_SUFFIX = BOOT + ".xml";

    private static final int CURRENT_HISTORY_LENGTH = 100;
    private static final int HISTORY_DAYS = 30;
    private static final String TIMESTAMP_STRING = "\\d\\d\\d\\d\\d\\d\\d\\d-\\d\\d\\d\\d\\d\\d\\d\\d\\d";
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile(TIMESTAMP_STRING);
    private static final String TIMESTAMP_FORMAT = "yyyyMMdd-HHmmssSSS";
    private static final Pattern VERSION_PATTERN = Pattern.compile("v\\d+");
    private static final Pattern FILE_WITH_VERSION_PATTERN = Pattern.compile("\\S*\\.v\\d+\\.xml");
    private static final Pattern SNAPSHOT_XML = Pattern.compile(TIMESTAMP_STRING + "\\S*\\.xml");


    private final AtomicInteger sequence = new AtomicInteger();
    private final AtomicBoolean doneBootup = new AtomicBoolean();
    private final File configurationDir;
    private final String rawFileName;
    private final String bootFileName;
    private volatile File bootFile;
    private final File mainFile;
    private final String mainFileName;
    private final File historyRoot;
    private final File currentHistory;
    private final File snapshotsDirectory;


    public ConfigurationFile(final File configurationDir, final String rawName, final String name) {
        if (!configurationDir.exists() || !configurationDir.isDirectory()) {
            throw MESSAGES.directoryNotFound(configurationDir.getAbsolutePath());
        }
        this.rawFileName = rawName;
        this.bootFileName = name != null ? name : rawName;
        this.configurationDir = configurationDir;
        this.historyRoot = new File(configurationDir, rawName.replace('.', '_') + "_history");
        this.currentHistory = new File(historyRoot, "current");
        this.snapshotsDirectory = new File(historyRoot, "snapshot");
        final File file = determineMainFile(configurationDir, rawName, name);
        try {
            this.mainFile = file.getCanonicalFile();
        } catch (IOException ioe) {
            throw MESSAGES.canonicalMainFileNotFound(ioe, file);
        }
        this.mainFileName = mainFile.getName();
    }

    public File getBootFile() {
        //System.out.println("----- Boot file " + bootFile.getAbsolutePath());
        if (bootFile == null) {
            synchronized (this) {
                if (bootFile == null) {
                    if (bootFileName.equals(rawFileName)) {
                        bootFile = mainFile;
                    } else {
                        bootFile = determineBootFile(configurationDir, bootFileName);
                        try {
                            bootFile = bootFile.getCanonicalFile();
                        } catch (IOException ioe) {
                            throw MESSAGES.canonicalBootFileNotFound(ioe, bootFile);
                        }
                    }
                }
            }
        }
        return bootFile;
    }

    private File determineMainFile(final File configurationDir, final String rawName, final String name) {

        String mainName = null;

        if (name == null) {
            mainName = rawName;
        } else if (name.equals(LAST) || name.equals(INITIAL) || name.equals(BOOT)) {
            // Search for a *single* file in the configuration dir with suffix == name.xml
            mainName = findMainFileFromBackupSuffix(historyRoot, name);
        } else if (VERSION_PATTERN.matcher(name).matches()) {
            // Search for a *single* file in the currentHistory dir with suffix == name.xml
            mainName = findMainFileFromBackupSuffix(currentHistory, name);
        }

        if (mainName == null) {
            // Search for a *single* file in the snapshots dir with prefix == name.xml
            mainName = findMainFileFromSnapshotPrefix(name);
        }
        if (mainName == null) {
            final File directoryFile = new File(configurationDir, name);
            if (directoryFile.exists()) {
                mainName = stripPrefixSuffix(name);
            }
        }
        if (mainName != null) {
            return new File(configurationDir, mainName);
        }

        throw MESSAGES.mainFileNotFound(name != null ? name : rawName, configurationDir);
    }

    /**
     * Finds a single file in {@code searchDir} whose name ends with "{@code .backupType.xml}"
     * and returns its name with {@code .backupType} removed.
     *
     * @param searchDir  the directory to search
     * @param backupType the backup type; {@link #LAST}, {@link #BOOT}, {@link #INITIAL} or {@code v\d+}
     * @return the single file that meets the criteria. Will not return {@code null}
     * @throws IllegalStateException    if no files meet the criteria or more than one does
     * @throws IllegalArgumentException if they file that meets the criteria's full name is "{@code backupType.xml}"
     */
    private String findMainFileFromBackupSuffix(File searchDir, String backupType) {

        final String suffix = "." + backupType + ".xml";
        File[] files = null;
        if (searchDir.exists() && searchDir.isDirectory()) {
            files = searchDir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(suffix);
                }

            });
        }

        if (files == null || files.length == 0) {
            throw MESSAGES.configurationFileNotFound(suffix, searchDir);
        } else if (files.length > 1) {
            throw MESSAGES.ambiguousConfigurationFiles(backupType, searchDir, suffix);
        }

        String matchName = files[0].getName();
        if (matchName.equals(suffix)) {
            throw MESSAGES.configurationFileNameNotAllowed(backupType);
        }
        String prefix = matchName.substring(0, matchName.length() - suffix.length());
        return prefix + ".xml";
    }

    /**
     * Finds a single file in the snapshots directory whose name starts with {@code prefix} and
     * returns its name with the prefix removed.
     *
     * @param prefix the prefix
     * @return the single file that meets the criterion {@code null} if none do
     * @throws IllegalStateException if more than one file meets the criteria
     */
    private String findMainFileFromSnapshotPrefix(final String prefix) {

        File[] files = null;
        if (snapshotsDirectory.exists() && snapshotsDirectory.isDirectory()) {
            files = snapshotsDirectory.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.startsWith(prefix);
                }

            });
        }

        if (files == null || files.length == 0) {
            return null;
        } else if (files.length > 1) {
            throw MESSAGES.ambiguousConfigurationFiles(prefix, snapshotsDirectory, prefix);
        }

        String matchName = files[0].getName();
        return matchName.substring(TIMESTAMP_FORMAT.length());
    }

    private String stripPrefixSuffix(String name) {
        if (SNAPSHOT_XML.matcher(name).matches()) {
            name = name.substring(TIMESTAMP_FORMAT.length());
        }
        if (FILE_WITH_VERSION_PATTERN.matcher(name).matches()) {
            int last = name.lastIndexOf('v');
            name = name.substring(0, last) + "xml";
        } else if (name.endsWith(LAST_SUFFIX)) {
            name = name.substring(0, name.length() - (LAST_SUFFIX).length()) + "xml";
        } else if (name.endsWith(ORIGINAL_SUFFIX)) {
            name = name.substring(0, name.length() - (ORIGINAL_SUFFIX).length()) + "xml";
        } else if (name.endsWith(INITIAL_SUFFIX)) {
            name = name.substring(0, name.length() - (INITIAL_SUFFIX).length()) + "xml";
        }
        return name;
    }

    private File determineBootFile(final File configurationDir, final String name) {
        if (name.equals(LAST) || name.equals(INITIAL) || name.equals(BOOT)) {
            return addSuffixToFile(new File(historyRoot, mainFile.getName()), name);
        } else if (VERSION_PATTERN.matcher(name).matches()) {
            File versioned = getVersionedFile(mainFile, name);
            if (versioned.exists()) {
                return versioned;
            }
        }
        final File snapshot = findSnapshotWithPrefix(name, false);
        if (snapshot != null) {
            return snapshot;
        }
        final File directoryFile = new File(configurationDir, name);
        if (directoryFile.exists()) {
            return directoryFile;
        }
        throw MESSAGES.fileNotFound(directoryFile.getAbsolutePath());
    }

    File getMainFile() {
        //System.out.println("----- Using file " + mainFile.getAbsolutePath());
        return mainFile;
    }

    void successfulBoot() throws ConfigurationPersistenceException {
        synchronized (this) {
            if (doneBootup.get()) {
                return;
            }

            try {
                if (!bootFile.equals(mainFile)) {
                    copyFile(bootFile, mainFile);
                }

                createHistoryDirectory();

                final File historyBase = new File(historyRoot, mainFile.getName());
                final File last = addSuffixToFile(historyBase, LAST);
                final File boot = addSuffixToFile(historyBase, BOOT);
                final File initial = addSuffixToFile(historyBase, INITIAL);

                if (!initial.exists()) {
                    copyFile(mainFile, initial);
                }

                copyFile(mainFile, last);
                copyFile(mainFile, boot);
            } catch (IOException e) {
                throw MESSAGES.failedToCreateConfigurationBackup(e, bootFile);
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
            throw MESSAGES.failedToBackup(e, mainFile);
        }
    }

    void fileWritten() throws ConfigurationPersistenceException {
        if (!doneBootup.get()) {
            return;
        }
        File last = addSuffixToFile(new File(historyRoot, mainFile.getName()), LAST);
        try {
            copyFile(mainFile, last);
        } catch (IOException e) {
            throw MESSAGES.failedToBackup(e, mainFile);
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
        String name = getTimeStamp(new Date()) + mainFileName;
        File snapshot = new File(snapshotsDirectory, name);
        try {
            copyFile(mainFile, snapshot);
        } catch (IOException e) {
            throw MESSAGES.failedToTakeSnapshot(e, mainFile, snapshot);
        }
        return snapshot.toString();
    }

    SnapshotInfo listSnapshots() {
        return new BackupSnapshotInfo();
    }

    void deleteSnapshot(final String prefix) {
        if (prefix.equals("all")) {
            if (snapshotsDirectory.exists() && snapshotsDirectory.isDirectory()) {
                for (String curr : snapshotsDirectory.list()) {
                    new File(snapshotsDirectory, curr).delete();
                }
            }

        } else {
            findSnapshotWithPrefix(prefix, true).delete();
        }
    }

    private File findSnapshotWithPrefix(final String prefix, boolean errorIfNoFiles) {
        List<String> names = new ArrayList<String>();
        if (snapshotsDirectory.exists() && snapshotsDirectory.isDirectory()) {
            for (String curr : snapshotsDirectory.list()) {
                if (curr.startsWith(prefix)) {
                    names.add(curr);
                }
            }
        }
        if (names.size() == 0 && errorIfNoFiles) {
            throw MESSAGES.fileNotFoundWithPrefix(prefix, snapshotsDirectory.getAbsolutePath());
        }
        if (names.size() > 1) {
            throw MESSAGES.ambiguousName(prefix, snapshotsDirectory.getAbsolutePath(), names);
        }

        return names.size() > 0 ? new File(snapshotsDirectory, names.get(0)) : null;
    }


    private void copyFile(final File file, final File backup) throws IOException {
        final FileInputStream fis = new FileInputStream(file);
        try {
            final FileOutputStream fos = new FileOutputStream(backup);
            try {
                StreamUtils.copyStream(fis, fos);
                fos.flush();
                fos.getFD().sync();
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
                throw MESSAGES.notADirectory(currentHistory.getAbsolutePath());
            }

            //Copy any existing history directory to a timestamped backup directory
            final Date date = new Date();
            if (currentHistory.listFiles().length > 0) {
                final String backupName = getTimeStamp(date);
                final File old = new File(historyRoot, backupName);
                if (!new File(currentHistory.getAbsolutePath()).renameTo(old)) {
                    throw MESSAGES.cannotRename(currentHistory.getAbsolutePath(), old.getAbsolutePath());
                }
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
            throw MESSAGES.cannotCreate(currentHistory.getAbsolutePath());
        }
    }

    private void deleteRecursive(final File file) {
        if (file.isDirectory()) {
            for (String name : file.list()) {
                deleteRecursive(new File(file, name));
            }
        }
        if (!file.delete()) {
            throw MESSAGES.cannotDelete(file);
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
        final SimpleDateFormat sfd = new SimpleDateFormat(TIMESTAMP_FORMAT);
        return sfd.format(date);
    }

    private File mkdir(final File dir) {
        if (!dir.exists()) {
            if (!dir.mkdir()) {
                throw MESSAGES.cannotCreate(historyRoot.getAbsolutePath());
            }
        } else if (!dir.isDirectory()) {
            throw MESSAGES.notADirectory(dir.getAbsolutePath());
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
