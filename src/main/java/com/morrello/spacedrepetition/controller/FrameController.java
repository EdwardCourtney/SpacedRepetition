package com.morrello.spacedrepetition.controller;

import com.morrello.spacedrepetition.database.NoteRepository;
import com.morrello.spacedrepetition.database.ReviewItemRepository;
import com.morrello.spacedrepetition.model.ReviewItem;
import com.morrello.spacedrepetition.util.DateCalculator;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class FrameController {
    private final NoteRepository noteRepository = new NoteRepository();
    private final ReviewItemRepository reviewItemRepository = new ReviewItemRepository();

    @FXML
    private TextField subjectField;

    @FXML
    private TextField lessonField;

    @FXML
    private VBox todayWorkList;

    @FXML
    private VBox completedList;

    @FXML
    private VBox allErrandList;

    @FXML
    private TextArea noteArea;

    @FXML
    private void initialize() {
        noteArea.setText(noteRepository.load());
        reviewItemRepository.refreshDueItems();
        loadCards();
    }

    @FXML
    public void addCard() {
        String subject = subjectField.getText().trim();
        String lesson = lessonField.getText().trim();

        if (subject.isEmpty() || lesson.isEmpty()) {
            showNotification("Subject and lesson are required.");
            return;
        }

        if (reviewItemRepository.existsBySubjectAndLesson(subject, lesson)) {
            showNotification("This errand already exists.");
            return;
        }

        long id = reviewItemRepository.create(subject, lesson, DateCalculator.today());
        ReviewItem item = reviewItemRepository.findById(id);

        todayWorkList.getChildren().add(createTodayCard(item));
        allErrandList.getChildren().add(createAllErrandCard(item));

        subjectField.clear();
        lessonField.clear();
    }

    @FXML
    public void saveNote() {
        noteRepository.save(noteArea.getText());
    }

    private void loadCards() {
        todayWorkList.getChildren().clear();
        completedList.getChildren().clear();
        allErrandList.getChildren().clear();

        for (ReviewItem item : reviewItemRepository.findTodayWork()) {
            todayWorkList.getChildren().add(createTodayCard(item));
        }

        for (ReviewItem item : reviewItemRepository.findCompletedToday()) {
            completedList.getChildren().add(createCompletedCard(item));
        }

        for (ReviewItem item : reviewItemRepository.findAll()) {
            allErrandList.getChildren().add(createAllErrandCard(item));
        }
    }

    private Node createTodayCard(ReviewItem item) {
        return createCard(item, "Day: " + item.day(), "Done", () -> completeCard(item), "Delay", () -> delayCard(item));
    }

    private Node createCompletedCard(ReviewItem item) {
        return createCard(item, "Day: " + item.day(), "Undo", () -> undoCompletedCard(item));
    }

    private Node createAllErrandCard(ReviewItem item) {
        return createCard(item, "Next: " + item.nextReviewDay(), "Delete", () -> deleteCard(item));
    }

    private Node createCard(ReviewItem item, String dateText, String primaryText, Runnable primaryAction) {
        return createCard(item, dateText, primaryText, primaryAction, null, null);
    }

    private Node createCard(
            ReviewItem item,
            String dateText,
            String primaryText,
            Runnable primaryAction,
            String secondaryText,
            Runnable secondaryAction
    ) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Card.fxml"));
            Node card = loader.load();
            card.getProperties().put("reviewItemId", item.id());
            CardController controller = loader.getController();
            controller.setCard(item, dateText, primaryText, primaryAction, secondaryText, secondaryAction);
            return card;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load card UI.", exception);
        }
    }

    private void completeCard(ReviewItem item) {
        reviewItemRepository.markRepeated(item.id());
        ReviewItem updatedItem = reviewItemRepository.findById(item.id());

        removeCardById(todayWorkList, item.id());
        completedList.getChildren().add(createCompletedCard(updatedItem));
        loadCards();
    }

    private void undoCompletedCard(ReviewItem item) {
        reviewItemRepository.undoCompletion(item.id());
        removeCardById(completedList, item.id());
        loadCards();
    }

    private void deleteCard(ReviewItem item) {
        if (!confirm("Delete " + item.subject() + " - " + item.lesson() + "? This cannot be undone.")) {
            return;
        }

        reviewItemRepository.delete(item.id());
        loadCards();
    }

    private void delayCard(ReviewItem item) {
        reviewItemRepository.delayOneDay(item.id());
        loadCards();
    }

    private void showNotification(String message) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Notification.fxml"));
            Parent root = loader.load();
            NotificationController controller = loader.getController();
            controller.setMessage(message);

            Stage stage = new Stage();
            stage.setTitle("Notification");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load notification UI.", exception);
        }
    }

    private boolean confirm(String message) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Confirmation.fxml"));
            Parent root = loader.load();
            ConfirmationController controller = loader.getController();
            controller.setMessage(message);

            Stage stage = new Stage();
            stage.setTitle("Confirm");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

            return controller.isConfirmed();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load confirmation UI.", exception);
        }
    }

    private void removeCardById(VBox list, long id) {
        list.getChildren().removeIf(card -> {
            Object cardId = card.getProperties().get("reviewItemId");
            return cardId instanceof Long && (Long) cardId == id;
        });
    }
}
