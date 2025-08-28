export interface AuthRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  id: number;
  email: string;
  role: string;
  accessToken: string;
  lastLoginAt: string;
}