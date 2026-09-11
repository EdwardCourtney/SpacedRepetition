package com.morrello.spacedrepetition;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class App extends Application {
    @Override
    public void start(Stage stage) {
        Label title = new Label("Spaced Repetition");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        Label status = new Label("JavaFX is ready.");

        Button button = new Button("Start");
        button.setOnAction(event -> status.setText("Ready to build your app."));

        VBox root = new VBox(16, title, status, button);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(24));

        Scene scene = new Scene(root, 640, 420);

        stage.setTitle("Spaced Repetition");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
