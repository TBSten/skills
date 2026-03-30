import { Link, useMatchRoute } from "@tanstack/react-router";
import { cn } from "@/lib/utils";
import type { LucideIcon } from "lucide-react";

interface NavLinkProps {
  to: string;
  icon: LucideIcon;
  label: string;
}

export function NavLink({ to, icon: Icon, label }: NavLinkProps) {
  const matchRoute = useMatchRoute();
  const isActive = !!matchRoute({ to, fuzzy: to !== "/" });

  return (
    <Link
      to={to}
      className={cn(
        "flex items-center gap-1.5 rounded-md px-2 py-2 text-sm font-medium transition-colors md:px-3",
        isActive
          ? "bg-primary text-primary-foreground"
          : "text-muted-foreground hover:bg-muted hover:text-foreground",
      )}
    >
      <Icon className="size-4" />
      <span className="hidden md:inline">{label}</span>
    </Link>
  );
}
