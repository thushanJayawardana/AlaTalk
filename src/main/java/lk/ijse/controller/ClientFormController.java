package lk.ijse.controller;
import com.jfoenix.controls.JFXButton;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lk.ijse.controller.ServerFormController;
import lk.ijse.emoji.EmojiPicker;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.ResourceBundle;

public class ClientFormController implements Initializable {
    @FXML
    private AnchorPane anchorPane;

    @FXML
    private TextField enterMessageField;

    @FXML
    private ScrollPane scrollPane;

    @FXML
    private VBox vBox;

    @FXML
    private JFXButton emojiButton;

    private Socket socket;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;
    private String clientName = "Client";

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    socket = new Socket("localhost", 3002);
                    dataInputStream = new DataInputStream(socket.getInputStream());
                    dataOutputStream = new DataOutputStream(socket.getOutputStream());
                    System.out.println("Client Connected");
                    ServerFormController.receiveMessage(clientName + " joined..");

                    while (socket.isConnected()){
                        String receivedMessage = dataInputStream.readUTF();
                        receiveMessage(receivedMessage, ClientFormController.this.vBox);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();

        this.vBox.heightProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number oldValue, Number newValue) {
                scrollPane.setVvalue((Double) newValue);
            }
        });
        emoji();
    }

    @FXML
    void imageInsertBtnOnAction(ActionEvent event) throws IOException {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Image File");
        FileChooser.ExtensionFilter filteredImage = new FileChooser.ExtensionFilter("Image Files", ".png", ".jpg", "*.jpeg");
        fileChooser.getExtensionFilters().add(filteredImage);
        File selectedFile = fileChooser.showOpenDialog(new Stage());
//        sendImage(selectedFile);
        if (selectedFile != null) {
            // Display a confirmation dialog
            Alert confirmationDialog = new Alert(Alert.AlertType.CONFIRMATION);
            confirmationDialog.setTitle("Image Confirmation");
            confirmationDialog.setHeaderText("Do you want to send the selected image?");
            confirmationDialog.setContentText("Selected Image: " + selectedFile.getName());

            Optional<ButtonType> result = confirmationDialog.showAndWait();

            if (result.isPresent() && result.get() == ButtonType.OK) {
                sendImage(selectedFile.getAbsolutePath());
            }
        }
    }

    @FXML
    void sendMsgBtnOnAction(ActionEvent event) {
        sendMsg(enterMessageField.getText());
    }

    private void sendMsg(String imgToSend) {
        if (!imgToSend.isEmpty()){
            if (!imgToSend.matches(".*\\.(png|jpe?g|gif)$")){

                HBox hBox = new HBox();
                hBox.setAlignment(Pos.CENTER_RIGHT);
                hBox.setPadding(new Insets(5, 5, 0, 10));

                Text text = new Text(imgToSend);
                text.setStyle("-fx-font-size: 14");
                TextFlow textFlow = new TextFlow(text);

//              #0693e3 #37d67a #40bf75
                textFlow.setStyle("-fx-background-color: #0693e3; -fx-font-weight: bold; -fx-color: white; -fx-background-radius: 20px");
                textFlow.setPadding(new Insets(5, 10, 5, 10));
                text.setFill(Color.color(1, 1, 1));

                hBox.getChildren().add(textFlow);

                HBox hBoxTime = new HBox();
                hBoxTime.setAlignment(Pos.CENTER_RIGHT);
                hBoxTime.setPadding(new Insets(0, 5, 5, 10));
                String stringTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
                Text time = new Text(stringTime);
                time.setStyle("-fx-font-size: 8");

                hBoxTime.getChildren().add(time);

                vBox.getChildren().add(hBox);
                vBox.getChildren().add(hBoxTime);


                try {
                    dataOutputStream.writeUTF(clientName + "-" + imgToSend);
                    dataOutputStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                enterMessageField.clear();
            }
        }
    }
    public void shutdown() {
        // cleanup code here...
        ServerFormController.receiveMessage(clientName+" left.");
    }
//    private void sendImage(Image imgToSend) {
//        ImageView imageView = new ImageView(imgToSend);
//        imageView.setFitHeight(200);
//        imageView.setFitWidth(200);
////        TextFlow textFlow = new TextFlow(imageView);
//        HBox hBox = new HBox();
//        hBox.setPadding(new Insets(5,5,5,10));
//        hBox.getChildren().add(imageView);
//        hBox.setAlignment(Pos.CENTER_RIGHT);
//
//        vBox.getChildren().add(hBox);
//
//        try {
//            dataOutputStream.writeUTF(clientName + "-" +imgToSend);
//            dataOutputStream.flush();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
    private void sendImage(String imagePath) {
        try {
            // Read the image into a byte array
            byte[] imageBytes = Files.readAllBytes(Paths.get(imagePath));

            // Send the image data to the server
            dataOutputStream.writeInt(imageBytes.length);
            dataOutputStream.write(imageBytes);
            dataOutputStream.flush();

            // Display the image locally on the client
            Image image = new Image(new ByteArrayInputStream(imageBytes));
            ImageView imageView = new ImageView(image);
            imageView.setFitHeight(200);
            imageView.setFitWidth(200);

            HBox hBox = new HBox();
            hBox.setPadding(new Insets(5, 5, 5, 10));
            hBox.getChildren().add(imageView);
            hBox.setAlignment(Pos.CENTER_RIGHT);

            vBox.getChildren().add(hBox);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void receiveMessage(String msg, VBox vBox) throws IOException {
        if (msg.matches(".*\\.(png|jpe?g|gif)$")){
            HBox hBoxName = new HBox();
            hBoxName.setAlignment(Pos.CENTER_LEFT);
            Text textName = new Text(msg.split("[-]")[0]);
            TextFlow textFlowName = new TextFlow(textName);
            hBoxName.getChildren().add(textFlowName);

            Image image = new Image(msg.split("[-]")[1]);
            ImageView imageView = new ImageView(image);
            imageView.setFitHeight(200);
            imageView.setFitWidth(200);
            HBox hBox = new HBox();
            hBox.setAlignment(Pos.CENTER_LEFT);
            hBox.setPadding(new Insets(5,5,5,10));
            hBox.getChildren().add(imageView);
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    vBox.getChildren().add(hBoxName);
                    vBox.getChildren().add(hBox);
                }
            });

        }else {
            String name = msg.split("-")[0];
            String msgFromServer = msg.split("-")[1];

            HBox hBox = new HBox();
            hBox.setAlignment(Pos.CENTER_LEFT);
            hBox.setPadding(new Insets(5,5,5,10));

            HBox hBoxName = new HBox();
            hBoxName.setAlignment(Pos.CENTER_LEFT);
            Text textName = new Text(name);
            TextFlow textFlowName = new TextFlow(textName);
            hBoxName.getChildren().add(textFlowName);

            Text text = new Text(msgFromServer);
            TextFlow textFlow = new TextFlow(text);
            textFlow.setStyle("-fx-background-color: #abb8c3; -fx-font-weight: bold; -fx-background-radius: 20px");
            textFlow.setPadding(new Insets(5,10,5,10));
            text.setFill(Color.color(0,0,0));

            hBox.getChildren().add(textFlow);

            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    vBox.getChildren().add(hBoxName);
                    vBox.getChildren().add(hBox);
                }
            });
        }
    }
    private void emoji() {
        // Create the EmojiPicker
        EmojiPicker emojiPicker = new EmojiPicker();

        VBox vBox = new VBox(emojiPicker);
        vBox.setPrefSize(150,300);
        vBox.setLayoutX(400);
        vBox.setLayoutY(175);
        vBox.setStyle("-fx-font-size: 30");

        anchorPane.getChildren().add(vBox);

        // Set the emoji picker as hidden initially
        emojiPicker.setVisible(false);

        // Show the emoji picker when the button is clicked
        emojiButton.setOnAction(event -> {
            if (emojiPicker.isVisible()){
                emojiPicker.setVisible(false);
            }else {
                emojiPicker.setVisible(true);
            }
        });

        // Set the selected emoji from the picker to the text field
        emojiPicker.getEmojiListView().setOnMouseClicked(event -> {
            String selectedEmoji = emojiPicker.getEmojiListView().getSelectionModel().getSelectedItem();
            if (selectedEmoji != null) {
                enterMessageField.setText(enterMessageField.getText()+selectedEmoji);
            }
            emojiPicker.setVisible(false);
        });
    }
    public void setClientName(String name) {
        clientName = name;
    }
}
