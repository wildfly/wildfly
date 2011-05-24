package org.jboss.as.txn;

/**
 * Enums for the known com.arjuna.ats.arjuna.utils.Process implementation types
 * @author Scott stark (sstark@redhat.com) (C) 2011 Red Hat Inc.
 * @version $Revision:$
 */
public enum ProcessIdType {
    UUID("uuid", "com.arjuna.ats.internal.arjuna.utils.UuidProcessId"),
    FILE("file", "com.arjuna.ats.internal.arjuna.utils.FileProcessId"),
    MBEAN("mbean", "com.arjuna.ats.internal.arjuna.utils.MBeanProcessId"),
    SOCKET("socket", "com.arjuna.ats.internal.arjuna.utils.SocketProcessId")
    ;
    private final String name;
    private final String clazz;

    ProcessIdType(final String name, final String clazz) {
        this.name = name;
        this.clazz = clazz;
    }

    public String getClazz() {
        return clazz;
    }

    public String getName() {
        return name;
    }
}
