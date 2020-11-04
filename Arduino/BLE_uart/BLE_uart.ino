/*
    Video: https://www.youtube.com/watch?v=oCMOYS71NIU
    Based on Neil Kolban example for IDF: https://github.com/nkolban/esp32-snippets/blob/master/cpp_utils/tests/BLE%20Tests/SampleNotify.cpp
    Ported to Arduino ESP32 by Evandro Copercini

   Create a BLE server that, once we receive a connection, will send periodic notifications.
   The service advertises itself as: 6E400001-B5A3-F393-E0A9-E50E24DCCA9E
   Has a characteristic of: 6E400002-B5A3-F393-E0A9-E50E24DCCA9E - used for receiving data with "WRITE" 
   Has a characteristic of: 6E400003-B5A3-F393-E0A9-E50E24DCCA9E - used to send data with  "NOTIFY"

   The design of creating the BLE server is:
   1. Create a BLE Server
   2. Create a BLE Service
   3. Create a BLE Characteristic on the Service
   4. Create a BLE Descriptor on the characteristic
   5. Start the service.
   6. Start advertising.

   In this example rxValue is the data received (only accessible inside that function).
   And txValue is the data to be sent, in this example just a byte incremented every second. 
*/
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#include <MQ135.h>

BLEServer *pServer = NULL;
BLECharacteristic * pTxCharacteristic;
bool deviceConnected = false;
bool oldDeviceConnected = false;
uint16_t txValue = 0;

uint32_t counter = 0;

int sensorPin = A0;    // select the input pin for the potentiometer
uint16_t sensorValue = 0;  // variable to store the value coming from the sensor
uint8_t sensorArray[2] = {0,0};

MQ135 gasSensor = MQ135(A0);
// See the following for generating UUIDs:
// https://www.uuidgenerator.net/

#define SERVICE_UUID           "6E400001-B5A3-F393-E0A9-E50E24DCCA9E" // UART service UUID
#define CHARACTERISTIC_UUID_RX "6E400002-B5A3-F393-E0A9-E50E24DCCA9E"
#define CHARACTERISTIC_UUID_TX "6E400003-B5A3-F393-E0A9-E50E24DCCA9E"



class MyServerCallbacks: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
      deviceConnected = true;
    };

    void onDisconnect(BLEServer* pServer) {
      deviceConnected = false;
    }
};

class MyCallbacks: public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic *pCharacteristic) {
      std::string rxValue = pCharacteristic->getValue();

      if (rxValue.length() > 0) {
//        Serial.println("*********");
//        Serial.print("Received Value: ");
//        for (int i = 0; i < rxValue.length(); i++)
//          Serial.print(rxValue[i]);
//
//        Serial.println();
//        Serial.println("*********");
      }
    }
};


void setup() {
  Serial.begin(115200);

  // Create the BLE Device
  BLEDevice::init("UART Service");

  // Create the BLE Server
  pServer = BLEDevice::createServer();
  pServer->setCallbacks(new MyServerCallbacks());

  // Create the BLE Service
  BLEService *pService = pServer->createService(SERVICE_UUID);

  // Create a BLE Characteristic
  pTxCharacteristic = pService->createCharacteristic(
										CHARACTERISTIC_UUID_TX,
										BLECharacteristic::PROPERTY_NOTIFY
									);
                      
  pTxCharacteristic->addDescriptor(new BLE2902());

  BLECharacteristic * pRxCharacteristic = pService->createCharacteristic(
											 CHARACTERISTIC_UUID_RX,
											BLECharacteristic::PROPERTY_WRITE
										);

  pRxCharacteristic->setCallbacks(new MyCallbacks());

  // Start the service
  pService->start();

  // Start advertising
  pServer->getAdvertising()->start();
  Serial.println("Waiting a client connection to notify...");
}

void loop() {

//float resistance = ((1023./(float)analogRead(sensorPin)) * 5. - 1.)*RLOAD;

uint16_t sensorValue = analogRead(sensorPin);

uint16_t sendValue = ((sensorValue >> 8) & 0b0000000011111111) | ((sensorValue << 8) & 0b1111111100000000);

std::string sensorValueString((char*)&sensorValue, 1);

//Serial.print(counter);
//Serial.print(",");
//Serial.println(gasSensor.getRZero());

Serial.print("Raw value: ");
Serial.print(sensorValue);
Serial.print(" PPM: ");
Serial.print(gasSensor.getCO2PPM());
Serial.print(" Rzero: ");
Serial.println(gasSensor.getRZeroCO2());

//Serial.println(sensorValue);
//Serial.println("PPM:");
//Serial.println(gasSensor.getPPM());

delay(200);

    if (deviceConnected) {
        // Send txValue when testing the full range of the app
        // Send sensorValue to send the value of the sensor to the app
        pTxCharacteristic->setValue(sensorValue);
//        pTxCharacteristic->setValue(txValue); // Sending a test value to the app
        
        pTxCharacteristic->notify();

        // Reset the "test" value
        if(txValue++ > 500) txValue = 0;
        
		    
		    delay(10); // bluetooth stack will go into congestion, if too many packets are sent
	}

    // disconnecting
    if (!deviceConnected && oldDeviceConnected) {
        delay(500); // give the bluetooth stack the chance to get things ready
        pServer->startAdvertising(); // restart advertising
        Serial.println("start advertising");
        oldDeviceConnected = deviceConnected;
    }
    // connecting
    if (deviceConnected && !oldDeviceConnected) {
		// do stuff here on connecting
        oldDeviceConnected = deviceConnected;
    }
  counter++;
}
