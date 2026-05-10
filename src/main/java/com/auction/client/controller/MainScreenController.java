package com.auction.client.controller;

import com.auction.client.Network.NetworkClient;
import com.auction.server.dao.ItemDAO;
import com.auction.shared.model.Auction;
import com.auction.shared.model.Item;
import com.auction.shared.model.user.Bidder;
import com.auction.shared.model.user.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.event.ActionEvent;

import java.io.IOException;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;

public class MainScreenController implements Initializable {

    @FXML
    private Button btnUpdate;
    @FXML
    private Button btnQuaylai2;
    @FXML
    private TableView<Item> tbvWillPresent;
    @FXML
    private TextField txtSearch1;
    @FXML
    private TableView<Item> tbvIsPresenting;
    @FXML
    private Label lblTime5;
    @FXML
    private Button btnDangXuat1;
    @FXML
    private Label lblName;
    @FXML
    private TableColumn<Item, String> NameColumn1;
    @FXML
    private TableColumn<Item, Integer> IDcolumn1;
    @FXML
    private TableColumn<Item, String> typeColumn1;
    @FXML
    private TableColumn<Item, Double> RecentPriceColumn;
    @FXML
    private TableColumn<Item, Date> EndTimeColumn1;
    @FXML
    private TableColumn<Item, String> NameColumn2;
    @FXML
    private TableColumn<Item, Integer> IDcolumn2;
    @FXML
    private TableColumn<Item, String> typeColumn2;
    @FXML
    private TableColumn<Item, Double> StartPriceColumn;
    @FXML
    private TableColumn<Item, Date> StartTimeColumn;
    @FXML
    private TableColumn<Item, Date> EndTimeColumn2;


    public void initialize(URL url, ResourceBundle resourceBundle) {
        clockInit();
    }

    public void clockInit() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss  dd/MM/yyyy");
        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> {
            lblTime5.setText(LocalDateTime.now().format(formatter));
        }), new KeyFrame(Duration.seconds(1)));

        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();
    }

    private void switchScence(ActionEvent event, String fxmlFile) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(fxmlFile));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    public void toLoginScreen(ActionEvent event) throws IOException {
        switchScence(event, "/SignInScreen.fxml");
    }

    public void Table1Presenting(URL url, ResourceBundle resourceBundle) {
        NameColumn1.setCellValueFactory(new PropertyValueFactory<>("name"));
        IDcolumn1.setCellValueFactory(new PropertyValueFactory<>("itemID"));
        typeColumn1.setCellValueFactory(new PropertyValueFactory<>("type"));
        RecentPriceColumn.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        EndTimeColumn1.setCellValueFactory(new PropertyValueFactory<>("endTime"));

        loadtoTable1();

        tbvIsPresenting.setOnMouseClicked(event -> {

            if (event.getClickCount() == 2) {
                Item selectedItem = tbvIsPresenting.getSelectionModel().getSelectedItem();

                if (selectedItem != null) {
                    System.out.println("Bạn muốn đấu giá vật phẩm: " + selectedItem.getName());
                    openAuctionScreen(selectedItem);
                }
            }
        });
    }

    //Kết nối với Server
    public void loadtoTable1() {
        List<Item> activeItems = NetworkClient.takeActiveItems();
        ObservableList<Item> activeList = FXCollections.observableArrayList(activeItems);
        tbvIsPresenting.setItems(activeList);
    }

    public void openAuctionScreen(Item chosenItem) {

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AuctionDetail.fxml"));
            Parent root = loader.load();
            AuctionDetailController auctionDetailController = loader.getController();
            auctionDetailController.setItemData(chosenItem);

            Stage stage = new Stage();
            stage.setTitle("Chi tiết: " + chosenItem.getName());
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void Table2Presenting(URL url, ResourceBundle resourceBundle) {
        NameColumn2.setCellValueFactory(new PropertyValueFactory<>("name"));
        IDcolumn2.setCellValueFactory(new PropertyValueFactory<>("itemID"));
        typeColumn2.setCellValueFactory(new PropertyValueFactory<>("type"));
        StartPriceColumn.setCellValueFactory(new PropertyValueFactory<>("startingPrice"));
        StartTimeColumn.setCellValueFactory(new PropertyValueFactory<>("startTime"));
        EndTimeColumn2.setCellValueFactory(new PropertyValueFactory<>("endTime"));

        loadtoTable2();

        tbvWillPresent.setOnMouseClicked(event -> {

            if (event.getClickCount() == 2) {
                Item selectedItem = tbvWillPresent.getSelectionModel().getSelectedItem();

                if (selectedItem != null) {
                    System.out.println("Bạn muốn đấu giá vật phẩm: " + selectedItem.getName());
                    openAuctionScreen(selectedItem);
                }
            }
        });
    }


    public void loadtoTable2() {
        List<Item> activeItems = NetworkClient.takePreparedItems();
        ObservableList<Item> activeList = FXCollections.observableArrayList(activeItems);
        tbvWillPresent.setItems(activeList);
    }

    public void setDisplayName(String currentUser){
        lblName.setText(currentUser);
    }

    public void toSellerLoginScreen(ActionEvent event) throws IOException{
        switchScence(event,"/SellerLoginScreen.fxml"); //(Yêu cầu nhập lại họ tên, sdt,.. và lần sau đăng nhập nick này sẽ mở ra giao diện nguời bán luôn)
    }

    public void Search(){
        String search=txtSearch1.getText();
    }
}