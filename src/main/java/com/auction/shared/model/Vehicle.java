package com.auction.shared.model;
public class Vehicle extends Item{
    private int warrantyPeriod;
    private String brand;
    private int numberOfSeats;
    private String fuelType;
    private String engineCapacity;

    public Vehicle(String name, String brand,int numberOfSeats,String fuelType,String engineCapacity, int warrantyPeriod,double currentPrice, double minIncrement){
        super(name,currentPrice,minIncrement);
        this.brand=brand;
        this.warrantyPeriod=warrantyPeriod;
        this.engineCapacity=engineCapacity;
        this.fuelType=fuelType;
        this.numberOfSeats=numberOfSeats;
    }

    public int getWarrantyPeriod(){ return warrantyPeriod;}
    public void setWarrantyPeriod(int newWarrantyPeriod){ warrantyPeriod=newWarrantyPeriod; }

    public String getBrand(){ return brand;}
    public void setBrand(String newBrand){ brand=newBrand; }

    public String getFuelType(){ return fuelType;}
    public void setFuelType(String newFuelType){ fuelType=newFuelType; }

    public String getEngineCapacity(){ return engineCapacity;}
    public void setEngineCapacity(String newEngineCapacity){ engineCapacity=newEngineCapacity; }

    public int getNumberOfSeats(){ return numberOfSeats;}
    public void setNumberOfSeats(int newNumberOfSeats){ numberOfSeats=newNumberOfSeats; }

    public void printInfo(){
        System.out.println("Tên phương tiện:\n"+getName()+getNumberOfSeats()+" chỗ\n"+"Hãng:\n"+getBrand()+"Loại nhiên liệu:\n"+getFuelType()+"Dung tích động cơ/Công suất:\n"+getEngineCapacity()+"Thời hạn bảo hành còn: "+getWarrantyPeriod()+" tháng");
    }
}