
import { Injectable, OnDestroy } from '@angular/core';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import { BehaviorSubject, Observable } from 'rxjs';

export interface ChatMessage {
  messageId: number;
  conversationId: number;
  senderId: number;
  senderName: string;
  content: string;
  sentAt: string;
}

@Injectable({
  providedIn: 'root'
})
export class ChatWebSocketService implements OnDestroy {

  private client?: Client;
  private subscription?: StompSubscription;

  private messagesSubject = new BehaviorSubject<ChatMessage[]>([]);
  messages$: Observable<ChatMessage[]> =
    this.messagesSubject.asObservable();

  connect(token: string, conversationId: number): void {
    this.disconnect();

    this.messagesSubject.next([]);

    this.client = new Client({
      brokerURL: 'ws://localhost:8080/ws',
      connectHeaders: {
        Authorization: `Bearer ${token}`
      },
      reconnectDelay: 5000,
      onConnect: () => {
        this.subscription = this.client!.subscribe(
          `/topic/conversations/${conversationId}`,
          (message: IMessage) => {
            const chatMessage: ChatMessage =
              JSON.parse(message.body) as ChatMessage;

            this.messagesSubject.next([
              ...this.messagesSubject.value,
              chatMessage
            ]);
          }
        );
      },
      onStompError: (frame) => {
        console.error('STOMP error:', frame.headers['message']);
      },
      onWebSocketError: (error) => {
        console.error('WebSocket error:', error);
      }
    });

    this.client.activate();
  }

  sendMessage(conversationId: number, content: string): void {
    if (!this.client?.connected) {
      console.error('WebSocket is not connected.');
      return;
    }

    const trimmedContent = content.trim();

    if (!trimmedContent) {
      return;
    }

    this.client.publish({
      destination: `/app/chat/${conversationId}`,
      body: JSON.stringify({
        content: trimmedContent
      })
    });
  }

  disconnect(): void {
    this.subscription?.unsubscribe();
    this.subscription = undefined;

    if (this.client) {
      void this.client.deactivate();
      this.client = undefined;
    }
  }

  ngOnDestroy(): void {
    this.disconnect();
  }
}
