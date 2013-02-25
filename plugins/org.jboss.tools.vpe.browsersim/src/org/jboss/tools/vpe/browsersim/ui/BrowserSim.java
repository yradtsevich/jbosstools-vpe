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
package org.jboss.tools.vpe.browsersim.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.browser.OpenWindowListener;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.browser.StatusTextEvent;
import org.eclipse.swt.browser.StatusTextListener;
import org.eclipse.swt.browser.TitleEvent;
import org.eclipse.swt.browser.TitleListener;
import org.eclipse.swt.browser.WindowEvent;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.ShellListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.jboss.tools.vpe.browsersim.browser.BrowserSimBrowser;
import org.jboss.tools.vpe.browsersim.browser.PlatformUtil;
import org.jboss.tools.vpe.browsersim.browser.WebKitBrowserFactory;
import org.jboss.tools.vpe.browsersim.model.Device;
import org.jboss.tools.vpe.browsersim.model.DeviceOrientation;
import org.jboss.tools.vpe.browsersim.model.DevicesList;
import org.jboss.tools.vpe.browsersim.model.DevicesListHolder;
import org.jboss.tools.vpe.browsersim.model.DevicesListStorage;
import org.jboss.tools.vpe.browsersim.ui.debug.firebug.FireBugLiteLoader;
import org.jboss.tools.vpe.browsersim.ui.menu.BrowserSimMenuCreator;
import org.jboss.tools.vpe.browsersim.ui.skin.BrowserSimSkin;
import org.jboss.tools.vpe.browsersim.ui.skin.ResizableSkinSizeAdvisor;
import org.jboss.tools.vpe.browsersim.ui.skin.ResizableSkinSizeAdvisorImpl;
import org.jboss.tools.vpe.browsersim.util.BrowserSimUtil;
import org.jboss.tools.vpe.browsersim.util.ImageList;

/**
 * @author Yahor Radtsevich (yradtsevich)
 * @author Konstantin Marmalyukov (kmarmaliykov)
 */
public class BrowserSim {
	public static final String BROWSERSIM_PLUGIN_ID = "org.jboss.tools.vpe.browsersim"; //$NON-NLS-1$
	private static final String[] BROWSERSIM_ICONS = {"icons/browsersim_16px.png", "icons/browsersim_32px.png", "icons/browsersim_64px.png", "icons/browsersim_128px.png", "icons/browsersim_256px.png", }; //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$//$NON-NLS-5$

	public static boolean isStandalone;
	private static List<BrowserSim> instances;

	private String homeUrl;
	public DevicesListHolder devicesListHolder;
	private DeviceOrientation deviceOrientation;
	public BrowserSimSkin skin;
	private ControlHandler controlHandler;
	private ImageList imageList;
	private Image[] icons;
	private Point currentLocation;
	private ProgressListener progressListener;

	static {
		instances = new ArrayList<BrowserSim>();
	}

	public BrowserSim(String homeUrl) {
		this.homeUrl = homeUrl;
	}

	public void open() {
		DevicesList devicesList = DevicesListStorage.loadUserDefinedDevicesList();
		if (devicesList == null) {
			devicesList = DevicesListStorage.loadDefaultDevicesList();
		}

		open(devicesList, null);
	}

	public void open(DevicesList devicesList, String url) {
		if (url == null) {
			url = homeUrl;
		}
		Device defaultDevice = devicesList.getDevices().get(devicesList.getSelectedDeviceIndex());

		initDevicesListHolder();
		devicesListHolder.setDevicesList(devicesList);

		initSkin(BrowserSimUtil.getSkinClass(defaultDevice, devicesList.getUseSkins()), devicesList.getLocation());
		devicesListHolder.notifyObservers();
		controlHandler.goToAddress(url);
		
		skin.getShell().open();
	}

