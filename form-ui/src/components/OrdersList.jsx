import { useEffect, useMemo, useState } from "react";
import { Copy, Edit3, Eye, Loader2, RefreshCcw, Search } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { getAllOrders } from "../services/api";

function formatDateTime(value) {
  if (!value) {
    return "-";
  }
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString();
}

export default function OrdersList({ onNotify }) {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [search, setSearch] = useState("");
  const navigate = useNavigate();

  const filteredOrders = useMemo(() => {
    const term = search.trim().toLowerCase();
    if (!term) {
      return orders;
    }
    return orders.filter((order) => {
      return (
        String(order.orderId || "").toLowerCase().includes(term) ||
        String(order.userName || "").toLowerCase().includes(term) ||
        String(order.formVersionId || "").toLowerCase().includes(term)
      );
    });
  }, [orders, search]);

  const summary = useMemo(() => {
    return orders.reduce(
      (acc, order) => {
        acc.total += 1;
        if (order.orderStatus === "COMMITTED") {
          acc.committed += 1;
        }
        if (order.orderStatus === "WIP") {
          acc.wip += 1;
        }
        return acc;
      },
      { total: 0, committed: 0, wip: 0 }
    );
  }, [orders]);

  const loadOrders = async () => {
    setLoading(true);
    setError("");
    try {
      const response = await getAllOrders();
      setOrders(Array.isArray(response) ? response : []);
    } catch (apiError) {
      const message = apiError.message || "Unable to load orders";
      setError(message);
      onNotify({ type: "error", title: "Order List Load Failed", message });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadOrders();
  }, []);

  const copyOrderId = async (orderId) => {
    await navigator.clipboard.writeText(orderId);
    onNotify({ title: "Copied", message: `${orderId} copied to clipboard` });
  };

  const openHistory = (orderId) => {
    navigate(`/history?orderId=${encodeURIComponent(orderId)}`);
  };

  const resumeDraft = (orderId) => {
    navigate(`/orders?resumeOrderId=${encodeURIComponent(orderId)}`);
  };

  return (
    <section className="space-y-6">
      <article className="glass-card p-6">
        <div className="mb-5 grid gap-3 sm:grid-cols-3">
          <div className="rounded-lg border border-slate-700 bg-slate-900/40 p-3 text-center">
            <p className="text-xs uppercase tracking-wide text-slate-300">Orders</p>
            <p className="mt-1 text-2xl font-semibold text-white">{summary.total}</p>
          </div>
          <div className="rounded-lg border border-green-500/40 bg-green-500/10 p-3 text-center">
            <p className="text-xs uppercase tracking-wide text-green-300">Latest Committed</p>
            <p className="mt-1 text-2xl font-semibold text-green-200">{summary.committed}</p>
          </div>
          <div className="rounded-lg border border-yellow-500/40 bg-yellow-500/10 p-3 text-center">
            <p className="text-xs uppercase tracking-wide text-yellow-300">Latest WIP</p>
            <p className="mt-1 text-2xl font-semibold text-yellow-200">{summary.wip}</p>
          </div>
        </div>

        <div className="mb-4 flex flex-wrap items-end gap-3">
          <label className="block min-w-[260px] flex-1">
            <span className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-300">
              Search Orders
            </span>
            <div className="flex gap-2">
              <input
                className="form-input"
                placeholder="Order ID, user, schema..."
                value={search}
                onChange={(event) => setSearch(event.target.value)}
              />
              <button className="secondary-btn" type="button">
                <Search className="h-4 w-4" />
                Filter
              </button>
            </div>
          </label>

          <button className="secondary-btn" type="button" onClick={loadOrders} disabled={loading}>
            <RefreshCcw className={`h-4 w-4 ${loading ? "animate-spin" : ""}`} />
            Refresh
          </button>
        </div>

        {error ? (
          <div className="mb-4 rounded-lg border border-red-500/50 bg-red-500/20 px-3 py-2 text-sm text-red-200">
            {error}
          </div>
        ) : null}

        {loading ? (
          <div className="flex items-center gap-2 text-sm text-slate-300">
            <Loader2 className="h-4 w-4 animate-spin" /> Loading orders...
          </div>
        ) : filteredOrders.length === 0 ? (
          <p className="text-sm text-slate-300">No orders found.</p>
        ) : (
          <div className="overflow-auto rounded-xl border border-slate-700 bg-slate-900/40">
            <table className="min-w-full text-left text-sm text-slate-200">
              <thead className="bg-slate-900/90 text-xs uppercase tracking-wide text-slate-300">
                <tr>
                  <th className="px-4 py-3">Order ID</th>
                  <th className="px-4 py-3">Latest Version</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="px-4 py-3">Schema</th>
                  <th className="px-4 py-3">Version Counts</th>
                  <th className="px-4 py-3">Updated By</th>
                  <th className="px-4 py-3">Updated At</th>
                  <th className="px-4 py-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredOrders.map((order) => (
                  <tr key={order.orderId} className="border-t border-slate-700/70 hover:bg-slate-800/50">
                    <td className="px-4 py-3 font-semibold text-white">{order.orderId}</td>
                    <td className="px-4 py-3">{order.latestVersionNumber}</td>
                    <td className="px-4 py-3">
                      <span
                        className={`rounded px-2 py-0.5 text-xs font-semibold ${
                          order.orderStatus === "COMMITTED" ? "status-committed" : "status-wip"
                        }`}
                      >
                        {order.orderStatus}
                      </span>
                    </td>
                    <td className="px-4 py-3">{order.formVersionId || "-"}</td>
                    <td className="px-4 py-3 text-xs text-slate-300">
                      Total {order.totalVersions || 0} | C {order.committedVersions || 0} | W{" "}
                      {order.wipVersions || 0}
                    </td>
                    <td className="px-4 py-3">{order.userName || "-"}</td>
                    <td className="px-4 py-3 text-xs">{formatDateTime(order.timestamp)}</td>
                    <td className="px-4 py-3">
                      <div className="flex justify-end gap-2">
                        <button className="secondary-btn px-3 py-1.5" type="button" onClick={() => copyOrderId(order.orderId)}>
                          <Copy className="h-3.5 w-3.5" /> Copy
                        </button>
                        <button className="primary-btn px-3 py-1.5" type="button" onClick={() => openHistory(order.orderId)}>
                          <Eye className="h-3.5 w-3.5" /> History
                        </button>
                        {order.orderStatus === "WIP" ? (
                          <button
                            className="secondary-btn px-3 py-1.5"
                            type="button"
                            onClick={() => resumeDraft(order.orderId)}
                          >
                            <Edit3 className="h-3.5 w-3.5" /> Resume Draft
                          </button>
                        ) : null}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </article>
    </section>
  );
}
