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
package org.jboss.as.jpa.hibernate5.scan;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.hibernate.boot.archive.internal.ArchiveHelper;
import org.hibernate.boot.archive.scan.internal.ClassDescriptorImpl;
import org.hibernate.boot.archive.scan.internal.ScanResultCollector;
import org.hibernate.boot.archive.scan.internal.StandardScanOptions;
import org.hibernate.boot.archive.scan.spi.AbstractScannerImpl;
import org.hibernate.boot.archive.scan.spi.MappingFileDescriptor;
import org.hibernate.boot.archive.spi.ArchiveDescriptor;
import org.jboss.as.jpa.hibernate5.VirtualFileSystemArchiveDescriptorFactory;

import org.hibernate.jpa.test.pack.Cat;
import org.hibernate.jpa.test.pack.Distributor;
import org.hibernate.jpa.test.pack.Item;
import org.hibernate.jpa.test.pack.Kitten;
import org.hibernate.jpa.test.pack.cfgxmlpar.Morito;
import org.hibernate.jpa.test.pack.defaultpar.ApplicationServer;
import org.hibernate.jpa.test.pack.defaultpar.IncrementListener;
import org.hibernate.jpa.test.pack.defaultpar.Lighter;
import org.hibernate.jpa.test.pack.defaultpar.Money;
import org.hibernate.jpa.test.pack.defaultpar.Mouse;
import org.hibernate.jpa.test.pack.defaultpar.OtherIncrementListener;
import org.hibernate.jpa.test.pack.defaultpar.Version;
import org.hibernate.jpa.test.pack.excludehbmpar.Caipirinha;
import org.hibernate.jpa.test.pack.explodedpar.Carpet;
import org.hibernate.jpa.test.pack.explodedpar.Elephant;
import org.hibernate.jpa.test.pack.externaljar.Scooter;
import org.hibernate.jpa.test.pack.spacepar.Bug;
import org.hibernate.jpa.test.pack.various.Airplane;
import org.hibernate.jpa.test.pack.various.Seat;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ExplodedExporter;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.vfs.TempFileProvider;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Steve Ebersole
 */
public class ScannerTests {
	protected static ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
	protected static ClassLoader bundleClassLoader;

	protected static TempFileProvider tempFileProvider;

	protected static File testSrcDirectory;

	/**
	 * Directory where shrink-wrap built archives are written
	 */
	protected static File shrinkwrapArchiveDirectory;

	static {
		try {
			tempFileProvider = TempFileProvider.create("test", new ScheduledThreadPoolExecutor(2));
		}
		catch (IOException e) {
			throw new RuntimeException( e );
		}

		// we make an assumption here that the directory which holds compiled classes (nested) also holds
		// sources.   We therefore look for our module directory name, and use that to locate bundles
		final URL scannerTestsClassFileUrl = originalClassLoader.getResource(
				ScannerTests.class.getName().replace( '.', '/' ) + ".class"
		);
		if ( scannerTestsClassFileUrl == null ) {
			// blow up
			fail( "Could not find ScannerTests class file url" );
		}

		// look for the module name in that url
		final int position = scannerTestsClassFileUrl.getFile().lastIndexOf( "/hibernate5/" );

		if ( position == -1 ) {
			fail( "Unable to setup packaging test" );
		}

		final String moduleDirectoryPath = scannerTestsClassFileUrl.getFile().substring( 0, position + "/hibernate5".length() );
		final File moduleDirectory = new File( moduleDirectoryPath );

		testSrcDirectory = new File( new File( moduleDirectory, "src" ), "test" );
		final File bundlesDirectory = new File( testSrcDirectory, "bundles" );
		try {
			bundleClassLoader = new URLClassLoader( new URL[] { bundlesDirectory.toURL() }, originalClassLoader );
		}
		catch ( MalformedURLException e ) {
			fail( "Unable to build custom class loader" );
		}

		shrinkwrapArchiveDirectory = new File( moduleDirectory, "target/packages" );
		shrinkwrapArchiveDirectory.mkdirs();
	}

	@Before
	public void prepareTCCL() {
		// add the bundle class loader in order for ShrinkWrap to build the test package
		Thread.currentThread().setContextClassLoader( bundleClassLoader );
	}

	@After
	public void resetTCCL() throws Exception {
		// reset the classloader
		Thread.currentThread().setContextClassLoader( originalClassLoader );
	}

