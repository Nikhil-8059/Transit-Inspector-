package com.application.transitinspector;

public class ViolationRecord {
    public String driverName;
    public String vehicleNumber;
    public String violationType;
    public String timestamp;

    public ViolationRecord(String driverName, String vehicleNumber, String violationType, String timestamp) {
        this.driverName = driverName;
        this.vehicleNumber = vehicleNumber;
        this.violationType = violationType;
        this.timestamp = timestamp;
    }
}
