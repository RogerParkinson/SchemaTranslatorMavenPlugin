/**
 * 
 */
package nz.co.senanque.schematranslator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.hibernate.engine.jdbc.internal.Formatter;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;

/**
 * @author Alexey Zvolinskiy
 * @author Roger Parkinson (Adapted to Maven plugin)
 *
 */
public class SchemaTranslator {

	private Configuration config = null;
	private ClassLoader m_classLoader;
	private boolean m_drops = false;
	
	public SchemaTranslator() {
		config = new Configuration();
		m_classLoader = ClassLoader.getSystemClassLoader();
	}
	
	public SchemaTranslator setDialect(String dialect) {
		config.setProperty(AvailableSettings.DIALECT, dialect);
		return this;
	}
	
	public SchemaTranslator setClassLoader(ClassLoader classLoader) {
		m_classLoader = classLoader;
		return this;
	}
	
	public SchemaTranslator setDrops(boolean drops) {
		m_drops = drops;
		return this;
	}
	
	/**
	 * Method determines classes which will be used for DDL generation. 
	 * @param annotatedClasses - entities annotated with Hibernate annotations.
	 */
	public SchemaTranslator addAnnotatedClasses(Class<?>[] annotatedClasses) {
		for (Class<?> clazz : annotatedClasses) {
			config.addAnnotatedClass(clazz);
		}
		return this;
	}
	/**
	 * Method determines classes used for DDL generation by examining the persistence file
	 * and the persistence unit in it.
	 * @param persistenceFile
	 * @param persistenceUnit
	 * @return SchemaTranslator
	 * @throws Exception
	 */
	public SchemaTranslator addAnnotatedClasses(String persistenceFile, String persistenceUnit) throws Exception {
		File f = new File(persistenceFile);
		String cname = "http://java.sun.com/xml/ns/persistence";
		Namespace ns = Namespace.getNamespace(cname);
		Document build = new org.jdom.input.SAXBuilder().build(f);

		Element pu = build.getRootElement().getChild("persistence-unit", ns);
		for (Object clz : pu.getChildren("class", ns)) {
			String className = ((Element) clz).getText();
			try {
				Class<?> clazz = Class.forName(className,false,m_classLoader);
				config.addAnnotatedClass(clazz);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return this;
	}
	
	
	/**
	 * Method performs translation of entities in table schemas.
	 * It generates 'CREATE' and 'DELETE' scripts for the Hibernate entities.
	 * Current implementation involves usage of {@link #write(FileOutputStream, String[], Formatter)} method.
	 * --Removed the drops because this is mainly for in-memory databases and there are errors in the drops. 
	 * @param outputStream - stream will be used for *.sql file creation.
	 * @throws IOException
	 */
	public SchemaTranslator translate(FileOutputStream outputStream) throws IOException {
		Dialect requiredDialect = Dialect.getDialect(config.getProperties());
		String[] query = null;
		outputStream.write(("/* Schema definition for "+
				config.getProperty(AvailableSettings.DIALECT)+
				"*/").getBytes());
		
		if (m_drops) {
			query = config.generateDropSchemaScript(requiredDialect);
			write(outputStream, query, FormatStyle.DDL.getFormatter());
		}
		query = config.generateSchemaCreationScript(requiredDialect);
		write(outputStream, query, FormatStyle.DDL.getFormatter());
		
		return this;
	}
	
	/**
	 * Method writes line by line DDL scripts in the output stream.
	 * Also each line logs in the console.
	 * @throws IOException
	 */
	private void write(FileOutputStream outputStream, String[] lines, Formatter formatter) 
			throws IOException {
		String tempStr = null;
		
		for (String line : lines) {
			tempStr = formatter.format(line)+";";
//			System.out.println(tempStr);
			outputStream.write(tempStr.getBytes());
		}
	}

}