	protected void addPackageToClasspath(File... files) throws MalformedURLException {
		List<URL> urlList = new ArrayList<URL>();
		for ( File file : files ) {
			urlList.add( file.toURL() );
		}
		URLClassLoader classLoader = new URLClassLoader(
				urlList.toArray( new URL[urlList.size()] ), originalClassLoader
		);
		Thread.currentThread().setContextClassLoader( classLoader );
	}

	protected void addPackageToClasspath(URL... urls) throws MalformedURLException {
		List<URL> urlList = new ArrayList<URL>();
		urlList.addAll( Arrays.asList( urls ) );
		URLClassLoader classLoader = new URLClassLoader(
				urlList.toArray( new URL[urlList.size()] ), originalClassLoader
		);
		Thread.currentThread().setContextClassLoader( classLoader );
	}

	protected File buildDefaultPar() {
		final String fileName = "defaultpar.par";
		final File physicalParFile = new File( shrinkwrapArchiveDirectory, fileName );
		if ( physicalParFile.exists() ) {
			return physicalParFile;
		}

		JavaArchive archive = ShrinkWrap.create( JavaArchive.class, fileName );
		archive.addClasses(
				ApplicationServer.class,
				Lighter.class,
				Money.class,
				Mouse.class,
				OtherIncrementListener.class,
				IncrementListener.class,
				Version.class
		);
		ArchivePath path = ArchivePaths.create( "META-INF/orm.xml" );
		archive.addAsResource( "defaultpar/META-INF/orm.xml", path );

		path = ArchivePaths.create( "META-INF/persistence.xml" );
		archive.addAsResource( "defaultpar/META-INF/persistence.xml", path );

		path = ArchivePaths.create( "org/hibernate/jpa/test/pack/defaultpar/Mouse.hbm.xml" );
		archive.addAsResource( "defaultpar/org/hibernate/jpa/test/pack/defaultpar/Mouse.hbm.xml", path );

		path = ArchivePaths.create( "org/hibernate/jpa/test/pack/defaultpar/package-info.class" );
		archive.addAsResource( "org/hibernate/jpa/test/pack/defaultpar/package-info.class", path );

		archive.as( ZipExporter.class ).exportTo( physicalParFile, true );
		return physicalParFile;
	}

	protected File buildExplicitPar() {
		String fileName = "explicitpar.par";
		JavaArchive archive = ShrinkWrap.create( JavaArchive.class, fileName );
		archive.addClasses(
				Airplane.class,
				Seat.class,
				Cat.class,
				Kitten.class,
				Distributor.class,
				Item.class
		);

		ArchivePath path = ArchivePaths.create( "META-INF/orm.xml" );
		archive.addAsResource( "explicitpar/META-INF/orm.xml", path );

		path = ArchivePaths.create( "META-INF/persistence.xml" );
		archive.addAsResource( "explicitpar/META-INF/persistence.xml", path );

		File testPackage = new File( shrinkwrapArchiveDirectory, fileName );
		archive.as( ZipExporter.class ).exportTo( testPackage, true );
		return testPackage;
	}

	protected File buildExplodedPar() {
		String fileName = "explodedpar";
		JavaArchive archive = ShrinkWrap.create(  JavaArchive.class,fileName );
		archive.addClasses(
				Elephant.class,
				Carpet.class
		);

		ArchivePath path = ArchivePaths.create( "META-INF/persistence.xml" );
		archive.addAsResource( "explodedpar/META-INF/persistence.xml", path );

		path = ArchivePaths.create( "org/hibernate/jpa/test/pack/explodedpar/Elephant.hbm.xml" );
		archive.addAsResource( "explodedpar/org/hibernate/jpa/test/pack/explodedpar/Elephant.hbm.xml", path );

		path = ArchivePaths.create( "org/hibernate/jpa/test/pack/explodedpar/package-info.class" );
		archive.addAsResource( "org/hibernate/jpa/test/pack/explodedpar/package-info.class", path );

		File testPackage = new File( shrinkwrapArchiveDirectory, fileName );
		archive.as( ExplodedExporter.class ).exportExploded( shrinkwrapArchiveDirectory );
		return testPackage;
	}

