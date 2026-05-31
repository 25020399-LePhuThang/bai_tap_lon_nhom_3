package com.auction.server.controller;

import com.auction.server.network.AuctionServer;
import com.auction.shared.model.item.Item;
import com.auction.server.dao.BidDAO;
import com.auction.server.dao.ItemDAO;
import java.util.Timer;
import java.util.TimerTask;

public class AuctionTimer {
    private Timer timer;
    private Item item;

    public AuctionTimer(Item item) {
        this.item = item;
        this.timer = new Timer();
    }

    public AuctionTimer() {
        this.timer = new Timer();
    }

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
        cancel();
        start();
    }

    private void processEndAuction() {
        System.out.println(">>> THÔNG BÁO: Phiên đấu giá [" + item.getName() + "] đã kết thúc!");

        String winner = item.getLastBidderId();
        String sellerId = item.getSeller_ID();
        double price = item.getCurrentPrice();

        if (winner != null && !winner.equalsIgnoreCase("null") && !winner.equals("Không có người thắng")) {

            BidDAO bidDAO = new BidDAO();
            boolean success = bidDAO.processAuctionPayment(item.getId(), winner, sellerId, price);

            if (success) {
                item.setStatus("SOLD");
                System.out.println(">>> Đã thanh toán thành công và chốt đơn cho: " + winner);
                AuctionServer.broadcast("AUCTION_END|" + item.getId() + "|" + winner + "|" + price);
                AuctionServer.broadcast("NOTIFY_SELLER|" + sellerId + "|" + item.getId() + "|" + item.getName() + "|" + price);
            } else {
                item.setStatus("FAILED");
                System.out.println(">>> Thanh toán thất bại (Người mua không đủ tiền hoặc lỗi hệ thống).");
                AuctionServer.broadcast("AUCTION_FAILED|" + item.getId() + "|INSUFFICIENT_FUNDS");
            }

        } else {
            item.setStatus("UNSOLD");
            System.out.println(">>> Không có người thắng.");

            ItemDAO itemDAO = new ItemDAO();
            itemDAO.updateStatusToSold(item.getId());

            AuctionServer.broadcast("AUCTION_END|" + item.getId() + "|Không có người thắng|" + price);
        }
    }

    public void setItem(Item item) {
        this.item = item;
    }
}