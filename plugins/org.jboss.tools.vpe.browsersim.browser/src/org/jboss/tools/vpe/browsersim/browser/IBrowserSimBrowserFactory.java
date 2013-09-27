package org.jboss.tools.vpe.browsersim.browser;

import org.eclipse.swt.widgets.Composite;

public interface IBrowserSimBrowserFactory {
	IBrowser createBrowser(Composite parent, int style);
}