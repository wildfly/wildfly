/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.persistence.jipijapa.hibernate7;


import org.hibernate.archive.scan.spi.AbstractScannerImpl;
import org.hibernate.boot.archive.scan.spi.Scanner;

/**
 * Annotation scanner for Hibernate.  Essentially just passes along the VFS-based ArchiveDescriptorFactory
 *
 * @author Steve Ebersole
 */
public class HibernateArchiveScanner extends AbstractScannerImpl implements Scanner {
    public HibernateArchiveScanner() {
        super(VirtualFileSystemArchiveDescriptorFactory.INSTANCE);
    }
}
