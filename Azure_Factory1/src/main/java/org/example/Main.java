package org.example;

import com.google.gson.Gson;
import com.microsoft.azure.sdk.iot.device.*;
import com.microsoft.azure.sdk.iot.device.DeviceTwin.*;
import com.microsoft.azure.sdk.iot.service.Device;
import com.microsoft.azure.sdk.iot.service.RegistryManager;
import com.microsoft.azure.sdk.iot.service.exceptions.IotHubException;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseDirection;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseResultMask;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.*;

import javax.xml.crypto.Data;
import java.io.Console;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class Main {

    private static final int MAX_DEVICES = 3;
    private static final String STATUS_ENDPOINT = "/ProductionStatus";
    private static final String WORK_ORDER_ID_ENDPOINT = "/WorkorderId";
    private static final String COUNT_GOOD_ENDPOINT = "/GoodCount";
    private static final String COUNT_BAD_ENDPOINT = "/BadCount";
    private static final String TEMPERATURE_ENDPOINT = "/Temperature";
    private static final String DEVICE_ERROR_ENDPOINT = "/DeviceError";
    private static final String PRODUCTION_RATE_ENDPOINT = "/ProductionRate";
    private static final String RESET_ERRORS_ENDPOINT = "/ResetErrorStatus";
    private static final String EMERGENCY_STOP_ENDPOINT = "/EmergencyStop";

    Properties props = new Properties();
    private String serverURL, ownerConnectionString, deviceConnectionString;
    private ArrayList<CloudDevice> cloudDevices;
    private ArrayList<Machine> machines = new ArrayList<>();
    private OpcUaClient client;

    public static void main(String[] args) {
        Main main = new Main();
        // Ustaw maksymaln� ilo�� urz�dze� oraz endpointy
        if (main.readProperties()) {
            main.setupProperties();
            if (main.serverURL == null || main.ownerConnectionString == null || main.deviceConnectionString == null) {
                System.out.println("Please, fill the required properties!");
                return;
            }
        } else {
            return;
        }
        // Pobierz urz�dzenia chmury
        main.getCloudDevices();
        // Po��cz z klientem OPC UA
        main.connectToClient();
        // Pobierz dane maszyn
        main.getMachines();
        // Wywo�aj metod� setupTwins() z klasy Main, aby utworzy� "bli�niacze" urz�dzenia w chmurze.
        main.setupTwins();
        main.start();


    }

    private void start(){
        Thread thread = new Thread(new MainThread());
        thread.start();
    }

    private boolean readProperties() {
        try {
            // otw�rz strumie� wej�ciowy z pliku konfiguracyjnego "config.properties"
            InputStream inputStream = new FileInputStream("config.properties");
            
            // wczytaj w�a�ciwo�ci z pliku konfiguracyjnego do obiektu props
            this.props.load(inputStream);
            return true;
        } catch (IOException ex) {
            System.out.println("Error reading config file: " + ex.getMessage());
            return false;
        }
    }

    private void setupProperties() { // pobierz warto�ci w�a�ciwo�ci server.url, connectionString.owner i connectionString.device z obiektu props.
        this.serverURL = this.props.getProperty("server.url");
        this.ownerConnectionString = this.props.getProperty("connectionString.owner");
        this.deviceConnectionString = this.props.getProperty("connectionString.device");
    }

    private void getCloudDevices() { // metoda odpowiedzialna za pobieranie listy urz�dze� z chmury.
        this.cloudDevices = new ArrayList<>();
        try {
            // Tworzymy nowy obiekt RegistryManager z po��czeniem do chmury na podstawie connection stringa w�a�ciciela.
            RegistryManager registryManager = RegistryManager.createFromConnectionString(this.ownerConnectionString);
            
            // Pobieramy list� urz�dze� z chmury, ograniczon� do warto�ci sta�ej MAX_DEVICES.
            ArrayList<Device> devices = registryManager.getDevices(MAX_DEVICES);

             // Dla ka�dego urz�dzenia na li�cie tworzymy nowy obiekt CloudDevice z jego identyfikatorem oraz kluczem prywatnym i dodajemy go do listy cloudDevices.
            for (Device device : devices) {
                this.cloudDevices.add(new CloudDevice(device.getDeviceId(), device.getPrimaryKey()));
            }

            registryManager.close();

        } catch (IOException | IotHubException e) {
            System.out.println("Error getting device list: " + e.getMessage());
        }

    }

    private void connectToClient() {
        try { 
            // Utw�rz nowego klienta OPC UA z podanym adresem URL serwera.
            this.client = OpcUaClient.create(this.serverURL);
            this.client.connect().get();
        } catch (UaException | ExecutionException | InterruptedException ex) {
            // Obs�u� wyj�tki, kt�re mog� wyst�pi� podczas procesu ��czenia.
            System.out.println("UA Server connection problem: " + ex.getMessage());
        }
    }

    private void getMachines() { // Ta metoda pobiera list� urz�dze� z serwera OPC UA.
        try {
            // Wykonaj ��danie przegl�dania w�z��w i pobierz wynik.
            BrowseResult result = this.client.browse(new BrowseDescription(
                    Identifiers.ObjectsFolder,
                    BrowseDirection.Both,
                    Identifiers.References,
                    true,
                    UInteger.valueOf(NodeClass.Unspecified.getValue()),
                    UInteger.valueOf(BrowseResultMask.BrowseName.getValue())
            )).get();

            // Przetw�rz wynik przegl�dania i utw�rz obiekty Machine dla ka�dego w�z�a "Device".
            int index = 0;
            for (ReferenceDescription rd : result.getReferences()) {
                if (Objects.requireNonNull(rd.getBrowseName().getName()).contains("Device")) {
                    UShort fullNodeId = rd.getNodeId().getNamespaceIndex();
                    DeviceClient deviceClient = new DeviceClient(this.cloudDevices.get(index).getConnectionString(), IotHubClientProtocol.MQTT);
                    Machine machine = new Machine(fullNodeId.intValue(), rd.getBrowseName().getName(), this.cloudDevices.get(index), deviceClient);
                    this.machines.add(machine);
                    index += 1;
                    Thread thread = new Thread(new EmergencyStop(machine));
                    thread.start();

                }
            }

        } catch (Exception e) {
            System.out.println("Machines browsing error: " + e.getMessage());
        }
    }

    private void setupTwins(){ // Ta metoda ustawia OPC-UA twin dla ka�dej maszyny z listy maszyn.
        for (Machine machine : this.machines){ // Przechodzi przez ka�dy obiekt Machine w li�cie maszyn i wywo�uje metod� startTwin dla ka�dej maszyny.
            startTwin(machine);
        }
    }

    private void readMachineData() {  // Ta metoda odczytuje dane z w�z��w serwera OPC UA i aktualizuje obiekty Machine.
        
        // Dla ka�dego obiektu Machine na li�cie machines wykonaj odczyt danych z w�z��w serwera OPC UA i zaktualizuj warto�ci obiektu.
        for (Machine machine : this.machines) {
            machine.setProductionStatus(Integer.parseInt(Objects.requireNonNull(readNode(machine, STATUS_ENDPOINT)).toString()));
            machine.setWorkOrderId((String) readNode(machine, WORK_ORDER_ID_ENDPOINT));
            machine.setProductionRate(Integer.parseInt(Objects.requireNonNull(readNode(machine, PRODUCTION_RATE_ENDPOINT)).toString()));
            long goodCount = Long.parseLong(Objects.requireNonNull(readNode(machine, COUNT_GOOD_ENDPOINT)).toString()) - machine.getGoodCount();
            machine.setGoodCount(goodCount);
            long badCount = Long.parseLong(Objects.requireNonNull(readNode(machine, COUNT_BAD_ENDPOINT)).toString()) - machine.getBadCount();
            machine.setBadCount(badCount);
            machine.setTemperature(Double.parseDouble(Objects.requireNonNull(readNode(machine, TEMPERATURE_ENDPOINT)).toString()));
            machine.setDeviceError(Integer.parseInt(Objects.requireNonNull(readNode(machine, DEVICE_ERROR_ENDPOINT)).toString()));
            
            // Sprawd�, czy produkcja jest uruchomiona dla danego urz�dzenia i wy�lij odpowiednie wiadomo�ci.
            if (machine.getProductionStatus() == 1) {
                System.out.println("Trying to send data with D2C message for " + machine.getDeviceName());
                this.sendD2CMessage(machine);
                setTwin(machine);
            }
        }
    }

    private Object readNode(Machine machine, String endpoint) { // Ta metoda odczytuje warto�� w�z�a serwera OPC UA dla danego urz�dzenia i punktu ko�cowego.
        
        // Tworzy si� obiekt NodeId na podstawie pe�nego identyfikatora w�z�a oraz punktu ko�cowego.
        NodeId nodeId = new NodeId(machine.getFullNodeId(), machine.getDeviceName() + endpoint);
        try {
            // Odczytuje si� warto�� w�z�a za pomoc� klienta OPC UA.
            Variant variant = this.client.readValue(Double.MAX_VALUE, TimestampsToReturn.Server, nodeId).get().getValue();
            return variant.getValue();
        } catch (Exception e) {
            System.out.println("Node values reading error: " + e.getMessage());
            return null;
        }
    }

    private void sendD2CMessage(Machine machine) {
        try {
            // Tworzymy now� instancj� klasy DeviceClient, kt�ra umo�liwi nam wys�anie wiadomo�ci do chmury.
            DeviceClient deviceClient = new DeviceClient(machine.getCloudDevice().getConnectionString(), IotHubClientProtocol.MQTT);
            deviceClient.open();

            // Tworzymy obiekt MachineDTO z danymi z maszyny.
            MachineDTO dto = new MachineDTO(machine.getCloudDevice().getDevice_id(), machine.getWorkOrderId(), machine.getProductionRate(), machine.getDeviceError(),
                    machine.getTemperature(), machine.getGoodCount(), machine.getBadCount());
            
            // Konwertujemy obiekt MachineDTO na JSON.
            Gson gson = new Gson();
            String jsonMessage = gson.toJson(dto);

            // Tworzymy now� wiadomo�� z tekstem JSON.
            Message message = new Message(jsonMessage);

            // Wysy�amy wiadomo�� asynchronicznie do chmury.
            deviceClient.sendEventAsync(message, (iotHubStatusCode, o) -> {
                System.out.println("IoT Hub responded to message with status: " + iotHubStatusCode.name() + " for " + machine.getDeviceName());
                try {
                    deviceClient.closeNow();
                } catch (IOException e) {
                    System.out.println("Error sending D2C message: " + e.getMessage());
                }
            }, null);
        } catch (Exception e) {
            System.out.println("Error sending D2C message: " + e.getMessage());
        }
    }

    public class DeviceMethodCall implements DeviceMethodCallback {

        @Override
        public DeviceMethodData call(String s, Object o, Object o1) {
            Machine machine = (Machine) o1;
            CallMethodRequest request;
            switch (s) {
                case "EmergencyStop" -> { // Obs�uga wywo�ania metody EmergencyStop.
                    System.out.println("EMERGENCY STOP RECEIVED FOR " + machine.getDeviceName());
                    request = new CallMethodRequest(
                            new NodeId(machine.getFullNodeId(), machine.getDeviceName()),
                            new NodeId(machine.getFullNodeId(), machine.getDeviceName() + EMERGENCY_STOP_ENDPOINT),
                            null);
                    client.call(request);
                }
                case "ResetErrors" -> { // Obs�uga wywo�ania metody ResetErrors.
                    System.out.println("RESET ERRORS RECEIVED FOR " + machine.getDeviceName());
                    request = new CallMethodRequest(
                            new NodeId(machine.getFullNodeId(), machine.getDeviceName()),
                            new NodeId(machine.getFullNodeId(), machine.getDeviceName() + RESET_ERRORS_ENDPOINT),
                            null);
                    client.call(request);
                }
                case "ProductionReduce" -> { // Obs�uga wywo�ania metody ProductionReduce.
                    System.out.println("PRODUCTION REDUCE RECEIVED FOR " + machine.getDeviceName());
                    Variant newValue = new Variant(machine.getProductionRate() - 10);
                    NodeId nodeId = new NodeId(machine.getFullNodeId(), machine.getDeviceName() + PRODUCTION_RATE_ENDPOINT);
                    try {
                        UaVariableNode variableNode = client.getAddressSpace().getVariableNode(nodeId);
                        variableNode.writeValue(newValue);
                    } catch (UaException ignored) {
                    }
                }
            }
            return new DeviceMethodData(200, "Ok");
        }
    }

    private void UpdateLastMaintenanceDate(Machine machine) { // Otw�rz po��czenie z urz�dzeniem.
        try {
            machine.getDeviceClient().open();
            // Utw�rz zbi�r w�a�ciwo�ci i ustaw warto�� dla "LastMaintenanceDate" na bie��cy czas.
            Set<Property> propertySet = new HashSet<>();
            propertySet.add(new Property("LastMaintenanceDate", LocalDateTime.now()));
            // Wysy�anie w�a�ciwo�ci urz�dzenia do chmury IoT przy u�yciu funkcji sendReportedProperties.
            try {
                machine.getDeviceClient().sendReportedProperties(propertySet);
            } catch (IOException e) {
                System.out.println("Error when updating last maintenance date: " + e.getMessage());
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void setTwin(Machine machine) { // Metoda ta aktualizuje bli�niacze urz�dzenie o najnowsze w�a�ciwo�ci maszyny
        try {
            machine.getDeviceClient().open();
            Set<Property> propertySet = new HashSet<>();
            // Dodaj w�a�ciwo�ci maszyny do zestawu w�a�ciwo�ci
            propertySet.add(new Property("Errors", machine.getDeviceError()));
            propertySet.add(new Property("ProductionRate", machine.getProductionRate()));
            // Dodaj w�a�ciwo�� LastErrorDate, je�li b��d urz�dzenia nie jest zerowy
            if (machine.getDeviceError() != 0) {
                propertySet.add(new Property("LastErrorDate", Instant.now().getEpochSecond()));
            }
            try {
                // Wy�lij zestaw w�a�ciwo�ci, aby zaktualizowa� bli�niacze urz�dzenie
                machine.getDeviceClient().sendReportedProperties(propertySet);
            } catch (IOException e) {
                // Je�li wyst�pi b��d, ponawiamy pr�b� uruchomienia bli�niaczego urz�dzenia
                System.out.println("Error when updating device twin: " + e.getMessage());
                startTwin(machine);
            }

        } catch (IOException e) {
            System.out.println("Error when updating device twin: " + e.getMessage());
            startTwin(machine);
        }
    }

    private void startTwin(Machine machine) { // Ta metoda uruchamia bli�niacze urz�dzenie dla podanej maszyny
        try {
            DeviceClient deviceClient = machine.getDeviceClient();
            // Otw�rz klienta urz�dzenia i czekaj na zako�czenie
            CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                try {
                    deviceClient.open();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                return true;
            });
            future.get();
            // Uruchom bli�niacze urz�dzenie i poczekaj na zako�czenie
            future = CompletableFuture.supplyAsync(() -> {
                try {
                    deviceClient.startDeviceTwin((iotHubStatusCode, o) -> {
                        System.out.println("Twin has been changed for " + machine.getDeviceName());
                    }, null, (o, o2, o3) -> {
                    }, null);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                return true;
            });
            future.get();
            // Zamknij
            deviceClient.closeNow();
        } catch (IOException | ExecutionException | InterruptedException e) {
            System.out.println("Error when start device twin: " + e.getMessage());
            startTwin(machine);
        }
    }

    public class EmergencyStop implements Runnable {

        private Machine machine;

        public EmergencyStop(Machine machine) {
            this.machine = machine;
        }
        // Implementacja metody run() interfejsu Runnable
        public void run() {
            try {
                this.machine.getDeviceClient().open();
                this.machine.getDeviceClient().subscribeToDeviceMethod(new DeviceMethodCall(), machine, (iotHubStatusCode, o) -> {
                }, machine);
                Thread.sleep(Long.MAX_VALUE);
            } catch (IOException | InterruptedException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }

        public Machine getMachine() {
            return machine;
        }

        public void setMachine(Machine machine) {
            this.machine = machine;
        }
    }

    public class MainThread implements Runnable{

        @Override
        public void run() {
            while (true) {
                readMachineData();
                try {
                    Thread.sleep(5000);
                } catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
                }
            }
        }
    }

}

