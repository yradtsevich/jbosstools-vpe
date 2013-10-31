package org.jboss.tools.vpe.browsersim.browser;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swt.FXCanvas;
import javafx.event.EventHandler;
import javafx.scene.web.PopupFeatures;
import javafx.scene.web.PromptData;
import javafx.scene.web.WebEvent;
import javafx.util.Callback;
import netscape.javascript.JSException;
import netscape.javascript.JSObject;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.browser.StatusTextListener;
import org.eclipse.swt.browser.TitleEvent;
import org.eclipse.swt.browser.TitleListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.MessageBox;

import com.sun.javafx.scene.web.Debugger;

public class WebViewBrowser extends FXCanvas implements IBrowser {
	private IWebView webView;
	private List<LocationListener> locationListeners = new ArrayList<LocationListener>();
	private List<TitleListener> titleListeners = new ArrayList<TitleListener>();
	private List<StatusTextListener> statusTextListeners = new ArrayList<StatusTextListener>();
	private List<ExtendedOpenWindowListener> openWindowListeners = new ArrayList<ExtendedOpenWindowListener>();
	private List<ProgressListener> progressListeners = new ArrayList<ProgressListener>();
	private List<ExtendedVisibilityWindowListener> visibilityWindowListeners = new ArrayList<ExtendedVisibilityWindowListener>();
	
	Map<Class<?>, String> interfaceToOriginalMap = new HashMap<Class<?>, String>();
	{
		interfaceToOriginalMap.put(IWebView.class, "javafx.scene.web.WebView");
		interfaceToOriginalMap.put(IWebEngine.class, "javafx.scene.web.WebEngine");
	}
	
	<T> T createProxy(ClassLoader cl, final Object originalInstance, Class<T> proxyInterface) {
		return (T) Proxy.newProxyInstance(cl, new Class<?>[]{proxyInterface}, new JavaFXProxyHandler(originalInstance, cl));
	}
	
	Object proxifyIfNeeded(ClassLoader cl, Object originalInstance) throws Exception {
		if (originalInstance == null) {
			return null;
		}
		Set<Entry<Class<?>, String>> interfaceToOriginalEntries = interfaceToOriginalMap.entrySet();
		for (Entry<Class<?>, String> interfaceToOriginalEntry : interfaceToOriginalEntries) {
			String originalClassName = interfaceToOriginalEntry.getValue();
			Class<?> originalClass = cl.loadClass(originalClassName);
			if (originalClass.isAssignableFrom(originalInstance.getClass())) {
				Class<?> proxyInterface = interfaceToOriginalEntry.getKey();
				return createProxy(cl, originalInstance, proxyInterface);
			}
		}
		return originalInstance;
	}
	
	Object deproxifyIfNeeded(ClassLoader cl, Object instance) {
		if (instance == null) {
			return null;
		}
		if (!Proxy.isProxyClass(instance.getClass())) {
			return instance;
		}
		
		InvocationHandler invocationHandler = Proxy.getInvocationHandler(instance);
		if (invocationHandler instanceof JavaFXProxyHandler) {
			return ((JavaFXProxyHandler)invocationHandler).getOriginalInstance();
		}
		
		return instance;
	}
	
	class JavaFXProxyHandler implements InvocationHandler {
		private Object originalInstance;
		private ClassLoader cl;
		public JavaFXProxyHandler(Object originalInstance, ClassLoader cl) {
			this.originalInstance = originalInstance;
			this.cl = cl;
		}

		@Override
		public Object invoke(Object proxy, Method proxyMethod, Object[] proxiedArgs)
				throws Throwable {
			if (proxiedArgs == null) {
				proxiedArgs = new Object[0];
			}
			
			Object[] args = new Object[proxiedArgs.length];
			for (int i = 0; i < proxiedArgs.length; i++) {
				args[i]  = deproxifyIfNeeded(cl, proxiedArgs[i]);
			}
			
			Object result = invoke(originalInstance, args, proxyMethod.getName());
			return proxifyIfNeeded(cl, result);
		}
		
		private Object invoke(Object targetObject, Object[] parameters,
		        String methodName) throws Exception {
		    for (Method method : targetObject.getClass().getMethods()) {
		        if (!method.getName().equals(methodName)) {
		            continue;
		        }
		        Class<?>[] parameterTypes = method.getParameterTypes();
		        boolean matches = true;
		        for (int i = 0; i < parameterTypes.length; i++) {
		            if (!parameterTypes[i].isAssignableFrom(parameters[i]
		                    .getClass())) {
		                matches = false;
		                break;
		            }
		        }
		        if (matches) {
		            // obtain a Class[] based on the passed arguments as Object[]
		            return method.invoke(targetObject, parameters);
		        }
		    }
		    
		    throw new Exception();//XXX
		}


