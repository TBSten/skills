# Data Layer Abstraction Pattern

## Principle

Page components must never import the Supabase client directly. All data access goes through custom hooks in `src/data/<domain>/`.

## Directory Structure

```
src/data/
├── auth/
│   ├── use-login.ts
│   └── use-user-profile.ts
├── <domain-a>/
│   ├── use-<resource>.ts       # Query (read)
│   └── use-<mutation>.ts       # Mutation (write)
└── <domain-b>/
    └── ...
```

## Query Hook Pattern

```typescript
import { useQuery } from "@tanstack/react-query";
import { supabase } from "@/lib/supabase";
import { useAuth } from "@/auth/auth-context";

export function useItems() {
  const { user } = useAuth();

  const { data, isLoading } = useQuery({
    queryKey: ["items", user?.id],
    queryFn: async () => {
      const { data, error } = await supabase
        .from("items")
        .select("*")
        .eq("user_id", user!.id);
      if (error) throw error;
      return data;
    },
    enabled: !!user,
  });

  return { items: data ?? [], loading: isLoading };
}
```

## Mutation Hook Pattern

```typescript
import { useMutate } from "@/lib/use-mutate";
import { supabase } from "@/lib/supabase";
import { handleSupabaseResult } from "@/lib/api";

export function useCreateItem() {
  return useMutate({
    mutationFn: async (params: { name: string }) => {
      const result = await supabase
        .from("items")
        .insert({ name: params.name });
      return handleSupabaseResult(result);
    },
    invalidateKeys: [["items"]],
    successMessage: "Item created",
  });
}
```

## Query Key Conventions

Use hierarchical, deterministic keys:

```
["<domain>", "<scope>", ...identifiers]
```

Examples:
- `["items", user?.id]`
- `["items", "detail", itemId]`
- `["items", "monthly", year, month]`

Mutations should invalidate the relevant domain prefix:
- `invalidateKeys: [["items"]]`

## Benefits

- Backend can be swapped by changing only `src/data/` internals
- Consistent caching and invalidation via TanStack Query
- Clear separation of concerns between UI and data
