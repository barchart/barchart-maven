/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.tooling.features;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.karaf.tooling.utils.MojoSupport;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Generates the features XML file
 * 
 * @version $Revision: 1243453 $
 * @goal features-generate-descriptor-flat
 * @phase compile
 * @execute phase="compile"
 * @requiresDependencyResolution runtime
 * @inheritByDefault true
 * @description generate flattened feature file
 */
public class GenerateFlatDescriptorMojo extends MojoSupport {

	private final static String KARAF_CORE_STANDARD_FEATURE_URL = "mvn:org.apache.karaf.features/standard/%s/xml/features";
	private final static String KARAF_CORE_ENTERPRISE_FEATURE_URL = "mvn:org.apache.karaf.features/enterprise/%s/xml/features";

	/**
	 * name to use for both <features name=""> and <feature name=""> tags
	 * 
	 * @parameter default-value="default-feature"
	 */
	private String featureName;

	/**
	 * The output single/flat feature file to generate
	 * 
	 * @parameter 
	 *            default-value="${project.build.directory}/feature/feature-flat.xml"
	 */
	private File outputFile;

	/**
	 * @parameter
	 */
	private List<String> descriptors;

	/**
	 * @parameter
	 */
	private List<String> features;

	/**
	 * the target karaf version used to resolve Karaf core features descriptors
	 * 
	 * @parameter
	 */
	private String karafVersion;

	/**
	 * @parameter default-value="false"
	 */
	private boolean includeMvnBasedDescriptors;

	/**
	 * @parameter default-value="true"
	 */
	private boolean skipNonMavenProtocols;

	/**
	 * @parameter default-value="true"
	 */
	private boolean failOnArtifactResolutionError;

	/**
	 * @parameter default-value="true"
	 */
	private boolean resolveDefinedRepositoriesRecursively;

	/**
	 * @parameter default-value="true"
	 */
	private boolean addTransitiveFeatures;

	// #############################

	/***/
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		if (karafVersion == null) {
			final Package p = Package
					.getPackage("org.apache.karaf.tooling.features");
			karafVersion = p.getImplementationVersion();
		}

		final String karafCoreEnterpriseFeatureUrl = String.format(
				KARAF_CORE_ENTERPRISE_FEATURE_URL, karafVersion);

		final Artifact enterpriseFeatureDescriptor = resourceToArtifact(
				karafCoreEnterpriseFeatureUrl, true);

		if (enterpriseFeatureDescriptor != null) {
			try {
				resolveBundle(enterpriseFeatureDescriptor, remoteRepos);
				descriptors.add(0, karafCoreEnterpriseFeatureUrl);
			} catch (final Exception e) {
				getLog().warn(
						"Can't add " + karafCoreEnterpriseFeatureUrl
								+ " in the descriptors set");
				getLog().debug(e);
			}
		}

		final String karafCoreStandardFeatureUrl = String.format(
				KARAF_CORE_STANDARD_FEATURE_URL, karafVersion);

		final Artifact standardFeatureDescriptor = resourceToArtifact(
				karafCoreStandardFeatureUrl, true);

		if (standardFeatureDescriptor != null) {
			try {
				resolveBundle(standardFeatureDescriptor, remoteRepos);
				descriptors.add(0, karafCoreStandardFeatureUrl);
			} catch (final Exception e) {
				getLog().warn(
						"Can't add " + karafCoreStandardFeatureUrl
								+ " in the descriptors set");
				getLog().debug(e);
			}
		}

		// #######

