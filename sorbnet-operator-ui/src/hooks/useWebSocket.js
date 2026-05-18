import { useEffect, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

export function useWebSocket(topics, onMessage) {
  const clientRef = useRef(null);

  useEffect(() => {
    const client = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8083/ws'),
      onConnect: () => {
        topics.forEach(topic => {
          client.subscribe(topic, (msg) => {
            onMessage(topic, JSON.parse(msg.body));
          });
        });
      },
      reconnectDelay: 5000,
    });
    client.activate();
    clientRef.current = client;
    return () => client.deactivate();
  }, []);
}