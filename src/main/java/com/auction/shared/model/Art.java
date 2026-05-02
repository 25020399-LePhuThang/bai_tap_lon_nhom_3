package com.auction.shared.model;

public class Art extends Item{
    protected String author;
    protected int creationYear;

    public Art(String name, String author,int creationYear,double currentPrice, double minIncrement){
        super(name,currentPrice,minIncrement);
        this.author=author;
        this.creationYear=creationYear;
    }

    public String getAuthor(){ return author;}
    public void setAuthor(String newAuthor){ author=newAuthor; }

    public int getCreationYear(){ return creationYear;}
    public void setCreationYear(int newCreationYear){ creationYear= newCreationYear; }

    public void printInfo(){
        System.out.println("Tên tác phẩm:\n"+getName()+"Tác giả:\n"+getAuthor()+"Năm tạo"+getCreationYear());
    }

}