// Q-Core ESP32 firmware. NOT TESTED ON HARDWARE: check pins, power and libraries for your build.
// Libraries: ArduinoJson (v7), Adafruit NeoPixel, Adafruit GFX, Adafruit SSD1306.
// The signal passes through five layers, top to bottom, like the dashboard:
//   stage 1 room temperature, 2 4K, 3 1K, 4 100 mK, 5 the 10 mK qubit chip (gates, then the result)
// LED strip: 30 LEDs = 5 layers x 6 LEDs. Done layers turn green, the active layer pulses.
#include <WiFi.h>
#include <WebServer.h>
#include <ArduinoJson.h>
#include <Adafruit_NeoPixel.h>
#include <Wire.h>
#include <Adafruit_GFX.h>
#include <Adafruit_SSD1306.h>

const char* WIFI_SSID = "YOUR_WIFI";
const char* WIFI_PASS = "YOUR_PASSWORD";
const int LED_PIN = 5, LED_COUNT = 30, PER_LAYER = 6, STEP_PIN = 18, DIR_PIN = 19;   // OLED on SDA=21, SCL=22

Adafruit_NeoPixel strip(LED_COUNT, LED_PIN, NEO_GRB + NEO_KHZ800);
Adafruit_SSD1306 oled(128, 64, &Wire, -1);
WebServer server(80);
const char* LAYER[] = {"Idle", "Room temp", "4K stage", "1K stage", "100 mK stage", "10 mK core"};

String mode = "idle", gate = "", measured = "";
int stage = 0, stepNo = 0, steps = 0;
String labels[8]; float probs[8]; int nStates = 0;
unsigned long lastStepUs = 0, lastLed = 0, lastEvent = 0;

bool resultShown() { return stage == 5 && measured.length() > 0; }

unsigned long stepIntervalUs() {            // smaller = faster rotation
  if (stage == 0) return 8000;
  if (stage < 5) return 4000;
  return resultShown() ? 9000 : 1800;       // gates on the chip: fast, result: slow
}

void drawOled() {
  oled.clearDisplay(); oled.setTextSize(1); oled.setTextColor(SSD1306_WHITE);
  oled.setCursor(0, 0); oled.print("Q-CORE "); oled.println(LAYER[stage]);
  for (int i = 0; i < nStates && i < 4; i++) {
    oled.setCursor(0, 12 + i * 10);
    oled.print("|"); oled.print(labels[i]); oled.print(">  "); oled.print((int)(probs[i] * 100 + 0.5)); oled.println("%");
  }
  oled.setCursor(0, 54);
  if (resultShown()) { oled.print("Result |"); oled.print(measured); oled.print(">"); }
  else if (stage == 5 && stepNo > 0) { oled.print("Gate "); oled.print(stepNo); oled.print("/"); oled.print(steps); oled.print(" "); oled.print(gate); }
  else if (stage > 0) { oled.print("Layer "); oled.print(stage); oled.print(" of 5"); }
  else oled.print("Idle");
  oled.display();
}

void handleState() {
  JsonDocument doc;
  if (deserializeJson(doc, server.arg("plain"))) { server.send(400, "text/plain", "bad json"); return; }
  stage = doc["stage"] | 0; mode = String((const char*)(doc["mode"] | "idle"));
  stepNo = doc["step"] | 0; steps = doc["steps"] | 0;
  gate = String((const char*)(doc["gate"] | "")); measured = String((const char*)(doc["measured"] | ""));
  nStates = 0;
  for (JsonPair kv : doc["p"].as<JsonObject>()) { if (nStates < 8) { labels[nStates] = String(kv.key().c_str()); probs[nStates++] = kv.value().as<float>(); } }
  lastEvent = millis(); drawOled();
  server.send(200, "text/plain", "ok");
}

uint32_t scaled(uint8_t r, uint8_t g, uint8_t b, float k) { return strip.Color(r * k, g * k, b * k); }

void drawLeds() {
  uint8_t R = 40, G = 220, B = 255;                                  // cyan while computing
  if (mode == "superposition") { R = 220; G = 80; B = 245; }
  float pulse = 0.5 + 0.5 * sin(millis() / 160.0);
  for (int i = 0; i < LED_COUNT; i++) {
    int layer = i / PER_LAYER;                                       // 0..4, top to bottom
    uint32_t c;
    if (stage == 0) c = scaled(20, 90, 255, 0.35 + 0.25 * sin(millis() / 600.0));   // idle: breathing blue
    else if (resultShown()) c = strip.Color(40, 230, 110);                          // result: all green
    else if (layer < stage - 1) c = strip.Color(40, 230, 110);                      // finished layers: green
    else if (layer == stage - 1) c = scaled(R, G, B, 0.3 + 0.7 * pulse);            // active layer: pulse
    else c = strip.Color(4, 6, 10);                                                 // not reached yet
    strip.setPixelColor(i, c);
  }
  strip.show();
}

void setup() {
  pinMode(STEP_PIN, OUTPUT); pinMode(DIR_PIN, OUTPUT); digitalWrite(DIR_PIN, HIGH);
  strip.begin(); strip.setBrightness(90);
  Wire.begin(21, 22); oled.begin(SSD1306_SWITCHCAPVCC, 0x3C);
  oled.clearDisplay(); oled.setTextColor(SSD1306_WHITE); oled.setCursor(0, 0); oled.println("Q-CORE: connecting..."); oled.display();
  WiFi.begin(WIFI_SSID, WIFI_PASS);
  while (WiFi.status() != WL_CONNECTED) delay(300);
  oled.clearDisplay(); oled.setCursor(0, 0); oled.println("Q-CORE ready"); oled.println(WiFi.localIP()); oled.display();  // put this IP in application.properties
  server.on("/state", HTTP_POST, handleState); server.begin();
}

void loop() {
  server.handleClient();
  unsigned long now = micros();
  if (now - lastStepUs >= stepIntervalUs()) {                       // non-blocking stepper pulse
    lastStepUs = now; digitalWrite(STEP_PIN, HIGH); delayMicroseconds(5); digitalWrite(STEP_PIN, LOW);
  }
  if (millis() - lastLed > 40) { lastLed = millis(); drawLeds(); }
  if (stage != 0 && millis() - lastEvent > 25000) { stage = 0; mode = "idle"; measured = ""; drawOled(); }   // fail-safe
}
