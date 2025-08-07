import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.scene.control.Button;

public class HelloFX extends Application {

    @Override
    public void start(Stage stage) {
        String javaVersion = System.getProperty("java.version");
        String javafxVersion = System.getProperty("javafx.version");
        Button b = new Button("Quit");
        Label l = new Label("Hello, JavaFX " + javafxVersion 
            + ", running on Java " + javaVersion + ".");
        BorderPane bp = new BorderPane();
        bp.setCenter(l);
        bp.setTop(b);
        b.setOnAction(e -> System.exit(0));
        Scene scene = new Scene(bp, 640, 480);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
