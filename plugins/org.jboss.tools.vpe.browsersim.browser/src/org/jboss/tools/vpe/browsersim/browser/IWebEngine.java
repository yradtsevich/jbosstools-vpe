package org.jboss.tools.vpe.browsersim.browser;

import javafx.beans.property.ReadOnlyStringProperty;
import javafx.concurrent.Worker;
import javafx.event.EventHandler;
import javafx.scene.web.PopupFeatures;
import javafx.scene.web.PromptData;
import javafx.scene.web.WebEvent;
import javafx.scene.web.WebHistory;
import javafx.util.Callback;

import com.sun.javafx.scene.web.Debugger;

public interface IWebEngine {
	Debugger impl_getDebugger();
	Worker<Void> getLoadWorker();
	String getLocation();
	ReadOnlyStringProperty titleProperty();
	void setOnAlert(EventHandler<WebEvent<java.lang.String>> handler);
	void setConfirmHandler(Callback<java.lang.String,java.lang.Boolean> handler);
	void setPromptHandler(Callback<PromptData, String> callback);
	void setCreatePopupHandler(Callback<PopupFeatures,IWebEngine> handler);
	WebHistory getHistory();
	Object executeScript(String string);
	void reload();
	void load(String location);
	
}
