import { Link } from "@tanstack/react-router";
import { Button } from "@/components/ui/button";

// 404 ページ (router.tsx の notFoundComponent)
export function NotFoundPage() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-4 bg-background p-4">
      <p className="text-6xl font-bold text-muted-foreground">404</p>
      <p className="text-sm text-muted-foreground">Page not found</p>
      <Button asChild variant="outline">
        <Link to="/">Back to home</Link>
      </Button>
    </div>
  );
}
