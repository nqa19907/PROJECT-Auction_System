package com.auction.shared.model;

public class Electronic extends Item {
    private String brand;
    private int warrantyMonths;

    public Electronic(String itemName, String description, Double startPrice, String sellerId, String condition, String imagePath,
                      String brand, int warrantyMonths) {
        super(itemName, description, startPrice, sellerId, condition, imagePath);
        this.brand = brand;
        this.warrantyMonths = warrantyMonths;
    }

    @Override
    public String getDisplayDetails() {
        // to be coded
        return "";
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public int getWarrantyMonths() {
        return warrantyMonths;
    }

    public void setWarrantyMonths(int warrantyMonths) {
        this.warrantyMonths = warrantyMonths;
    }

    @Override
    public String toString() {
        return super.toString() + " -> Electronic{" +
                "brand='" + brand + '\'' +
                ", warrantyMonths=" + warrantyMonths +
                '}';
    }
}
