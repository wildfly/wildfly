/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.iiop.openjdk.rmi;


/**
 * Value member analysis.
 * <p/>
 * Routines here are conforming to the "Java(TM) Language to IDL Mapping
 * Specification", version 1.1 (01-06-07).
 *
 * @author <a href="mailto:osh@sparre.dk">Ole Husgaard</a>
 * @version $Revision: 81018 $
 */
public class ValueMemberAnalysis extends AbstractAnalysis {

    /**
     * Java type.
     */
    private final Class cls;

    /**
     * Flags that this member is public.
     */
    private final boolean publicMember;

    ValueMemberAnalysis(final String javaName, final Class cls, final boolean publicMember) {
        super(javaName);

        this.cls = cls;
        this.publicMember = publicMember;
    }

    /**
     * Return my Java type.
     */
    public Class getCls() {
        return cls;
    }

    /**
     * Returns true iff this member has private visibility.
     */
    public boolean isPublic() {
        return publicMember;
    }

}

