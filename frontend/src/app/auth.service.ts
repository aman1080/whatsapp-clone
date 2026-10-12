import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

const API_URL = 'http://localhost:8080/api/auth';

export interface AuthResponse {
  token: string;
  userId: number;
  email: string;
  displayName: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  constructor(private http: HttpClient) {}

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${API_URL}/login`, {
      email,
      password
    }).pipe(tap(response => this.saveSession(response)));
  }

  register(
    displayName: string,
    email: string,
    password: string
  ): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${API_URL}/register`, {
      displayName,
      email,
      password
    }).pipe(tap(response => this.saveSession(response)));
  }

  private saveSession(response: AuthResponse): void {
    localStorage.setItem('chat_token', response.token);
    localStorage.setItem('chat_user', JSON.stringify({
      userId: response.userId,
      email: response.email,
      displayName: response.displayName
    }));
  }
}