		public Object getOriginalInstance() {
			return originalInstance;
		}
	}
	
	<T> T create(Class<T> type, Object... constructorArgs) {
		try {
			ClassLoader cl = getClass().getClassLoader();
			final Class<?> originalClass = cl.loadClass(interfaceToOriginalMap.get(type));
			List<Class> constructorTypes = new ArrayList<Class>();
			for (Object constructorArg : constructorArgs) {
				constructorTypes.add(cl.loadClass( interfaceToOriginalMap.get(constructorArg.getClass()) ));
			}
			Object originalInstance = originalClass.getConstructor(
					constructorTypes.toArray(new Class[constructorTypes.size()])).newInstance(constructorArgs);
			return createProxy(cl, originalInstance, type);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public WebViewBrowser(Composite parent) {
		super(parent, SWT.NONE);
		webView = create(IWebView.class);

//		this.setScene(new Scene(webView));
		
		Debugger debugger = getEngine().impl_getDebugger();

		debugger.setEnabled(true);
		debugger.sendMessage("{\"id\" : -1, \"method\" : \"Network.enable\"}");
		getEngine().getLoadWorker().progressProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observable,
					Number oldValue, Number newValue) {
				if (oldValue.doubleValue() == 0.0 && newValue.doubleValue() > 0.0) {
					LocationEvent event = new LocationEvent(WebViewBrowser.this);
					event.widget = WebViewBrowser.this;
					event.location = getEngine().getLocation();
					event.top = true; // XXX
					for (LocationListener locationListener: locationListeners) {
						locationListener.changed(event);
					}
				}
			}
		});
		
