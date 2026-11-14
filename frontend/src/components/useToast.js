import { createContext, useContext } from 'react';

export const ToastContext = createContext({ show: () => {} });

// const toast = useToast();  toast.show('Saved!', 'success')
export function useToast() {
  return useContext(ToastContext);
}
