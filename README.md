SchemaTranslatorMavenPlugin
===========================

A maven plugin that generates a DDL file from JPA annotated classes.
Example of use:

`<plugin>
	<groupId>nz.co.senanque</groupId>
	<artifactId>schema-translator-maven-plugin</artifactId>
	<version>1.0.0</version>
	<executions>
		<execution>
			<id>sql-generate</id>
			<phase>package</phase>
			<goals>
				<goal>sql</goal>
			</goals>
		</execution>
	</executions>
	<configuration>
		<persistenceFile>src/main/resources/META-INF/persistence-workflow.xml</persistenceFile>
		<persistenceUnit>pu-workflow</persistenceUnit>
		<dialect>org.hibernate.dialect.H2Dialect</dialect>
		<drops>false</drops>
	</configuration>
	<dependencies>
		<!-- Need this because my entity classes refer to items in MaduraObjects -->
		<dependency>
			<groupId>nz.co.senanque</groupId>
			<artifactId>madura-objects</artifactId>
			<version>2.2.2-SNAPSHOT</version>
		</dependency>
	</dependencies>
</plugin>`

The dependency may not be needed in your project, or you may need other dependencies. If your JPA Entity classes
refer to other project-related classes or interfaces or annotations they need to be in this dependencies list.

The resulting file will be placed in target/${project.artifactId}-${project.version}.sql though you can change that
using `<destFile>` parameter.