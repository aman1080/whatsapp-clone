export interface Message {
  id?: number;
  chatId?: number;
  senderId: number;
  senderName?: string;
  content: string;
  createdAt?: string;
}