	protected File buildExcludeHbmPar() {
		String fileName = "excludehbmpar.par";
		JavaArchive archive = ShrinkWrap.create( JavaArchive.class,fileName );
		archive.addClasses(
				Caipirinha.class
		);

		ArchivePath path = ArchivePaths.create( "META-INF/orm2.xml" );
		archive.addAsResource( "excludehbmpar/META-INF/orm2.xml", path );

		path = ArchivePaths.create( "META-INF/persistence.xml" );
		archive.addAsResource( "excludehbmpar/META-INF/persistence.xml", path );

		path = ArchivePaths.create( "org/hibernate/jpa/test/pack/excludehbmpar/Mouse.hbm.xml" );
		archive.addAsResource( "excludehbmpar/org/hibernate/jpa/test/pack/excludehbmpar/Mouse.hbm.xml", path );

		File testPackage = new File( shrinkwrapArchiveDirectory, fileName );
		archive.as( ZipExporter.class ).exportTo( testPackage, true );
		return testPackage;
	}

	protected File buildCfgXmlPar() {
		String fileName = "cfgxmlpar.par";
		JavaArchive archive = ShrinkWrap.create( JavaArchive.class,fileName );
		archive.addClasses(
				Morito.class,
				Item.class
		);

		ArchivePath path = ArchivePaths.create( "META-INF/persistence.xml" );
		archive.addAsResource( "cfgxmlpar/META-INF/persistence.xml", path );

		path = ArchivePaths.create( "org/hibernate/jpa/test/pack/cfgxmlpar/hibernate.cfg.xml" );
		archive.addAsResource( "cfgxmlpar/org/hibernate/jpa/test/pack/cfgxmlpar/hibernate.cfg.xml", path );

		File testPackage = new File( shrinkwrapArchiveDirectory, fileName );
		archive.as( ZipExporter.class ).exportTo( testPackage, true );
		return testPackage;
	}

	protected File buildSpacePar() {
		String fileName = "space par.par";
		JavaArchive archive = ShrinkWrap.create( JavaArchive.class, fileName );
		archive.addClasses(
				Bug.class
		);

		ArchivePath path = ArchivePaths.create( "META-INF/persistence.xml" );
		archive.addAsResource( "space par/META-INF/persistence.xml", path );

		File testPackage = new File( shrinkwrapArchiveDirectory, fileName );
		archive.as( ZipExporter.class ).exportTo( testPackage, true );
		return testPackage;
	}

	protected File buildOverridenPar() {
		String fileName = "overridenpar.jar";
		JavaArchive archive = ShrinkWrap.create( JavaArchive.class, fileName );
		archive.addClasses(
				org.hibernate.jpa.test.pack.overridenpar.Bug.class
		);

		ArchivePath path = ArchivePaths.create( "META-INF/persistence.xml" );
		archive.addAsResource( "overridenpar/META-INF/persistence.xml", path );

		path = ArchivePaths.create( "overridenpar.properties" );
		archive.addAsResource( "overridenpar/overridenpar.properties", path );

		File testPackage = new File( shrinkwrapArchiveDirectory, fileName );
		archive.as( ZipExporter.class ).exportTo( testPackage, true );
		return testPackage;
	}

	protected File buildExternalJar() {
		String fileName = "externaljar.jar";
		JavaArchive archive = ShrinkWrap.create( JavaArchive.class, fileName );
		archive.addClasses(
				Scooter.class
		);

		ArchivePath path = ArchivePaths.create( "META-INF/orm.xml" );
		archive.addAsResource( "externaljar/META-INF/orm.xml", path );

		File testPackage = new File( shrinkwrapArchiveDirectory, fileName );
		archive.as( ZipExporter.class ).exportTo( testPackage, true );
		return testPackage;
	}

	protected File buildLargeJar() {
		final String fileName = "large.jar";
		final JavaArchive archive = ShrinkWrap.create( JavaArchive.class, fileName );

		// Build a large jar by adding a lorem ipsum file repeatedly.
		final File loremipsumTxtFile = new File( testSrcDirectory, "resources/org/hibernate/jpa/test/packaging/loremipsum.txt" );
		for ( int i = 0; i < 100; i++ ) {
			ArchivePath path = ArchivePaths.create( "META-INF/file" + i );
			archive.addAsResource( loremipsumTxtFile, path );
		}

		File testPackage = new File( shrinkwrapArchiveDirectory, fileName );
		archive.as( ZipExporter.class ).exportTo( testPackage, true );
		return testPackage;
	}

