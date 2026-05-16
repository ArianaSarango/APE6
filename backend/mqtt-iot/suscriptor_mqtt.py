import json
import sqlite3
from datetime import datetime
# pyrefly: ignore [missing-import]
import paho.mqtt.client as mqtt

# --- Configuración MQTT ---
BROKER = "localhost"
PUERTO = 1883
DB_PATH = "telemetria.db"

# Umbrales de alerta
TEMP_MAX_C = 4.0
FUEL_MIN_PCT = 20.0

def inicializar_base_datos():
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute("""CREATE TABLE IF NOT EXISTS gps_data (id INTEGER PRIMARY KEY AUTOINCREMENT, vehicle_id TEXT, lat REAL, lng REAL, speed REAL, timestamp TEXT)""")
    cursor.execute("""CREATE TABLE IF NOT EXISTS temp_data (id INTEGER PRIMARY KEY AUTOINCREMENT, vehicle_id TEXT, temperature REAL, unit TEXT, timestamp TEXT)""")
    cursor.execute("""CREATE TABLE IF NOT EXISTS fuel_data (id INTEGER PRIMARY KEY AUTOINCREMENT, vehicle_id TEXT, fuel_level REAL, unit TEXT, timestamp TEXT)""")
    conn.commit()
    conn.close()
    print("Base de datos inicializada.")

def al_recibir_mensaje(cliente, datos_usuario, mensaje):
    topic = mensaje.topic
    try:
        payload = json.loads(mensaje.payload.decode("utf-8"))
    except Exception as e:
        print(f"Error decodificando payload: {e}")
        return

    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()

    if "/gps" in topic:
        cursor.execute("INSERT INTO gps_data (vehicle_id, lat, lng, speed, timestamp) VALUES (?, ?, ?, ?, ?)",
                       (payload["vehicleId"], payload["lat"], payload["lng"], payload["speed"], payload["timestamp"]))
        print(f"[GPS] {payload['vehicleId']} actualizado.")
    elif "/temperatura" in topic:
        if payload["temperature"] > TEMP_MAX_C:
            print(f"[ALERTA TEMP] {payload['vehicleId']}: {payload['temperature']}°C")
        cursor.execute("INSERT INTO temp_data (vehicle_id, temperature, unit, timestamp) VALUES (?, ?, ? , ?)",
                       (payload["vehicleId"], payload["temperature"], payload["unit"], payload["timestamp"]))
    elif "/combustible" in topic:
        if payload["fuelLevel"] < FUEL_MIN_PCT:
            print(f"[ALERTA FUEL] {payload['vehicleId']}: {payload['fuelLevel']}%")
        cursor.execute("INSERT INTO fuel_data (vehicle_id, fuel_level, unit, timestamp) VALUES (?, ?, ?, ?)",
                       (payload["vehicleId"], payload["fuelLevel"], payload["unit"], payload["timestamp"]))

    conn.commit()
    conn.close()

def principal():
    inicializar_base_datos()
    cliente = mqtt.Client()
    cliente.on_message = al_recibir_mensaje
    cliente.connect(BROKER, PUERTO)
    cliente.subscribe("flota/+/gps")
    cliente.subscribe("flota/+/temperatura")
    cliente.subscribe("flota/+/combustible")
    print("Suscriptor MQTT activo...")
    cliente.loop_forever()

if __name__ == "__main__":
    principal()
