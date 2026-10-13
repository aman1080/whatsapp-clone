
import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { forkJoin } from 'rxjs';
import {
  ChatMessage,
  ChatWebSocketService
} from '../services/chat-websocket.service';

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './chat.component.html',
  styleUrl: './chat.component.css'
})
export class ChatComponent implements OnInit, OnDestroy {


  @Input({ required: true }) token!: string;
  @Input({ required: true }) conversationId!: number;
  @Input() currentUserId!: number;

  messages: ChatMessage[] = [];
  newMessage = '';
  private messagesSubscription?: Subscription;

  constructor(
  private chatWebSocketService: ChatWebSocketService,
  private http: HttpClient
) {}

  ngOnInit(): void {
  this.loadMessageHistory();

  this.messagesSubscription =
    this.chatWebSocketService.messages$.subscribe(messages => {
      this.messages = [...this.messages, ...messages];
    });

  this.chatWebSocketService.connect(
    this.token,
    this.conversationId
  );
}

  sendMessage(): void {
    const content = this.newMessage.trim();

    if (!content) {
      return;
    }

    this.chatWebSocketService.sendMessage(
      this.conversationId,
      content
    );

    this.newMessage = '';
  }

  ngOnDestroy(): void {
    this.messagesSubscription?.unsubscribe();
    this.chatWebSocketService.disconnect();
  }

  loadMessageHistory(): void {
  const headers = new HttpHeaders({
    Authorization: `Bearer ${this.token}`
  });

  const params = new HttpParams()
    .set('page', '0')
    .set('size', '50');

  this.http.get<ChatMessage[]>(
    `http://localhost:8080/api/conversations/${this.conversationId}/messages`,
    { headers, params }
  ).subscribe({
    next: (history) => {
      // The backend returns newest messages first.
      this.messages = [...history].reverse();
    },
    error: (error) => {
      console.error('Failed to load message history:', error);
    }
  });
}
}
