# pyrefly: ignore [missing-import]
import paho.mqtt.client as mqtt
import pika
import json
from datetime import datetime

# --- Configuración ---
MQTT_HOST = "localhost"
MQTT_PORT = 1883

RABBITMQ_HOST = "localhost"
RABBITMQ_PORT = 5672
RABBITMQ_USER = "admin"
RABBITMQ_PASS = "admin123"

FLEET_EXCHANGE = "exchange.fleet"

ROUTING_KEYS = {
    "gps": "gps.routing",
    "temperatura": "temp.alert",
    "combustible": "fuel.routing",
}

rabbitmq_channel = None

def connect_rabbitmq():
    global rabbitmq_channel
    credentials = pika.PlainCredentials(RABBITMQ_USER, RABBITMQ_PASS)
    params = pika.ConnectionParameters(host=RABBITMQ_HOST, port=RABBITMQ_PORT, credentials=credentials)
    connection = pika.BlockingConnection(params)
    rabbitmq_channel = connection.channel()
    rabbitmq_channel.exchange_declare(exchange=FLEET_EXCHANGE, exchange_type="direct", durable=True)
    print(f"[RabbitMQ] Conectado en {RABBITMQ_HOST}")
    return connection

def publish_to_rabbitmq(routing_key, payload):
    payload["bridgeTimestamp"] = datetime.utcnow().isoformat() + "Z"
    body = json.dumps(payload)
    rabbitmq_channel.basic_publish(
        exchange=FLEET_EXCHANGE,
        routing_key=routing_key,
        body=body,
        properties=pika.BasicProperties(
            delivery_mode=2,
            content_type="text/plain",
            content_encoding="utf-8"
        )
    )
    print(f"[Bridge] → RabbitMQ: {routing_key}")

def on_connect(client, userdata, flags, rc):
    if rc == 0:
        print("[MQTT] Conectado")
        client.subscribe("flota/+/gps", qos=1)
        client.subscribe("flota/+/temperatura", qos=1)
        client.subscribe("flota/+/combustible", qos=1)
    else:
        print(f"Error MQTT: {rc}")

def on_message(client, userdata, msg):
    parts = msg.topic.split("/")
    if len(parts) < 3: return
    tipo = parts[2]
    routing_key = ROUTING_KEYS.get(tipo)
    if routing_key:
        try:
            data = json.loads(msg.payload.decode())
            publish_to_rabbitmq(routing_key, data)
        except Exception as e:
            print(f"Error: {e}")

def main():
    print("=== Puente MQTT → RabbitMQ ===")
    conn = connect_rabbitmq()
    client = mqtt.Client(client_id="bridge-service")
    client.on_connect = on_connect
    client.on_message = on_message
    client.connect(MQTT_HOST, MQTT_PORT)
    try:
        client.loop_forever()
    except KeyboardInterrupt:
        print("Cerrando...")
    finally:
        client.disconnect()
        conn.close()

if __name__ == "__main__":
    main()
