package com.auction.shared.model.item;

import java.io.Serial;

public class Vehicle extends Item {
    @Serial
    private static final long serialVersionUID = 1L;

    private int warrantyPeriod;
    private String brand;
    private String fuelType;
    private String engineCapacity;

    public Vehicle(String name, String brand, String fuelType, String engineCapacity, int warrantyPeriod, double startingPrice, double minIncrement) {
        super(name, startingPrice, minIncrement);
        this.brand = brand;
        this.warrantyPeriod = warrantyPeriod;
        this.engineCapacity = engineCapacity;
        this.fuelType = fuelType;
    }

    public Vehicle() {
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

    public String getFuelType() {
        return fuelType;
    }

    public void setFuelType(String newFuelType) {
        fuelType = newFuelType;
    }

    public String getEngineCapacity() {
        return engineCapacity;
    }

    public void setEngineCapacity(String newEngineCapacity) {
        engineCapacity = newEngineCapacity;
    }

    public void printInfo() {
        System.out.println("Tên phương tiện:\n" + getName() + " chỗ\n" + "Hãng:\n" + getBrand() + "Loại nhiên liệu:\n" + getFuelType() + "Dung tích động cơ/Công suất:\n" + getEngineCapacity() + "Thời hạn bảo hành còn: " + getWarrantyPeriod() + " tháng");
    }
}