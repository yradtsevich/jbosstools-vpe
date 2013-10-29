/*******************************************************************************
 * Copyright (c) 2007-2013 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.vpe.browsersim.ui.debug.firebug;

import org.eclipse.swt.SWT;
import org.jboss.tools.vpe.browsersim.browser.ExtendedVisibilityWindowListener;
import org.jboss.tools.vpe.browsersim.browser.ExtendedWindowEvent;
import org.jboss.tools.vpe.browsersim.browser.IBrowser;
import org.jboss.tools.vpe.browsersim.browser.WebKitBrowserFactory;
import org.eclipse.swt.browser.CloseWindowListener;
import org.eclipse.swt.browser.VisibilityWindowListener;
import org.eclipse.swt.browser.WindowEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Shell;
import org.jboss.tools.vpe.browsersim.browser.PlatformUtil;
import org.jboss.tools.vpe.browsersim.ui.skin.BrowserSimSkin;
import org.jboss.tools.vpe.browsersim.util.BrowserSimUtil;

/**
 * @author "Yahor Radtsevich (yradtsevich)"
 */
public class FireBugLiteLoader {
	private static final String FIREBUG_LITE_JS_URL = "https://getfirebug.com/firebug-lite.js"; //$NON-NLS-1$
	
	public static void startFireBugOpening(IBrowser browser) {
		browser.execute("window._fireBugLiteLoading = true;");
		Object fireBugScriptLoaded = browser.evaluate("return !!window.Firebug");
		if (!Boolean.TRUE.equals(fireBugScriptLoaded)) {
			browser.execute(
				"(function() {" +
				   "var initializeFireBug = function() {" +
						  /* Cordova's InAppBrowser API overrides window.open function, so we have to restore it
						   * (see RippleInjector.java and JBIDE-14625) */
						  "var cordovaOpen = window.open;" +
						  "window.open = window._bsOriginalWindowOpen || window.open;" +
						  "window.Firebug.chrome.toggle(true, true);" +
						  "window.open = cordovaOpen;" +
					"};" +
					"var e = document.createElement('script');" +
					"e.type = 'text/javascript';" +
					"e.src = '" + FIREBUG_LITE_JS_URL + "';" +
					"e.addEventListener('load'," +
						"function() {" +
							/* XXX: Two timeouts because we need to run our initializeFireBug method
							 * AFTER two inner FireBug Lite timeouts */
							"setTimeout(function() {" +
								"setTimeout(initializeFireBug, 0)" +
							 "}, 0)" +
						"}, false);" +
					"document.head.appendChild(e);" +
				"})()");
		} else {
			browser.execute(
					"var cordovaOpen = window.open;" +
					"window.open = window._bsOriginalWindowOpen || window.open;" +
					"window.Firebug.chrome.close();" +  // cannot open FireBug Lite twice without this line 
					"window.Firebug.chrome.toggle(true, true);" +
					"window.open = cordovaOpen;"
			);
		}
	}
	
	public static boolean isFireBugPopUp(WindowEvent openWindowEvent) {
		IBrowser parentBrowser = (IBrowser) openWindowEvent.widget;
		return Boolean.TRUE.equals(parentBrowser.evaluate("return !!window._fireBugLiteLoading"));
	}
	
	public static void processFireBugPopUp(ExtendedWindowEvent openWindowEvent, BrowserSimSkin skin) {
		final IBrowser parentBrowser = (IBrowser) openWindowEvent.widget;
		parentBrowser.execute("window._fireBugLiteLoading = false;");
		
		Shell shell = new Shell(BrowserSimUtil.getParentShell(skin), SWT.SHELL_TRIM);
		shell.setLayout(new FillLayout());
		
		final IBrowser fireBugBrowser = new WebKitBrowserFactory().createBrowser(shell, SWT.NONE);
		openWindowEvent.browser = fireBugBrowser;
		
		fireBugBrowser.addVisibilityWindowListener(new ExtendedVisibilityWindowListener() {
			@Override
			public void show(ExtendedWindowEvent event) {
				IBrowser fblBrowser = (IBrowser)event.widget;
				final Shell shell = fblBrowser.getShell();
				if (event.location != null) {
					shell.setLocation(event.location);
				}
				shell.open();
			}
			
			@Override
			public void hide(ExtendedWindowEvent event) {
				IBrowser browser = (IBrowser)event.widget;
				Shell shell = browser.getShell();
				shell.setVisible(false);
			}
		});
		
		skin.getShell().addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent arg0) {
				if (!fireBugBrowser.isDisposed() && !fireBugBrowser.getShell().isDisposed()) {
					fireBugBrowser.getShell().dispose();
				}
			}
		});
		
		// yradtsevich: fix for JBIDE-13625 Browsersim closes unexpectively when closing Firebug Lite
		if (PlatformUtil.OS_MACOSX.equals(PlatformUtil.getOs())) {
			shell.addShellListener(new ShellAdapter() {
				@Override
				public void shellClosed(ShellEvent e) {
					if (!parentBrowser.isDisposed()) {
						parentBrowser.execute("window.Firebug.chrome.close();");
					}
				}
			});
		} else {
//			fireBugBrowser.addCloseWindowListener(new CloseWindowListener() {
//				public void close(WindowEvent event) {
//					IBrowser browser = (IBrowser)event.widget;
//					Shell shell = browser.getShell();
//					shell.close();
//				}
//			});
		}
	}
}
