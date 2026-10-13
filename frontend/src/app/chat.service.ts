
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import {
  Client,
  IMessage,
  StompSubscription
} from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { Observable, Subject, map } from 'rxjs';

// Models

export interface User {
  id: number;
  displayName: string;
  email: string;
}

export interface Chat {
  id: number;
  type: string;
  name: string | null;
  members?: User[];
  createdAt: string;
}

export interface ChatMessage {
  id: number;
  chatId: number;
  senderId: number;
  senderName: string;
  content: string;
  sentAt: string;
}

export interface AuthResponse {
  token: string;
  userId: number;
  displayName: string;
  email: string;
}

interface ConversationResponse {
  id: number;
  type: string;
  createdAt: string;
}

interface DirectConversationResponse {
  conversationId: number;
  message: string;
}

interface MessageResponse {
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
export class ChatService {

  private readonly api = 'http://localhost:8080/api';
  private readonly wsUrl = 'http://localhost:8080/ws';

  private client?: Client;

  // Tracks subscriptions for both direct and group conversations.
  private readonly chatSubscriptions =
    new Map<number, StompSubscription>();

  private privateSubscription?: StompSubscription;

  private readonly messagesSubject = new Subject<ChatMessage>();

  readonly messages$: Observable<ChatMessage> =
    this.messagesSubject.asObservable();

  constructor(private http: HttpClient) {}

  // Authentication

  register(
    displayName: string,
    email: string,
    password: string
  ): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(
      `${this.api}/auth/register`,
      { displayName, email, password }
    );
  }

  login(
    email: string,
    password: string
  ): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(
      `${this.api}/auth/login`,
      { email, password }
    );
  }

  // Users

  users(): Observable<User[]> {
    return this.http.get<User[]>(
      `${this.api}/users`
    );
  }

  // Conversations

  chats(): Observable<Chat[]> {
    return this.http.get<ConversationResponse[]>(
      `${this.api}/conversations`
    ).pipe(
      map(conversations =>
        conversations.map(conversation => ({
          id: conversation.id,
          type: conversation.type,
          name: null,
          members: [],
          createdAt: conversation.createdAt
        }))
      )
    );
  }

  // Open a direct conversation with another user

  privateChat(otherUserId: number): Observable<Chat> {
    return this.http.post<DirectConversationResponse>(
      `${this.api}/conversations/direct/${otherUserId}`,
      {}
    ).pipe(
      map(response => ({
        id: response.conversationId,
        type: 'DIRECT',
        name: null,
        members: [],
        createdAt: new Date().toISOString()
      }))
    );
  }

  // Message history

  history(chatId: number): Observable<ChatMessage[]> {
    return this.http.get<MessageResponse[]>(
      `${this.api}/conversations/${chatId}/messages`,
      {
        params: {
          page: 0,
          size: 100
        }
      }
    ).pipe(
      map(messages =>
        messages.map(message => ({
          id: message.messageId,
          chatId: message.conversationId,
          senderId: message.senderId,
          senderName: message.senderName,
          content: message.content,
          sentAt: message.sentAt
        }))
      )
    );
  }

  // WebSocket connection

  connect(token: string): void {
    this.disconnect();

    this.client = new Client({
      webSocketFactory: () => new SockJS(this.wsUrl),

      connectHeaders: {
        Authorization: `Bearer ${token}`
      },

      reconnectDelay: 3000,

      debug: (message) => console.log('[STOMP]', message)
    });

    this.client.onConnect = () => {
      console.log('[STOMP] Connected successfully');

      // Personal queue subscription.
      this.privateSubscription = this.client?.subscribe(
        '/user/queue/messages',
        (message: IMessage) => this.handleMessage(message)
      );

      // Restore all conversation subscriptions after reconnecting.
      for (const chatId of this.chatSubscriptions.keys()) {
        this.subscribeChatNow(chatId);
      }
    };

    this.client.onStompError = frame => {
      console.error(
        'WebSocket STOMP error:',
        frame.headers['message'],
        frame.body
      );
    };

    this.client.onWebSocketError = error => {
      console.error('WebSocket error:', error);
    };

    this.client.onWebSocketClose = event => {
      console.warn('[STOMP] WebSocket closed', event.code);
    };

    this.client.activate();
  }

  // Process messages received from the server.

  private handleMessage(message: IMessage): void {
    try {
      const response = JSON.parse(message.body) as MessageResponse;

      const chatMessage: ChatMessage = {
        id: response.messageId,
        chatId: response.conversationId,
        senderId: response.senderId,
        senderName: response.senderName,
        content: response.content,
        sentAt: response.sentAt
      };

      console.log(
        '[STOMP] Received chat message:',
        chatMessage
      );

      this.messagesSubject.next(chatMessage);

    } catch (error) {
      console.error(
        'Failed to parse chat message:',
        error
      );
    }
  }

  // Subscribe to a direct or group conversation.

  subscribeChat(chatId: number): void {
    if (this.chatSubscriptions.has(chatId)) {
      return;
    }

    // Register the chat even if the WebSocket is not connected yet.
    // It will be subscribed when the connection becomes ready.
    this.chatSubscriptions.set(
      chatId,
      undefined as unknown as StompSubscription
    );

    if (this.client?.connected) {
      this.subscribeChatNow(chatId);
    }
  }

  private subscribeChatNow(chatId: number): void {
    if (!this.client?.connected) {
      return;
    }

    const existing = this.chatSubscriptions.get(chatId);

    if (existing) {
      return;
    }

    const subscription = this.client.subscribe(
      `/topic/chat/${chatId}`,
      (message: IMessage) => this.handleMessage(message)
    );

    this.chatSubscriptions.set(chatId, subscription);

    console.log(
      `[STOMP] Subscribed to /topic/chat/${chatId}`
    );
  }

  // Unsubscribe from a conversation when needed.

  unsubscribeChat(chatId: number): void {
    const subscription = this.chatSubscriptions.get(chatId);

    subscription?.unsubscribe();
    this.chatSubscriptions.delete(chatId);
  }

  // Send message

  send(chatId: number, content: string): void {
    if (!this.client?.connected) {
      throw new Error('WebSocket is not connected yet');
    }

    const trimmedContent = content.trim();

    if (!trimmedContent) {
      return;
    }

    this.client.publish({
      destination: '/app/chat.send',
      body: JSON.stringify({
        chatId,
        content: trimmedContent
      })
    });

    console.log('[STOMP] Message sent for chat:', chatId);
  }

  // Disconnect

  disconnect(): void {
    this.privateSubscription?.unsubscribe();
    this.privateSubscription = undefined;

    for (const subscription of this.chatSubscriptions.values()) {
      subscription?.unsubscribe();
    }

    this.chatSubscriptions.clear();

    if (this.client) {
      void this.client.deactivate();
      this.client = undefined;
    }
  }
}
