package snippet;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.web.PopupFeatures;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Callback;

public class Snippet extends Application {
	 
	  /**
	   * @param args the command line arguments
	   */
	  public static void main(String[] args) {
	      launch(args);
	  }
	 
	  @Override
	  public void start(Stage primaryStage) {
		WebView wv = new WebView();
		wv.getEngine().setCreatePopupHandler(
				new Callback<PopupFeatures, WebEngine>() {
					@Override
					public WebEngine call(PopupFeatures p) {
						Stage stage = new Stage(StageStyle.UTILITY);
						WebView wv2 = new WebView();
						stage.setScene(new Scene(wv2));
						stage.show();
						return wv2.getEngine();
					}
				});

		StackPane root = new StackPane();
		root.getChildren().add(wv);

		Scene scene = new Scene(root, 300, 250);

		
		primaryStage.setTitle("Hello World!");
		primaryStage.setScene(scene);
		primaryStage.show();
		wv.getEngine().load("http://www.i-am-bored.com/pop_up_blocker_test.html");
	}
}
