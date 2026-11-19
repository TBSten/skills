import type { PostgrestError } from "@supabase/supabase-js";
import { getErrorMessage } from "./error-messages";
import { logger } from "./logger";

export class ApiError extends Error {
  code?: string;
  originalError?: PostgrestError;

  constructor(message: string, code?: string, originalError?: PostgrestError) {
    super(message);
    this.name = "ApiError";
    this.code = code;
    this.originalError = originalError;
  }
}

export async function handleSupabaseResult<T>(
  result: { data: T | null; error: PostgrestError | null },
): Promise<T> {
  if (result.error) {
    logger.error("Supabase error", { error: result.error });
    throw new ApiError(
      getErrorMessage(result.error.code, result.error.message),
      result.error.code,
      result.error,
    );
  }
  return result.data as T;
}