	private void initSkin(Class<? extends BrowserSimSkin> skinClass, Point location) {
		try {
			skin = skinClass.newInstance();//new AppleIPhone3Skin();//new NativeSkin();
		} catch (InstantiationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IllegalAccessException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		skin.setBrowserFactory(new WebKitBrowserFactory());
		
		Display display = Display.getDefault();
		
		try {
			skin.createControls(display, location);
			currentLocation = location;
		} catch (SWTError e) {
			e.printStackTrace();
			ExceptionNotifier.showWebKitLoadError(new Shell(display), e);
			display.dispose();
			return;
		}
		
		final Shell shell = skin.getShell();
		resizableSkinSizeAdvisor = new ResizableSkinSizeAdvisorImpl(devicesListHolder.getDevicesList(), shell);
		shell.addControlListener(new ControlAdapter() {
			@Override
			public void controlMoved(ControlEvent e) {
				currentLocation = shell.getLocation();
				super.controlMoved(e);
			}
		});
		shell.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				if (devicesListHolder != null) {
					DevicesListStorage.saveUserDefinedDevicesList(devicesListHolder.getDevicesList(), currentLocation);
				}
			}
		});

		final BrowserSimMenuCreator menuCreator = new BrowserSimMenuCreator(skin, devicesListHolder, controlHandler, homeUrl);
		
		shell.addShellListener(new ShellListener() {
			@Override
			public void shellIconified(ShellEvent e) {
			}
			
			@Override
			public void shellDeiconified(ShellEvent e) {
			}
			
			@Override
			public void shellDeactivated(ShellEvent e) {
			}
			
			@Override
			public void shellClosed(ShellEvent e) {
			}
			
			@Override
			public void shellActivated(ShellEvent e) {
				//adding menu on activation to make it working properly on every sync window
				menuCreator.addMenuBar();
			}
		});
		menuCreator.addMenuBar();
		setShellAttibutes();
		
		final BrowserSimBrowser browser = getBrowser();
		controlHandler = new ControlHandlerImpl(browser);
		skin.setControlHandler(controlHandler);
		
		Menu contextMenu = new Menu(shell);
		skin.setContextMenu(contextMenu);
		menuCreator.createMenuItemsForContextMenu(contextMenu);

		progressListener = new ProgressListener() {
			public void changed(ProgressEvent event) {
				int ratio;
				if (event.current == event.total || event.total == 0) {
					ratio = -1;
				} else {
					ratio = event.current * 100 / event.total;
				}
				skin.progressChanged(ratio);
			}

			public void completed(ProgressEvent event) {
				skin.progressChanged(-1);
			}
		};
		browser.addProgressListener(progressListener);

		browser.addStatusTextListener(new StatusTextListener() {
			public void changed(StatusTextEvent event) {
				skin.statusTextChanged(event.text);
			}
		});
		browser.addLocationListener(new LocationListener() {
			public void changed(LocationEvent event) {
				if (event.top) {
					BrowserSimBrowser browser = (BrowserSimBrowser) event.widget;
					skin.locationChanged(event.location, browser.isBackEnabled(), browser.isForwardEnabled());
				}
			}

			public void changing(LocationEvent event) {
			}
		});

		browser.addLocationListener(new LocationAdapter() {
			public void changed(LocationEvent event) {
				initOrientation(deviceOrientation.getOrientationAngle());
			}
		});

		//JBIDE-12191 - custom scrollbars work satisfactorily on windows only
		if (PlatformUtil.OS_WIN32.equals(PlatformUtil.getOs())) {
			browser.addLocationListener(new LocationAdapter() {
				@Override
				public void changed(LocationEvent event) {
					Browser browser = (Browser) event.widget;
					setCustomScrollbarStyles(browser);
				}

				@SuppressWarnings("nls")
				private void setCustomScrollbarStyles(Browser browser) {
				
					browser.execute(
						"if (window._browserSim_customScrollBarStylesSetter === undefined) {"
							+"window._browserSim_customScrollBarStylesSetter = function () {"
							+	"document.removeEventListener('DOMSubtreeModified', window._browserSim_customScrollBarStylesSetter, false);"
							+	"var head = document.head;"
							+	"var style = document.createElement('style');"
							+	"style.type = 'text/css';"
							+	"style.id='browserSimStyles';"
							+	"head.appendChild(style);"
							+	"style.innerText='"
							// The following two rules fix a problem with showing scrollbars in Google Mail and similar,
							// but autohiding of navigation bar stops to work with it. That is why they are commented.
							//+	"html {"
							//+		"overflow: hidden;"
							//+	"}"
							//+	"body {"
							//+		"position: absolute;"
							//+		"top: 0px;"
							//+		"left: 0px;"
							//+		"bottom: 0px;"
							//+		"right: 0px;"
							//+		"margin: 0px;"
							//+		"overflow-y: auto;"
							//+		"overflow-x: auto;"
							//+	"}"
							+		"::-webkit-scrollbar {"
							+			"width: 5px;"
							+			"height: 5px;"
							+		"}"
							+		"::-webkit-scrollbar-thumb {"
							+			"background: rgba(0,0,0,0.4); "
							+		"}"
							+		"::-webkit-scrollbar-corner, ::-webkit-scrollbar-thumb:window-inactive {"
							+			"background: rgba(0,0,0,0.0);"
							+		"};"
							+	"';"
							+"};"
							+ "document.addEventListener('DOMSubtreeModified', window._browserSim_customScrollBarStylesSetter, false);"
						+ "}"
					);
				}
			});
		};

		browser.addOpenWindowListener(new OpenWindowListener() {
			public void open(WindowEvent event) {
				if (FireBugLiteLoader.isFireBugPopUp(event)) {
					FireBugLiteLoader.processFireBugPopUp(event);
				} else {
					event.browser = browser;
				}
			}
		});

		browser.addLocationListener(new LocationListener() {
			private BrowserFunction scrollListener = null;

			@SuppressWarnings("nls")
			public void changed(LocationEvent event) {
				if (scrollListener != null) {
					scrollListener.dispose();
				}
				scrollListener = new BrowserFunction(((Browser)event.widget), "_browserSim_scrollListener") {
					public Object function(Object[] arguments) {
						double pageYOffset = (Double) arguments[0];
						if (pageYOffset > 0.0) {
							skin.getShell().getDisplay().asyncExec(new Runnable() {
								public void run() {
									if (skin != null && skin.getShell() != null && !skin.getShell().isDisposed()) {
										skin.setAddressBarVisible(false);
									}
								}
							});
						}
						return null;
					}
				};

				Browser browser = (Browser)event.widget;
				browser.execute(
								"(function() {" +
									"var scrollListener = function(e){" +
										"window._browserSim_scrollListener(window.pageYOffset)" +
									"};" +
									"window.addEventListener('scroll', scrollListener);" +
									"window.addEventListener('beforeunload', function(e){" +
										"window.removeEventListener('scroll', scrollListener);" +
										"delete window._browserSim_scrollListener;" +
									"})" +
								"})();");

			}

			public void changing(LocationEvent event) {
				skin.setAddressBarVisible(true);
			}
		});

		browser.addLocationListener(new LocationAdapter() {
			@Override
			public void changed(LocationEvent event) {
				if (getBrowser().equals(Display.getDefault().getFocusControl()) && event.top) {
					for (BrowserSim bs : instances) {
						if (bs.skin != skin) {
							bs.skin.getBrowser().setUrl(event.location);
						}
					}
				}
			}
		});

		browser.addTitleListener(new TitleListener() {
			@Override
			public void changed(TitleEvent event) {
				skin.pageTitleChanged(event.title);
			}
		});

		instances.add(BrowserSim.this);
	}
	
	private void initImages() {
		imageList = new ImageList(skin.getShell());
		this.icons = new Image[BROWSERSIM_ICONS.length];
		for (int i = 0; i < BROWSERSIM_ICONS.length; i++) {
			icons[i] = imageList.getImage(BROWSERSIM_ICONS[i]);
		}
	}
	
	private void setShellAttibutes() {
		Shell shell = skin.getShell();
		if (shell != null) {
			initImages();
			shell.setImages(icons);
			shell.setText(Messages.BrowserSim_BROWSER_SIM);
		}
	}

	public void initDevicesListHolder() {
		devicesListHolder = new DevicesListHolder();
		devicesListHolder.addObserver(new Observer() {
			public void update(Observable o, Object arg) {
				DevicesListHolder devicesManager = (DevicesListHolder) o;
				DevicesList devicesList = devicesManager.getDevicesList();
				if (devicesList.getSelectedDeviceIndex() < devicesList.getDevices().size()) {
					setSelectedDevice(devicesList);
				}
				devicesList.addObserver(new Observer() {
					public void update(Observable o, Object arg) {
						setSelectedDevice((DevicesList)o);
					}
				});
			}
		});
	}

	private void setSelectedDevice(DevicesList devicesList) {
		final Device device = devicesList.getDevices().get(devicesList.getSelectedDeviceIndex());
		Class<? extends BrowserSimSkin> newSkinClass = BrowserSimUtil.getSkinClass(device, devicesList.getUseSkins());
		String oldSkinUrl = null;
		if (newSkinClass != skin.getClass()) {
			oldSkinUrl = getBrowser().getUrl();
			Point currentLocation = skin.getShell().getLocation();
			getBrowser().removeProgressListener(progressListener);
			getBrowser().getShell().dispose();//XXX
			initSkin(newSkinClass, currentLocation);
		}

		deviceOrientation = new DeviceOrientation(device.getWidth() < device.getHeight()
								? DeviceOrientation.PORTRAIT
								: DeviceOrientation.LANDSCAPE);
		Rectangle clientArea = BrowserSimUtil.getMonitorClientArea(skin.getShell().getMonitor());
		Point size = BrowserSimUtil.getSizeInDesktopPixels(device);
		skin.setOrientationAndSize(deviceOrientation.getOrientationAngle(), size, resizableSkinSizeAdvisor);
		BrowserSimUtil.fixShellLocation(skin.getShell(), clientArea);
		deviceOrientation.addObserver(new Observer() {
			public void update(Observable o, Object arg) {
				int orientationAngle = ((DeviceOrientation) o).getOrientationAngle();

				Point size = BrowserSimUtil.getSizeInDesktopPixels(device);
				int minSize = Math.min(size.x, size.y);
				int maxSize = Math.max(size.x, size.y);
				Point browserSize;
				if (orientationAngle == DeviceOrientation.LANDSCAPE
						|| orientationAngle == DeviceOrientation.LANDSCAPE_INVERTED) {
					browserSize = new Point(maxSize, minSize);
				} else {
					browserSize = new Point(minSize, maxSize);
				}

				fireOrientationChangeEvent(orientationAngle, browserSize);
			}
		});

		getBrowser().setDefaultUserAgent(device.getUserAgent());

		if (oldSkinUrl != null) {
			getBrowser().setUrl(oldSkinUrl); // skin (and browser instance) is changed
		} else {
			getBrowser().refresh(); // only user agent and size of the browser is changed
		}

		skin.getShell().open();
	}
	
	public static Class<? extends BrowserSimSkin> getSkinClass(Device device, boolean useSkins) {
		return SkinMap.getInstance().getSkinClass(useSkins ? device.getSkinId() : null);
	}

	@SuppressWarnings("nls")
	private void initOrientation(int orientation) {
		getBrowser().execute("window.onorientationchange = null;" + "window.orientation = " + orientation + ";");
	}

	private void rotateDevice(boolean counterclockwise) {
		deviceOrientation.turnDevice(counterclockwise);
		deviceOrientation.notifyObservers();
	}
	
	@SuppressWarnings("nls")
	private void fireOrientationChangeEvent(int orientation, Point browserSize) {
		Rectangle clientArea = BrowserSimUtil.getMonitorClientArea(skin.getShell().getMonitor());
		skin.setOrientationAndSize(orientation, browserSize, resizableSkinSizeAdvisor);
		BrowserSimUtil.fixShellLocation(skin.getShell(), clientArea);
		getBrowser().execute("window.orientation = " + orientation + ";"
				+ "(function(){"
				+ 		"var event = document.createEvent('Event');"
				+ 		"event.initEvent('orientationchange', false, false);" // http://jsbin.com/azefow/6   https://developer.mozilla.org/en/DOM/document.createEvent
				+ 		"window.dispatchEvent(event);"
				+ 		"if (typeof window.onorientationchange === 'function') {"
				+			"window.onorientationchange(event);"
				+ 		"}"
				+	"})();"
		);
	}
	
	public BrowserSimBrowser getBrowser() {
		return skin != null ? skin.getBrowser() : null;
	}
	
	public class ControlHandlerImpl implements ControlHandler {
		private Browser browser;

		public ControlHandlerImpl(Browser browser) {
			this.browser = browser;
		}

		@Override
		public void goBack() {
			browser.back();
			browser.setFocus();
		}

		@Override
		public void goForward() {
			browser.forward();
			browser.setFocus();
		}

		@Override
		public void goHome() {
			browser.setUrl(homeUrl);
			browser.setFocus();
		}

		@Override
		public void goToAddress(String address) {
			browser.setUrl(address);
			browser.setFocus();
		}

		@Override
		public void showContextMenu() {
			// TODO Auto-generated method stub//XXX
		}

		@Override
		public void rotate(boolean counterclockwise) {
			rotateDevice(counterclockwise);
		}

		@Override
		public void stop() {
			browser.stop();
			browser.setFocus();
		}

		@Override
		public void refresh() {
			browser.refresh();
			browser.setFocus();
		}
	}
}
