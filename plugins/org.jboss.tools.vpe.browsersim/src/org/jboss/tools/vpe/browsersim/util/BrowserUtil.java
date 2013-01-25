package org.jboss.tools.vpe.browsersim.util;
import java.io.IOException;
import org.eclipse.swt.browser.Browser;

/**
 * @author Yahor Radtsevich (yradtsevich)
 */
public class BrowserUtil {
	public static void executeScriptFromResource(Browser browser, String resourceName) throws IOException {
		String script = ResourcesUtil.getResourceAsString(resourceName);
		browser.execute(script);
	}
}
