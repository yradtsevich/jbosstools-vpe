package org.jboss.tools.vpe.browsersim.browser;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.OpenWindowListener;
import org.eclipse.swt.browser.WindowEvent;
import org.eclipse.swt.widgets.Composite;

public abstract class BrowserSimBrowser extends Browser implements IBrowser {
	private Map<ExtendedOpenWindowListener, OpenWindowListener> openWindowListenerMap =
			new HashMap<ExtendedOpenWindowListener, OpenWindowListener>();
	
	public BrowserSimBrowser(Composite parent, int style) {
		super(parent, style);
	}

	public abstract void setDefaultUserAgent(String defaultUserAgent);

	@Override
	protected void checkSubclass() {
	}

	@Override
	public void addOpenWindowListener(final ExtendedOpenWindowListener extendedListener) {
		OpenWindowListener listener = new OpenWindowListener() {
			@Override
			public void open(WindowEvent event) {
				ExtendedWindowEvent extendedEvent = new ExtendedWindowEvent(event.widget);
				extendedEvent.addressBar = event.addressBar; 
				extendedEvent.data = event.data;       
				extendedEvent.display = event.display;    
				extendedEvent.location = event.location;   
				extendedEvent.menuBar = event.menuBar;    
				extendedEvent.required = event.required;   
				extendedEvent.size = event.size;       
				extendedEvent.statusBar = event.statusBar;  
				extendedEvent.time = event.time;       
				extendedEvent.toolBar = event.toolBar; 
				
				extendedListener.open(extendedEvent);
				if (extendedEvent.browser instanceof Browser) {
					event.browser = (Browser) extendedEvent.browser;
				}
			}
		};
		addOpenWindowListener(listener);
		openWindowListenerMap.put(extendedListener, listener);
	}
	
	@Override
	public void removeOpenWindowListener(ExtendedOpenWindowListener extendedListener) {
		OpenWindowListener listener = openWindowListenerMap.remove(extendedListener);
		if (listener != null) {
			removeOpenWindowListener(listener);
		}
	}
}
