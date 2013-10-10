package org.jboss.tools.vpe.browsersim.browser;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.OpenWindowListener;
import org.eclipse.swt.browser.WindowEvent;
import org.eclipse.swt.widgets.Composite;

public abstract class BrowserSimBrowser extends Browser implements org.jboss.tools.vpe.browsersim.browser.IBrowser {
	public BrowserSimBrowser(Composite parent, int style) {
		super(parent, style);
	}

	public abstract void setDefaultUserAgent(String defaultUserAgent);

	@Override
	protected void checkSubclass() {
	}

	@Override
	public void addOpenWindowListener(final ExtendedOpenWindowListener extendedListener) {
		addOpenWindowListener(new OpenWindowListener() {
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
		});		
	}
}
