package org.example;

import java.time.Instant;

public class MachineDTO {

    private String deviceId, workOrderId;
    private int productionRate, deviceError;
    private double temperature;
    private long countGood, countBad;
    private long timestamp;

    public MachineDTO(String deviceId, String workOrderId, int productionRate, int deviceError, double temperature, long countGood, long countBad) {
        this.deviceId = deviceId;
        this.workOrderId = workOrderId;
        this.productionRate = productionRate;
        this.deviceError = deviceError;
        this.temperature = temperature;
        this.countGood = countGood;
        this.countBad = countBad;
        this.timestamp = Instant.now().getEpochSecond();
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getWorkOrderId() {
        return workOrderId;
    }

    public void setWorkOrderId(String workOrderId) {
        this.workOrderId = workOrderId;
    }

    public int getProductionRate() {
        return productionRate;
    }

    public void setProductionRate(int productionRate) {
        this.productionRate = productionRate;
    }

    public int getDeviceError() {
        return deviceError;
    }

    public void setDeviceError(int deviceError) {
        this.deviceError = deviceError;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public long getCountGood() {
        return countGood;
    }

    public void setCountGood(long countGood) {
        this.countGood = countGood;
    }

    public long getCountBad() {
        return countBad;
    }

    public void setCountBad(long countBad) {
        this.countBad = countBad;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
