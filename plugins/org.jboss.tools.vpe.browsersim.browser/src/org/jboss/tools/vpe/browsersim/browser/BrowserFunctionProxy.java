package org.jboss.tools.vpe.browsersim.browser;

import netscape.javascript.JSException;
import netscape.javascript.JSObject;

public class BrowserFunctionProxy {
	private IBrowserFunction browserFunction;
	
    public BrowserFunctionProxy(IBrowserFunction browserFunction) {
		this.browserFunction = browserFunction;
	}

	public Object func(JSObject arguments) {
		int length = (Integer) arguments.getMember("length");
		System.out.println(length);
		Object[] argumentsArray = new Object[length];
		for (int i = 0; i < length; i++) {
			Object argument = arguments.getSlot(i);
			if (argument instanceof Integer) {
				// JavaFX WebView may pass Integer for numbers, but SWT Browser
				// always passes Double - make this to be uniform
				argumentsArray[i] = ((Integer) argument).doubleValue();
			} else if ("undefined".equals(argument)) {
				// JavaFX WebView passes "undefined" for JS undefined, but SWT Browser passes null
				argumentsArray[i] = null;
			} else {
				argumentsArray[i] = arguments.getSlot(i);
			}
		}
		
		try {
			return browserFunction.function(argumentsArray);
		} catch (Exception e) {
			return new JSException(e.getMessage());
		}
    }
}
