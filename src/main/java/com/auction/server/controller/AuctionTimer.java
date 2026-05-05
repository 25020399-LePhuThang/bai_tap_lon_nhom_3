package com.auction.client.controller;

import com.auction.shared.model.Item;
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
            
            processEndAuction();
        }
    }

    
    public void cancel() {
        if (timer != null) {
            timer.cancel();
            timer = new Timer(); 
        }
    }

    private void processEndAuction() {
        System.out.println(">>> THÔNG BÁO: Phiên đấu giá [" + item.getName() + "] đã kết thúc!");
        
    }
}