/**
 * Flexmojos is a set of maven goals to allow maven users to compile, optimize and test Flex SWF, Flex SWC, Air SWF and Air SWC.
 * Copyright (C) 2008-2012  Marvin Froeder <marvin@flexmojos.net>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package info.rvin.flexmojos.encrypter;

import info.rvin.flexmojos.encrypter.encryptations.AesEncrypter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.util.Base64;

/**
 * Goal which encrypt swf files.
 *
 * @goal encrypt-swf
 * @phase package
 */
public class EncrypterMojo extends AbstractMojo {

	/**
	 * The maven project.
	 *
	 * @parameter expression="${project}"
	 * @required
	 * @readonly
	 */
	protected MavenProject project;

	/**
	 * @component
	 */
	protected MavenProjectHelper projectHelper;

	/**
	 * @parameter expression="${encrypt.key}"
	 */
	private String key;

	/**
	 * Initialization vector
	 *
	 * @parameter expression="${encrypt.iv}"
	 */
	private String iv;

	@SuppressWarnings("unchecked")
	private List<Artifact> getGeneratedSwf() {
		List<Artifact> artifacts = new ArrayList<Artifact>();
		List<Artifact> attachedArtifacts = project.getAttachedArtifacts();
		for (Artifact artifact : attachedArtifacts) {
			if ("swf".equals(artifact.getType())) {
				artifacts.add(artifact);
			}
		}
		if ("swf".equals(project.getArtifact().getType())) {
			artifacts.add(project.getArtifact());
		}
		return artifacts;
	}

	public void execute() throws MojoExecutionException, MojoFailureException {
		if(key == null) {
			key = generateKey(project.getArtifact().toString());
			getLog().warn("Attetion, no encryption key defined.  Generating one: " + key);
		}

		if(iv == null) {
			iv = generateKey(project.getGroupId());
			getLog().warn("Attetion, no initialization vector defined.  Generating one: " + iv);
		}

		List<Artifact> artifacts = getGeneratedSwf();

		for (Artifact artifact : artifacts) {
			getLog().info("Encrypting artifact " + artifact);
			File enc = getDestinationFile(project.getBuild(), artifact);
			getLog().debug("Encrypted file: " + enc);
			encrypt(artifact, enc);
			projectHelper.attachArtifact(project, "eswf", artifact
					.getClassifier(), enc);
		}
	}

	private String generateKey(String baseString) {
		byte[] baseBytes = baseString.getBytes();
		byte[] baseKey = Base64.encodeBase64(baseBytes);
		String key = "";
		for (byte b : baseKey) {
			key += b;
		}

		while(key.length() < 32) {
			key += key;
		}

		while(key.length() % 2 == 1 || key.length() > 32) {
			key = key.substring(1);
		}

		return key;
	}

	private File getDestinationFile(Build build, Artifact artifact) {
		String fileName = project.getBuild().getFinalName();
		if (artifact.getClassifier() != null) {
			fileName += "-" + artifact.getClassifier();
		}
		fileName += ".eswf";

		return new File(project.getBuild().getDirectory(), fileName);
	}

	private void encrypt(Artifact artifact, File enc)
			throws MojoExecutionException {
		try {
			new AesEncrypter(getLog()).encrypt(key, iv, artifact.getFile(), enc);
		} catch (IOException e) {
			throw new MojoExecutionException("Unable to encrypt artifact "
					+ artifact, e);
		}
	}

}
