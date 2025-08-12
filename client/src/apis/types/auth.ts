export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  id: number;
  email: string;
  role: string;
  roleName: string;
  accessToken: string;
  lastLoginAt: string;
}