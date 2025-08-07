import javafx.application.Application;
import javafx.scene.*;
import javafx.scene.image.Image;
import javafx.scene.paint.*;
import javafx.scene.shape.Sphere;
import javafx.stage.Stage;

public class EarthSphereApp extends Application {
    @Override
    public void start(Stage stage) {
        Sphere earth = new Sphere(150);
        Image earthImage = new Image("file:earth.jpg");
        PhongMaterial material = new PhongMaterial();
        material.setDiffuseMap(earthImage);
        earth.setMaterial(material);

        Group root = new Group(earth);
        Scene scene = new Scene(root, 600, 600, true);
        scene.setFill(Color.BLACK);
        scene.setCamera(new PerspectiveCamera());

        stage.setTitle("Earth Sphere");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}

