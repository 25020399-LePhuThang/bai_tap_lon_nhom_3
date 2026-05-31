package com.auction.shared.model.item;

import java.io.Serial;

public class Electronic extends Item {
    @Serial
    private static final long serialVersionUID = 1L;

    private int warrantyPeriod;
    private String brand;

    public Electronic(String name, String brand, int warrantyPeriod, double startingPrice, double minIncrement) {
        super(name, startingPrice, minIncrement);
        currentPrice = startingPrice;
        this.brand = brand;
        this.warrantyPeriod = warrantyPeriod;
    }

    public Electronic() {
    }

    public int getWarrantyPeriod() {
        return warrantyPeriod;
    }

    public void setWarrantyPeriod(int newWarrantyPeriod) {
        warrantyPeriod = newWarrantyPeriod;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String newBrand) {
        brand = newBrand;
    }

    public void printInfo() {
        System.out.println("Tên đồ điện:\n" + getName() + "Hãng:\n" + getBrand() + "Thời hạn bảo hành còn: " + getWarrantyPeriod() + " tháng");
    }


}