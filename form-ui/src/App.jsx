import { useCallback, useMemo, useState } from "react";
import { Navigate, Route, Routes } from "react-router-dom";
import { AlertCircle, CheckCircle2, X } from "lucide-react";
import Dashboard from "./components/Dashboard";
import OrderForm from "./components/OrderForm";
import OrdersList from "./components/OrdersList";
import VersionHistory from "./components/VersionHistory";
import SchemaManager from "./components/SchemaManager";

function Toast({ toast, onClose }) {
  const isError = toast.type === "error";

  return (
    <div
      className={`pointer-events-auto flex items-start gap-3 rounded-xl border px-4 py-3 shadow-glass backdrop-blur-lg animate-slide-up ${
        isError
          ? "border-red-500/50 bg-red-600/20"
          : "border-green-500/50 bg-green-600/20"
      }`}
    >
      {isError ? (
        <AlertCircle className="mt-0.5 h-5 w-5 text-red-300" />
      ) : (
        <CheckCircle2 className="mt-0.5 h-5 w-5 text-green-300" />
      )}
      <div className="flex-1">
        <p className="text-sm font-semibold text-white">{toast.title}</p>
        {toast.message ? (
          <p className="mt-1 text-sm text-slate-200">{toast.message}</p>
        ) : null}
      </div>
      <button
        className="rounded p-1 text-slate-300 hover:bg-white/10 hover:text-white"
        onClick={() => onClose(toast.id)}
        aria-label="Close notification"
      >
        <X className="h-4 w-4" />
      </button>
    </div>
  );
}

export default function App() {
  const [toasts, setToasts] = useState([]);

  const notify = useCallback((payload) => {
    const id = crypto.randomUUID();
    const nextToast = {
      id,
      title: payload.title,
      message: payload.message,
      type: payload.type || "success"
    };

    setToasts((prev) => [nextToast, ...prev].slice(0, 4));

    window.setTimeout(() => {
      setToasts((prev) => prev.filter((toast) => toast.id !== id));
    }, 4500);
  }, []);

  const closeToast = useCallback((id) => {
    setToasts((prev) => prev.filter((toast) => toast.id !== id));
  }, []);

  const routes = useMemo(
    () => (
      <Routes>
        <Route path="/" element={<Dashboard />}>
          <Route index element={<Navigate to="/orders" replace />} />
          <Route path="orders" element={<OrderForm onNotify={notify} />} />
          <Route path="orders-list" element={<OrdersList onNotify={notify} />} />
          <Route path="history" element={<VersionHistory onNotify={notify} />} />
          <Route path="schemas" element={<SchemaManager onNotify={notify} />} />
        </Route>
        <Route path="*" element={<Navigate to="/orders" replace />} />
      </Routes>
    ),
    [notify]
  );

  return (
    <>
      {routes}
      <div className="pointer-events-none fixed right-4 top-4 z-50 flex w-full max-w-sm flex-col gap-3">
        {toasts.map((toast) => (
          <Toast key={toast.id} toast={toast} onClose={closeToast} />
        ))}
      </div>
    </>
  );
}
