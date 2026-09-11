package com.morrello.spacedrepetition;

import com.morrello.spacedrepetition.controller.FrameController;
import com.morrello.spacedrepetition.database.Database;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.image.Image;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        Database.initialize();

        FXMLLoader loader = new FXMLLoader(App.class.getResource("/Frame.fxml"));
        Parent root = loader.load();
        FrameController controller = loader.getController();

        stage.setTitle("Spaced Repetition");
        stage.getIcons().add(new Image(App.class.getResourceAsStream("/Icon.png")));
        stage.setScene(new Scene(root));
        stage.setOnCloseRequest(event -> controller.saveNote());
        stage.setMaximized(true);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
