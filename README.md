# 🚛 Fleet Monitor — Práctica 6
**Mensajería Distribuida con RabbitMQ y MQTT**  
Sistemas Distribuidos 

Ariana Sarango, Juan Alverca, Steven Jimenez

---

## 📋 Requisitos previos

Antes de empezar, asegúrate de tener instalado:

| Herramienta | Instalación |
|---|---|
| Python 3.10+ | `sudo apt install python3 python3-venv` |
| Java JDK 17 | `sudo apt install openjdk-17-jdk` |
| Maven | `sudo apt install maven` |
| Docker | `sudo apt install docker.io docker-compose` |

Activa Docker para tu usuario (cierra y abre la terminal después):
```bash
sudo systemctl enable --now docker
sudo usermod -aG docker $USER
```

---

## 📁 Estructura del proyecto

```
APE6/
├── backend/
│   ├── docker-compose.yml        ← Mosquitto + RabbitMQ
│   ├── mosquitto/config/
│   │   └── mosquitto.conf
│   ├── mqtt-iot/
│   │   ├── sensor_simulador.py   ← Publica datos de sensores
│   │   ├── suscriptor_mqtt.py    ← Guarda en SQLite
│   │   ├── bridge.py             ← Puente MQTT → RabbitMQ
│   │   └── requirements.txt
│   └── spring-boot/              ← Microservicios Java
│       ├── pom.xml
│       └── src/
└── frontend/
    ├── index.html
    ├── style.css
    └── script.js
```

---

## 🚀 Pasos para ejecutar

### PASO 1 — Levantar Docker (Mosquitto + RabbitMQ)

```bash
cd ~/APE6/backend
docker-compose up -d
```

Verifica que estén corriendo:
```bash
docker ps
```
Debes ver `mosquitto_broker` y `rabbitmq_broker`.

> Panel de RabbitMQ: http://localhost:15672  
> Usuario: `admin` | Contraseña: `admin123`

---

### PASO 2 — Crear el entorno virtual de Python

Solo la primera vez:
```bash
cd ~/APE6/backend/mqtt-iot
python3 -m venv .venv
source .venv/bin/activate
pip install paho-mqtt pika
```

---

### PASO 3 — Correr los scripts Python

Abre **3 terminales separadas** y en cada una entra a la carpeta y activa el entorno:

```bash
cd ~/APE6/backend/mqtt-iot
source .venv/bin/activate
```

**Terminal 1 — Suscriptor MQTT:**
```bash
python3 suscriptor_mqtt.py
```

**Terminal 2 — Bridge MQTT → RabbitMQ:**
```bash
python3 bridge.py
```

**Terminal 3 — Simulador de sensores:**
```bash
python3 sensor_simulador.py
```

---

### PASO 4 — Correr Spring Boot

Abre una **4ta terminal**:

```bash
cd ~/APE6/backend/spring-boot
mvn spring-boot:run
```

Espera hasta ver:
```
Started FleetMonitorApplication in X seconds
```

Verifica que la API responde:
```bash
curl http://localhost:8080/api/fleet/status
curl http://localhost:8080/api/fleet/vehicle/VH-001/telemetria
```

---

### PASO 5 — Abrir el frontend

Abre el archivo directamente en el navegador:
```
APE6/frontend/index.html
```

O desde la terminal:
```bash
xdg-open ~/APE6/frontend/index.html
```

Deberías ver el dashboard con:
- ✅ **En línea** en la esquina superior derecha
- 🚗 Los 3 vehículos con datos GPS en tiempo real
- 🚨 Alertas en rojo cuando temperatura > 4°C o combustible < 20%

---

## 🔁 Orden de ejecución resumido

```
Docker → suscriptor.py → bridge.py → sensor_simulador.py → Spring Boot → frontend
```

---

## 🛑 Para detener todo

```bash
# Detener scripts Python: Ctrl+C en cada terminal

# Detener Spring Boot: Ctrl+C en esa terminal

# Detener Docker:
cd ~/APE6/backend
docker-compose down
```

---

## 🌐 URLs útiles

| Servicio | URL |
|---|---|
| API REST - Estado flota | http://localhost:8080/api/fleet/status |
| API REST - Telemetría VH-001 | http://localhost:8080/api/fleet/vehicle/VH-001/telemetria |
| API REST - Alertas activas | http://localhost:8080/api/fleet/alerts |
| RabbitMQ Management | http://localhost:15672 |
| H2 Console (base de datos) | http://localhost:8080/h2-console |

---

## ⚠️ Problemas comunes

**`ModuleNotFoundError: No module named 'paho'`**  
→ El entorno virtual no está activo. Corre `source .venv/bin/activate`

**`docker-compose: No existe el archivo o el directorio`**  
→ Docker fue instalado como snap. Reinstala con: `sudo apt install docker.io docker-compose`

**El frontend muestra "Sin conexión"**  
→ Spring Boot no está corriendo. Verifica el PASO 4.

**El frontend muestra datos en 0**  
→ El bridge no está corriendo o hubo un error de conversión. Reinicia los 3 scripts Python.

**`permission denied` al eliminar contenedor Docker**  
→ Corre `sudo aa-remove-unknown` y luego `sudo docker rm -f nombre_contenedor`
