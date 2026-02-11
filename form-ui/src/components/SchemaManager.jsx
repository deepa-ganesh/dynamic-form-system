import { useEffect, useMemo, useState } from "react";
import {
  CalendarClock,
  CheckCircle2,
  Copy,
  Eye,
  FileJson,
  Info,
  Loader2,
  PlusCircle,
  Trash2,
  User2,
  X
} from "lucide-react";
import {
  activateSchema,
  createSchema,
  deprecateSchema,
  DEMO_ROLES,
  getDemoRole,
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

function formatDateTime(value) {
  if (!value) {
    return "-";
  }

  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString();
}

export default function SchemaManager({ onNotify }) {
  const [activeSchema, setActiveSchema] = useState(null);
  const [schemas, setSchemas] = useState([]);
  const [demoRole, setDemoRole] = useState(DEMO_ROLES.ADMIN);
  const [selectedSchemaDetails, setSelectedSchemaDetails] = useState(null);
  const [loadingSelectedDetails, setLoadingSelectedDetails] = useState(false);
  const [selectedSchemaVersionId, setSelectedSchemaVersionId] = useState("");
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [activatingId, setActivatingId] = useState("");
  const [deprecatingId, setDeprecatingId] = useState("");
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [error, setError] = useState("");
  const isAdmin = demoRole === DEMO_ROLES.ADMIN;

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

  const orderedSchemas = useMemo(() => {
    const active = sortedSchemas.find((schema) => schema.isActive);
    if (!active) {
      return sortedSchemas;
    }
    return [active, ...sortedSchemas.filter((schema) => !schema.isActive)];
  }, [sortedSchemas]);

  const selectedFieldCount = useMemo(() => {
    const fields = selectedSchemaDetails?.fieldDefinitions?.fields;
    return Array.isArray(fields) ? fields.length : 0;
  }, [selectedSchemaDetails]);

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
      const requests = [getActiveSchema()];
      if (isAdmin) {
        requests.push(getAllSchemas());
      }
      const [active, all] = await Promise.allSettled(requests);

      let activeValue = null;
      let allSchemas = [];

      if (active.status === "fulfilled") {
        activeValue = active.value;
        setActiveSchema(activeValue);
      } else {
        setActiveSchema(null);
      }

      if (isAdmin) {
        if (all?.status === "fulfilled") {
          allSchemas = all.value;
          setSchemas(allSchemas);
        } else {
          setSchemas([]);
          throw new Error(all?.reason?.message || "Unable to load schema list");
        }
      } else {
        allSchemas = activeValue ? [activeValue] : [];
        setSchemas(allSchemas);
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
    setDemoRole(getDemoRole());

    const onRoleChange = (event) => {
      const nextRole = event?.detail || getDemoRole();
      setDemoRole(nextRole);
    };

    window.addEventListener("dynamic-form-demo-role-change", onRoleChange);
    return () => {
      window.removeEventListener("dynamic-form-demo-role-change", onRoleChange);
    };
  }, []);

  useEffect(() => {
    loadSchemas();
  }, [isAdmin]);

  useEffect(() => {
    if (!isCreateModalOpen) {
      return;
    }

    const onKeyDown = (event) => {
      if (event.key === "Escape" && !submitting) {
        setIsCreateModalOpen(false);
      }
    };

    window.addEventListener("keydown", onKeyDown);
    return () => {
      window.removeEventListener("keydown", onKeyDown);
    };
  }, [isCreateModalOpen, submitting]);

  useEffect(() => {
    if (!isAdmin && isCreateModalOpen) {
      setIsCreateModalOpen(false);
    }
  }, [isAdmin, isCreateModalOpen]);

  useEffect(() => {
    const previousOverflow = document.body.style.overflow;

    if (isCreateModalOpen) {
      document.body.style.overflow = "hidden";
    } else {
      document.body.style.overflow = previousOverflow || "";
    }

    return () => {
      document.body.style.overflow = previousOverflow || "";
    };
  }, [isCreateModalOpen]);

  const openCreateModal = () => {
    if (!isAdmin) {
      return;
    }
    setError("");
    setIsCreateModalOpen(true);
  };

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

      setIsCreateModalOpen(false);
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
    if (!isAdmin) {
      return;
    }
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
    if (!isAdmin) {
      return;
    }
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
        await loadSchemaDetails(activeSchema?.formVersionId || "", false);
      }
    } catch (apiError) {
      const message = apiError.message || "Unable to deprecate schema";
      setError(message);
      onNotify({ type: "error", title: "Schema Deprecation Failed", message });
    } finally {
      setDeprecatingId("");
    }
  };

  const copySchemaJson = async () => {
    if (!selectedSchemaDetails?.fieldDefinitions) {
      return;
    }

    await navigator.clipboard.writeText(
      JSON.stringify(selectedSchemaDetails.fieldDefinitions, null, 2)
    );
    onNotify({ title: "Copied", message: "Field definitions copied" });
  };

  return (
    <section className="space-y-6">
      <article className="glass-card p-5">
        <div className="grid gap-5 xl:grid-cols-[1.05fr,1fr]">
          <div className="rounded-xl border border-slate-700 bg-slate-900/40 p-4">
            <div className="mb-3 flex items-center justify-between gap-2">
              <h4 className="text-base font-semibold text-white">Schema Catalog</h4>
              <button
                className={`secondary-btn ${!isAdmin ? "cursor-not-allowed opacity-50" : ""}`}
                type="button"
                onClick={openCreateModal}
                disabled={!isAdmin}
                title={!isAdmin ? "Admin role required" : "Create new schema"}
              >
                <PlusCircle className="h-4 w-4" /> New
              </button>
            </div>

            {loading ? (
              <div className="flex items-center gap-2 text-sm text-slate-300">
                <Loader2 className="h-4 w-4 animate-spin" /> Loading schemas...
              </div>
            ) : orderedSchemas.length === 0 ? (
              <p className="text-sm text-slate-300">No schemas returned from API.</p>
            ) : (
              <ul className="space-y-3">
                {orderedSchemas.map((schema) => {
                  const isActive = Boolean(schema.isActive);
                  const isBusy = activatingId === schema.formVersionId;
                  const isDeprecating = deprecatingId === schema.formVersionId;
                  const isSelected = selectedSchemaVersionId === schema.formVersionId;

                  return (
                    <li
                      key={schema.id || schema.formVersionId}
                      className={`rounded-xl border p-3 transition ${
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
                            className={`secondary-btn ${!isAdmin ? "cursor-not-allowed opacity-50" : ""}`}
                            type="button"
                            onClick={() => handleActivate(schema.formVersionId)}
                            disabled={!isAdmin || isBusy || isDeprecating}
                            title={!isAdmin ? "Admin role required" : "Activate schema"}
                          >
                            {isBusy ? <Loader2 className="h-4 w-4 animate-spin" /> : null}
                            Activate
                          </button>
                        ) : null}

                        {!isActive ? (
                          <button
                            className={`secondary-btn border-red-500/50 bg-red-600/20 text-red-100 hover:bg-red-600/30 ${
                              !isAdmin ? "cursor-not-allowed opacity-50" : ""
                            }`}
                            type="button"
                            onClick={() => handleDeprecate(schema)}
                            disabled={!isAdmin || isBusy || isDeprecating}
                            title={!isAdmin ? "Admin role required" : "Deprecate schema"}
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
          </div>

          <div className="rounded-xl border border-slate-700 bg-slate-900/40 p-4">
            <h4 className="mb-3 text-base font-semibold text-white">Schema Inspector</h4>

            {loadingSelectedDetails ? (
              <div className="flex items-center gap-2 text-sm text-slate-300">
                <Loader2 className="h-4 w-4 animate-spin" /> Loading schema details...
              </div>
            ) : selectedSchemaDetails ? (
              <div className="space-y-4">
                <div className="rounded-xl border border-slate-700 bg-slate-900/50 p-4">
                  <div className="flex flex-wrap items-start justify-between gap-3">
                    <div>
                      <p className="text-lg font-semibold text-white">{selectedSchemaDetails.formName}</p>
                      <p className="text-sm text-slate-300">{selectedSchemaDetails.formVersionId}</p>
                    </div>
                  </div>

                  <div className="mt-4 grid gap-2 text-xs text-slate-300 sm:grid-cols-2">
                    <p className="inline-flex items-center gap-1">
                      <CalendarClock className="h-3.5 w-3.5" /> Created: {formatDateTime(selectedSchemaDetails.createdDate)}
                    </p>
                    <p className="inline-flex items-center gap-1">
                      <User2 className="h-3.5 w-3.5" /> Author: {selectedSchemaDetails.createdBy || "-"}
                    </p>
                    <p className="inline-flex items-center gap-1">
                      <Info className="h-3.5 w-3.5" /> Fields: {selectedFieldCount}
                    </p>
                    <p className="inline-flex items-center gap-1">
                      <Info className="h-3.5 w-3.5" /> Deprecated: {formatDateTime(selectedSchemaDetails.deprecatedDate)}
                    </p>
                  </div>

                  {selectedSchemaDetails.description ? (
                    <p className="mt-3 rounded-lg border border-slate-700 bg-slate-950/50 p-3 text-xs text-slate-300">
                      {selectedSchemaDetails.description}
                    </p>
                  ) : null}
                </div>

                <div className="rounded-xl border border-slate-700 bg-slate-900/50">
                  <div className="flex items-center justify-between border-b border-slate-700 px-4 py-3">
                    <p className="inline-flex items-center gap-2 text-sm font-semibold text-white">
                      <FileJson className="h-4 w-4 text-blue-300" /> Field Definitions JSON
                    </p>
                    <button className="secondary-btn" type="button" onClick={copySchemaJson}>
                      <Copy className="h-4 w-4" /> Copy
                    </button>
                  </div>
                  <pre className="max-h-[28rem] overflow-auto bg-slate-950/70 p-4 text-xs text-slate-200">
                    {JSON.stringify(selectedSchemaDetails.fieldDefinitions || {}, null, 2)}
                  </pre>
                </div>
              </div>
            ) : (
              <p className="text-sm text-slate-300">Select a schema from catalog to inspect full details.</p>
            )}
          </div>
        </div>
      </article>

      {error ? (
        <div className="rounded-lg border border-red-500/50 bg-red-500/20 px-3 py-2 text-sm text-red-200">
          {error}
        </div>
      ) : null}

      {isCreateModalOpen ? (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/80 p-4 backdrop-blur-sm"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget && !submitting) {
              setIsCreateModalOpen(false);
            }
          }}
        >
          <div className="glass-card max-h-[90vh] w-full max-w-3xl overflow-auto p-6">
            <div className="mb-4 flex items-center justify-between gap-3">
              <h4 className="text-lg font-semibold text-white">Create New Schema</h4>
              <button
                className="secondary-btn"
                type="button"
                disabled={submitting}
                onClick={() => setIsCreateModalOpen(false)}
              >
                <X className="h-4 w-4" /> Close
              </button>
            </div>

            <div className="space-y-4 rounded-xl border border-slate-700 bg-slate-900/40 p-4">
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
                  className="form-input min-h-72 font-mono text-xs"
                  value={formData.fieldDefinitionsText}
                  onChange={(event) =>
                    setFormData((prev) => ({ ...prev, fieldDefinitionsText: event.target.value }))
                  }
                />
              </label>

              <button
                className="primary-btn"
                type="button"
                disabled={!isAdmin || submitting}
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
          </div>
        </div>
      ) : null}
    </section>
  );
}
