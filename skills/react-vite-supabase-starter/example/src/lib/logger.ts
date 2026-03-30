type LogContext = Record<string, unknown>;

class Logger {
  private globalContext: LogContext = {};

  setContext(context: LogContext) {
    this.globalContext = { ...this.globalContext, ...context };
  }

  error(message: string, context?: LogContext) {
    const merged = { ...this.globalContext, ...context };
    const errorObj = context?.error;
    if (errorObj instanceof Error) {
      merged.stack = errorObj.stack;
    }
    console.error(`[ERROR] ${message}`, merged);
  }

  warn(message: string, context?: LogContext) {
    console.warn(`[WARN] ${message}`, { ...this.globalContext, ...context });
  }

  info(message: string, context?: LogContext) {
    console.info(`[INFO] ${message}`, { ...this.globalContext, ...context });
  }
}

export const logger = new Logger();
