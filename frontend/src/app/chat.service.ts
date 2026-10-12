import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { Observable, Subject } from 'rxjs';
export interface User {id:number;displayName:string;email:string}
export interface Chat {id:number;type:string;name:string|null;members:User[];createdAt:string}
export interface ChatMessage {id:number;chatId:number;senderId:number;senderName:string;content:string;sentAt:string}
export interface AuthResponse {token:string;userId:number;displayName:string;email:string}
@Injectable({providedIn:'root'}) export class ChatService {
 private api='http://localhost:8080/api'; private client?:Client; private groupSubscriptions=new Set<number>(); private messagesSubject=new Subject<ChatMessage>(); messages$:Observable<ChatMessage>=this.messagesSubject.asObservable();
 constructor(private http:HttpClient){}
 register(displayName:string,email:string,password:string){return this.http.post<AuthResponse>(`${this.api}/auth/register`,{displayName,email,password});}
 login(email:string,password:string){return this.http.post<AuthResponse>(`${this.api}/auth/login`,{email,password});}
 users(){return this.http.get<User[]>(`${this.api}/users`);}
 chats(){return this.http.get<Chat[]>(`${this.api}/chats`);}
 privateChat(otherUserId:number){return this.http.post<Chat>(`${this.api}/chats/private`,{otherUserId});}
 group(name:string,memberIds:number[]){return this.http.post<Chat>(`${this.api}/chats/group`,{name,memberIds});}
 history(chatId:number){return this.http.get<ChatMessage[]>(`${this.api}/chats/${chatId}/messages?limit=100`);}
 connect(token:string){this.disconnect();this.client=new Client({webSocketFactory:()=>new SockJS('http://localhost:8080/ws'),connectHeaders:{Authorization:`Bearer ${token}`},reconnectDelay:3000,debug:()=>{}});this.client.onConnect=()=>{this.client?.subscribe('/user/queue/messages',(m:IMessage)=>this.messagesSubject.next(JSON.parse(m.body)));for(const id of this.groupSubscriptions)this.subscribeGroupNow(id);};this.client.activate();}
 subscribeGroup(chatId:number){this.groupSubscriptions.add(chatId);if(this.client?.connected)this.subscribeGroupNow(chatId);}
 private subscribeGroupNow(chatId:number){this.client?.subscribe(`/topic/chat/${chatId}`,(m:IMessage)=>this.messagesSubject.next(JSON.parse(m.body)));}
 send(chatId:number,content:string){if(!this.client?.connected)throw new Error('WebSocket is not connected yet');this.client.publish({destination:'/app/chat.send',body:JSON.stringify({chatId,content})});}
 disconnect(){if(this.client){void this.client.deactivate();this.client=undefined;}this.groupSubscriptions.clear();}
}
