import { Home, LogOut } from "lucide-react";
import { Link, useNavigate } from "@tanstack/react-router";
import { useAuth } from "@/auth/auth-context";
import { Button } from "@/components/ui/button";
import { NavLink } from "./nav-link";

// アプリ表示名の差し込み点。
// scripts/scaffold.sh が --app-name の値で "<app-name>" を置換する。
// 手動セットアップ時はこの文字列を直接書き換える。
const APP_NAME = "<app-name>";

export function Header() {
  const { signOut } = useAuth();
  const navigate = useNavigate();

  async function handleSignOut() {
    await signOut();
    await navigate({ to: "/login" });
  }

  return (
    <header className="border-b bg-card">
      <div className="mx-auto flex h-14 max-w-5xl items-center justify-between gap-4 px-4">
        <Link to="/" className="text-base font-bold tracking-tight">
          {APP_NAME}
        </Link>
        <nav className="flex flex-1 items-center gap-1">
          <NavLink to="/" icon={Home} label="Home" />
        </nav>
        {/* md 未満ではテキストが hidden になりアイコンのみのボタンになるため、
            accessible name を aria-label で常に与える */}
        <Button
          variant="ghost"
          size="sm"
          onClick={handleSignOut}
          aria-label="Sign out"
        >
          <LogOut className="size-4" />
          <span className="hidden md:inline">Sign out</span>
        </Button>
      </div>
    </header>
  );
}
