/*
 SCP1000 Barometric Pressure Sensor Display

 Shows the output of a Barometric Pressure Sensor on a
 Uses the SPI library. For details on the sensor, see:
 http://www.sparkfun.com/commerce/product_info.php?products_id=8161
 http://www.vti.fi/en/support/obsolete_products/pressure_sensors/

 This sketch adapted from Nathan Seidle's SCP1000 example for PIC:
 http://www.sparkfun.com/datasheets/Sensors/SCP1000-Testing.zip

 Circuit:
 SCP1000 sensor attached to pins 6, 7, 10 - 13:
 DRDY: pin 6
 CSB: pin 7
 MOSI: pin 11
 MISO: pin 12
 SCK: pin 13

 created 31 July 2010
 modified 14 August 2010
 by Tom Igoe
 */

// the sensor communicates using SPI, so include the library:
#include <SPI.h>
#include <math.h>

//Sensor's memory register addresses:
const int PRESSURE = 0x1F;      //3 most significant bits of pressure
const int PRESSURE_LSB = 0x20;  //16 least significant bits of pressure
const int TEMPERATURE = 0x21;   //16 bit temperature reading
const byte READ = 0b11111100;     // SCP1000's read command
const byte WRITE = 0b00000010;   // SCP1000's write command
float pressure;
long unsigned int output;

// pins used for the connection with the sensor
// the other you need are controlled by the SPI library):
const int dataReadyPin = 6;
const int chipSelectPin1 = 9;
const int chipSelectPin2 = 8;

void setup() {
  Serial.begin(9600);

  // start the SPI library:
  SPI.begin();

  // initalize the  data ready and chip select pins:
  //pinMode(dataReadyPin, INPUT);
  pinMode(chipSelectPin1, OUTPUT);
  pinMode(chipSelectPin2, OUTPUT);

  //Configure SCP1000 for low noise configuration:
  //writeRegister(0x02, 0x2D);
  //writeRegister(0x01, 0x03);
  //writeRegister(0x03, 0x02);
  // give the sensor time to set up:
  delay(100);
}


void loop() {
  //Select High Resolution Mode
  //writeRegister(0x03, 0x0A);
 
  delay(1000);
  digitalWrite(chipSelectPin1, LOW);
  delay(10);

   unsigned int data1 = SPI.transfer(0x00);
   unsigned int data2 = SPI.transfer(0x00);
   unsigned int data3 = SPI.transfer(0x00);
   unsigned int data4 = SPI.transfer(0x00);
   
   unsigned int first = data1;
   unsigned int second = data2;

   Serial.print("Data from sensor1: ");
   Serial.print(data1, HEX);
   Serial.print(" ");
   Serial.print(data2, HEX);
   Serial.print(" ");
   Serial.print(data3, HEX);
   Serial.print(" ");
   Serial.println(data4, HEX);

   output = first*(pow(2,8)) + second;
   Serial.println(output, DEC);
   
   pressure = (1.0 - 0.0)*(output - 1638.0)/13107.0 + 0.0;
   Serial.println(pressure,DEC);
   
   digitalWrite(chipSelectPin1, HIGH);
   
   delay(10);
   digitalWrite(chipSelectPin2, LOW);
   delay(10);

   data1 = SPI.transfer(0x00);
   data2 = SPI.transfer(0x00);
   data3 = SPI.transfer(0x00);
   data4 = SPI.transfer(0x00);
   
   first = data1;
   second = data2;

   Serial.print("Data from sensor2: ");
   Serial.print(data1, HEX);
   Serial.print(" ");
   Serial.print(data2, HEX);
   Serial.print(" ");
   Serial.print(data3, HEX);
   Serial.print(" ");
   Serial.println(data4, HEX);

   output = first*(pow(2,8)) + second;
   Serial.println(output, DEC);
   
   pressure = (1.0 - 0.0)*(output - 1638.0)/13107.0 + 0.0;
   Serial.println(pressure,DEC);
   
   digitalWrite(chipSelectPin2, HIGH);

}

