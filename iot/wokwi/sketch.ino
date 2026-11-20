#include <WiFi.h>
#include <HTTPClient.h>

const char* WIFI_SSID = "Wokwi-GUEST";
const char* WIFI_CLAVE = "";

const char* THINGSPEAK_WRITE_KEY = "TU_WRITE_API_KEY";

const int PIN_TRIG = 5;
const int PIN_ECHO = 18;

const float DISTANCIA_VACIO_CM = 120.0;
const float DISTANCIA_LLENO_CM = 20.0;

const unsigned long INTERVALO_ENVIO_MS = 20000;
const int LECTURAS_POR_MEDICION = 5;

unsigned long ultimoEnvio = 0;

void conectarWifi() {
  if (WiFi.status() == WL_CONNECTED) return;
  Serial.print("Conectando a WiFi");
  WiFi.begin(WIFI_SSID, WIFI_CLAVE, 6);
  while (WiFi.status() != WL_CONNECTED) {
    delay(250);
    Serial.print(".");
  }
  Serial.println(" conectado");
}

float leerDistanciaCm() {
  digitalWrite(PIN_TRIG, LOW);
  delayMicroseconds(2);
  digitalWrite(PIN_TRIG, HIGH);
  delayMicroseconds(10);
  digitalWrite(PIN_TRIG, LOW);
  unsigned long duracion = pulseIn(PIN_ECHO, HIGH, 30000);
  if (duracion == 0) return -1;
  return duracion * 0.0343 / 2;
}

float medirDistanciaCm() {
  float suma = 0;
  int validas = 0;
  for (int i = 0; i < LECTURAS_POR_MEDICION; i++) {
    float d = leerDistanciaCm();
    if (d > 0) {
      suma += d;
      validas++;
    }
    delay(60);
  }
  return validas == 0 ? -1 : suma / validas;
}

float porcentajeDeLlenado(float distanciaCm) {
  float porcentaje = (DISTANCIA_VACIO_CM - distanciaCm) / (DISTANCIA_VACIO_CM - DISTANCIA_LLENO_CM) * 100.0;
  return constrain(porcentaje, 0.0, 100.0);
}

void enviarAThingSpeak(float porcentaje, float distanciaCm) {
  HTTPClient http;
  String url = String("http://api.thingspeak.com/update?api_key=") + THINGSPEAK_WRITE_KEY +
               "&field1=" + String(porcentaje, 1) +
               "&field2=" + String(distanciaCm, 1);
  http.begin(url);
  int codigo = http.GET();
  String respuesta = http.getString();
  http.end();
  if (codigo == 200 && respuesta != "0") {
    Serial.println("Enviado a ThingSpeak (entrada " + respuesta + ")");
  } else {
    Serial.printf("No se pudo enviar (HTTP %d, respuesta \"%s\")\n", codigo, respuesta.c_str());
  }
}

void setup() {
  Serial.begin(115200);
  pinMode(PIN_TRIG, OUTPUT);
  pinMode(PIN_ECHO, INPUT);
  conectarWifi();
}

void loop() {
  if (ultimoEnvio != 0 && millis() - ultimoEnvio < INTERVALO_ENVIO_MS) return;
  ultimoEnvio = millis();

  float distancia = medirDistanciaCm();
  if (distancia < 0) {
    Serial.println("Sensor sin lectura: revisa las conexiones");
    return;
  }
  float porcentaje = porcentajeDeLlenado(distancia);
  Serial.printf("Distancia: %.1f cm · Nivel: %.1f %%\n", distancia, porcentaje);

  conectarWifi();
  enviarAThingSpeak(porcentaje, distancia);
}
