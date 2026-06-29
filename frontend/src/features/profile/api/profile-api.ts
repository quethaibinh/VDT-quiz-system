import { apiClient } from "@/lib/http/api-client";
import type { UserProfile, UpdateUserProfileRequest, UpdatePasswordRequest } from "../model/profile-types";

export async function getProfile(): Promise<UserProfile> {
  const response = await apiClient.get<UserProfile>("/v1/api/auth-service/profile");
  return response.data;
}

export async function updateProfile(input: UpdateUserProfileRequest): Promise<UserProfile> {
  const response = await apiClient.put<UserProfile>("/v1/api/auth-service/profile", input);
  return response.data;
}

export async function updatePassword(input: UpdatePasswordRequest): Promise<void> {
  await apiClient.put("/v1/api/auth-service/profile/password", input);
}
