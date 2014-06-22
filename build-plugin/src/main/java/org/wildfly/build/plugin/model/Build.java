package org.wildfly.build.plugin.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Stuart Douglas
 */
public class Build {

    private boolean extractSchema;
    private final List<Server> servers = new ArrayList<>();
    private final List<CopyArtifact> copyArtifacts = new ArrayList<>();
    private final List<ConfigFile> standaloneConfigs = new ArrayList<>();
    private final List<ConfigFile> domainConfigs = new ArrayList<>();
    private final List<FilePermission> filePermissions = new ArrayList<>();
    private final List<String> mkDirs = new ArrayList<>();
    private final LineEndings windows = new LineEndings(LineEndings.Type.WINDOWS);
    private final LineEndings unix = new LineEndings(LineEndings.Type.UNIX);
    private boolean copyModuleArtifacts;

    public List<Server> getServers() {
        return servers;
    }

    public List<CopyArtifact> getCopyArtifacts() {
        return copyArtifacts;
    }

    public List<ConfigFile> getStandaloneConfigs() {
        return standaloneConfigs;
    }

    public List<ConfigFile> getDomainConfigs() {
        return domainConfigs;
    }

    public List<FilePermission> getFilePermissions() {
        return filePermissions;
    }

    public List<String> getMkDirs() {
        return mkDirs;
    }

    public boolean isExtractSchema() {
        return extractSchema;
    }

    public void setExtractSchema(boolean extractSchema) {
        this.extractSchema = extractSchema;
    }

    public void setCopyModuleArtifacts(boolean copyModuleArtifacts) {
        this.copyModuleArtifacts = copyModuleArtifacts;
    }

    public boolean isCopyModuleArtifacts() {
        return copyModuleArtifacts;
    }

    public LineEndings getWindows() {
        return windows;
    }

    public LineEndings getUnix() {
        return unix;
    }
}
