package org.wildfly.build.plugin.model;

import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Stuart Douglas
 */
public class FilePermission {

    private final Set<PosixFilePermission> permission;
    private final List<FileFilter> filters = new ArrayList<>();

    public FilePermission(String permission) {
        this.permission = fromString(permission);
    }

    private static Set<PosixFilePermission> fromString(String permission) {

        if(permission.length() != 3) {
            throw new RuntimeException("Permission string must be 3 digits");
        }
        final Set<PosixFilePermission> permissions = new HashSet<>();
        int user = Integer.parseInt(Character.toString(permission.charAt(0)));
        if((user & 1) != 0) {
            permissions.add(PosixFilePermission.OWNER_EXECUTE);
        }
        if((user & 2) != 0) {
            permissions.add(PosixFilePermission.OWNER_WRITE);
        }
        if((user & 4) != 0) {
            permissions.add(PosixFilePermission.OWNER_READ);
        }
        int group = Integer.parseInt(Character.toString(permission.charAt(1)));
        if((group & 1) != 0) {
            permissions.add(PosixFilePermission.GROUP_EXECUTE);
        }
        if((group & 2) != 0) {
            permissions.add(PosixFilePermission.GROUP_WRITE);
        }
        if((group & 4) != 0) {
            permissions.add(PosixFilePermission.GROUP_READ);
        }
        int others = Integer.parseInt(Character.toString(permission.charAt(2)));
        if((others & 1) != 0) {
            permissions.add(PosixFilePermission.OTHERS_EXECUTE);
        }
        if((others & 2) != 0) {
            permissions.add(PosixFilePermission.OTHERS_WRITE);
        }
        if((others & 4) != 0) {
            permissions.add(PosixFilePermission.OTHERS_READ);
        }
        return permissions;
    }

    public Set<PosixFilePermission> getPermission() {
        return permission;
    }

    public List<FileFilter> getFilters() {
        return filters;
    }

    public boolean includeFile(final String path) {
        for(FileFilter filter : filters) {
            if(filter.matches(path)) {
                return filter.isInclude();
            }
        }
        return false; //default exclude
    }
}