		try {

			final Set<String> bundlesSet = new HashSet<String>();
			final Map<String, Feature> featuresMap = new HashMap<String, Feature>();

			for (final String uri : descriptors) {
				retrieveDescriptorsRecursively(uri, bundlesSet, featuresMap);
			}

			/** no features specified, handle all of them */
			if (features == null) {
				features = new ArrayList<String>(featuresMap.keySet());
			}

			final Set<String> featuresBundles = new HashSet<String>();
			final Set<String> transitiveFeatures = new HashSet<String>();

			addFeatures(features, featuresBundles, transitiveFeatures,
					featuresMap);

			/** add the bundles of the configured features to the bundles list */
			bundlesSet.addAll(featuresBundles);

			/**
			 * // if transitive features are enabled we add the contents of
			 * those // features to the bundles list
			 */
			if (addTransitiveFeatures) {
				for (final String feature : transitiveFeatures) {
					/** transitiveFeatures contains name/version */
					final Feature f = featuresMap.get(feature);
					getLog().info(
							"Adding contents of transitive feature: " + feature);
					bundlesSet.addAll(f.getBundles());
					/**
					 * // Treat the config files as bundles, since it is only //
					 * copying
					 */
					bundlesSet.addAll(f.getConfigFiles());
				}
			}

			/**
			 * // bundles with explicitely specified remote repos. // key ->
			 * bundle, value -> remote repo
			 */
			final List<Artifact> explicitRepoBundles = new ArrayList<Artifact>();

			getLog().info("Base repo: " + localRepo.getUrl());

			for (final String bundle : bundlesSet) {

				final Artifact artifact = resourceToArtifact(bundle,
						skipNonMavenProtocols);

				if (artifact == null) {
					continue;
				}

				if (artifact.getRepository() != null) {
					explicitRepoBundles.add(artifact);
				} else {
					/**
					 * bundle URL without repository information are resolved
					 * now
					 */
					resolveBundle(artifact, remoteRepos);
				}

			}

			/**
			 * // resolving all bundles with explicitly specified remote
			 * repository
			 */
			for (final Artifact explicitBundle : explicitRepoBundles) {
				resolveBundle(explicitBundle,
						Collections.singletonList(explicitBundle
								.getRepository()));
			}

			for (final Map.Entry<String, Feature> entry : featuresMap
					.entrySet()) {
				final String name = entry.getKey();
				final Feature feature = entry.getValue();
				getLog().info("name : " + name + " feature : " + feature);
			}

		} catch (final MojoExecutionException e) {
			throw e;
		} catch (final MojoFailureException e) {
			throw e;
		} catch (final Exception e) {
			throw new MojoExecutionException("Error populating repository", e);
		}

	}

	private void retrieveDescriptorsRecursively(final String uri,
			final Set<String> bundles, final Map<String, Feature> featuresMap)
			throws Exception {

		/**
		 * // let's ensure a mvn: based url is sitting in the local repo before
		 * we // try reading it
		 */

		final Artifact descriptor = resourceToArtifact(uri, true);

		if (descriptor != null) {
			resolveBundle(descriptor, remoteRepos);
		}

		if (includeMvnBasedDescriptors) {
			bundles.add(uri);
		}

		final Repository repo = new Repository(
				URI.create(translateFromMaven(uri.replaceAll(" ", "%20"))));

		for (final Feature f : repo.getFeatures()) {
			featuresMap.put(f.getName() + "/" + f.getVersion(), f);
		}

		if (resolveDefinedRepositoriesRecursively) {
			for (final String r : repo.getDefinedRepositories()) {
				retrieveDescriptorsRecursively(r, bundles, featuresMap);
			}
		}

	}

	/**
	 * // resolves the bundle in question // TODO neither remoteRepos nor
	 * bundle's Repository are used, only the local // repo?????
	 */
	private void resolveBundle(final Artifact bundle,
			final List<ArtifactRepository> remoteRepos) throws IOException,
			MojoFailureException {

		/** TODO consider DefaultRepositoryLayout */

		// final String entry = MavenUtil.artifactToMvn(bundle);
		// getLog().info("entry=" + entry);

		try {

			getLog().info("resolving bundle: " + bundle);

			resolver.resolve(bundle, remoteRepos, localRepo);

		} catch (final ArtifactResolutionException e) {
			if (failOnArtifactResolutionError) {
				throw new MojoFailureException(
						"Can't resolve bundle " + bundle, e);
			}
			getLog().error("Can't resolve bundle " + bundle, e);
		} catch (final ArtifactNotFoundException e) {
			if (failOnArtifactResolutionError) {
				throw new MojoFailureException(
						"Can't resolve bundle " + bundle, e);
			}
			getLog().error("Can't resolve bundle " + bundle, e);
		}
	}

	private void addFeatures(final List<String> features,
			final Set<String> featuresBundles,
			final Set<String> transitiveFeatures,
			final Map<String, Feature> featuresMap) {

		for (String feature : features) {

			// feature could be only the name or name/version
			final int delimIndex = feature.indexOf('/');
			String version = null;
			if (delimIndex > 0) {
				version = feature.substring(delimIndex + 1);
				feature = feature.substring(0, delimIndex);
			}

			Feature f = null;

			if (version != null) {
				// looking for a specific feature with name and version
				f = featuresMap.get(feature + "/" + version);
			} else {
				// looking for the first feature name (whatever the version is)
				for (final String key : featuresMap.keySet()) {
					final String[] nameVersion = key.split("/");
					if (feature.equals(nameVersion[0])) {
						f = featuresMap.get(key);
						break;
					}
				}
			}

			if (f == null) {
				throw new IllegalArgumentException(
						"Unable to find the feature '" + feature + "'");
			}

			// only add the feature to transitives if it is not
			// listed in the features list defined by the config
			if (!this.features.contains(f.getName() + "/" + f.getVersion())) {
				transitiveFeatures.add(f.getName() + "/" + f.getVersion());
			} else {
				// add the bundles of the feature to the bundle set
				getLog().info(
						"Adding contents for feature: " + f.getName() + "/"
								+ f.getVersion());
				featuresBundles.addAll(f.getBundles());
				// Treat the config files as bundles, since it is only copying
				featuresBundles.addAll(f.getConfigFiles());
			}

			addFeatures(f.getDependencies(), featuresBundles,
					transitiveFeatures, featuresMap);

		}
	}

	public static class Feature {

		private final String name;
		private String version;
		private final List<String> dependencies = new ArrayList<String>();
		private final List<String> bundles = new ArrayList<String>();
		private final Map<String, Map<String, String>> configs = new HashMap<String, Map<String, String>>();
		private final List<String> configFiles = new ArrayList<String>();

		public Feature(final String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public String getVersion() {
			return version;
		}

		public void setVersion(final String version) {
			this.version = version;
		}

		public List<String> getDependencies() {
			return dependencies;
		}

		public List<String> getBundles() {
			return bundles;
		}

		public Map<String, Map<String, String>> getConfigurations() {
			return configs;
		}

		public List<String> getConfigFiles() {
			return configFiles;
		}

		public void addDependency(final String dependency) {
			dependencies.add(dependency);
		}

		public void addBundle(final String bundle) {
			bundles.add(bundle);
		}

		public void addConfig(final String name,
				final Map<String, String> properties) {
			configs.put(name, properties);
		}

		public void addConfigFile(final String configFile) {
			configFiles.add(configFile);
		}
	}

	public static class Repository {

		private final URI uri;
		private List<Feature> features;
		private List<String> repositories;

		public Repository(final URI uri) {
			this.uri = uri;
		}

		public URI getURI() {
			return uri;
		}

		public Feature[] getFeatures() throws Exception {
			if (features == null) {
				loadFeatures();
			}
			return features.toArray(new Feature[features.size()]);
		}

		public String[] getDefinedRepositories() throws Exception {
			if (repositories == null) {
				loadRepositories();
			}
			return repositories.toArray(new String[repositories.size()]);
		}

		private void loadRepositories() throws IOException {
			try {
				repositories = new ArrayList<String>();
				final DocumentBuilderFactory factory = DocumentBuilderFactory
						.newInstance();
				final Document doc = factory.newDocumentBuilder().parse(
						uri.toURL().openStream());
				final NodeList nodes = doc.getDocumentElement().getChildNodes();
				for (int i = 0; i < nodes.getLength(); i++) {
					final org.w3c.dom.Node node = nodes.item(i);
					if (!(node instanceof Element)
							|| !"repository".equals(node.getNodeName())) {
						continue;
					}
					final Element e = (Element) nodes.item(i);
					repositories.add(e.getTextContent());
				}
			} catch (final SAXException e) {
				throw (IOException) new IOException().initCause(e);
			} catch (final ParserConfigurationException e) {
				throw (IOException) new IOException().initCause(e);
			}
		}

		private void loadFeatures() throws IOException {
			try {
				features = new ArrayList<Feature>();
				final DocumentBuilderFactory factory = DocumentBuilderFactory
						.newInstance();
				final Document doc = factory.newDocumentBuilder().parse(
						uri.toURL().openStream());
				final NodeList nodes = doc.getDocumentElement().getChildNodes();
				for (int i = 0; i < nodes.getLength(); i++) {
					final org.w3c.dom.Node node = nodes.item(i);
					if (!(node instanceof Element)
							|| !"feature".equals(node.getNodeName())) {
						continue;
					}
					final Element e = (Element) nodes.item(i);
					final String name = e.getAttribute("name");
					final String version = e.getAttribute("version");
					final Feature f = new Feature(name);
					f.setVersion(version);
					final NodeList featureNodes = e
							.getElementsByTagName("feature");
					for (int j = 0; j < featureNodes.getLength(); j++) {
						final Element b = (Element) featureNodes.item(j);
						f.addDependency(b.getTextContent());
					}
					final NodeList configNodes = e
							.getElementsByTagName("config");
					for (int j = 0; j < configNodes.getLength(); j++) {
						final Element c = (Element) configNodes.item(j);
						final String cfgName = c.getAttribute("name");
						final String data = c.getTextContent();
						final Properties properties = new Properties();
						properties.load(new ByteArrayInputStream(data
								.getBytes()));
						final Map<String, String> hashtable = new Hashtable<String, String>();
						for (final Object key : properties.keySet()) {
							final String n = key.toString();
							hashtable.put(n, properties.getProperty(n));
						}
						f.addConfig(cfgName, hashtable);
					}
					final NodeList configFileNodes = e
							.getElementsByTagName("configfile");
					for (int j = 0; j < configFileNodes.getLength(); j++) {
						final Element c = (Element) configFileNodes.item(j);
						f.addConfigFile(c.getTextContent());
					}
					final NodeList bundleNodes = e
							.getElementsByTagName("bundle");
					for (int j = 0; j < bundleNodes.getLength(); j++) {
						final Element b = (Element) bundleNodes.item(j);
						f.addBundle(b.getTextContent());
					}
					features.add(f);
				}
			} catch (final SAXException e) {
				throw (IOException) new IOException().initCause(e);
			} catch (final ParserConfigurationException e) {
				throw (IOException) new IOException().initCause(e);
			}
		}

	}
}
