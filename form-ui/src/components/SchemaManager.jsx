import { useEffect, useMemo, useState } from "react";
import {
  CheckCircle2,
  Eye,
  FileJson,
  Loader2,
  PlusCircle,
  RefreshCcw,
  Trash2
} from "lucide-react";
import {
  activateSchema,
  createSchema,
  deprecateSchema,
  getActiveSchema,
  getAllSchemas,
  getSchemaByVersionId
} from "../services/api";

const schemaTemplate = {
  fields: [
    {
      fieldName: "orderId",
      fieldType: "text",
      label: "Order ID",
      required: true
    },
    {
      fieldName: "deliveryLocations",
      fieldType: "multivalue",
      label: "Delivery Locations"
    }
  ]
};

export default function SchemaManager({ onNotify }) {
  const [activeSchema, setActiveSchema] = useState(null);
  const [schemas, setSchemas] = useState([]);
  const [selectedSchemaDetails, setSelectedSchemaDetails] = useState(null);
  const [loadingSelectedDetails, setLoadingSelectedDetails] = useState(false);
  const [selectedSchemaVersionId, setSelectedSchemaVersionId] = useState("");
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [activatingId, setActivatingId] = useState("");
  const [deprecatingId, setDeprecatingId] = useState("");
  const [error, setError] = useState("");

  const [formData, setFormData] = useState({
    formVersionId: "",
    formName: "",
    description: "",
    fieldDefinitionsText: JSON.stringify(schemaTemplate, null, 2)
  });

  const sortedSchemas = useMemo(() => {
    return [...schemas].sort((a, b) => {
      const aDate = new Date(a.createdDate || 0).getTime();
      const bDate = new Date(b.createdDate || 0).getTime();
      return bDate - aDate;
    });
  }, [schemas]);

  const loadSchemaDetails = async (formVersionId, showErrorToast = true) => {
    if (!formVersionId) {
      setSelectedSchemaVersionId("");
      setSelectedSchemaDetails(null);
      return;
    }

    setLoadingSelectedDetails(true);
    setSelectedSchemaVersionId(formVersionId);

    try {
      const details = await getSchemaByVersionId(formVersionId);
      setSelectedSchemaDetails(details);
    } catch (apiError) {
      const message = apiError.message || "Unable to load schema details";
      setSelectedSchemaDetails(null);

      if (showErrorToast) {
        onNotify({ type: "error", title: "Schema Detail Load Failed", message });
      }
    } finally {
      setLoadingSelectedDetails(false);
    }
  };

  const loadSchemas = async () => {
    setLoading(true);
    setError("");

    try {
      const [active, all] = await Promise.allSettled([getActiveSchema(), getAllSchemas()]);

      let activeValue = null;
      let allSchemas = [];

      if (active.status === "fulfilled") {
        activeValue = active.value;
        setActiveSchema(activeValue);
      } else {
        setActiveSchema(null);
      }

      if (all.status === "fulfilled") {
        allSchemas = all.value;
        setSchemas(allSchemas);
      } else {
        setSchemas([]);
        throw new Error(all.reason?.message || "Unable to load schema list");
      }

      const preferredVersionId =
        selectedSchemaVersionId || activeValue?.formVersionId || allSchemas[0]?.formVersionId;

      if (preferredVersionId) {
        await loadSchemaDetails(preferredVersionId, false);
      } else {
        setSelectedSchemaDetails(null);
        setSelectedSchemaVersionId("");
      }
    } catch (apiError) {
      const message = apiError.message || "Unable to load schemas";
      setError(message);
      onNotify({ type: "error", title: "Schema Load Failed", message });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadSchemas();
  }, []);

  const handleCreateSchema = async () => {
    setSubmitting(true);
    setError("");

    try {
      const fieldDefinitions = JSON.parse(formData.fieldDefinitionsText);
      const payload = {
        formVersionId: formData.formVersionId.trim(),
        formName: formData.formName.trim(),
        description: formData.description.trim(),
        fieldDefinitions
      };

      const response = await createSchema(payload);
      onNotify({
        title: "Schema Created",
        message: `${response.formVersionId} created successfully`
      });

      setFormData((prev) => ({
        ...prev,
        formVersionId: "",
        formName: "",
        description: ""
      }));

      await loadSchemas();
      await loadSchemaDetails(response.formVersionId, false);
    } catch (apiError) {
      const message = apiError.message || "Unable to create schema";
      setError(message);
      onNotify({ type: "error", title: "Schema Create Failed", message });
    } finally {
      setSubmitting(false);
    }
  };

  const handleActivate = async (formVersionId) => {
    setActivatingId(formVersionId);
    setError("");

    try {
      await activateSchema(formVersionId);
      onNotify({
        title: "Schema Activated",
        message: `${formVersionId} is now active`
      });
      await loadSchemas();
      await loadSchemaDetails(formVersionId, false);
    } catch (apiError) {
      const message = apiError.message || "Unable to activate schema";
      setError(message);
      onNotify({ type: "error", title: "Schema Activation Failed", message });
    } finally {
      setActivatingId("");
    }
  };

  const handleDeprecate = async (schema) => {
    if (schema.isActive) {
      onNotify({
        type: "error",
        title: "Invalid Action",
        message: "Activate another schema before deprecating the current active schema."
      });
      return;
    }

    const confirmed = window.confirm(
      `Deprecate schema ${schema.formVersionId}? This will soft-delete the schema.`
    );

    if (!confirmed) {
      return;
    }

    setDeprecatingId(schema.formVersionId);
    setError("");

    try {
      await deprecateSchema(schema.formVersionId);
      onNotify({
        title: "Schema Deprecated",
        message: `${schema.formVersionId} was marked as deprecated`
      });

      await loadSchemas();

      if (selectedSchemaVersionId === schema.formVersionId) {
        const fallback = activeSchema?.formVersionId || "";
        await loadSchemaDetails(fallback, false);
      }
    } catch (apiError) {
      const message = apiError.message || "Unable to deprecate schema";
      setError(message);
      onNotify({ type: "error", title: "Schema Deprecation Failed", message });
    } finally {
      setDeprecatingId("");
    }
  };

  return (
    <section className="grid gap-6 xl:grid-cols-[1.1fr,1fr]">
      <article className="glass-card p-6">
        <div className="mb-4 flex items-center justify-between gap-3">
          <h2 className="text-xl font-semibold text-white">Schema Manager</h2>
          <button className="secondary-btn" type="button" onClick={loadSchemas} disabled={loading}>
            <RefreshCcw className={`h-4 w-4 ${loading ? "animate-spin" : ""}`} /> Refresh
          </button>
        </div>

        {activeSchema ? (
          <div className="mb-5 rounded-xl border border-green-500/40 bg-green-500/10 p-4">
            <p className="text-xs font-semibold uppercase tracking-wider text-green-300">
              Active Schema
            </p>
            <p className="mt-1 text-lg font-semibold text-white">{activeSchema.formName}</p>
            <p className="text-sm text-green-200">Version {activeSchema.formVersionId}</p>
          </div>
        ) : (
          <div className="mb-5 rounded-xl border border-yellow-500/40 bg-yellow-500/10 p-4 text-sm text-yellow-200">
            No active schema found.
          </div>
        )}

        <div className="space-y-4 rounded-xl border border-slate-700 bg-slate-900/40 p-4">
          <h3 className="text-sm font-semibold uppercase tracking-wide text-slate-200">
            Create New Schema
          </h3>

          <div className="grid gap-4 md:grid-cols-2">
            <label className="block">
              <span className="mb-1 block text-xs text-slate-300">Form Version ID</span>
              <input
                className="form-input"
                placeholder="v1.0.0"
                value={formData.formVersionId}
                onChange={(event) =>
                  setFormData((prev) => ({ ...prev, formVersionId: event.target.value }))
                }
              />
            </label>
            <label className="block">
              <span className="mb-1 block text-xs text-slate-300">Form Name</span>
              <input
                className="form-input"
                placeholder="Order Entry Form"
                value={formData.formName}
                onChange={(event) =>
                  setFormData((prev) => ({ ...prev, formName: event.target.value }))
                }
              />
            </label>
          </div>

          <label className="block">
            <span className="mb-1 block text-xs text-slate-300">Description</span>
            <input
              className="form-input"
              placeholder="Initial order form schema"
              value={formData.description}
              onChange={(event) =>
                setFormData((prev) => ({ ...prev, description: event.target.value }))
              }
            />
          </label>

          <label className="block">
            <span className="mb-1 block text-xs text-slate-300">Field Definitions (JSON)</span>
            <textarea
              className="form-input min-h-56 font-mono text-xs"
              value={formData.fieldDefinitionsText}
              onChange={(event) =>
                setFormData((prev) => ({ ...prev, fieldDefinitionsText: event.target.value }))
              }
            />
          </label>

          <button
            className="primary-btn"
            type="button"
            disabled={submitting}
            onClick={handleCreateSchema}
          >
            {submitting ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              <PlusCircle className="h-4 w-4" />
            )}
            Create Schema
          </button>
        </div>

        {error ? (
          <div className="mt-4 rounded-lg border border-red-500/50 bg-red-500/20 px-3 py-2 text-sm text-red-200">
            {error}
          </div>
        ) : null}
      </article>

      <aside className="glass-card p-5">
        <h3 className="mb-3 text-lg font-semibold text-white">Available Schemas</h3>

        {loading ? (
          <div className="flex items-center gap-2 text-sm text-slate-300">
            <Loader2 className="h-4 w-4 animate-spin" /> Loading schemas...
          </div>
        ) : sortedSchemas.length === 0 ? (
          <p className="text-sm text-slate-300">No schemas returned from API.</p>
        ) : (
          <ul className="space-y-3">
            {sortedSchemas.map((schema) => {
              const isActive = Boolean(schema.isActive);
              const isBusy = activatingId === schema.formVersionId;
              const isDeprecating = deprecatingId === schema.formVersionId;
              const isSelected = selectedSchemaVersionId === schema.formVersionId;

              return (
                <li
                  key={schema.id || schema.formVersionId}
                  className={`rounded-xl border p-3 ${
                    isSelected
                      ? "border-blue-400/50 bg-blue-500/10"
                      : isActive
                        ? "border-green-500/40 bg-green-500/10"
                        : "border-slate-700 bg-slate-900/50"
                  }`}
                >
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <p className="font-semibold text-white">{schema.formName}</p>
                      <p className="text-sm text-slate-300">{schema.formVersionId}</p>
                      {schema.description ? (
                        <p className="mt-1 text-xs text-slate-400">{schema.description}</p>
                      ) : null}
                    </div>
                    {isActive ? (
                      <span className="inline-flex items-center gap-1 rounded-full border border-green-500/40 bg-green-500/20 px-2.5 py-1 text-xs font-semibold text-green-200">
                        <CheckCircle2 className="h-3.5 w-3.5" /> Active
                      </span>
                    ) : null}
                  </div>

                  <div className="mt-3 flex flex-wrap gap-2">
                    <button
                      className="secondary-btn"
                      type="button"
                      onClick={() => loadSchemaDetails(schema.formVersionId)}
                    >
                      <Eye className="h-4 w-4" /> View
                    </button>

                    {!isActive ? (
                      <button
                        className="secondary-btn"
                        type="button"
                        onClick={() => handleActivate(schema.formVersionId)}
                        disabled={isBusy || isDeprecating}
                      >
                        {isBusy ? <Loader2 className="h-4 w-4 animate-spin" /> : null}
                        Activate
                      </button>
                    ) : null}

                    {!isActive ? (
                      <button
                        className="secondary-btn border-red-500/50 bg-red-600/20 text-red-100 hover:bg-red-600/30"
                        type="button"
                        onClick={() => handleDeprecate(schema)}
                        disabled={isBusy || isDeprecating}
                      >
                        {isDeprecating ? (
                          <Loader2 className="h-4 w-4 animate-spin" />
                        ) : (
                          <Trash2 className="h-4 w-4" />
                        )}
                        Deprecate
                      </button>
                    ) : null}
                  </div>
                </li>
              );
            })}
          </ul>
        )}

        <div className="mt-5 rounded-xl border border-slate-700 bg-slate-900/50 p-4">
          <p className="mb-2 text-sm font-semibold text-white">Selected Schema Details</p>

          {loadingSelectedDetails ? (
            <p className="inline-flex items-center gap-2 text-sm text-slate-300">
              <Loader2 className="h-4 w-4 animate-spin" /> Loading schema detail...
            </p>
          ) : selectedSchemaDetails ? (
            <div className="space-y-3 text-sm">
              <p>
                <span className="text-slate-400">Version:</span> {selectedSchemaDetails.formVersionId}
              </p>
              <p>
                <span className="text-slate-400">Name:</span> {selectedSchemaDetails.formName}
              </p>
              <p>
                <span className="text-slate-400">Created By:</span> {selectedSchemaDetails.createdBy || "-"}
              </p>
              <p>
                <span className="text-slate-400">Created:</span> {selectedSchemaDetails.createdDate || "-"}
              </p>
              <p className="inline-flex items-center gap-1 text-xs font-semibold uppercase tracking-wide text-slate-300">
                <FileJson className="h-3.5 w-3.5" /> Field Definitions
              </p>
              <pre className="max-h-64 overflow-auto rounded-lg border border-slate-700 bg-slate-950/70 p-3 text-xs text-slate-200">
                {JSON.stringify(selectedSchemaDetails.fieldDefinitions || {}, null, 2)}
              </pre>
            </div>
          ) : (
            <p className="text-sm text-slate-300">Select a schema to inspect details.</p>
          )}
        </div>
      </aside>
    </section>
  );
}
