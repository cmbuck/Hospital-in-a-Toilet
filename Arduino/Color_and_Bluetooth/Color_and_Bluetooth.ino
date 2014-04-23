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

#define DEBUG 1

int bluetoothTx = 2;  // TX-O pin of bluetooth mate, Arduino D2
int bluetoothRx = 3;  // RX-I pin of bluetooth mate, Arduino D3
int sensorRx = 4;     // Rx pin on arduino for sensor, D4
int sensorTx = 5;     // Tx pin on arduino for sensor, D5
int LEDpin = 13;
int waiting = 0;
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
  pinMode(LEDpin, OUTPUT);
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
  while(bluetooth.available())  // If the bluetooth sent any characters
  {
    // Send any characters the bluetooth prints to the serial monitor
    Serial.print((char)bluetooth.read());  
  }
  
  while(Serial.available())  // If stuff was typed in the serial monitor
  {
    // Send any characters the Serial monitor prints to the bluetooth
    //bluetooth.print((char)Serial.read());
    //sensor.print((char)Serial.read());
    
    char inchar = (char)Serial.read(); 
    inputstring += inchar; 
    if (inchar == '\r') {
      input_stringcomplete = true;
      waiting = 1;
    } 
  }
  
  
  if (input_stringcomplete){ 
    while (waiting)
    {
      sensor.print(inputstring); 
      delay(1300);
      if (sensor.available())
      {
        waiting = 0;
      }
    }
    inputstring = ""; 
    input_stringcomplete = false; 
    
  }
  
  while (sensor.available()) { 
  //if (1) {
    char inchar = (char)sensor.read(); 
    //char inchar = '9';
    sensorstring += inchar; 
    inchar = '\r';
    sensorstring += inchar; 
    if (inchar == '\r') 
    {
      sensor_stringcomplete = true;
      digitalWrite(LEDpin, !digitalRead(LEDpin));
    } 
    delay(100);
  }
  
  if (sensor_stringcomplete){ 
    //Serial.print(sensorstring); 
    bluetooth.println(sensorstring);
    if (DEBUG)
      Serial.println(sensorstring);
    sensorstring = ""; 
    sensor_stringcomplete = false; 
  }
  // and loop forever and ever!
}
