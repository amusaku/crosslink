### Paho MQTT Python Client

This guide provides a ready-to-use example of how to use the **Paho MQTT Python Client** library with TBMQ. 
Using it, you will learn how to connect an MQTT client, subscribe to a topic, publish a message, and handle MQTT events.

##### Prerequisites
In order to run the Python code please make sure you have installed:
* [python3](https://www.python.org/downloads)
* [paho-mqtt](https://github.com/eclipse/paho.mqtt.python)

This guide was developed by using Python v3.10.12 and the new Paho-MQTT 2.1.0 version (which contains some breaking [changes](https://github.com/eclipse/paho.mqtt.python/blob/master/docs/migrations.rst) comparing to older 1.X version). 

Use the next commands to check your Python and Paho versions:

```bash
python3 --version{:copy-code}
```

```bash
pip3 show paho-mqtt{:copy-code}
```

##### Connect to the TBMQ
The script below sets up a Paho MQTT client to connect to the TBMQ broker using default credentials `TBMQ WebSockets MQTT Credentials`, handles basic MQTT operations such as publishing a message and subscribing to a topic.

In case you have changed the `TBMQ WebSockets MQTT Credentials`, don't forget to update the client ID, username, and password in the guide.

You can paste this code into a new file in your project `tbmq-python.py`.

```bash
import paho.mqtt.client as pahoMqtt
from paho import mqtt

host = "{:mqttHost}"
port = {:mqttPort}
topic = "sensors/temperature"
payload = "Hello, TBMQ!"
qos = 1
retain = False
clean_session = True
userdata = None
protocol = pahoMqtt.MQTTv311 # or MQTTv5, or MQTTv31

# client credentials
username = "tbmq_websockets_username"
clientId = "tbmq_test_client"
password = None

# This function initializes and returns an MQTT client.
def init_mqtt_client() -> pahoMqtt:
    # Instantiate an MQTT client
    client = pahoMqtt.Client(pahoMqtt.CallbackAPIVersion.VERSION2, clientId, clean_session, userdata, protocol)
    # Set the client's username and password.
    client.username_pw_set(username, password)
    # Connect the client to the TBMQ broker
    client.connect(host, port)
    return client

# This is the callback function that is called when the client is connected with the MQTT server.
def on_connect(client, userdata, flags, rc, properties=None):
    if rc == 0:
        print("Client connected!")
        # Publish a message after client is connected
        client.publish(topic, payload, qos, retain)
    else:
        print("Client not connected, reason code: ", rc)

# This is the callback function that is called when a message is received after subscribing to a topic.
def on_message(client, userdata, msg):
    message_str = msg.payload.decode('utf-8')  # Decoding bytes to string
    print('Received message \'' + message_str + '\' on topic ' + msg.topic)

# This is the callback function that is called when there is any error during MQTT operations.
def on_error(client, userdata, err):
    print("Error: " + str(err))

# This is the callback function that is called when the client is disconnected from the MQTT server.
def on_disconnect(client, userdata, rc):
    print("Disconnecting with reason code: " + str(rc))

client = init_mqtt_client()
client.subscribe(topic, qos)

# Assign the event handler functions to the client instance.
client.on_connect = on_connect
client.on_message = on_message
client.on_error = on_error
client.on_disconnect = on_disconnect

# Start a forever loop to process the MQTT events.
client.loop_forever()

{:copy-code}
```

To run this Python application you may use next command:

```bash
python3 tbmq-python.py
{:copy-code}
```

The output from executing the `tbmq-python.py` file:
```bash
Client connected!
Received message 'Hello, TBMQ!' on topic sensors/temperature
```

#### See also
The full documenation on Paho MQTT Python Client [API](https://eclipse.dev/paho/files/paho.mqtt.python/html/client.html).
