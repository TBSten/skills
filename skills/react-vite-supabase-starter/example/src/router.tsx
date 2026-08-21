import type { QueryClient } from "@tanstack/react-query";
import {
  createRootRouteWithContext,
  createRoute,
  createRouter,
  redirect,
} from "@tanstack/react-router";
import type { User } from "@supabase/supabase-js";
import { AppLayout } from "@/components/layout/app-layout";
import { queryClient } from "@/lib/query-client";
import { ErrorPage } from "@/pages/error";
import { HomePage } from "@/pages/home";
import { LoginPage } from "@/pages/login";
import { NotFoundPage } from "@/pages/not-found";

// RouterProvider (App.tsx) から注入されるルーターコンテキスト
export interface RouterContext {
  user: User | null;
  userLoading: boolean;
  queryClient: QueryClient;
}

const rootRoute = createRootRouteWithContext<RouterContext>()({
  notFoundComponent: NotFoundPage,
  errorComponent: ErrorPage,
});

// 認証ガード付きレイアウトルート:
// 未認証ユーザーは beforeLoad で /login へリダイレクトする
const authedLayoutRoute = createRoute({
  getParentRoute: () => rootRoute,
  id: "authed",
  beforeLoad: ({ context }) => {
    if (!context.user) {
      throw redirect({ to: "/login" });
    }
  },
  component: AppLayout,
});

const homeRoute = createRoute({
  getParentRoute: () => authedLayoutRoute,
  path: "/",
  component: HomePage,
});

// 認証済みなら /login からホームへ戻す
const loginRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/login",
  beforeLoad: ({ context }) => {
    if (context.user) {
      throw redirect({ to: "/" });
    }
  },
  component: LoginPage,
});

const routeTree = rootRoute.addChildren([
  authedLayoutRoute.addChildren([homeRoute]),
  loginRoute,
]);

export const router = createRouter({
  routeTree,
  // 初期値。実際の値は App.tsx の RouterProvider context で毎レンダー上書きされる
  context: {
    user: null,
    userLoading: true,
    queryClient,
  },
});

// Link の to などに型推論を効かせるためのモジュール拡張
declare module "@tanstack/react-router" {
  interface Register {
    router: typeof router;
  }
}
