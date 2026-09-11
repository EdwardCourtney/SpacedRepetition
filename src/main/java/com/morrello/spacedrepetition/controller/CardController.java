package com.morrello.spacedrepetition.controller;

import com.morrello.spacedrepetition.model.ReviewItem;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class CardController {
    @FXML
    private Label titleLabel;

    @FXML
    private Label dayLabel;

    @FXML
    private Label repeatedLabel;

    @FXML
    private Button primaryButton;

    @FXML
    private Button secondaryButton;

    public void setCard(ReviewItem item, String dateText, String primaryText, Runnable primaryAction) {
        setCard(item, dateText, primaryText, primaryAction, null, null);
    }

    public void setCard(
            ReviewItem item,
            String dateText,
            String primaryText,
            Runnable primaryAction,
            String secondaryText,
            Runnable secondaryAction
    ) {
        titleLabel.setText(item.subject() + " - " + item.lesson());
        dayLabel.setText(dateText);
        repeatedLabel.setText("Repeated: " + item.repeated());
        primaryButton.setText(primaryText);
        primaryButton.setOnAction(event -> primaryAction.run());
        primaryButton.getStyleClass().removeAll("primary-button", "secondary-command-button", "danger-button");
        primaryButton.getStyleClass().add(styleClassFor(primaryText));

        boolean hasSecondaryAction = secondaryText != null && secondaryAction != null;
        secondaryButton.setVisible(hasSecondaryAction);
        secondaryButton.setManaged(hasSecondaryAction);
        if (hasSecondaryAction) {
            secondaryButton.setText(secondaryText);
            secondaryButton.setOnAction(event -> secondaryAction.run());
            secondaryButton.getStyleClass().removeAll("primary-button", "secondary-command-button", "danger-button");
            secondaryButton.getStyleClass().add(styleClassFor(secondaryText));
        }
    }

    private String styleClassFor(String actionText) {
        return switch (actionText) {
            case "Delete" -> "danger-button";
            case "Done", "Add" -> "primary-button";
            default -> "secondary-command-button";
        };
    }
}