	protected File buildWar() {
		String fileName = "war.war";
		WebArchive archive = ShrinkWrap.create( WebArchive.class, fileName );
		archive.addClasses(
				org.hibernate.jpa.test.pack.war.ApplicationServer.class,
				org.hibernate.jpa.test.pack.war.IncrementListener.class,
				org.hibernate.jpa.test.pack.war.Lighter.class,
				org.hibernate.jpa.test.pack.war.Money.class,
				org.hibernate.jpa.test.pack.war.Mouse.class,
				org.hibernate.jpa.test.pack.war.OtherIncrementListener.class,
				org.hibernate.jpa.test.pack.war.Version.class
		);

		ArchivePath path = ArchivePaths.create( "WEB-INF/classes/META-INF/orm.xml" );

		archive.addAsResource( "war/WEB-INF/classes/META-INF/orm.xml", path );

		path = ArchivePaths.create( "WEB-INF/classes/META-INF/persistence.xml" );
		archive.addAsResource( "war/WEB-INF/classes/META-INF/persistence.xml", path );

		path = ArchivePaths.create( "WEB-INF/classes/org/hibernate/jpa/test/pack/war/Mouse.hbm.xml" );
		archive.addAsResource( "war/WEB-INF/classes/org/hibernate/jpa/test/pack/war/Mouse.hbm.xml", path );

		File testPackage = new File( shrinkwrapArchiveDirectory, fileName );
		archive.as( ZipExporter.class ).exportTo( testPackage, true );
		return testPackage;
	}

	protected File buildNestedEar(File includeFile) {
		String fileName = "nestedjar.ear";
		JavaArchive archive = ShrinkWrap.create( JavaArchive.class, fileName );
		archive.addAsResource( includeFile );

		File testPackage = new File( shrinkwrapArchiveDirectory, fileName );
		archive.as( ZipExporter.class ).exportTo( testPackage, true );
		return testPackage;
	}

