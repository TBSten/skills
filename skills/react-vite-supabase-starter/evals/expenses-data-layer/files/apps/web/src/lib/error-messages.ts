const ERROR_MAP: Record<string, string> = {
  "23505": "Already exists",
  "23503": "Referenced record not found",
  "42501": "Permission denied",
  invalid_credentials: "Invalid credentials",
  user_not_found: "User not found",
};

export function getErrorMessage(code?: string, message?: string): string {
  if (code && ERROR_MAP[code]) {
    return ERROR_MAP[code];
  }
  if (message?.includes("Failed to fetch") || message?.includes("NetworkError")) {
    return "Network error. Please check your connection.";
  }
  return "An unexpected error occurred";
}
