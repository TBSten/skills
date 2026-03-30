import { logger } from "./logger";

export function setupGlobalErrorHandlers() {
  window.addEventListener("error", (event) => {
    logger.error("Uncaught error", {
      error: event.error,
      message: event.message,
    });
  });

  window.addEventListener("unhandledrejection", (event) => {
    logger.error("Unhandled rejection", {
      error: event.reason,
    });
  });
}