	protected File buildNestedEarDir(File includeFile) {
		String fileName = "nesteddir.ear";
		JavaArchive archive = ShrinkWrap.create( JavaArchive.class, fileName );
		archive.addAsResource( includeFile );

		File testPackage = new File( shrinkwrapArchiveDirectory, fileName );
		archive.as( ExplodedExporter.class ).exportExploded( shrinkwrapArchiveDirectory );
		return testPackage;
	}
/***
	@Test
	public void testHttp() throws Exception {
		URL url = ArchiveHelper.getJarURLFromURLEntry(
                new URL(
                        "jar:http://www.ibiblio.org/maven/hibernate/jars/hibernate-annotations-3.0beta1.jar!/META-INF/persistence.xml"
                ),
                "/META-INF/persistence.xml"
        );
		try {
			URLConnection urlConnection = url.openConnection();
			urlConnection.connect();
		}
		catch ( IOException ie ) {
			//fail silently
			return;
		}
		ArchiveDescriptor archiveDescriptor = VirtualFileSystemArchiveDescriptorFactory.INSTANCE.buildArchiveDescriptor( url );
        ScanResultCollector resultCollector = new ScanResultCollector ( new StandardScanOptions() );
		archiveDescriptor.visitArchive(
				new AbstractScannerImpl.ArchiveContextImpl(
						new PersistenceUnitDescriptorAdapter(),
						true,
						resultCollector
				)
		);
		assertEquals( 0, resultCollector.getClassDescriptorSet().size() );
		assertEquals( 0, resultCollector.getPackageDescriptorSet().size() );
		assertEquals( 0, resultCollector.getMappingFileSet().size() );
	}

	@Test
	public void testInputStreamZippedJar() throws Exception {
		File defaultPar = buildDefaultPar();
		addPackageToClasspath( defaultPar );

		final VirtualFile virtualFile = VFS.getChild( defaultPar.getAbsolutePath() );
		Closeable closeable = VFS.mountZip( virtualFile, virtualFile, tempFileProvider );

		try {
			ArchiveDescriptor archiveDescriptor = VirtualFileSystemArchiveDescriptorFactory.INSTANCE.buildArchiveDescriptor( defaultPar.toURI().toURL() );
			AbstractScannerImpl.ResultCollector resultCollector = new AbstractScannerImpl.ResultCollector( new StandardScanOptions() );
			archiveDescriptor.visitArchive(
					new AbstractScannerImpl.ArchiveContextImpl(
							new PersistenceUnitDescriptorAdapter(),
							true,
							resultCollector
					)
			);

			validateResults( resultCollector, ApplicationServer.class, Version.class );
		}
		finally {
			closeable.close();
		}
	}

	private void validateResults(AbstractScannerImpl.ResultCollector resultCollector, Class... expectedClasses) throws IOException {
		assertEquals( 3, resultCollector.getClassDescriptorSet().size() );
		for ( Class expectedClass : expectedClasses ) {
			assertTrue(
					resultCollector.getClassDescriptorSet().contains(
							new ClassDescriptorImpl( expectedClass.getName(), null )
					)
			);
		}

		assertEquals( 2, resultCollector.getMappingFileSet().size() );
		for ( MappingFileDescriptor mappingFileDescriptor : resultCollector.getMappingFileSet() ) {
			assertNotNull( mappingFileDescriptor.getStreamAccess() );
			final InputStream stream = mappingFileDescriptor.getStreamAccess().accessInputStream();
			assertNotNull( stream );
			stream.close();
		}
	}

	@Test
	public void testNestedJarProtocol() throws Exception {
		File defaultPar = buildDefaultPar();
		File nestedEar = buildNestedEar( defaultPar );
		addPackageToClasspath( nestedEar );

		final VirtualFile nestedEarVirtualFile = VFS.getChild( nestedEar.getAbsolutePath() );
		Closeable closeable = VFS.mountZip( nestedEarVirtualFile, nestedEarVirtualFile, tempFileProvider );

		try {
			VirtualFile parVirtualFile = nestedEarVirtualFile.getChild( "defaultpar.par" );
			Closeable closeable2 = VFS.mountZip( parVirtualFile, parVirtualFile, tempFileProvider );
			try {
				ArchiveDescriptor archiveDescriptor = VirtualFileSystemArchiveDescriptorFactory.INSTANCE.buildArchiveDescriptor( parVirtualFile.toURL() );

				AbstractScannerImpl.ResultCollector resultCollector = new AbstractScannerImpl.ResultCollector( new StandardScanOptions() );
				archiveDescriptor.visitArchive(
						new AbstractScannerImpl.ArchiveContextImpl(
								new PersistenceUnitDescriptorAdapter(),
								true,
								resultCollector
						)
				);

				validateResults( resultCollector, ApplicationServer.class, Version.class );
			}
			finally {
				closeable2.close();
			}
		}
		finally {
			closeable.close();
		}

		File nestedEarDir = buildNestedEarDir( defaultPar );
		final VirtualFile nestedEarDirVirtualFile = VFS.getChild( nestedEarDir.getAbsolutePath() );

		try {
			VirtualFile parVirtualFile = nestedEarDirVirtualFile.getChild( "defaultpar.par" );
			closeable = VFS.mountZip( parVirtualFile, parVirtualFile, tempFileProvider );
			try {
				ArchiveDescriptor archiveDescriptor = VirtualFileSystemArchiveDescriptorFactory.INSTANCE.buildArchiveDescriptor( parVirtualFile.toURL() );
				AbstractScannerImpl.ResultCollector resultCollector = new AbstractScannerImpl.ResultCollector( new StandardScanOptions() );
				archiveDescriptor.visitArchive(
						new AbstractScannerImpl.ArchiveContextImpl(
								new PersistenceUnitDescriptorAdapter(),
								true,
								resultCollector
						)
				);

				validateResults( resultCollector, ApplicationServer.class, Version.class );
			}
			finally {
				closeable.close();
			}
		}
		finally {
			closeable.close();
		}
	}

	@Test
	public void testJarProtocol() throws Exception {
		File war = buildWar();
		addPackageToClasspath( war );

		final VirtualFile warVirtualFile = VFS.getChild( war.getAbsolutePath() );
		Closeable closeable = VFS.mountZip( warVirtualFile, warVirtualFile, tempFileProvider );

		try {
			ArchiveDescriptor archiveDescriptor = VirtualFileSystemArchiveDescriptorFactory.INSTANCE.buildArchiveDescriptor(
					warVirtualFile.toURL()
			);

			AbstractScannerImpl.ResultCollector resultCollector = new AbstractScannerImpl.ResultCollector( new StandardScanOptions() );
			archiveDescriptor.visitArchive(
					new AbstractScannerImpl.ArchiveContextImpl(
							new PersistenceUnitDescriptorAdapter(),
							true,
							resultCollector
					)
			);

			validateResults(
					resultCollector,
					org.hibernate.jpa.test.pack.war.ApplicationServer.class,
					org.hibernate.jpa.test.pack.war.Version.class
			);
		}
		finally {
			closeable.close();
		}
	}

	@Test
	public void testZippedJar() throws Exception {
		File defaultPar = buildDefaultPar();
		addPackageToClasspath( defaultPar );

		final VirtualFile virtualFile = VFS.getChild( defaultPar.getAbsolutePath() );
		Closeable closeable = VFS.mountZip( virtualFile, virtualFile, tempFileProvider );

		try {
			ArchiveDescriptor archiveDescriptor = VirtualFileSystemArchiveDescriptorFactory.INSTANCE.buildArchiveDescriptor(
					virtualFile.toURL()
			);

			AbstractScannerImpl.ResultCollector resultCollector = new AbstractScannerImpl.ResultCollector( new StandardScanOptions() );
			archiveDescriptor.visitArchive(
					new AbstractScannerImpl.ArchiveContextImpl(
							new PersistenceUnitDescriptorAdapter(),
							true,
							resultCollector
					)
			);

			validateResults( resultCollector, ApplicationServer.class, Version.class );
		}
		finally {
			closeable.close();
		}
	}

	@Test
	public void testExplodedJar() throws Exception {
		File explodedPar = buildExplodedPar();
		addPackageToClasspath( explodedPar );

		String dirPath = explodedPar.getAbsolutePath();
		if ( dirPath.endsWith( "/" ) ) {
			dirPath = dirPath.substring( 0, dirPath.length() - 1 );
		}

		final VirtualFile virtualFile = VFS.getChild( dirPath );

		ArchiveDescriptor archiveDescriptor = VirtualFileSystemArchiveDescriptorFactory.INSTANCE.buildArchiveDescriptor(
				virtualFile.toURL()
		);

		AbstractScannerImpl.ResultCollector resultCollector = new AbstractScannerImpl.ResultCollector( new StandardScanOptions() );
		archiveDescriptor.visitArchive(
				new AbstractScannerImpl.ArchiveContextImpl(
						new PersistenceUnitDescriptorAdapter(),
						true,
						resultCollector
				)
		);

		assertEquals( 1, resultCollector.getClassDescriptorSet().size() );
		assertEquals( 1, resultCollector.getPackageDescriptorSet().size() );
		assertEquals( 1, resultCollector.getMappingFileSet().size() );

		assertTrue(
				resultCollector.getClassDescriptorSet().contains(
						new ClassDescriptorImpl( Carpet.class.getName(), null )
				)
		);

		for ( MappingFileDescriptor mappingFileDescriptor : resultCollector.getMappingFileSet() ) {
			assertNotNull( mappingFileDescriptor.getStreamAccess() );
			final InputStream stream = mappingFileDescriptor.getStreamAccess().accessInputStream();
			assertNotNull( stream );
			stream.close();
		}
	}
****************/

