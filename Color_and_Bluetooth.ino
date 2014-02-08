/*
  Example Bluetooth Serial Passthrough Sketch
 by: Jim Lindblom
 SparkFun Electronics
 date: February 26, 2013
 license: Public domain

 This example sketch converts an RN-42 bluetooth module to
 communicate at 9600 bps (from 115200), and passes any serial
 data between Serial Monitor and bluetooth module.
 */
#include <SoftwareSerial.h>  

int bluetoothTx = 2;  // TX-O pin of bluetooth mate, Arduino D2
int bluetoothRx = 3;  // RX-I pin of bluetooth mate, Arduino D3
int sensorRx = 4;     // Rx pin on arduino for sensor, D4
int sensorTx = 5;     // Tx pin on arduino for sensor, D5
String inputstring = "";
String sensorstring = "";
boolean input_stringcomplete = false;
boolean sensor_stringcomplete = false;

SoftwareSerial bluetooth(bluetoothTx, bluetoothRx);
SoftwareSerial sensor(sensorRx, sensorTx);

void setup()
{
  Serial.begin(9600);  // Begin the serial monitor at 9600bps

  bluetooth.begin(115200);  // The Bluetooth Mate defaults to 115200bps
  bluetooth.print("$");  // Print three times individually
  bluetooth.print("$");
  bluetooth.print("$");  // Enter command mode
  delay(100);  // Short delay, wait for the Mate to send back CMD
  bluetooth.println("U,9600,N");  // Temporarily Change the baudrate to 9600, no parity
  // 115200 can be too fast at times for NewSoftSerial to relay the data reliably
  bluetooth.begin(9600);  // Start bluetooth serial at 9600
  sensor.begin(38400);
  inputstring.reserve(10); 
  sensorstring.reserve(30); 
}

//Interrupt routine
/*
void serialEvent() { 
  char inchar = (char)Serial.read(); 
  inputstring += inchar; 
  if(inchar == '\r') {input_stringcomplete = true;} 
} */

void loop()
{
  if(bluetooth.available())  // If the bluetooth sent any characters
  {
    // Send any characters the bluetooth prints to the serial monitor
    Serial.print((char)bluetooth.read());  
  }
  
  if(Serial.available())  // If stuff was typed in the serial monitor
  {
    // Send any characters the Serial monitor prints to the bluetooth
    //bluetooth.print((char)Serial.read());
    sensor.print((char)Serial.read());
  }
  
  /*
  if (input_stringcomplete){ 
    sensor.print(inputstring); 
    inputstring = ""; 
    input_stringcomplete = false; 
  }*/
  
  while (sensor.available()) { 
    char inchar = (char)sensor.read(); 
    sensorstring += inchar; 
    if (inchar == '\r') {sensor_stringcomplete = true;} 
  }
  
  if (sensor_stringcomplete){ 
    //Serial.print(sensorstring); 
    bluetooth.print(sensorstring);
    sensorstring = ""; 
    sensor_stringcomplete = false; 
  }
  // and loop forever and ever!
}
