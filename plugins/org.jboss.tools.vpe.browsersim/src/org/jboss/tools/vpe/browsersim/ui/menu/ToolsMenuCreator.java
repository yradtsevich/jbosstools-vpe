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
package org.jboss.tools.vpe.browsersim.ui.menu;

import java.util.Map;
import java.util.UUID;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.jboss.tools.vpe.browsersim.browser.IBrowser;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.vpe.browsersim.model.Device;
import org.jboss.tools.vpe.browsersim.model.preferences.BrowserSimSpecificPreferences;
import org.jboss.tools.vpe.browsersim.model.preferences.CommonPreferences;
import org.jboss.tools.vpe.browsersim.model.preferences.SpecificPreferences;
import org.jboss.tools.vpe.browsersim.ui.BrowserSim;
import org.jboss.tools.vpe.browsersim.ui.Messages;
import org.jboss.tools.vpe.browsersim.ui.debug.firebug.FireBugLiteLoader;
import org.jboss.tools.vpe.browsersim.ui.skin.BrowserSimSkin;
import org.jboss.tools.vpe.browsersim.util.BrowserSimUtil;

/**
 * @author Yahor Radtsevich (yradtsevich)
 * @author Konstantin Marmalyukov (kmarmaliykov)
 */

public class ToolsMenuCreator {
	
	public static void addDebugItem(Menu menu, BrowserSimSkin skin, String weinreScriptUrl, String weinreClientUrl) {
		MenuItem debug = new MenuItem(menu, SWT.CASCADE);
		debug.setText(Messages.BrowserSim_DEBUG);
		Menu subMenu = new Menu(debug);
		addFireBugLiteItem(subMenu, skin);
//		addWeinreItem(subMenu, skin, weinreScriptUrl, weinreClientUrl);
		debug.setMenu(subMenu);
	}
	
	private static void addFireBugLiteItem(Menu menu, final BrowserSimSkin skin) {
		MenuItem fireBugLite = new MenuItem(menu, SWT.PUSH);
		fireBugLite.setText(Messages.BrowserSim_FIREBUG_LITE);
		fireBugLite.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
//				FireBugLiteLoader.startFireBugOpening(skin.getBrowser());
			}
		});
	}
	
//	private static void addWeinreItem(Menu menu, final BrowserSimSkin skin, final String weinreScriptUrl,
//			final String weinreClientUrl) {
//		
//		MenuItem weinre = new MenuItem(menu, SWT.PUSH);
//		weinre.setText(Messages.BrowserSim_WEINRE);
//		weinre.addSelectionListener(new SelectionAdapter() {
//			public void widgetSelected(SelectionEvent e) {
//				//check if weinre url injected by user only in first page
//				String clientUrl = (String) skin.getBrowser().evaluate("if(window.WeinreServerURL && window.WeinreServerId) {return window.WeinreServerURL + 'client/'} else {return null}");
//				String id = (String) skin.getBrowser().evaluate("if(window.WeinreServerURL && window.WeinreServerId) {return window.WeinreServerId} else {return null}");
//				
//				if (clientUrl == null || id == null) {
//					id = UUID.randomUUID().toString();
//					clientUrl = weinreClientUrl;
//					injectUrl(skin.getBrowser(), weinreScriptUrl, id);
//				}
//
//				createWeinreShell(skin, clientUrl + "#" + id, weinreScriptUrl, id).open();
//			}
//		});
//	}
	
	public static void addScreenshotMenuItem(Menu menu, final BrowserSimSkin skin, final CommonPreferences commonPreferences) {
		MenuItem screenshot = new MenuItem(menu, SWT.CASCADE);
		screenshot.setText(Messages.Screenshots_Screenshot);

		Menu subMenu = ScreenshotMenuCreator.createScreenshotsMenu(menu, Display.getDefault(), skin.getShell(), commonPreferences);
		screenshot.setMenu(subMenu);
	}
	
	public static void addSyncronizedWindowItem(Menu menu, final BrowserSimSkin skin,
			final Map<String, Device> devices, final Boolean useSkins, final Boolean enableLiveReload, final int liveReloadPort, final int orientationAngle, final String homeUrl) {
		MenuItem syncWindow = new MenuItem(menu, SWT.CASCADE);
		syncWindow.setText(Messages.BrowserSim_SYNCHRONIZED_WINDOW);
		Menu subMenu = new Menu(menu);
		syncWindow.setMenu(subMenu);

		for (final Device device : devices.values()) {
			MenuItem deviceMenuItem = new MenuItem(subMenu, SWT.RADIO);
			deviceMenuItem.setText(device.getName());
			deviceMenuItem.setData(device.getId());

			deviceMenuItem.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					MenuItem menuItem = (MenuItem) e.widget;
					if (menuItem.getSelection()) {
						Device selected = devices.get(menuItem.getData());
						SpecificPreferences sp = new BrowserSimSpecificPreferences(selected.getId(), useSkins,
								enableLiveReload, liveReloadPort, orientationAngle, null);

						BrowserSim browserSim1 = new BrowserSim(homeUrl, BrowserSimUtil.getParentShell(skin));
						browserSim1.open(sp, skin.getBrowser().getUrl());
					}
				};
			});
		}
	}

	public static void addLiveReloadItem(Menu menu, final SpecificPreferences specificPreferences) {
		MenuItem liveReloadMenuItem = new MenuItem(menu, SWT.CHECK);
		liveReloadMenuItem.setText(Messages.BrowserSim_ENABLE_LIVE_RELOAD);
		liveReloadMenuItem.setSelection(specificPreferences.isEnableLiveReload());
		liveReloadMenuItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				MenuItem menuItem = (MenuItem) e.widget;
				specificPreferences.setEnableLiveReload(menuItem.getSelection());
				specificPreferences.notifyObservers();
			}
		});
	}
	
	private static void injectUrl(IBrowser browser, String scriptUrl, String ID) {
		browser.execute("var head = document.head;"
				+		"var script = document.createElement('script');"
				+		"head.appendChild(script);"
				+		"script.src='" + scriptUrl + "#" + ID + "'");
	}
	
