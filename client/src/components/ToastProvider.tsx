import type {ReactNode} from 'react';
import { createPortal } from 'react-dom';
import { useToastStore } from '@stores/toast';

export function ToastProvider({ children }: { children: ReactNode }) {
  const toasts = useToastStore((state) => state.toasts); // Get toasts from Zustand store

  return (
    <>
      {children}
      {createPortal(
        <div style={{
          position: 'fixed',
          top: '20px',
          right: '20px',
          zIndex: 1000,
          display: 'flex',
          flexDirection: 'column',
          gap: '10px',
        }}>
          {toasts.map((toast) => (
            <div
              key={toast.id}
              style={{
                padding: '10px 20px',
                borderRadius: '5px',
                color: 'white',
                backgroundColor: toast.type === 'error' ? '#dc3545' : toast.type === 'success' ? '#28a745' : '#007bff',
                boxShadow: '0 2px 10px rgba(0,0,0,0.1)',
                minWidth: '200px',
                textAlign: 'center',
              }}
            >
              {toast.message}
            </div>
          ))}
        </div>,
        document.getElementById('toast-root') || document.body
      )}
    </>
  );
}