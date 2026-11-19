import log from 'loglevel';

const MAX = 500;
const ring: string[] = [];

function push(level: string, msg: string) {
  ring.push(`${new Date().toISOString()} ${level} ${msg}`);
  if (ring.length > MAX) ring.shift();
}

export const logger = {
  info: (msg: string) => { push('info', msg); log.info(msg); },
  warn: (msg: string) => { push('warn', msg); log.warn(msg); },
  error: (msg: string) => { push('error', msg); log.error(msg); },
};

export function recentLogs(): string[] {
  return [...ring];
}
