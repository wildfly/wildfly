package org.wildfly.test.security.common.elytron;

import java.security.Permission;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public final class PermissionRef {
    private final String className;
    private final String module;
    private final String targetName;
    private final String action;

    public PermissionRef(Builder builder) {
        this.className = builder.className;
        this.module = builder.module;
        this.targetName = builder.targetName;
        this.action = builder.action;
    }

    public String getClassName() {
        return className;
    }

    public String getModule() {
        return module;
    }

    public String getTargetName() {
        return targetName;
    }

    public String getAction() {
        return action;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static PermissionRef fromPermission(Permission perm) {
        return fromPermission(perm, null);
    }

    public static PermissionRef fromPermission(Permission perm, String module) {
        return builder().className(perm.getClass().getName()).action(perm.getActions()).targetName(perm.getName())
                .module(module).build();
    }

    public String toCLIString() {
        StringBuilder result = new StringBuilder();
        result.append("{");
        List<String> arguments = new ArrayList<>();
        if (action != null) {
            arguments.add("action=" + action);
        }
        if (module != null) {
            arguments.add("module=" + module);
        }
        if (targetName != null) {
            arguments.add("target-name=" + targetName);
        }
        if (className != null) {
            arguments.add("class-name=" + className);
        }
        result.append(StringUtils.join(arguments, ","));
        result.append("}");
        return result.toString();
    }

    public static class Builder {
        private String className;
        private String module;
        private String targetName;
        private String action;

        public Builder() {
        }

        public Builder className(String className) {
            this.className = className;
            return this;
        }

        public Builder module(String module) {
            this.module = module;
            return this;
        }

        public Builder targetName(String targetName) {
            this.targetName = "".equals(targetName) ? null : targetName;
            return this;
        }

        public Builder action(String action) {
            this.action = "".equals(action) ? null : action;
            return this;
        }

        public PermissionRef build() {
            return new PermissionRef(this);
        }
    }

}
