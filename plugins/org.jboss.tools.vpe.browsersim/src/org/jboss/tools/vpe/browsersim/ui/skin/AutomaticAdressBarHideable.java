package org.jboss.tools.vpe.browsersim.ui.skin;

import org.jboss.tools.vpe.browsersim.ui.skin.ios.AppleIPadMiniResizableSkin;
import org.jboss.tools.vpe.browsersim.ui.skin.ios.AppleIPadResizableSkin;

public interface AutomaticAdressBarHideable {
	
	/**
	 * Indicates whether address bar should be hidden automatically.  
	 * {@link NativeSkin}, {@link AppleIPadResizableSkin} and {@link AppleIPadMiniResizableSkin} 
	 * should not hide the adressBar on scroll, whereas other skins should. 
	 * 
	 * @return <tt>true</tt> if address bar should be hidden automatically
	 */
	boolean automaticallyHideAddressBar(); // JBIDE-15850
}
