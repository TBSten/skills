import { Outlet } from "@tanstack/react-router";
import { Header } from "./header";
import { Toaster } from "@/components/ui/sonner";

export function AppLayout() {
  return (
    <div className="flex min-h-screen flex-col bg-background">
      <Header />
      <main className="flex-1">
        <Outlet />
      </main>
      <Toaster />
    </div>
  );
}
