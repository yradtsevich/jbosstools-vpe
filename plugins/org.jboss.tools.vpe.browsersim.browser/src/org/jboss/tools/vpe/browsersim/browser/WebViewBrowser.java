package org.jboss.tools.vpe.browsersim.browser;

import java.awt.event.TextEvent;
import java.util.ArrayList;
import java.util.List;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker.State;
import javafx.embed.swt.FXCanvas;
import javafx.scene.Scene;
import javafx.scene.web.WebView;
import javafx.util.Callback;
import netscape.javascript.JSException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.browser.StatusTextListener;
import org.eclipse.swt.browser.TitleEvent;
import org.eclipse.swt.browser.TitleListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;

import com.sun.javafx.scene.web.Debugger;

public class WebViewBrowser extends FXCanvas implements IBrowser {
	private WebView webView;
	private List<LocationListener> locationListeners = new ArrayList<LocationListener>();
	private List<TitleListener> titleListeners = new ArrayList<TitleListener>();
	private List<StatusTextListener> statusTextListeners = new ArrayList<StatusTextListener>();

	public WebViewBrowser(Composite parent) {
		super(parent, SWT.NONE);
		webView = new WebView();
		this.setScene(new Scene(webView));
		
		Debugger debugger = webView.getEngine().impl_getDebugger();
		debugger.setMessageCallback(new Callback<String, Void>() {
			@Override
			public Void call(String message) {
				System.out.println(message);
				return null;
			}
		});
		debugger.setEnabled(true);
		debugger.sendMessage("{\"id\" : 1232, \"method\" : \"Network.enable\"}");
		
		webView.getEngine().getLoadWorker().stateProperty().addListener(new ChangeListener<State>() {
			@Override
			public void changed(ObservableValue ov, State oldState, State newState) {
				if (newState == State.SUCCEEDED || newState == State.RUNNING) {
					LocationEvent event = new LocationEvent(WebViewBrowser.this);
					event.widget = WebViewBrowser.this;
					event.location = webView.getEngine().getLocation();
					event.top = true; // XXX
					for (LocationListener locationListener: locationListeners) {
						if (newState == State.SUCCEEDED) {
							locationListener.changed(event);
						} else if (newState == State.RUNNING) {
							locationListener.changing(event); //XXX: not sure this is correct
						}
					}
				}
			}
		});
		
		webView.getEngine().titleProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue ov, String oldState, String newState) {
				TitleEvent event = new TitleEvent(WebViewBrowser.this);
				event.widget = WebViewBrowser.this;
				event.title = newState;
				for (TitleListener titleListener : titleListeners) {
					titleListener.changed(event);
				}
			}
		});
	}
	
//	@Override
//	public Point computeSize(int wHint, int hHint, boolean changed) {
//        getScene().getWindow().sizeToScene();
//        int width = (int) getScene().getWidth();
//        int height = (int) getScene().getHeight();
//        return new Point(width, height);
//    }

	@Override
	public void addProgressListener(ProgressListener progressListener) {
		// TODO Auto-generated method stub
		webView.get
	}

	@Override
	public void addLocationListener(final LocationListener locationListener) {
		locationListeners.add(locationListener);
	}

	@Override
	public void addStatusTextListener(StatusTextListener statusTextListener) {
		statusTextListeners.add(statusTextListener);
	}

	@Override
	public boolean execute(String string) {
		try {
			webView.getEngine().executeScript(string);
			return true;
		} catch (JSException e) {
			return false;
		}
	}
	
	@Override
	public Object evaluate(String script) {
		return webView.getEngine().executeScript("(function(){" + script + "}())");
	}

	@Override
	public boolean forward() {
		boolean success = isForwardEnabled();
		webView.getEngine().getHistory().go(1);
		return success;
	}

	@Override
	public boolean back() {
		boolean success = isBackEnabled();
		webView.getEngine().getHistory().go(-1);
		return success;
	}

	@Override
	public void addTitleListener(TitleListener titleListener) {
		titleListeners.add(titleListener);
	}

	@Override
	public String getText() {
		String doctypeScript = 
			"var node = document.doctype;" +
			"var doctypeText = \"<!DOCTYPE \"" +
			         "+ node.name" + 
			         "+ (node.publicId ? ' PUBLIC \"' + node.publicId + '\"' : '')" +
			         "+ (!node.publicId && node.systemId ? ' SYSTEM' : '')" + 
			         "+ (node.systemId ? ' \"' + node.systemId + '\"' : '')" +
			         "+ '>';" +
			 "return doctypeText";
		System.out.println(doctypeScript);
		String doctypeText = (String) evaluate(doctypeScript);
		String innerHtml = (String) evaluate("return window.document.documentElement.innerHTML");
		return doctypeText + '\n' + innerHtml;
	}

	@Override
	public String getUrl() {
		return webView.getEngine().getLocation();
	}

	@Override
	public boolean isBackEnabled() {
		return webView.getEngine().getHistory().getCurrentIndex() > 0;
	}

	@Override
	public boolean isForwardEnabled() {
		return webView.getEngine().getHistory().getCurrentIndex() + 1 < webView.getEngine().getHistory().getEntries().size();  
	}

	@Override
	public void refresh() {
		webView.getEngine().reload();
	}

	@Override
	public void removeLocationListener(LocationListener locationListener) {
		locationListeners.remove(locationListener);
	}

	@Override
	public void removeProgressListener(ProgressListener progressListener) {
		// TODO Auto-generated method stub
	}

	@Override
	public void setDefaultUserAgent(String userAgent) {
		webView.getEngine().impl_getDebugger().sendMessage("{\"id\" : 123, \"method\" : \"Network.setUserAgentOverride\","
				+ "\"params\" : { "
				+ 		"\"userAgent\" : \""+ userAgent +"\"" //XXX: escape userAgent
				+ "}}");
	}

	@Override
	public boolean setUrl(String location) {
//		webView.getEngine().impl_getDebugger()
//			.sendMessage("{\"id\" : 123, \"method\" : \"Network.setUserAgentOverride\","
//				+ "\"params\" : { "
//				+ 		"\"userAgent\" : \"myuseragent\""
//				+ "}}");
		location = location.trim();
		if (!location.startsWith("http://") && !location.startsWith("https://")) {
			
		}
		webView.getEngine().load(location);
		
//		webView.getEngine().impl_getDebugger()
//		.sendMessage("{\"id\" : 123, \"method\" : \"Network.setUserAgentOverride\","
//			+ "\"params\" : { "
//			+ 		"\"userAgent\" : \"myuseragent\""
//			+ "}}");
		return true; //XXX
	}

	@Override
	public void stop() {
		execute("window.stop()");
	}

	@Override
	public IDisposable registerBrowserFunction(String name,
			IBrowserFunction iBrowserFunction) {
		return null;
	}

	public Debugger getDebugger() {
		return webView.getEngine().impl_getDebugger();
	}
}
