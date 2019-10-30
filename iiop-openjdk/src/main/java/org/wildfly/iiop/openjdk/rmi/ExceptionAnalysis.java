/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.wildfly.iiop.openjdk.rmi;

import org.wildfly.iiop.openjdk.logging.IIOPLogger;

/**
 * Exception analysis.
 * <p/>
 * Routines here are conforming to the "Java(TM) Language to IDL Mapping
 * Specification", version 1.1 (01-06-07).
 *
 * @author <a href="mailto:osh@sparre.dk">Ole Husgaard</a>
 */
public class ExceptionAnalysis extends ValueAnalysis {

    private static WorkCacheManager cache
            = new WorkCacheManager(ExceptionAnalysis.class);

    private String exceptionRepositoryId;

    public static ExceptionAnalysis getExceptionAnalysis(Class cls)
            throws RMIIIOPViolationException {
        return (ExceptionAnalysis) cache.getAnalysis(cls);
    }

    public static void clearCache(final ClassLoader classLoader) {
        cache.clearClassLoader(classLoader);
    }

    protected ExceptionAnalysis(Class cls) {
        super(cls);
    }

    protected void doAnalyze() throws RMIIIOPViolationException {
        super.doAnalyze();

        if (!Exception.class.isAssignableFrom(cls) ||
                RuntimeException.class.isAssignableFrom(cls))
            throw IIOPLogger.ROOT_LOGGER.badRMIIIOPExceptionType(cls.getName(), "1.2.6");

        // calculate exceptionRepositoryId
        StringBuffer b = new StringBuffer("IDL:");
        String pkgName = cls.getPackage().getName();

        while (!"".equals(pkgName)) {
            int idx = pkgName.indexOf('.');
            String n = (idx == -1) ? pkgName : pkgName.substring(0, idx);
            b.append(Util.javaToIDLName(n)).append('/');
            pkgName = (idx == -1) ? "" : pkgName.substring(idx + 1);
        }

        String base = cls.getName();
        base = base.substring(base.lastIndexOf('.') + 1);
        if (base.endsWith("Exception"))
            base = base.substring(0, base.length() - 9);
        base = Util.javaToIDLName(base + "Ex");

        b.append(base).append(":1.0");
        exceptionRepositoryId = b.toString();
    }

    /**
     * Return the repository ID for the mapping of this analysis
     * to an exception.
     */
    public String getExceptionRepositoryId() {
        return exceptionRepositoryId;
    }

}