	@Test
	public void testGetBytesFromInputStream() throws Exception {
		File file = buildLargeJar();

		long start = System.currentTimeMillis();
		InputStream stream = new BufferedInputStream(
				new FileInputStream( file ) );
		int oldLength = getBytesFromInputStream( stream ).length;
		stream.close();
		long oldTime = System.currentTimeMillis() - start;

		start = System.currentTimeMillis();
		stream = new BufferedInputStream( new FileInputStream( file ) );
		int newLength = ArchiveHelper.getBytesFromInputStream( stream ).length;
		stream.close();
		long newTime = System.currentTimeMillis() - start;

		assertEquals( oldLength, newLength );
		assertTrue( oldTime > newTime );
	}

	// This is the old getBytesFromInputStream from JarVisitorFactory before
	// it was changed by HHH-7835. Use it as a regression test.
	private byte[] getBytesFromInputStream(InputStream inputStream) throws IOException {
		int size;

		byte[] entryBytes = new byte[0];
		for ( ;; ) {
			byte[] tmpByte = new byte[4096];
			size = inputStream.read( tmpByte );
			if ( size == -1 )
				break;
			byte[] current = new byte[entryBytes.length + size];
			System.arraycopy( entryBytes, 0, current, 0, entryBytes.length );
			System.arraycopy( tmpByte, 0, current, entryBytes.length, size );
			entryBytes = current;
		}
		return entryBytes;
	}

	@Test
	public void testGetBytesFromZeroInputStream() throws Exception {
		// Ensure that JarVisitorFactory#getBytesFromInputStream
		// can handle 0 length streams gracefully.
		URL emptyTxtUrl = getClass().getResource( "/org/hibernate/jpa/test/packaging/empty.txt" );
		if ( emptyTxtUrl == null ) {
			throw new RuntimeException( "Bah!" );
		}
		InputStream emptyStream = new BufferedInputStream( emptyTxtUrl.openStream() );
		int length = ArchiveHelper.getBytesFromInputStream( emptyStream ).length;
		assertEquals( length, 0 );
		emptyStream.close();
	}
}
