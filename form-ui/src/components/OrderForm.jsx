import { useMemo, useState } from "react";
import {
  CirclePlus,
  Loader2,
  Save,
  Send,
  Trash2,
  CheckCircle2,
  Clipboard
} from "lucide-react";
import { createOrder } from "../services/api";

const emptyItem = {
  itemNumber: "",
  quantity: "",
  price: ""
};

function buildPayload(formData, isFinalSave) {
  const locations = formData.deliveryLocations
    .split(",")
    .map((loc) => loc.trim())
    .filter(Boolean);

  const items = formData.items
    .filter((item) => item.itemNumber.trim())
    .map((item) => ({
      itemNumber: item.itemNumber.trim(),
      quantity: Number(item.quantity || 0),
      price: Number(item.price || 0)
    }));

  return {
    orderId: formData.orderId.trim(),
    deliveryLocations: locations,
    data: {
      deliveryCompany: {
        companyId: formData.companyId.trim(),
        name: formData.companyName.trim()
      },
      items
    },
    finalSave: isFinalSave,
    changeDescription: formData.changeDescription.trim() || undefined
  };
}

export default function OrderForm({ onNotify }) {
  const [formData, setFormData] = useState({
    orderId: "",
    deliveryLocations: "",
    companyId: "",
    companyName: "",
    changeDescription: "",
    items: [{ ...emptyItem }]
  });

  const [loadingAction, setLoadingAction] = useState("");
  const [error, setError] = useState("");
  const [result, setResult] = useState(null);

  const canSubmit = useMemo(() => {
    return (
      /^ORD-[0-9]{5}$/.test(formData.orderId.trim()) &&
      formData.deliveryLocations.trim().length > 0 &&
      formData.companyId.trim().length > 0 &&
      formData.companyName.trim().length > 0
    );
  }, [formData]);

  const updateField = (key, value) => {
    setFormData((prev) => ({ ...prev, [key]: value }));
  };

  const updateItem = (index, key, value) => {
    setFormData((prev) => {
      const items = [...prev.items];
      items[index] = { ...items[index], [key]: value };
      return { ...prev, items };
    });
  };

  const addItem = () => {
    setFormData((prev) => ({ ...prev, items: [...prev.items, { ...emptyItem }] }));
  };

  const removeItem = (index) => {
    setFormData((prev) => {
      if (prev.items.length === 1) {
        return prev;
      }
      return {
        ...prev,
        items: prev.items.filter((_, itemIndex) => itemIndex !== index)
      };
    });
  };

  const handleSubmit = async (isFinalSave) => {
    setError("");
    setLoadingAction(isFinalSave ? "submit" : "draft");

    try {
      const payload = buildPayload(formData, isFinalSave);
      const response = await createOrder(payload);
      setResult(response);

      onNotify({
        title: isFinalSave ? "Order Submitted" : "Draft Saved",
        message: `Version ${response.orderVersionNumber} created for ${response.orderId}`
      });

      setFormData((prev) => ({
        ...prev,
        changeDescription: ""
      }));
    } catch (apiError) {
      const message = apiError.message || "Unable to create order version";
      setError(message);
      onNotify({
        type: "error",
        title: "Order Save Failed",
        message
      });
    } finally {
      setLoadingAction("");
    }
  };

  const copyOrderId = async () => {
    if (!result?.orderId) {
      return;
    }

    await navigator.clipboard.writeText(result.orderId);
    onNotify({ title: "Order ID Copied", message: `${result.orderId} copied to clipboard` });
  };

  return (
    <section className="grid gap-6 lg:grid-cols-[2fr,1fr]">
      <article className="glass-card p-6">
        <h2 className="mb-4 text-xl font-semibold text-white">Create Order Version</h2>
        <div className="grid gap-4 md:grid-cols-2">
          <label className="block">
            <span className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-300">
              Order ID
            </span>
            <input
              className="form-input"
              value={formData.orderId}
              onChange={(event) => updateField("orderId", event.target.value.toUpperCase())}
              placeholder="ORD-12345"
              maxLength={9}
            />
          </label>

          <label className="block">
            <span className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-300">
              Delivery Locations
            </span>
            <input
              className="form-input"
              value={formData.deliveryLocations}
              onChange={(event) => updateField("deliveryLocations", event.target.value)}
              placeholder="New York, Boston"
            />
          </label>

          <label className="block">
            <span className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-300">
              Delivery Company ID
            </span>
            <input
              className="form-input"
              value={formData.companyId}
              onChange={(event) => updateField("companyId", event.target.value)}
              placeholder="DC-001"
            />
          </label>

          <label className="block">
            <span className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-300">
              Delivery Company Name
            </span>
            <input
              className="form-input"
              value={formData.companyName}
              onChange={(event) => updateField("companyName", event.target.value)}
              placeholder="FastShip Logistics"
            />
          </label>

          <label className="block md:col-span-2">
            <span className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-300">
              Change Description
            </span>
            <input
              className="form-input"
              value={formData.changeDescription}
              onChange={(event) => updateField("changeDescription", event.target.value)}
              placeholder="Initial draft"
            />
          </label>
        </div>

        <div className="mt-6 rounded-xl border border-slate-700 bg-slate-900/40 p-4">
          <div className="mb-3 flex items-center justify-between">
            <h3 className="text-sm font-semibold uppercase tracking-wider text-slate-200">
              Items
            </h3>
            <button className="secondary-btn" type="button" onClick={addItem}>
              <CirclePlus className="h-4 w-4" /> Add Item
            </button>
          </div>

          <div className="space-y-3">
            {formData.items.map((item, index) => (
              <div
                key={`item-${index}`}
                className="grid gap-3 rounded-lg border border-slate-700 bg-slate-950/60 p-3 md:grid-cols-[2fr_1fr_1fr_auto]"
              >
                <input
                  className="form-input"
                  placeholder="Item Number"
                  value={item.itemNumber}
                  onChange={(event) => updateItem(index, "itemNumber", event.target.value)}
                />
                <input
                  className="form-input"
                  placeholder="Quantity"
                  type="number"
                  min="0"
                  value={item.quantity}
                  onChange={(event) => updateItem(index, "quantity", event.target.value)}
                />
                <input
                  className="form-input"
                  placeholder="Price"
                  type="number"
                  min="0"
                  step="0.01"
                  value={item.price}
                  onChange={(event) => updateItem(index, "price", event.target.value)}
                />
                <button
                  className="secondary-btn justify-center"
                  type="button"
                  onClick={() => removeItem(index)}
                  title="Remove item"
                >
                  <Trash2 className="h-4 w-4" />
                </button>
              </div>
            ))}
          </div>
        </div>

        {error ? (
          <div className="mt-4 rounded-lg border border-red-500/50 bg-red-500/20 px-3 py-2 text-sm text-red-200">
            {error}
          </div>
        ) : null}

        <div className="mt-6 flex flex-wrap gap-3">
          <button
            className="secondary-btn"
            type="button"
            disabled={!canSubmit || loadingAction.length > 0}
            onClick={() => handleSubmit(false)}
          >
            {loadingAction === "draft" ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              <Save className="h-4 w-4" />
            )}
            Save as Draft
          </button>

          <button
            className="primary-btn"
            type="button"
            disabled={!canSubmit || loadingAction.length > 0}
            onClick={() => handleSubmit(true)}
          >
            {loadingAction === "submit" ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              <Send className="h-4 w-4" />
            )}
            Submit Order
          </button>
        </div>
      </article>

      <aside className="glass-card p-5">
        <h3 className="mb-3 text-lg font-semibold text-white">Creation Result</h3>
        {result ? (
          <div className="space-y-3 animate-slide-up">
            <div className="flex items-center gap-2 rounded-lg border border-green-500/50 bg-green-500/20 px-3 py-2 text-sm text-green-200">
              <CheckCircle2 className="h-4 w-4" />
              Version {result.orderVersionNumber} created successfully
            </div>
            <div className="rounded-lg border border-slate-700 bg-slate-900/50 p-3 text-sm">
              <p>
                <span className="text-slate-400">Order ID:</span>{" "}
                <span className="font-semibold text-white">{result.orderId}</span>
              </p>
              <p>
                <span className="text-slate-400">Status:</span>{" "}
                <span
                  className={`rounded px-2 py-0.5 text-xs font-semibold ${
                    result.orderStatus === "COMMITTED" ? "status-committed" : "status-wip"
                  }`}
                >
                  {result.orderStatus}
                </span>
              </p>
              <p>
                <span className="text-slate-400">Schema:</span> {result.formVersionId}
              </p>
              <p>
                <span className="text-slate-400">User:</span> {result.userName}
              </p>
            </div>
            <button className="secondary-btn" type="button" onClick={copyOrderId}>
              <Clipboard className="h-4 w-4" /> Copy Order ID
            </button>
          </div>
        ) : (
          <p className="text-sm text-slate-300">
            Submit an order draft or final version to see response details here.
          </p>
        )}
      </aside>
    </section>
  );
}
