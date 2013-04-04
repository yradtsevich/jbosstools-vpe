/*******************************************************************************
 * Copyright (c) 2007-2012 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.vpe.cordovasim.eclipse.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.osgi.framework.internal.core.BundleFragment;
import org.eclipse.osgi.framework.internal.core.BundleHost;
import org.eclipse.ui.IWorkbenchListener;
import org.eclipse.ui.PlatformUI;
import org.jboss.tools.vpe.browsersim.browser.PlatformUtil;
import org.jboss.tools.vpe.browsersim.eclipse.launcher.BrowserSimLauncher;
import org.jboss.tools.vpe.browsersim.eclipse.launcher.ExternalProcessCallback;
import org.jboss.tools.vpe.browsersim.eclipse.launcher.ExternalProcessLauncher;
import org.jboss.tools.vpe.cordovasim.eclipse.Activator;
import org.jboss.tools.vpe.cordovasim.eclipse.callbacks.CordovaSimCallback;
import org.jboss.tools.vpe.cordovasim.eclipse.callbacks.OpenFileCallback;
import org.jboss.tools.vpe.cordovasim.eclipse.callbacks.ViewSourceCallback;
import org.osgi.framework.Bundle;

/**
 * @author "Yahor Radtsevich (yradtsevich)"
 */
@SuppressWarnings("restriction")
public class CordovaSimLauncher {
	public static final String CORDOVASIM_CLASS_NAME = "org.jboss.tools.vpe.cordovasim.CordovaSimRunner"; //$NON-NLS-1$
	private static final List<ExternalProcessCallback> CORDOVASIM_CALLBACKS = BrowserSimLauncher.BROWSERSIM_CALLBACKS;
	private static final List<String> REQUIRED_BUNDLES = new ArrayList<String>(); 
	static {
		REQUIRED_BUNDLES.addAll(BrowserSimLauncher.REQUIRED_BUNDLES);
		REQUIRED_BUNDLES.addAll(Arrays.asList(
			"org.eclipse.jetty.continuation",
			"org.eclipse.jetty.http",
			"org.eclipse.jetty.io",
			"org.eclipse.jetty.server",
			"org.eclipse.jetty.servlet",
			"org.eclipse.jetty.util",
			"javax.servlet"
		));
	}
	
	private static final List<String> OPTIONAL_BUNDLES = BrowserSimLauncher.OPTIONAL_BUNDLES;	

	//if you change this parameter, see also @org.jbosstools.browsersim.ui.BrowserSim
	private static final String NOT_STANDALONE = BrowserSimLauncher.NOT_STANDALONE;	

	public static void launchCordovaSim(String initialUrl) {
		List<String> parameters = new ArrayList<String>();
		parameters.add(NOT_STANDALONE);

		File file = null;
		if (initialUrl != null) {
			try {
				file = new File(new URI(initialUrl));
			} catch (URISyntaxException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			}
			if (file != null) {
				parameters.add(file.toString());
			}
		}
		processBuilder.directory(FileLocator.getBundleFile(Platform.getBundle("org.jboss.tools.vpe.cordovasim"))); // еее
		ExternalProcessLauncher.launchAsExternalProcess(REQUIRED_BUNDLES, OPTIONAL_BUNDLES,
				CORDOVASIM_CALLBACKS, CORDOVASIM_CLASS_NAME, parameters);
	}
}
