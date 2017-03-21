package nz.co.senanque.schematranslator;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.net.URL;
import java.util.List;

import net.peachjean.slf4j.mojo.AbstractLoggingMojo;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.classworlds.ClassRealm;
import org.codehaus.classworlds.ClassWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Goal which generates a schema sql script from JPA annotated classes
 * 
 * @goal sql
 * @phase package
 * 
 */
public class SchemaTranslatorMojo
    extends AbstractLoggingMojo
{
	private static final Logger log = LoggerFactory
			.getLogger(SchemaTranslatorMojo.class);
	/**
	 * Maven project
	 * 
	 * @parameter expression="${project}"
	 * @required
	 * 
	 */
	protected MavenProject project;

	/**
	 * Database dialect.
	 * 
	 * @parameter expression="org.hibernate.dialect.H2Dialect"
	 * @required
	 * 
	 */
	private String dialect;

	/**
	 * Name of the dest file.
	 * 
	 * @parameter expression="${project.artifactId}-${project.version}.sql"
	 * @required
	 */
    private String destFile;
	/**
	 * Name of the wrapper file.
	 * @parameter
	 * 
	 */
    private String wrapperFile;

	/**
	 * Location of the dest dir.
	 * 
	 * @parameter expression="${basedir}/target/"
	 * @required
	 */
    private String destDir;

	/**
	 * Name of the persistence file.
	 * 
	 * @parameter
	 * @required
	 */
    private String persistenceFile;

	/**
	 * Name of the persistence unit.
	 * 
	 * @parameter
	 * @required
	 */
    private String persistenceUnit;
    
	/**
	 * drops flag.
	 * 
	 * @parameter expression="false"
	 */
    private boolean drops;
    
    private String getPath(String name)
    {
    	Object base = getPluginContext().get("project.base");
    	if (base != null && base instanceof String) {
    		return base.toString()+File.separatorChar+name;
    	}
    	return name;
    }

    public void executeWithLogging()
        throws MojoExecutionException
    {
        String packageDirectory = getPath(destDir)+File.separatorChar;
        File targetDir = new File(packageDirectory);
        targetDir.mkdirs();
        
        SchemaTranslator translator = new SchemaTranslator();

        FileOutputStream out;
        BufferedReader reader=null;
		try {
			out = new FileOutputStream(new File(destDir+destFile));
			if (StringUtils.isNotEmpty(wrapperFile)) {
				reader = new BufferedReader(new FileReader(wrapperFile));
				String line = reader.readLine();
				while (line != null && !line.startsWith("<insert>")) {
					out.write(line.getBytes());
					out.write('\n');
					line = reader.readLine();
				}
			}
			
			translator.setDialect(dialect)
				.setDrops(drops)
				.setClassLoader(getClassLoader(project.getTestClasspathElements()))
				.addAnnotatedClasses(getPath(persistenceFile),getPath(persistenceUnit))
				.translate(out);
			if (reader != null) {
				out.write('\n');
				String line = reader.readLine();
				while (line != null) {
					out.write(line.getBytes());
					out.write('\n');
					line = reader.readLine();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new MojoExecutionException(e.getMessage());
		}
    }

	/**
	 * Found this code <a href="http://4thline.org/articles/Extending%20the%20Maven%20plugin%20classpath.html>here</a>.
	 * @param elements
	 * @return valid classloader
	 * @throws MojoExecutionException
	 */
	protected ClassLoader getClassLoader(List<String> elements) throws MojoExecutionException {
	    ClassWorld world = new ClassWorld();
	    ClassRealm realm;
	    try {
	        realm = world.newRealm(
	                "maven.plugin." + getClass().getSimpleName(),
	                Thread.currentThread().getContextClassLoader()
	        );

	        for (String element : elements) {
	            File elementFile = new File(element);
	            getLog().debug("Adding element to plugin classpath" + elementFile.getPath());
	            URL url = new URL("file:///" + elementFile.getPath() + (elementFile.isDirectory() ? "/" : ""));
	            realm.addConstituent(url);
	        }
	    } catch (Exception ex) {
	        throw new MojoExecutionException(ex.toString(), ex);
	    }
	    Thread.currentThread().setContextClassLoader(realm.getClassLoader());
	    return realm.getClassLoader();
	}
	
	public String getDialect() {
		return dialect;
	}

	public void setDialect(String dialect) {
		this.dialect = dialect;
	}

	public String getDestDir() {
		return destDir;
	}

	public void setDestDir(String destdir) {
		this.destDir = destdir;
	}

	public String getPersistenceFile() {
		return persistenceFile;
	}

	public void setPersistenceFile(String persistenceFile) {
		this.persistenceFile = persistenceFile;
	}

	public String getPersistenceUnit() {
		return persistenceUnit;
	}

	public void setPersistenceUnit(String persistenceUnit) {
		this.persistenceUnit = persistenceUnit;
	}

	public String getWrapperFile() {
		return wrapperFile;
	}

	public void setWrapperFile(String wrapperFile) {
		this.wrapperFile = wrapperFile;
	}

}
