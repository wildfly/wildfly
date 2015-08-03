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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.net.URISyntaxException;
import java.net.URL;

import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;

/**
 * @author Steve Ebersole
 */
public class VirtualFileAdaptor {
	private static final long serialVersionUID = -4509594124653184347L;

	private static final ObjectStreamField[] serialPersistentFields = {
			new ObjectStreamField("path", String.class)
	};

	/** Minimal info to get full vfs file structure */
	private String path;
	/** The virtual file */
	private transient VirtualFile file;

	public VirtualFileAdaptor(VirtualFile file)
	{
		this.file = file;
	}

	public VirtualFileAdaptor(String path)
	{
		if (path == null)
			throw new IllegalArgumentException("Null path");

		this.path = path;
	}

	/**
	 * Get the virtual file.
	 * Create file from root url and path if it doesn't exist yet.
	 *
	 * @return virtual file root
	 * @throws IOException for any error
	 */
	@SuppressWarnings("deprecation")
	protected VirtualFile getFile() throws IOException
	{
		if (file == null)
		{
			file = VFS.getChild( path );
		}
		return file;
	}

	@SuppressWarnings("deprecation")
	public VirtualFileAdaptor findChild(String child) throws IOException
	{
		VirtualFile vf = getFile().getChild(child);
		return new VirtualFileAdaptor(vf);
	}

	public URL toURL()
	{
		try
		{
			return getFile().toURL();
		}
		catch (Exception e)
		{
			return null;
		}
	}

	private void writeObject(ObjectOutputStream out) throws IOException, URISyntaxException
	{
		String pathName = path;
		if (pathName == null)
			pathName = getFile().getPathName();

		ObjectOutputStream.PutField fields = out.putFields();
		fields.put("path", pathName);
		out.writeFields();
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
	{
		ObjectInputStream.GetField fields = in.readFields();
		path = (String) fields.get("path", null);
	}
}
