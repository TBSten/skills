import type { ReactNode } from "react";
import { Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";

interface MutateButtonProps
  extends Omit<React.ComponentProps<typeof Button>, "disabled"> {
  isPending: boolean;
  pendingText?: string;
  children: ReactNode;
}

export function MutateButton({
  isPending,
  pendingText,
  children,
  ...props
}: MutateButtonProps) {
  return (
    <Button disabled={isPending} {...props}>
      {isPending ? (
        <>
          <Loader2 className="size-4 animate-spin" />
          {pendingText ?? children}
        </>
      ) : (
        children
      )}
    </Button>
  );
}
