package org.jboss.tools.vpe.browsersim.browser;

import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.browser.StatusTextListener;
import org.eclipse.swt.browser.TitleListener;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

public interface IBrowser {
	void addCloseWindowListener(ExtendedCloseWindowListener closeWindowListener);
	void removeCloseWindowListener(ExtendedCloseWindowListener closeWindowListener);
	void addDisposeListener(DisposeListener disposeListener); 
	void removeDisposeListener(DisposeListener disposeListener); 
	void addLocationListener(LocationListener locationListener);
	void removeLocationListener(LocationListener liveReloadLocationAdapter);
	void addOpenWindowListener(ExtendedOpenWindowListener listener);
	void removeOpenWindowListener(ExtendedOpenWindowListener listener);
	void addProgressListener(ProgressListener progressListener);
	void removeProgressListener(ProgressListener progressListener);
	void addStatusTextListener(StatusTextListener statusTextListener);
	void removeStatusTextListener(StatusTextListener statusTextListener);
	void addTitleListener(TitleListener titleListener);
	void removeTitleListener(TitleListener titleListener);
	void addVisibilityWindowListener (ExtendedVisibilityWindowListener listener);
	void removeVisibilityWindowListener (ExtendedVisibilityWindowListener listener);
	
	boolean back();
	boolean forward();
	void refresh();
	void stop();
	
	Object evaluate(String script); 
	boolean execute(String string);
	
	boolean forceFocus();
	IDisposable registerBrowserFunction(String name, IBrowserFunction iBrowserFunction);
	
	Object getLayoutData();
	Shell getShell();
	String getText();
	String getUrl();
	boolean isBackEnabled();
	boolean isDisposed();
	boolean isForwardEnabled();
	
	void setDefaultUserAgent(String userAgent);
	boolean setFocus();
	void setLayoutData(Object layoutData);
	boolean setParent(Composite browserContainer);
	boolean setUrl(String location);
}
