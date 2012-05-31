package com.barchart.maven.sequence;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.joda.time.DateTime;

/**
 * 
 * @goal sequence
 * 
 * @phase validate
 */
public class MojoSequence extends AbstractMojo {

	/**
	 * This is an internal variable automatically set by Maven
	 * 
	 * @parameter expression="${project}"
	 */
	private MavenProject project;

	private void setProjectProperty(String key, String value) {
		project.getProperties().setProperty(key, value);
	}

	/**
	 * 
	 * @parameter default-value="${project.basedir}/sequence.properties"
	 */
	private File sequenceFile;

	/**
	 * configuration parameter
	 * 
	 * @parameter
	 */
	private Counter[] counters;

	public void setCounters(Counter[] counters) {
		this.counters = counters;
	}

	public void execute() throws MojoExecutionException {

		final Log log = getLog();

		try {

			if (counters == null || counters.length == 0) {
				log.error("no counters are configured for this plugin");
				return;
			}

			log.debug("sequenceFile=" + sequenceFile);

			if (!sequenceFile.exists()) {
				sequenceFile.createNewFile();
				log.info("created new sequenceFile=" + sequenceFile);
			}

			// Read properties file.
			Properties properties = new Properties();
			FileInputStream fis = new FileInputStream(sequenceFile);
			properties.load(fis);
			fis.close();

			// manage counters
			for (Counter counter : counters) {

				String counterName = counter.getName();

				String counterValueString = properties.getProperty(counterName);
				if (counterValueString == null) {
					counterValueString = counter.getStartValueString();
				}

				// use current counter value for current build
				setProjectProperty(counterName, counterValueString);

				// increment counter value for the next build
				counterValueString = counter
						.getValueStringWithIncrement(counterValueString);
				properties.setProperty(counterName, counterValueString);

			}

			// Write properties file.
			FileOutputStream fos = new FileOutputStream(sequenceFile);
			String comment = "counters file managed by " + getClass().getName();
			properties.store(fos, comment);
			fos.close();

		} catch (Exception e) {
			log.error("execute failed", e);
			throw new MojoExecutionException(e.getMessage());
		}

	}

}
