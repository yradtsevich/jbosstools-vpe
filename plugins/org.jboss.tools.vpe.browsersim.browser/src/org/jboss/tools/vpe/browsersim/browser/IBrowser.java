package org.jboss.tools.vpe.browsersim.browser;

import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.browser.StatusTextListener;
import org.eclipse.swt.browser.TitleListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

public interface IBrowser {
	void addProgressListener(ProgressListener progressListener);
	void addLocationListener(LocationListener locationListener);
	void addStatusTextListener(StatusTextListener statusTextListener);
	boolean execute(String string);
	Object evaluate(String script); 
	boolean forward();
	boolean back();
	void setLayoutData(Object layoutData);
	boolean setFocus();
	Shell getShell();
	void addTitleListener(TitleListener titleListener);
	Object getLayoutData();
	String getText();
	String getUrl();
	boolean isBackEnabled();
	boolean isForwardEnabled();
	void refresh();
	void removeLocationListener(LocationListener liveReloadLocationAdapter);
	void removeProgressListener(ProgressListener progressListener);
	void setDefaultUserAgent(String userAgent);
	boolean setParent(Composite browserContainer);
	boolean setUrl(String location);
	void stop();
	IDisposable registerBrowserFunction(String name, IBrowserFunction iBrowserFunction);
	public void addOpenWindowListener(ExtendedOpenWindowListener listener);
	boolean isDisposed();
	boolean forceFocus();
}
