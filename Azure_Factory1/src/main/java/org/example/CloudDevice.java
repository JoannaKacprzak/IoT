package org.example;

public class CloudDevice {

    private String connectionString, device_id, devicePrimaryKey;

    public CloudDevice(String device_id, String devicePrimaryKey) {
        this.device_id = device_id;
        this.devicePrimaryKey = devicePrimaryKey;
        this.connectionString = "HostName=iot-Kacprzak-standard-1234.azure-devices.net;DeviceId=" + this.device_id + ";SharedAccessKey=" + this.devicePrimaryKey;
    }

    public String getConnectionString() {
        return connectionString;
    }

    public void setConnectionString(String connectionString) {
        this.connectionString = connectionString;
    }

    public String getDevice_id() {
        return device_id;
    }

    public void setDevice_id(String device_id) {
        this.device_id = device_id;
    }

    public String getDevicePrimaryKey() {
        return devicePrimaryKey;
    }

    public void setDevicePrimaryKey(String devicePrimaryKey) {
        this.devicePrimaryKey = devicePrimaryKey;
    }
}
