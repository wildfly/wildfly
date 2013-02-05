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
import java.io.FilenameFilter;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
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
    private static final String CURRENT_HISTORY_LENGTH_PROPERTY = "jboss.config.current-history-length";
    private static final String HISTORY_DAYS_PROPERTY = "jboss.config.history-days";
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
    private volatile boolean reloadUsingLast;
    private final File mainFile;
    private final String mainFileName;
    private final File historyRoot;
    private final File currentHistory;
    private final File snapshotsDirectory;
    private final boolean persistOriginal;
    private volatile File lastFile;


    public ConfigurationFile(final File configurationDir, final String rawName, final String name, final boolean persistOriginal) {
        if (!configurationDir.exists() || !configurationDir.isDirectory()) {
            throw MESSAGES.directoryNotFound(configurationDir.getAbsolutePath());
        }
        this.rawFileName = rawName;
        this.bootFileName = name != null ? name : rawName;
        this.configurationDir = configurationDir;
        this.historyRoot = new File(configurationDir, rawName.replace('.', '_') + "_history");
        this.currentHistory = new File(historyRoot, "current");
        this.snapshotsDirectory = new File(historyRoot, "snapshot");
        this.persistOriginal = persistOriginal;
        final File file = determineMainFile(configurationDir, rawName, name);
        try {
            this.mainFile = file.getCanonicalFile();
        } catch (IOException ioe) {
            throw MESSAGES.canonicalMainFileNotFound(ioe, file);
        }
        this.mainFileName = mainFile.getName();
    }

    public synchronized void resetBootFile(boolean reloadUsingLast) {
        this.bootFile = null;
        this.reloadUsingLast = reloadUsingLast;
    }

    public File getBootFile() {
        if (bootFile == null) {
            synchronized (this) {
                if (bootFile == null) {
                    String bootFileName = this.bootFileName;
                    if (!persistOriginal && reloadUsingLast) {
                        //If we were reloaded, and it is not a persistent configuration we want to use the last from the history
                        bootFileName = "last";
                    }

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
            } else if (!persistOriginal) {
                final File absoluteFile = new File(name);
                if (absoluteFile.exists()) {
                    return absoluteFile;
                }
            }
        }
        if (mainName != null) {
            return new File(configurationDir, new File(mainName).getName());
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
        if (!persistOriginal) {
            File absoluteFile = new File(name);
            if (absoluteFile.exists()) {
                return absoluteFile;
            }
        }
        throw MESSAGES.fileNotFound(directoryFile.getAbsolutePath());
    }

    File getMainFile() {
        return mainFile;
    }

    void successfulBoot() throws ConfigurationPersistenceException {
        synchronized (this) {
            if (doneBootup.get()) {
                return;
            }

            final File copySource;
            if (persistOriginal) {
                copySource = mainFile;
            } else {
                copySource = new File(mainFile.getParentFile(), mainFile.getName() + ".boot");
                FilePersistenceUtils.deleteFile(copySource);
            }

            try {
                if (!bootFile.equals(copySource)) {
                    FilePersistenceUtils.copyFile(bootFile, copySource);
                }

                createHistoryDirectory();

                final File historyBase = new File(historyRoot, mainFile.getName());
                lastFile = addSuffixToFile(historyBase, LAST);
                final File boot = addSuffixToFile(historyBase, BOOT);
                final File initial = addSuffixToFile(historyBase, INITIAL);

                if (!initial.exists()) {
                    FilePersistenceUtils.copyFile(copySource, initial);
                }

                FilePersistenceUtils.copyFile(copySource, lastFile);
                FilePersistenceUtils.copyFile(copySource, boot);
            } catch (IOException e) {
                throw MESSAGES.failedToCreateConfigurationBackup(e, bootFile);
            } finally {
                if (!persistOriginal) {
                    //Delete the temporary file
                    try {
                        FilePersistenceUtils.deleteFile(copySource);
                    } catch (Exception ignore) {
                    }
                }
            }
            doneBootup.set(true);
        }
    }


    void backup() throws ConfigurationPersistenceException {
        if (!doneBootup.get()) {
            return;
        }
        try {
            if (persistOriginal) {
                //Move the main file to the versioned history
                moveFile(mainFile, getVersionedFile(mainFile));
            } else {
                //Copy the Last file to the versioned history
                moveFile(lastFile, getVersionedFile(mainFile));
            }
            int seq = sequence.get();
            // delete unwanted backup files
            int currentHistoryLength = getInteger(CURRENT_HISTORY_LENGTH_PROPERTY, CURRENT_HISTORY_LENGTH, 0);
            if (seq > currentHistoryLength) {
                for (int k = seq - currentHistoryLength; k > 0; k--) {
                    File delete = getVersionedFile(mainFile, k);
                    if (! delete.exists()) {
                        break;
                    }
                    delete.delete();
                }
            }
        } catch (IOException e) {
            throw MESSAGES.failedToBackup(e, mainFile);
        }
    }

    void commitTempFile(File temp) throws ConfigurationPersistenceException {
        if (!doneBootup.get()) {
            return;
        }
        if (persistOriginal) {
            FilePersistenceUtils.moveTempFileToMain(temp, mainFile);
        } else {
            FilePersistenceUtils.moveTempFileToMain(temp, lastFile);
        }
    }

    void fileWritten() throws ConfigurationPersistenceException {
        if (!doneBootup.get() || !persistOriginal) {
            return;
        }
        try {
            FilePersistenceUtils.copyFile(mainFile, lastFile);
        } catch (IOException e) {
            throw MESSAGES.failedToBackup(e, mainFile);
        }
    }


    private void moveFile(final File file, final File backup) throws IOException {

        if (backup.exists()) {
            backup.delete();
        }

        FilePersistenceUtils.rename(file, backup);
    }

    String snapshot() throws ConfigurationPersistenceException {
        String name = getTimeStamp(new Date()) + mainFileName;
        File snapshot = new File(snapshotsDirectory, name);
        try {
            FilePersistenceUtils.copyFile(mainFile, snapshot);
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

    boolean isPersistOriginal() {
        return persistOriginal;
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


    private void createHistoryDirectory() throws IOException {
        mkdir(this.historyRoot);
        mkdir(this.snapshotsDirectory);
        if (currentHistory.exists()) {
            if (!currentHistory.isDirectory()) {
                throw MESSAGES.notADirectory(currentHistory.getAbsolutePath());
            }

            //Copy any existing history directory to a timestamped backup directory
            Date date = new Date();
            File[] currentHistoryFiles = currentHistory.listFiles();
            if (currentHistoryFiles != null && currentHistoryFiles.length > 0) {
                String backupName = getTimeStamp(date);
                File old = new File(historyRoot, backupName);
                if (!new File(currentHistory.getAbsolutePath()).renameTo(old)) {
                    if (old.exists()) {
                        // AS7-5801. Unit tests sometimes fail on File.renameTo due to only having 100 ms
                        // precision on the timestamps we use for dir names on some systems. So, if that happens,
                        // we bump the timestamp once and try again before failing
                        date = new Date(date.getTime() + 100);
                        backupName = getTimeStamp(date);
                        old = new File(historyRoot, backupName);
                        if (!new File(currentHistory.getAbsolutePath()).renameTo(old)) {
                            throw MESSAGES.cannotRename(currentHistory.getAbsolutePath(), old.getAbsolutePath());
                        }
                    } else {
                        throw MESSAGES.cannotRename(currentHistory.getAbsolutePath(), old.getAbsolutePath());
                    }
                }
            }

            //Delete any old history directories
            int historyDays = getInteger(HISTORY_DAYS_PROPERTY, HISTORY_DAYS, 0);
            final String cutoffFileName = getTimeStamp(subtractDays(date, historyDays));
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

    private int getInteger(final String name, final int defaultValue, final int minimalValue) {
        int retVal = getInteger(name, defaultValue);
        return (retVal < minimalValue) ? defaultValue : retVal;
    }

    private int getInteger(final String name, final int defaultValue) {
        final SecurityManager sm = System.getSecurityManager();
        if (sm == null) {
            return Integer.getInteger(name, defaultValue);
        } else {
            return AccessController.doPrivileged(new PrivilegedAction<Integer>() {
                @Override
                public Integer run() {
                    return Integer.getInteger(name, defaultValue);
                }
            });
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
