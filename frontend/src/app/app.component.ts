
import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';

import {
    ChatService,
    User,
    Chat,
    ChatMessage
} from './chat.service';

import { Message } from './chat.model';
import { AuthService } from './auth.service';

@Component({
    selector: 'app-root',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './app.component.html',
    styleUrl: './app.component.css'
})
export class AppComponent implements OnInit, OnDestroy {

    me?: { userId: number; displayName?: string };

    page: 'login' | 'register' | 'chat' = 'login';

    email = '';
    password = '';
    displayName = '';
    authError = '';
    authLoading = false;

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
        private chat: ChatService,
        private auth: AuthService
    ) { }

    ngOnInit(): void {
        const savedUser = localStorage.getItem('chat_user');
        const savedToken = localStorage.getItem('chat_token');

        if (savedUser) {
            try {
                this.me = JSON.parse(savedUser);
            } catch {
                this.me = undefined;
            }
        }

        // Listen for incoming messages once for this component.
        this.sub = this.chat.messages$.subscribe(
            (chatMessage: ChatMessage) => {
                // Only display messages for the currently open conversation.
                if (this.active?.id !== chatMessage.chatId) {
                    return;
                }

                // Avoid adding a duplicate if the same message is delivered twice.
                const alreadyExists = this.messages.some(
                    message => message.id === chatMessage.id
                );

                if (alreadyExists) {
                    return;
                }

                // Map ChatMessage into the existing UI Message model.
                this.messages.push({
                    id: chatMessage.id,
                    chatId: chatMessage.chatId,
                    senderId: chatMessage.senderId,
                    senderName: chatMessage.senderName,
                    content: chatMessage.content,
                    sentAt: chatMessage.sentAt
                } as Message);
            }
        );

        if (this.me && savedToken) {
            this.page = 'chat';
            this.chat.connect(savedToken);
            this.loadChatData();
        }
    }

    private loadChatData(): void {
        this.refreshChats();

        this.chat.users().subscribe({
            next: users => this.users = users,
            error: () => this.error = 'Could not load users'
        });
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
            error: () => this.error = 'Could not load conversations'
        });
    }

    open(c: Chat): void {
        this.active = c;
        this.messages = [];
        this.error = '';

        // Subscribe to both DIRECT and GROUP conversations.
        this.chat.subscribeChat(c.id);

        this.chat.history(c.id).subscribe({
            next: messages => {
                const incoming = this.messages;
                const combined = [...messages, ...incoming];

                const unique = new Map<number, Message>();

                for (const message of combined) {
                    if (message.id !== undefined) {
                        unique.set(message.id, message);
                    }
                }

                this.messages = Array.from(unique.values());
            },
            error: () => this.error = 'Could not load messages'
        });
    }

    startPrivate(u: User): void {
        this.chat.privateChat(u.id).subscribe({
            next: c => {
                this.refreshChats();
                this.open(c);
            },
            error: err => {
                console.error(
                    'Could not create private conversation:',
                    err
                );

                this.error = err.error?.message
                    || 'Could not create private chat';
            }
        });
    }

    createGroup(): void {
        this.error =
            'Group chat is not supported by the backend yet.';
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
        } catch (err) {
            console.error('Could not send message:', err);

            this.error =
                'Connecting to chat server. Please retry in a moment.';
        }
    }

    title(c: Chat): string {
        if (c.type === 'GROUP') {
            return c.name || 'Group';
        }

        return c.members?.find(
            m => m.id !== this.me?.userId
        )?.displayName || c.name || 'Private chat';
    }

    logout(): void {
        this.chat.disconnect();
        this.sub?.unsubscribe();
        this.sub = undefined;

        localStorage.removeItem('chat_token');
        localStorage.removeItem('chat_user');

        this.me = undefined;
        this.active = undefined;
        this.messages = [];
        this.chats = [];
        this.users = [];
        this.draft = '';
        this.error = '';
        this.authError = '';
        this.page = 'login';
    }

    ngOnDestroy(): void {
        this.sub?.unsubscribe();
        this.chat.disconnect();
    }

    login(): void {
        this.authError = '';

        if (!this.email.trim() || !this.password) {
            this.authError = 'Enter your email and password.';
            return;
        }

        this.authLoading = true;

        this.auth.login(this.email.trim(), this.password).subscribe({
            next: response => {
                this.me = {
                    userId: response.userId,
                    displayName: response.displayName
                };

                localStorage.setItem('chat_token', response.token);
                localStorage.setItem(
                    'chat_user',
                    JSON.stringify(this.me)
                );

                this.page = 'chat';
                this.chat.connect(response.token);
                this.loadChatData();
                this.authLoading = false;
            },
            error: err => {
                this.authError = err.error?.message
                    || 'Login failed. Check your credentials.';

                this.authLoading = false;
            }
        });
    }

    register(): void {
        this.authError = '';

        if (
            !this.displayName.trim()
            || !this.email.trim()
            || !this.password
        ) {
            this.authError = 'Complete all fields.';
            return;
        }

        this.authLoading = true;

        this.auth.register(
            this.displayName.trim(),
            this.email.trim(),
            this.password
        ).subscribe({
            next: response => {
                this.me = {
                    userId: response.userId,
                    displayName: response.displayName
                };

                localStorage.setItem('chat_token', response.token);
                localStorage.setItem(
                    'chat_user',
                    JSON.stringify(this.me)
                );

                this.page = 'chat';
                this.chat.connect(response.token);
                this.loadChatData();
                this.authLoading = false;
            },
            error: err => {
                this.authError = err.error?.message
                    || 'Registration failed.';

                this.authLoading = false;
            }
        });
    }

    showLogin(): void {
        this.page = 'login';
        this.authError = '';
    }

    showRegister(): void {
        this.page = 'register';
        this.authError = '';
    }
}
