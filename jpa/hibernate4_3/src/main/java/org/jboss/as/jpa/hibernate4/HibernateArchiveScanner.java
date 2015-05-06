/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.jpa.hibernate4;

import org.hibernate.jpa.boot.scan.spi.AbstractScannerImpl;
import org.hibernate.jpa.boot.scan.spi.Scanner;

/**
 * Annotation scanner for Hibernate.  Essentially just passes along the VFS-based ArchiveDescriptorFactory
 *
 * @author Steve Ebersole
 */
public class HibernateArchiveScanner extends AbstractScannerImpl implements Scanner {
    public HibernateArchiveScanner() {
        super( VirtualFileSystemArchiveDescriptorFactory.INSTANCE );
    }
}
