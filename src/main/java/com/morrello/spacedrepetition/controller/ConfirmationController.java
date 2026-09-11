package com.morrello.spacedrepetition.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class ConfirmationController {
    @FXML
    private Label messageLabel;

    private boolean confirmed;

    public void setMessage(String message) {
        messageLabel.setText(message);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    @FXML
    private void confirm() {
        confirmed = true;
        close();
    }

    @FXML
    private void cancel() {
        confirmed = false;
        close();
    }

    private void close() {
        Stage stage = (Stage) messageLabel.getScene().getWindow();
        stage.close();
    }
}
