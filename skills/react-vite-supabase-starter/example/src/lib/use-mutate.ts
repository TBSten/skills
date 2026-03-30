import { useMutation, useQueryClient } from "@tanstack/react-query";
import { showSuccess, showError } from "./toast";

interface UseMutateOptions<TVariables> {
  mutationFn: (variables: TVariables) => Promise<unknown>;
  invalidateKeys?: string[][];
  successMessage?: string;
  errorMessage?: string;
  onSuccess?: () => void;
}

export function useMutate<TVariables>({
  mutationFn,
  invalidateKeys,
  successMessage,
  errorMessage,
  onSuccess,
}: UseMutateOptions<TVariables>) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn,
    onSuccess: () => {
      if (invalidateKeys) {
        invalidateKeys.forEach((key) =>
          queryClient.invalidateQueries({ queryKey: key }),
        );
      }
      if (successMessage) showSuccess(successMessage);
      onSuccess?.();
    },
    onError: (error) => {
      showError(errorMessage ?? error.message);
    },
  });
}
