package org.example;

import com.microsoft.azure.sdk.iot.device.DeviceClient;

public class Machine {

    private int fullNodeId;
    private String deviceName;
    private CloudDevice cloudDevice;

    private int productionStatus;
    private String workOrderId;
    private int productionRate;
    private long goodCount;
    private long badCount;
    private double temperature;
    private int deviceError;
    private DeviceClient deviceClient;

    public Machine(int fullNodeId, String deviceName, CloudDevice cloudDevice, DeviceClient deviceClient) {
        this.fullNodeId = fullNodeId;
        this.deviceName = deviceName;
        this.cloudDevice = cloudDevice;
        this.deviceClient = deviceClient;
    }

    public int getFullNodeId() {
        return fullNodeId;
    }

    public void setFullNodeId(int fullNodeId) {
        this.fullNodeId = fullNodeId;
    }

    public CloudDevice getCloudDevice() {
        return cloudDevice;
    }

    public void setCloudDevice(CloudDevice cloudDevice) {
        this.cloudDevice = cloudDevice;
    }

    public int getProductionStatus() {
        return productionStatus;
    }

    public void setProductionStatus(int productionStatus) {
        this.productionStatus = productionStatus;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
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

    public long getGoodCount() {
        return goodCount;
    }

    public void setGoodCount(long goodCount) {
        this.goodCount = goodCount;
    }

    public long getBadCount() {
        return badCount;
    }

    public void setBadCount(long badCount) {
        this.badCount = badCount;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public int getDeviceError() {
        return deviceError;
    }

    public void setDeviceError(int deviceError) {
        this.deviceError = deviceError;
    }

    public DeviceClient getDeviceClient() {
        return deviceClient;
    }

    public void setDeviceClient(DeviceClient deviceClient) {
        this.deviceClient = deviceClient;
    }
}
