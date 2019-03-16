#include <SoftwareSerial.h>


const int trigPin = 3; // Trigger
const int echo1Pin = 5; // Echo 1
const int echo2Pin = 6; // Echo 2

const int bluetooth_TX_PIN = 9;
const int bluetooth_RX_PIN = 10;

long duration, cm, inches;

SoftwareSerial bluetooth(bluetooth_RX_PIN, bluetooth_TX_PIN);

void setup() {
  Serial.begin (9600);
  bluetooth.begin(9600);
  //Define inputs and outputs
  pinMode(trigPin, OUTPUT);
  pinMode(echo1Pin, INPUT);
  pinMode(echo2Pin, INPUT);
}

void loop() {
  // Give a short LOW pulse beforehand to ensure a clean HIGH pulse:
    pulseTrigger();



  // Read the signal from sensor 1
  pinMode(echo1Pin, INPUT);
  duration = pulseIn(echo1Pin, HIGH, 500000);
  // Convert the time into a distance
  cm = (duration / 2) / 29.1;
  cm = constrain(cm, 2, 300);
  //if(cm==2) cm=1000;
  Serial.print("s1");
  Serial.println(cm);
  bluetooth.print("#");
  bluetooth.print(cm);
  bluetooth.print("!");


  
  delayMicroseconds(900000);



  // Give a short LOW pulse beforehand to ensure a clean HIGH pulse:
   pulseTrigger();

  // Read the signal from sensor 2
  pinMode(echo2Pin, INPUT);
  duration = pulseIn(echo2Pin, HIGH, 500000);
  cm = (duration / 2) / 29.1;
  cm = constrain(cm, 2, 300);
  //if(cm==2) cm=1000;
  Serial.print("s2");
  Serial.println(cm);
  bluetooth.print(cm);
  bluetooth.print("*");


  
  delayMicroseconds(900000);
}




void pulseTrigger() {
  digitalWrite(trigPin, LOW);
  delayMicroseconds(5);
  digitalWrite(trigPin, HIGH);
  delayMicroseconds(10);
  digitalWrite(trigPin, LOW);

}
