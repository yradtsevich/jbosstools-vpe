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
package org.jboss.tools.vpe.browsersim.scripting;

import org.jboss.tools.vpe.browsersim.browser.IBrowser;
import org.jboss.tools.vpe.browsersim.browser.IBrowserFunction;
import org.jboss.tools.vpe.browsersim.browser.IDisposable;
import org.jboss.tools.vpe.browsersim.util.BrowserSimResourcesUtil;

/**
 * @author Yahor Radtsevich (yradtsevich)
 */
public class WebSqlLoader {
	
	
	private static IDisposable loadPureJsWebSqlFunction;

	public static void initWebSql(final IBrowser browser) {
		if (loadPureJsWebSqlFunction != null && !loadPureJsWebSqlFunction.isDisposed()) {
			loadPureJsWebSqlFunction.dispose();
		}
		
		browser.registerBrowserFunction("loadPureJsWebSql", new IBrowserFunction() {
			@Override
			public Object function(Object[] arguments) {
				String purejswebsql = BrowserSimResourcesUtil.getResourceAsString("javascript/purejswebsql.js");
				browser.execute(purejswebsql);

				String sql = BrowserSimResourcesUtil.getResourceAsString("javascript/sql.js");
				browser.execute(sql);
				return null;
			}
		});
		
		browser.execute(
			"(function() {" +
				"if (!window.purejsOpenDatabase) {" +
					"var origOpenDatabase = window.openDatabase;" +
					"window.openDatabase = function() {" +
						"try {" +
							"var result = origOpenDatabase.apply(this, arguments);" +
							"window.openDatabase = origOpenDatabase;" + // always use origOpenDatabase
							"return result;" +
						"} catch (e) {" +
							"if (e.code === 18) {" +
								"loadPureJsWebSql();" +
								"window.openDatabase = purejsOpenDatabase;" + // always use purejsOpenDatabase
								"return window.openDatabase.apply(this, arguments);" +
							"} else {" +
								"throw e;" +
							"}" +
						"}" +
					"};" +
				"}" +
			"})();");
	}
}
