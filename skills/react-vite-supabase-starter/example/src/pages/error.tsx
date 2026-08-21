import { useEffect } from "react";
import { Link, type ErrorComponentProps } from "@tanstack/react-router";
import { Button } from "@/components/ui/button";
import { getErrorMessage } from "@/lib/error-messages";
import { logger } from "@/lib/logger";

// 500 相当のエラーページ (router.tsx の errorComponent)
export function ErrorPage({ error }: ErrorComponentProps) {
  useEffect(() => {
    logger.error("Route error", { error });
  }, [error]);

  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-4 bg-background p-4">
      <p className="text-6xl font-bold text-destructive">500</p>
      <p className="text-sm text-muted-foreground">
        {getErrorMessage(undefined, error.message)}
      </p>
      <div className="flex gap-2">
        <Button variant="outline" onClick={() => window.location.reload()}>
          Reload
        </Button>
        <Button asChild>
          <Link to="/">Back to home</Link>
        </Button>
      </div>
    </div>
  );
}