		getEngine().getLoadWorker().progressProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observable,
				Number oldValue, Number newValue) {
				ProgressEvent progressEvent = new ProgressEvent(WebViewBrowser.this);
//				double maximumValue = webView.getEngine().getLoadWorker().getTotalWork();
				double maximumValue = 1.0;
				
				progressEvent.total = 100;
				if (maximumValue > 0.0 && newValue.doubleValue() >= maximumValue) {
					progressEvent.current = progressEvent.total;
					for (ProgressListener progressListener : progressListeners) {
						progressListener.completed(progressEvent);
					}
				} else {
					if (maximumValue <= 0.0) {
						progressEvent.current = progressEvent.total / 2;
					} else {// maximumValue undefined
						progressEvent.current = (int) (newValue.doubleValue() / maximumValue * progressEvent.total);
					}
					for (ProgressListener progressListener : progressListeners) {
						progressListener.changed(progressEvent);
					}
				}
			}
		});
		
		getEngine().titleProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> ov, String oldState, String newState) {
				TitleEvent event = new TitleEvent(WebViewBrowser.this);
				event.widget = WebViewBrowser.this;
				event.title = newState != null ? newState : "";
				for (TitleListener titleListener : titleListeners) {
					titleListener.changed(event);
				}
			}
		});
		
		getEngine().setOnAlert(new EventHandler<WebEvent<String>>() {
			@Override
			public void handle(WebEvent<String> event) {
				MessageBox messageBox = new MessageBox(getShell());
				messageBox.setMessage(event.getData());
				messageBox.open();
			}
		});
		
		getEngine().setConfirmHandler(new Callback<String, Boolean>() {
			@Override
			public Boolean call(String message) {
				MessageBox messageBox = new MessageBox(getShell());
				messageBox.setMessage(message);
				return messageBox.open() == SWT.OK;
			}
		});
		
		getEngine().setPromptHandler(new Callback<PromptData, String>() {
			
			@Override
			public String call(PromptData param) {
				// TODO Auto-generated method stub
				return null;
			}
		});
		
		getEngine().setCreatePopupHandler(new Callback<PopupFeatures, IWebEngine>() {
			@Override
			public IWebEngine call(PopupFeatures popupFeatures) {// XXX: use popupFeatures
				ExtendedWindowEvent event = new ExtendedWindowEvent(WebViewBrowser.this);
				for (ExtendedOpenWindowListener openWindowListener : openWindowListeners) {
					openWindowListener.open(event);
				}
				
				WebViewBrowser popupWebViewBrowser = null;
				if (event.browser instanceof WebViewBrowser) {
					popupWebViewBrowser = (WebViewBrowser) event.browser;
				}
				if (popupWebViewBrowser != null && !popupWebViewBrowser.isDisposed()) {
					for (ExtendedVisibilityWindowListener visibilityWindowListener : visibilityWindowListeners) {
						visibilityWindowListener.show(event);
					}
					return popupWebViewBrowser.getEngine();
				}
				return null;
			}
		});
	}

	@Override
	public void addLocationListener(final LocationListener locationListener) {
		locationListeners.add(locationListener);
	}

	@Override
	public void removeLocationListener(LocationListener locationListener) {
		locationListeners.remove(locationListener);
	}

	@Override
	public void addOpenWindowListener(ExtendedOpenWindowListener openWindowListener) {
		openWindowListeners.add(openWindowListener);
	}

	@Override
	public void removeOpenWindowListener(ExtendedOpenWindowListener listener) {
		openWindowListeners.remove(listener);
	}

	@Override
	public void addProgressListener(ProgressListener progressListener) {
		progressListeners.add(progressListener);
	}
	
	@Override
	public void removeProgressListener(ProgressListener progressListener) {
		progressListeners.remove(progressListener);
	}

	@Override
	public void addStatusTextListener(StatusTextListener statusTextListener) {
		statusTextListeners.add(statusTextListener);
	}

	@Override
	public void removeStatusTextListener(StatusTextListener statusTextListener) {
		statusTextListeners.remove(statusTextListeners);
	}

	@Override
	public void addTitleListener(TitleListener titleListener) {
		titleListeners.add(titleListener);
	}
	
	@Override
	public void removeTitleListener(TitleListener titleListener) {
		titleListeners.remove(titleListener);
	}
	
	@Override
	public void addVisibilityWindowListener(ExtendedVisibilityWindowListener listener) {
		visibilityWindowListeners.add(listener);		
	}

	@Override
	public void removeVisibilityWindowListener(ExtendedVisibilityWindowListener listener) {
		visibilityWindowListeners.remove(listener);
	}

	@Override
	public boolean back() {
		boolean success = isBackEnabled();
		if (success) {
			getEngine().getHistory().go(-1);
		}
		return success;
	}

	@Override
	public boolean forward() {
		boolean success = isForwardEnabled();
		if (success) {
			getEngine().getHistory().go(1);
		}
		return success;
	}

	@Override
	public void refresh() {
		getEngine().reload();
	}

	@Override
	public void stop() {
		execute("window.stop()");
	}

	@Override
	public Object evaluate(String script) {
		return getEngine().executeScript("(function(){" + script + "}())");
	}

	@Override
	public boolean execute(String string) {
		try {
			getEngine().executeScript(string);
			return true;
		} catch (JSException e) {
			return false;
		}
	}

	@Override
	public IDisposable registerBrowserFunction(final String name, final IBrowserFunction iBrowserFunction) {
		JSObject window = (JSObject) evaluate("return window");
		
		final String id = "__webViewProxy_" + name;
		window.setMember(id, new BrowserFunctionProxy(iBrowserFunction));
		evaluate("window['" + name + "'] = function(){return window['" + id + "'].func(arguments)}");
		
		return new IDisposable() {
			@Override
			public void dispose() {
				evaluate("delete window['" + name + "']; delete window['" + id + "']");
			}
			
			@Override
			public boolean isDisposed() {
				return (Boolean) evaluate("return window['" + name + "'] === undefined && window['" + id + "'] === undefined");
			}
		}; 
	}

	public Debugger getDebugger() {
		return getEngine().impl_getDebugger();
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
		String doctypeText = (String) evaluate(doctypeScript);
		String innerHtml = (String) evaluate("return window.document.documentElement.innerHTML");
		return doctypeText + '\n' + innerHtml;
	}

	@Override
	public String getUrl() {
		return getEngine().getLocation();
	}

	@Override
	public boolean isBackEnabled() {
		return getEngine().getHistory().getCurrentIndex() > 0;
	}

	@Override
	public boolean isForwardEnabled() {
		return getEngine().getHistory().getCurrentIndex() + 1 < getEngine().getHistory().getEntries().size();  
	}

	@Override
	public void setDefaultUserAgent(String userAgent) {
		if (userAgent == null) {
			userAgent = ""; // empty string means 'default value' for DevTools
		}
		String escapedUserAgent = userAgent
				.replace("\\", "\\\\")
				.replace("\"", "\\\"");
		getDebugger().sendMessage("{\"id\" : -1, \"method\" : \"Network.setUserAgentOverride\","
				+ "\"params\" : { "
				+ 		"\"userAgent\" : \""+ escapedUserAgent +"\""
				+ "}}");
	}

	@Override
	public boolean setUrl(String location) {
		location = location.trim();
		if (!location.contains(":")) {
			location = "http://" + location;
		}
		getEngine().load(location);

		return true; //XXX
	}
	
	protected IWebEngine getEngine() {
		return webView.getEngine();
	}
}
