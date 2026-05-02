package com.auction.shared.model;
public class Electronic extends Item{
    private int warrantyPeriod;
    private String brand;

    public Electronic(String name, String brand, int warrantyPeriod,double currentPrice, double minIncrement){
        super(name,currentPrice,minIncrement);
        this.brand=brand;
        this.warrantyPeriod=warrantyPeriod;
    }

    public int getWarrantyPeriod(){ return warrantyPeriod;}
    public void setWarrantyPeriod(int newWarrantyPeriod){ warrantyPeriod=newWarrantyPeriod; }

    public String getBrand(){ return brand;}
    public void setBrand(String newBrand){ brand=newBrand; }

    public void printInfo(){
        System.out.println("Tên đồ điện:\n"+getName()+"Hãng:\n"+getBrand()+"Thời hạn bảo hành còn: "+getWarrantyPeriod()+" tháng");
    }


}