//	private static Shell createWeinreShell(final BrowserSimSkin skin, String clientUrl, final String scriptUrl, final String id) {
//		final Shell shell = new Shell(BrowserSimUtil.getParentShell(skin), SWT.SHELL_TRIM);
//		shell.setLayout(new FillLayout(SWT.VERTICAL | SWT.HORIZONTAL));
//		shell.setText("Weinre Inspector");
//		
//		Composite browserComposite = createBrowserComposite(shell, clientUrl);
//		final Browser weinreBrowser = createWeinreBrowser(browserComposite);
//		weinreBrowser.setUrl(clientUrl);
//		
//		final LocationAdapter locationAdapter = new LocationAdapter() {
//			@Override
//			public void changed(LocationEvent event) {
//				if (event.top) {
//					Browser browser = (Browser) event.widget;
//					browser.execute(
//						  "window.addEventListener('load', function() {"
//						+	"var head = document.head;"
//						+ 	"var script = document.createElement('script');"
//						+ 	"head.appendChild(script);"
//						+ 	"script.src='" + scriptUrl + "#" + id + "';"
//						+ "});");
//				}
//			}
//		};
//		skin.getBrowser().addLocationListener(locationAdapter);
//		
//		weinreBrowser.addDisposeListener(new DisposeListener() {
//			@Override
//			public void widgetDisposed(DisposeEvent e) {
//				skin.getBrowser().removeLocationListener(locationAdapter);
//			}
//		});
//		skin.getShell().addDisposeListener(new DisposeListener() {
//			@Override
//			public void widgetDisposed(DisposeEvent arg0) {
//				if (!weinreBrowser.isDisposed() && !weinreBrowser.getShell().isDisposed()) {
//					weinreBrowser.getShell().dispose();
//				}
//			}
//		});
//
//		return shell;
//	}
	
	private static Composite createBrowserComposite(final Shell weinreShell, String clientUrl) {
		Menu menuBar = Display.getDefault().getMenuBar();
		if (menuBar == null) {
			menuBar = new Menu(weinreShell, SWT.BAR);
			weinreShell.setMenuBar(menuBar);
		}
		
		MenuItem help = new MenuItem(menuBar, SWT.CASCADE);
		help.setText(Messages.BrowserSim_WEINRE_HELP);
		Menu subMenu = new Menu(menuBar);
		help.setMenu(subMenu);
		
		MenuItem about = new MenuItem(subMenu, SWT.NONE);
		about.setText(Messages.BrowserSim_WEINRE_ABOUT);
		about.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				BrowserSimUtil.showAboutDialog(weinreShell, Messages.BrowserSim_ABOUT_WEINRE_MESSAGE,
						Display.getDefault().getSystemImage(SWT.ICON_INFORMATION));
			}
		});
		
		Composite browserComposite = new Composite(weinreShell, SWT.NONE);
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 1;
		gridLayout.verticalSpacing = 0;
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		browserComposite.setLayout(gridLayout);
		
		Text locationText = new Text(browserComposite, SWT.BORDER | SWT.READ_ONLY);
		GridData data = new GridData();
		data.horizontalAlignment = GridData.FILL;
		data.grabExcessHorizontalSpace = true;
		data.widthHint = 0;
		locationText.setLayoutData(data);
		locationText.setText(clientUrl);
		
		return browserComposite;
	}
	
//	private static Browser createWeinreBrowser(Composite browserComposite) {
//		final Browser browser = new Browser(browserComposite, SWT.WEBKIT);
//		GridData browserData = new GridData();
//		browserData.horizontalAlignment = GridData.FILL;
//		browserData.verticalAlignment = GridData.FILL;
//		browserData.horizontalSpan = 1;
//		browserData.grabExcessHorizontalSpace = true;
//		browserData.grabExcessVerticalSpace = true;
//		browser.setLayoutData(browserData);
//
//		final ProgressBar progressBar = new ProgressBar(browserComposite, SWT.NONE);
//
//		GridData data = new GridData();
//		data.horizontalAlignment = GridData.END;
//		progressBar.setLayoutData(data);
//
//		browser.addProgressListener(new ProgressListener() {
//			public void changed(ProgressEvent event) {
//				int ratio;
//				if (event.current == event.total || event.total == 0) {
//					progressBar.setSelection(0);
//					progressBar.setEnabled(false);
//				} else {
//					ratio = event.current * 100 / event.total;
//					progressBar.setEnabled(true);
//					progressBar.setSelection(ratio);
//				}
//			}
//
//			public void completed(ProgressEvent event) {
//				progressBar.setSelection(0);
//				progressBar.setEnabled(false);
//			}
//		});
//		
//		return browser;
//	}
}
