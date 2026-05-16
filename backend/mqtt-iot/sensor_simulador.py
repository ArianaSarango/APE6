import json
import time
import random
from datetime import datetime
# pyrefly: ignore [missing-import]
import paho.mqtt.client as mqtt

# --- Configuración MQTT ---
BROKER = "localhost"
PUERTO = 1883
VEHICULOS = ["VH-001", "VH-002", "VH-003"]

def on_connect(cliente, datos_usuario, flags, codigo_respuesta):
    if codigo_respuesta == 0:
        print(f"[{datetime.now().isoformat()}] Conectado al broker MQTT")
    else:
        print(f"Error de conexión: {codigo_respuesta}")

def simular_gps(id_vehiculo):
    base_lat = -2.1709
    base_lng = -79.9224
    latitud = base_lat + random.uniform(-0.01, 0.01)
    longitud = base_lng + random.uniform(-0.01, 0.01)
    velocidad = random.uniform(20, 80)
    return {
        "lat": round(latitud, 6),
        "lng": round(longitud, 6),
        "speed": round(velocidad, 1)
    }

def simular_temperatura(id_vehiculo):
    temperatura = random.uniform(-5, 8)
    return {
        "temperature": round(temperatura, 1),
        "unit": "celsius"
    }

def simular_combustible(id_vehiculo):
    combustible = random.uniform(10, 100)
    return {
        "fuelLevel": round(combustible, 1),
        "unit": "percent"
    }

def principal():
    cliente = mqtt.Client()
    cliente.on_connect = on_connect
    cliente.connect(BROKER, PUERTO)
    cliente.loop_start()

    print("Iniciando simulador de sensores IoT...")

    try:
        while True:
            for vehiculo in VEHICULOS:
                timestamp = datetime.now().isoformat()

                # GPS
                gps = simular_gps(vehiculo)
                datos_gps = {"vehicleId": vehiculo, "timestamp": timestamp, **gps}
                cliente.publish(f"flota/{vehiculo}/gps", json.dumps(datos_gps), qos=1)

                # Temperatura
                temp = simular_temperatura(vehiculo)
                datos_temp = {"vehicleId": vehiculo, "timestamp": timestamp, **temp}
                cliente.publish(f"flota/{vehiculo}/temperatura", json.dumps(datos_temp), qos=1)

                # Combustible
                fuel = simular_combustible(vehiculo)
                datos_fuel = {"vehicleId": vehiculo, "timestamp": timestamp, **fuel}
                cliente.publish(f"flota/{vehiculo}/combustible", json.dumps(datos_fuel), qos=1)

                print(f"[{timestamp}] Publicado {vehiculo} | GPS:({gps['lat']},{gps['lng']}) Temp:{temp['temperature']}°C Fuel:{fuel['fuelLevel']}%")

            time.sleep(5)

    except KeyboardInterrupt:
        print("\nDeteniendo simulador...")
        cliente.loop_stop()
        cliente.disconnect()

if __name__ == "__main__":
    principal()
