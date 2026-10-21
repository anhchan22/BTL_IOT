#include <ESP8266WiFi.h>
#include <PubSubClient.h>
#include <DHT.h>
#include <Wire.h>
#include <BH1750.h>
#include <ArduinoJson.h>
#include "secrets.h"

const char* mqtt_user = MQTT_USERNAME;
const char* mqtt_pass = MQTT_PASSWORD;

const char* ssid = WIFI_SSID;
const char* password = WIFI_PASSWORD;

// const char* mqtt_server = "broker.emqx.io";
// const char* mqtt_server = "10.179.205.92";
const char* mqtt_server = "192.168.1.15";

// Keep these aligned with the BE MQTT configuration (raw MQTT/TCP listener).
const int mqtt_port = 2205;
const char* topic_sensor  = "data_sensors";
const char* topic_control = "device_control";
const char* topic_status  = "device_response";
#define DHTPIN D5        // GPIO14; keep boot-strap GPIO15 (D8) free during reset.
#define DHTTYPE DHT11
DHT dht(DHTPIN, DHTTYPE);
#define LIGHT_SDA_PIN D7
#define LIGHT_SCL_PIN D6
BH1750 lightMeter;
bool lightSensorReady = false;
#define LED1_PIN D0      
#define LED2_PIN D1    
#define LED3_PIN D2    

WiFiClient espClient;
PubSubClient client(espClient);

unsigned long lastMsgTime = 0;
const int SENSOR_READ_INTERVAL = 2000;

void setup_wifi() {
  delay(10);
  Serial.println();
  Serial.print("Đang kết nối WiFi tới: ");
  Serial.println(ssid);
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("");
  Serial.println("Đã kết nối WiFi thành công!");
  Serial.print("Địa chỉ IP: ");
  Serial.println(WiFi.localIP());
}
void callback(char* topic, byte* payload, unsigned int length) {
  Serial.print("Có tin nhắn đến từ topic [");
  Serial.print(topic);
  Serial.print("]: ");
  String messageTemp;
  for (int i = 0; i < length; i++) {
    messageTemp += (char)payload[i];
  }
  Serial.println(messageTemp);
  StaticJsonDocument<200> doc;
  DeserializationError error = deserializeJson(doc, messageTemp);
  if (error) {
    Serial.print("Lỗi đọc JSON: ");
    Serial.println(error.c_str());
    return;
  }
  const int device_id = doc["deviceId"] | -1;
  const char* action = doc["action"];
  if (action == nullptr) {
    Serial.println("Loi: thieu truong action trong lenh MQTT.");
    return;
  }
  String strAction = String(action);
  strAction.toUpperCase();
  const bool turnOn = strAction == "ON";
  const bool validAction = turnOn || strAction == "OFF";
  const bool validDevice = device_id >= 0 && device_id <= 3;
  const bool isSuccess = validAction && validDevice;
  const uint8_t outputLevel = turnOn ? HIGH : LOW;
  if (validAction && validDevice) {
    if (device_id == 0 || device_id == 1) digitalWrite(LED1_PIN, outputLevel);
    if (device_id == 0 || device_id == 2) digitalWrite(LED2_PIN, outputLevel);
    if (device_id == 0 || device_id == 3) digitalWrite(LED3_PIN, outputLevel);
  }

  if (isSuccess) {
    StaticJsonDocument<200> responseDoc;
    responseDoc["deviceId"] = device_id;
    responseDoc["action"] = strAction;
    responseDoc["status"] = "success";
   
    char responseBuffer[200];
    serializeJson(responseDoc, responseBuffer);
   
    client.publish(topic_status, responseBuffer);
    Serial.println("Đã gửi phản hồi: " + String(responseBuffer));
  } else {
    Serial.println("Lỗi: Không tìm thấy thiết bị hoặc action không hợp lệ.");
  }
}
void reconnect() {
  while (!client.connected()) {
    Serial.print("Đang cố gắng kết nối MQTT Broker...");
    String clientId = "ESP8266Client-";
    clientId += String(random(0, 0xffff), HEX);

    if (client.connect(clientId.c_str(), mqtt_user, mqtt_pass)) {
      Serial.println("Đã kết nối MQTT thành công!");
      client.subscribe(topic_control);
      Serial.println("Đã Subscribe topic: " + String(topic_control));
    } else {
      Serial.print("Thất bại, mã lỗi (rc)=");
      Serial.print(client.state());
      Serial.println(". Thử lại sau 5 giây...");
      delay(5000);
    }
  }
}
void setup() {
  Serial.begin(115200);
  randomSeed(ESP.getChipId() ^ micros());
  pinMode(LED1_PIN, OUTPUT);
  pinMode(LED2_PIN, OUTPUT);
  pinMode(LED3_PIN, OUTPUT);
  digitalWrite(LED1_PIN, LOW);
  digitalWrite(LED2_PIN, LOW);
  digitalWrite(LED3_PIN, LOW);

  dht.begin();
  Wire.begin(LIGHT_SDA_PIN, LIGHT_SCL_PIN);
  lightSensorReady = lightMeter.begin(BH1750::CONTINUOUS_HIGH_RES_MODE);
  if (!lightSensorReady) {
    Serial.println("BH1750 chua duoc phat hien; se thu ket noi lai.");
  }
  setup_wifi();
  client.setServer(mqtt_server, mqtt_port);
  client.setCallback(callback);
}
void loop() {
  if (!client.connected()) {
    reconnect();
  }
  client.loop();

  unsigned long now = millis();
  if (now - lastMsgTime > SENSOR_READ_INTERVAL) {
    lastMsgTime = now;

    float h = dht.readHumidity();
    float t = dht.readTemperature();
    if (!lightSensorReady) {
      lightSensorReady = lightMeter.begin(BH1750::CONTINUOUS_HIGH_RES_MODE);
    }
    float lux = lightSensorReady ? lightMeter.readLightLevel() : 0.0;
    if (!isfinite(lux) || lux < 0.0) lux = 0.0;
    const int rain = random(0, 101); // Simulated rainfall percentage (0-100%).

    if (isnan(h) || isnan(t)) {
      Serial.println("Lỗi: Không thể đọc dữ liệu từ cảm biến DHT!");
      return;
    }

    Serial.print("Nhiệt độ: "); Serial.print(t);
    Serial.print("°C | Độ ẩm: "); Serial.print(h);
    Serial.print("% | Ánh sáng: "); Serial.print(lux, 1);
    Serial.print(" Lux | Độ mưa: "); Serial.print(rain); Serial.println("%");

    StaticJsonDocument<200> doc;
    doc["temperature"] = t;
    doc["humidity"] = h;
    doc["light"] = round(lux * 10) / 10.0;
    doc["rain"] = rain;

    char msgBuffer[200];
    serializeJson(doc, msgBuffer);

    client.publish(topic_sensor, msgBuffer);
    Serial.println("Đã gửi MQTT: " + String(msgBuffer));
    Serial.println("-----------------------------------------");
  }
}
