import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

const WS_URL = process.env.REACT_APP_WS_URL || 'http://localhost:8080/ws';

class WebSocketService {
  constructor() {
    this.stompClient = null;
    this.subscriptions = [];
  }

  connect(onConnected, onError) {
    this.stompClient = new Client({
      webSocketFactory: () => new SockJS(WS_URL),
      
      // Enable debug logging
      debug: (msg) => console.log('[STOMP]', msg),
      
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,

      onConnect: (frame) => {
        console.log('✅ WebSocket Connected:', frame);
        if (onConnected) onConnected(frame);
      },

      onStompError: (error) => {
        console.error('❌ WebSocket Error:', error);
        if (onError) onError(error);
      },
    });

    this.stompClient.activate();
  }

  subscribe(topic, callback) {
    if (!this.stompClient || !this.stompClient.connected) {
      console.error('❌ WebSocket not connected');
      return null;
    }

    const subscription = this.stompClient.subscribe(topic, (message) => {
      try {
        const data = JSON.parse(message.body);
        callback(data);
      } catch (error) {
        console.error('Error parsing message:', error);
      }
    });

    this.subscriptions.push(subscription);
    return subscription;
  }

  disconnect() {
    if (this.stompClient) {
      this.subscriptions.forEach(sub => sub.unsubscribe());
      this.subscriptions = [];
      this.stompClient.deactivate();
      console.log('🔌 WebSocket Disconnected');
    }
  }

  isConnected() {
    return this.stompClient && this.stompClient.connected;
  }
}

const wsService = new WebSocketService();
export default wsService;
