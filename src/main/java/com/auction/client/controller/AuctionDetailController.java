package com.auction.client.controller;

import com.auction.client.network.NetworkClient;
import com.auction.server.dao.ItemDAO;
import com.auction.shared.model.Auction;
import com.auction.shared.model.item.Item;
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

public class AuctionDetailController {
    public void setItemData(Item chosenItem){}
}
