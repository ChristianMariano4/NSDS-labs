# Evaluation lab - Node-RED

## Group number: 26

## Group members

- Christian Mariano 
- Armando Fiorini
- MohammadAmin Rahimi

## Description of message flows

We left the debugger nodes for debug purpose, but we turned them off.

# Flow 1
- MQTT in node "GetTemperature" gets weather data from MQTT server from topic "neslabpolimi/smartcity/milan"
- function node "computeAverage" is triggered only if temperature field is defined in the msg.payload. Then it updates the window average by using a queue.
  It uses the node context to save the queue and the flow context to save the average.

# Flow 2
- MQTT in node "GetK" fetches the K value from MQTT server from topic "neslabpolimi/nsds/eval24/k"
- function node "KUpgrade" stores K in the flow context


# Flow 3
- this flow is triggered by an inject node every 1 minute
- openweathermap node "OpenWeatherMilan" fetches Milan's weather data from openweathermap and passes temperature field to function node "Compare"
- this function node compares the average value fetching it from flow context and the temperature value gotten from openweathermap node
- if the difference is greater than K value, fetched from flow context, then a message is publish over a MQTT server to topic "neslabpolimi/nsds/eval24/alarm"



## Extensions
