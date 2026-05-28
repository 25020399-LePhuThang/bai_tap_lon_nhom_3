package com.auction.server.controller;

import com.auction.server.network.AuctionServer;
import com.auction.shared.model.item.Item;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Date;

public class AuctionTimer {
    private Timer timer;
    private Item item;

    public AuctionTimer(Item item) {
        this.item = item;
        this.timer = new Timer();
    }
    public AuctionTimer(){this.timer = new Timer();}

    
    public void start() {
        if (item.getEndTime() == null) return;

        long delay = item.getEndTime().getTime() - System.currentTimeMillis();
        
        if (delay > 0) {
            
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    processEndAuction();
                }
            }, delay);
        } else {

            System.out.println(">>> Item " + item.getName() + " đã hết hạn, bỏ qua.");
        }
    }

    
    public void cancel() {
        if (timer != null) {
            timer.cancel();
            timer = new Timer(); 
        }
    }
 
    public void reschedule() {
        cancel();    // Hủy lịch cũ, tạo Timer mới 
        start();     // Lên lịch lại với endTime đã được cập nhập
    }

    private void processEndAuction() {
        System.out.println(">>> THÔNG BÁO: Phiên đấu giá [" + item.getName() + "] đã kết thúc!");
        System.out.println(">>> lastBidderId: " + item.getLastBidderId());
        System.out.println(">>> currentPrice: " + item.getCurrentPrice());

        item.setStatus("SOLD");
        com.auction.server.dao.ItemDAO itemDAO = new com.auction.server.dao.ItemDAO();
        itemDAO.updatePrice(item);
        itemDAO.updateStatusToSold(item.getId());
        String finalWinner = item.getLastBidderId();
        if (finalWinner != null && !finalWinner.equalsIgnoreCase("null")
                && !finalWinner.equals("Không có người thắng")) {
            com.auction.server.dao.UserDAO userDAO = new com.auction.server.dao.UserDAO();
            userDAO.withdraw(finalWinner, item.getCurrentPrice());

            String sellerId = item.getSeller_ID();
            if (sellerId != null) {
                userDAO.deposit(sellerId, item.getCurrentPrice());
            }
            System.out.println(">>> Đã trừ " + item.getCurrentPrice() + "$ của " + finalWinner);
        }

        String winner = item.getLastBidderId();
        if (winner == null || winner.equalsIgnoreCase("null")) {
            winner = "Không có người thắng";
        }

        AuctionServer.broadcast(
                "AUCTION_END|" + item.getId() + "|" + winner + "|" + item.getCurrentPrice()
        );

        System.out.println(">>> Người thắng: " + winner);
    }

    public void setItem(Item item) {
        this.item = item;
    }
}