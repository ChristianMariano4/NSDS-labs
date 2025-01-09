#include "contiki.h"
#include "net/routing/routing.h"
#include "net/netstack.h"
#include "net/ipv6/simple-udp.h"

#include "sys/log.h"
#define LOG_MODULE "App"
#define LOG_LEVEL LOG_LEVEL_INFO

#define UDP_CLIENT_PORT	8765
#define UDP_SERVER_PORT	5678
#define MAX_CLIENTS 5
#define SEND_INTERVAL	(60 * CLOCK_SECOND)
#define CLIENT_TIMEOUT (3 * SEND_INTERVAL) // 3 minutes timeout

static struct simple_udp_connection udp_conn;

PROCESS(udp_server_process, "UDP server");
AUTOSTART_PROCESSES(&udp_server_process);

typedef struct {
  int isEmpty;
  uip_ipaddr_t client_ip; // client
  uip_ipaddr_t parent_ip; // client's parent
  uint32_t last_timestamp; // Timestamp of the last message received
} client_info_t;

static client_info_t monitor_info[MAX_CLIENTS];
/*---------------------------------------------------------------------------*/
static void
udp_rx_callback(struct simple_udp_connection *c,
         const uip_ipaddr_t *sender_addr,
         uint16_t sender_port,
         const uip_ipaddr_t *receiver_addr,
         uint16_t receiver_port,
         const uint8_t *data,
         uint16_t datalen)
{
  uip_ipaddr_t parent_ip = *(uip_ipaddr_t *)data;
  LOG_INFO("Received request from ");
  LOG_INFO_6ADDR(sender_addr);
  LOG_INFO_("\n");

  int found = 0;
  uint32_t now = clock_time();

  //check if client is already there
  for(int i = 0; i < MAX_CLIENTS; i++) {
    //check if client is present
    if(monitor_info[i].isEmpty == 0 && uip_ipaddr_cmp(&monitor_info[i].client_ip, sender_addr)) {
      //update parent and timestamp info
      monitor_info[i].parent_ip = parent_ip;
      monitor_info[i].last_timestamp = now;
      found = 1;
      break;
    }
  }

  if(found == 0) {
    //add client if there a space for it
    for(int i = 0; i < MAX_CLIENTS; i++) {
      if(monitor_info[i].isEmpty == 1) {
        monitor_info[i].client_ip = *(sender_addr);
        monitor_info[i].parent_ip = parent_ip;
        monitor_info[i].isEmpty = 0;
        monitor_info[i].last_timestamp = now;
        break;
      }
    }
  }


}
/*---------------------------------------------------------------------------*/
PROCESS_THREAD(udp_server_process, ev, data)
{
  PROCESS_BEGIN();
  static struct etimer periodic_timer;

  /* Initialize DAG root */
  NETSTACK_ROUTING.root_start();

  // Initialize monitor struct
  for(int i = 0; i < MAX_CLIENTS; i++) {
    monitor_info[i].isEmpty = 1;
  }

  /* Initialize UDP connection */
  simple_udp_register(&udp_conn, UDP_SERVER_PORT, NULL,
                      UDP_CLIENT_PORT, udp_rx_callback);
  
  etimer_set(&periodic_timer, SEND_INTERVAL);
  while (1) {
    PROCESS_WAIT_EVENT_UNTIL(etimer_expired(&periodic_timer));

    uint32_t now = clock_time();

    // Check timestamp and print if the client is active
    // Otherwise remove it from the monitor_array
    LOG_INFO("Monitor info:\n");
    for(int i = 0; i < MAX_CLIENTS; i++) {
      if(!monitor_info[i].isEmpty && (now - monitor_info[i].last_timestamp > CLIENT_TIMEOUT)) {
        monitor_info[i].isEmpty = 1;
      } else if(!monitor_info[i].isEmpty) {
        LOG_INFO("Client ");
        LOG_INFO_6ADDR(&monitor_info[i].client_ip);
        LOG_INFO_(" - Parent ");
        LOG_INFO_6ADDR(&monitor_info[i].parent_ip);
        LOG_INFO("\n");
      }
    }
    
    // Reset the timer
    etimer_set(&periodic_timer, SEND_INTERVAL);
  }

  

  PROCESS_END();
}
/*---------------------------------------------------------------------------*/

