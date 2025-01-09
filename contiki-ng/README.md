# Evaluation lab - Contiki-NG

## Group number: 26

## Group members

- MohammadAmin Rahimi 
- Christian Mariano
- Armando Fiorini

## Solution description
Client
Every 1 minute, client send a new message to the monitor server. 
The parent is retrieve from rpl_parent_get_ipaddr and then send to the monitor server.
We use etimer_set to set a periodic time notion.

Server
We have use a structc called client_info_t that consists of client and parent ip , a tag to handle removing logic (isEmpty) and a timestamp(last_timestamp) to handle timeout for removal phase.
In each callback when a client send information we check if that client  is already in the array(monitor_info) of the clients and if its there we update the timestamp and if its not there and we have available space with respects to the MAX_CLIENTS we add it to the array.
In removal phase, we use clock_time() to get the current moment timestamp the we compare it with the clients. If the difference is more than 3 minutes we remove that client.
