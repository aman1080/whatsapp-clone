import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common'; import { FormsModule } from '@angular/forms'; import { Subscription } from 'rxjs';
import { ChatService, User, Chat } from './chat.service';
import { Message } from './chat.model';



@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit, OnDestroy {

  me?: { userId: number; displayName?: string };

  chats: Chat[] = [];
  users: User[] = [];
  messages: Message[] = [];

  active?: Chat;
  draft = '';
  searchText = '';
  error = '';

  groupName = '';
  selectedMembers: number[] = [];

  private sub?: Subscription;

  constructor(
    private chat: ChatService
  ) {}

  ngOnInit(): void {
    const savedUser = localStorage.getItem('chat_user');

    if (savedUser) {
      try {
        this.me = JSON.parse(savedUser);
      } catch {
        this.me = undefined;
      }
    }

    this.refreshChats();

    this.chat.users().subscribe({
      next: users => this.users = users,
      error: () => this.error = 'Could not load users'
    });

    // Keep your existing message subscription here if your
    // ChatService exposes a messages observable.
  }

  get filteredChats(): Chat[] {
    const query = this.searchText.trim().toLowerCase();

    if (!query) {
      return this.chats;
    }

    return this.chats.filter(c =>
      this.title(c).toLowerCase().includes(query)
    );
  }

  refreshChats(): void {
    this.chat.chats().subscribe({
      next: chats => this.chats = chats,
      error: () => this.error = 'Could not load chats'
    });
  }

  open(c: Chat): void {
    this.active = c;
    this.messages = [];
    this.error = '';

    if (c.type === 'GROUP') {
      this.chat.subscribeGroup(c.id);
    }

    this.chat.history(c.id).subscribe({
      next: messages => this.messages = messages,
      error: () => this.error = 'Could not load messages'
    });
  }

  startPrivate(u: User): void {
    this.chat.privateChat(u.id).subscribe({
      next: c => {
        this.refreshChats();
        this.open(c);
      },
      error: () => this.error = 'Could not create private chat'
    });
  }

  createGroup(): void {
    if (!this.groupName.trim() || !this.selectedMembers.length) {
      this.error = 'Enter a group name and select at least one member';
      return;
    }

    this.chat.group(this.groupName, this.selectedMembers).subscribe({
      next: c => {
        this.groupName = '';
        this.selectedMembers = [];
        this.refreshChats();
        this.open(c);
      },
      error: () => this.error = 'Could not create group'
    });
  }

  toggleMember(id: number, checked: boolean): void {
    this.selectedMembers = checked
      ? [...this.selectedMembers, id]
      : this.selectedMembers.filter(x => x !== id);
  }

  send(): void {
    const value = this.draft.trim();

    if (!this.active || !value) {
      return;
    }

    try {
      this.chat.send(this.active.id, value);
      this.draft = '';
      this.error = '';
    } catch {
      this.error = 'Connecting to chat server. Please retry in a moment.';
    }
  }

  title(c: Chat): string {
    if (c.type === 'GROUP') {
      return c.name || 'Group';
    }

    return c.members.find(m => m.id !== this.me?.userId)?.displayName
      || 'Private chat';
  }

  logout(): void {
    this.chat.disconnect();
    this.sub?.unsubscribe();

    localStorage.removeItem('chat_token');
    localStorage.removeItem('chat_user');

    this.me = undefined;
    this.active = undefined;
    this.messages = [];
    this.chats = [];
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
    this.chat.disconnect();
  }
}