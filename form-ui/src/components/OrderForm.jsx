import { useEffect, useMemo, useState } from "react";
import {
  ChevronDown,
  ChevronRight,
  CirclePlus,
  Clock3,
  Loader2,
  RefreshCcw,
  Save,
  Send,
  Trash2,
  Wand2
} from "lucide-react";
import {
  createOrder,
  getActiveSchema,
  getPrefillMappings,
  prefillFromDimensional
} from "../services/api";

function normalizeFieldType(fieldType) {
  return String(fieldType || "text").toLowerCase();
}

function isIndexSegment(segment) {
  return /^\d+$/.test(segment);
}

function joinPath(base, segment) {
  if (!base) {
    return segment;
  }
  return `${base}.${segment}`;
}

function getByPath(source, path) {
  if (!path) {
    return source;
  }

  return path.split(".").reduce((current, segment) => {
    if (current == null) {
      return undefined;
    }
    const key = isIndexSegment(segment) ? Number(segment) : segment;
    return current[key];
  }, source);
}

function setByPath(source, path, value) {
  const segments = path.split(".");
  const root = Array.isArray(source) ? [...source] : { ...(source || {}) };

  let current = root;
  for (let i = 0; i < segments.length - 1; i += 1) {
    const segment = segments[i];
    const nextSegment = segments[i + 1];
    const key = isIndexSegment(segment) ? Number(segment) : segment;

    const existing = current[key];
    let nextValue;

    if (existing == null) {
      nextValue = isIndexSegment(nextSegment) ? [] : {};
    } else {
      nextValue = Array.isArray(existing) ? [...existing] : { ...existing };
    }

    current[key] = nextValue;
    current = nextValue;
  }

  const lastSegment = segments[segments.length - 1];
  const lastKey = isIndexSegment(lastSegment) ? Number(lastSegment) : lastSegment;
  current[lastKey] = value;

  return root;
}

function deleteByPath(source, path) {
  const segments = path.split(".");
  const root = Array.isArray(source) ? [...source] : { ...(source || {}) };

  let current = root;
  for (let i = 0; i < segments.length - 1; i += 1) {
    const segment = segments[i];
    const key = isIndexSegment(segment) ? Number(segment) : segment;

    if (current[key] == null || typeof current[key] !== "object") {
      return root;
    }

    current[key] = Array.isArray(current[key]) ? [...current[key]] : { ...current[key] };
    current = current[key];
  }

  const lastSegment = segments[segments.length - 1];
  const lastKey = isIndexSegment(lastSegment) ? Number(lastSegment) : lastSegment;

  if (Array.isArray(current) && Number.isInteger(lastKey)) {
    current.splice(lastKey, 1);
  } else {
    delete current[lastKey];
  }

  return root;
}

function deepMerge(base, patch) {
  if (Array.isArray(patch)) {
    return [...patch];
  }

  if (patch && typeof patch === "object") {
    const baseObject = base && typeof base === "object" && !Array.isArray(base) ? base : {};
    const merged = { ...baseObject };
    Object.entries(patch).forEach(([key, value]) => {
      merged[key] = deepMerge(baseObject[key], value);
    });
    return merged;
  }

  return patch;
}

function isEmptyByType(value, fieldType) {
  const type = normalizeFieldType(fieldType);

  if (value == null) {
    return true;
  }

  if (type === "checkbox") {
    return value !== true;
  }

  if (type === "multivalue") {
    return !Array.isArray(value) || value.filter((item) => String(item || "").trim()).length === 0;
  }

  if (type === "table") {
    return !Array.isArray(value) || value.length === 0;
  }

  if (type === "subform") {
    return !(value && typeof value === "object") || Object.keys(value).length === 0;
  }

  return String(value).trim().length === 0;
}

function getOptionValue(option) {
  if (option == null) {
    return "";
  }
  if (typeof option === "string") {
    return option;
  }
  if (typeof option === "object") {
    return String(option.value ?? option.code ?? option.label ?? "");
  }
  return String(option);
}

function getOptionLabel(option) {
  if (option == null) {
    return "";
  }
  if (typeof option === "string") {
    return option;
  }
  if (typeof option === "object") {
    return String(option.label ?? option.name ?? option.value ?? option.code ?? "");
  }
  return String(option);
}

function getDefaultValueForField(field) {
  const type = normalizeFieldType(field?.fieldType);

  if (type === "multivalue") {
    return [];
  }

  if (type === "subform") {
    const subFields = Array.isArray(field?.subFields) ? field.subFields : [];
    return buildDefaultFormValues(subFields);
  }

  if (type === "table") {
    return [];
  }

  if (type === "checkbox") {
    return Boolean(field?.defaultValue ?? false);
  }

  if (type === "dropdown") {
    if (field?.defaultValue != null) {
      return String(field.defaultValue);
    }
    const options = Array.isArray(field?.options) ? field.options : [];
    return options.length > 0 ? getOptionValue(options[0]) : "";
  }

  if (type === "number") {
    return field?.defaultValue != null ? Number(field.defaultValue) : "";
  }

  return field?.defaultValue != null ? String(field.defaultValue) : "";
}

function buildDefaultFormValues(fields) {
  return (Array.isArray(fields) ? fields : []).reduce((acc, field) => {
    if (!field?.fieldName) {
      return acc;
    }
    return setByPath(acc, field.fieldName, getDefaultValueForField(field));
  }, {});
}

function getSchemaFields(schema) {
  const fields = schema?.fieldDefinitions?.fields;
  return Array.isArray(fields) ? fields : [];
}

function collectMissingRequiredFields(fields, values, basePath = "") {
  const missing = [];

  (Array.isArray(fields) ? fields : []).forEach((field) => {
    if (!field?.fieldName) {
      return;
    }

    const path = joinPath(basePath, field.fieldName);
    const value = getByPath(values, path);
    const label = field.label || field.fieldName;

    if (field.required && isEmptyByType(value, field.fieldType)) {
      missing.push(label);
    }

    const type = normalizeFieldType(field.fieldType);
    if (type === "subform") {
      const subFields = Array.isArray(field.subFields) ? field.subFields : [];
      missing.push(...collectMissingRequiredFields(subFields, values, path));
    }
  });

  return missing;
}

function parseDeliveryLocations(value) {
  if (Array.isArray(value)) {
    return value.map((entry) => String(entry || "").trim()).filter(Boolean);
  }

  if (typeof value === "string") {
    return value
      .split(",")
      .map((entry) => entry.trim())
      .filter(Boolean);
  }

  return [];
}

function buildOrderPayload(formValues, changeDescription, isFinalSave) {
  const orderId = String(getByPath(formValues, "orderId") || "").trim().toUpperCase();
  const deliveryLocations = parseDeliveryLocations(getByPath(formValues, "deliveryLocations"));

  let data = JSON.parse(JSON.stringify(formValues || {}));
  data = deleteByPath(data, "orderId");
  data = deleteByPath(data, "deliveryLocations");

  return {
    orderId,
    deliveryLocations,
    data,
    finalSave: isFinalSave,
    changeDescription: changeDescription?.trim() || undefined
  };
}

function fingerprintDraftPayload(payload) {
  const { finalSave, ...rest } = payload;
  return JSON.stringify(rest);
}

function formatDateTime(value) {
  if (!value) {
    return "";
  }
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleTimeString();
}

export default function OrderForm({ onNotify }) {
  const [activeSchema, setActiveSchema] = useState(null);
  const [schemaLoading, setSchemaLoading] = useState(false);

  const [formValues, setFormValues] = useState({});
  const [changeDescription, setChangeDescription] = useState("");

  const [prefillConfig, setPrefillConfig] = useState({
    formVersionId: "",
    sourceTable: "",
    sourceKeyColumn: "",
    sourceKeyValue: ""
  });
  const [isPrefillPanelOpen, setIsPrefillPanelOpen] = useState(false);
  const [prefillMappings, setPrefillMappings] = useState({
    formVersionId: "",
    tables: []
  });

  const [loadingAction, setLoadingAction] = useState("");
  const [prefillLoading, setPrefillLoading] = useState(false);
  const [mappingLoading, setMappingLoading] = useState(false);
  const [autoSaveEnabled, setAutoSaveEnabled] = useState(true);
  const [autoSaveState, setAutoSaveState] = useState("idle");
  const [lastSavedAt, setLastSavedAt] = useState("");
  const [lastDraftFingerprint, setLastDraftFingerprint] = useState("");
  const [hasInteracted, setHasInteracted] = useState(false);
  const [error, setError] = useState("");

  const schemaFields = useMemo(() => getSchemaFields(activeSchema), [activeSchema]);

  const hasCoreFields = useMemo(() => {
    const names = schemaFields.map((field) => field?.fieldName).filter(Boolean);
    return {
      hasOrderId: names.includes("orderId"),
      hasDeliveryLocations: names.includes("deliveryLocations")
    };
  }, [schemaFields]);

  const missingRequiredFields = useMemo(
    () => collectMissingRequiredFields(schemaFields, formValues),
    [schemaFields, formValues]
  );

  const draftPayload = useMemo(
    () => buildOrderPayload(formValues, changeDescription, false),
    [formValues, changeDescription]
  );

  const draftFingerprint = useMemo(() => fingerprintDraftPayload(draftPayload), [draftPayload]);

  const canSubmit = useMemo(() => {
    const isOrderIdValid = /^ORD-[0-9]{5}$/.test(draftPayload.orderId || "");
    const hasLocations = Array.isArray(draftPayload.deliveryLocations) && draftPayload.deliveryLocations.length > 0;
    return isOrderIdValid && hasLocations && missingRequiredFields.length === 0;
  }, [draftPayload.deliveryLocations, draftPayload.orderId, missingRequiredFields.length]);

  const selectedTableMapping = useMemo(() => {
    return (
      prefillMappings.tables.find((table) => table.sourceTable === prefillConfig.sourceTable) || null
    );
  }, [prefillMappings.tables, prefillConfig.sourceTable]);

  const updatePathValue = (path, value) => {
    setHasInteracted(true);
    setFormValues((prev) => setByPath(prev, path, value));
  };

  const markDraftSaved = (fingerprint) => {
    setLastDraftFingerprint(fingerprint);
    setLastSavedAt(new Date().toISOString());
  };

  const loadActiveSchema = async () => {
    setSchemaLoading(true);
    setError("");

    try {
      const schema = await getActiveSchema();
      const fields = getSchemaFields(schema);
      const defaults = buildDefaultFormValues(fields);

      setActiveSchema(schema);
      setFormValues((prev) => {
        if (prev && Object.keys(prev).length > 0) {
          return prev;
        }
        return defaults;
      });

      setPrefillConfig((prev) => ({
        ...prev,
        formVersionId: prev.formVersionId || schema.formVersionId || ""
      }));
    } catch (apiError) {
      const message = apiError.message || "Unable to load active schema";
      setError(message);
      onNotify({ type: "error", title: "Schema Load Failed", message });
    } finally {
      setSchemaLoading(false);
    }
  };

  const handleSubmit = async (isFinalSave) => {
    setError("");
    setLoadingAction(isFinalSave ? "submit" : "draft");

    try {
      const payload = buildOrderPayload(formValues, changeDescription, isFinalSave);
      const response = await createOrder(payload);
      markDraftSaved(draftFingerprint);
      setAutoSaveState("idle");

      onNotify({
        title: isFinalSave ? "Order Submitted" : "Draft Saved",
        message: `Version ${response.orderVersionNumber} created for ${response.orderId}`
      });
    } catch (apiError) {
      const message = apiError.message || "Unable to create order version";
      setError(message);
      onNotify({ type: "error", title: "Order Save Failed", message });
    } finally {
      setLoadingAction("");
    }
  };

  const handlePrefill = async () => {
    if (
      !prefillConfig.sourceTable.trim() ||
      !prefillConfig.sourceKeyColumn.trim() ||
      !prefillConfig.sourceKeyValue.trim()
    ) {
      setError("Source table, key column, and key value are required for prefill");
      return;
    }

    setError("");
    setPrefillLoading(true);

    try {
      const prefillData = await prefillFromDimensional({
        sourceTable: prefillConfig.sourceTable.trim(),
        sourceKeyColumn: prefillConfig.sourceKeyColumn.trim(),
        sourceKeyValue: prefillConfig.sourceKeyValue.trim(),
        formVersionId: prefillConfig.formVersionId.trim() || undefined
      });

      const safeData = prefillData && typeof prefillData === "object" ? prefillData : {};
      if (Object.keys(safeData).length === 0) {
        const message =
          "No prefill data found for the provided source and key. Check key value and mappings.";
        setError(message);
        onNotify({ type: "error", title: "Prefill Returned No Data", message });
        return;
      }

      setFormValues((prev) => deepMerge(prev, safeData));
      setHasInteracted(true);
      setLastDraftFingerprint("");

      onNotify({
        title: "Prefill Complete",
        message: "Dimensional data transformed into schema-driven form"
      });
    } catch (apiError) {
      const message = apiError.message || "Unable to prefill form from dimensional data";
      setError(message);
      onNotify({ type: "error", title: "Prefill Failed", message });
    } finally {
      setPrefillLoading(false);
    }
  };

  useEffect(() => {
    loadActiveSchema();
  }, []);

  useEffect(() => {
    if (!autoSaveEnabled || !canSubmit || loadingAction || prefillLoading || schemaLoading) {
      return undefined;
    }

    if (draftFingerprint === lastDraftFingerprint) {
      return undefined;
    }

    const timeoutId = window.setTimeout(async () => {
      setAutoSaveState("saving");

      try {
        const payload = {
          ...draftPayload,
          finalSave: false,
          changeDescription: draftPayload.changeDescription || "Auto-save snapshot"
        };

        const response = await createOrder(payload);
        markDraftSaved(draftFingerprint);
        setAutoSaveState("saved");
      } catch (apiError) {
        setAutoSaveState("error");
        setError(apiError.message || "Auto-save failed");
      }
    }, 12000);

    return () => {
      window.clearTimeout(timeoutId);
    };
  }, [
    autoSaveEnabled,
    canSubmit,
    draftFingerprint,
    draftPayload,
    lastDraftFingerprint,
    loadingAction,
    prefillLoading,
    schemaLoading
  ]);

  useEffect(() => {
    if (autoSaveState !== "saved" && autoSaveState !== "error") {
      return undefined;
    }

    const timeoutId = window.setTimeout(() => {
      setAutoSaveState("idle");
    }, 2500);

    return () => {
      window.clearTimeout(timeoutId);
    };
  }, [autoSaveState]);

  useEffect(() => {
    let cancelled = false;

    const loadPrefillMappings = async () => {
      setMappingLoading(true);
      try {
        const response = await getPrefillMappings(prefillConfig.formVersionId.trim() || undefined);
        if (cancelled) {
          return;
        }

        const tables = Array.isArray(response?.tables) ? response.tables : [];
        setPrefillMappings({
          formVersionId: response?.formVersionId || "",
          tables
        });

        const firstTable = tables[0];
        const firstSuggestedKey = firstTable?.suggestedKeyColumns?.[0] || "";

        setPrefillConfig((prev) => ({
          ...prev,
          sourceTable: prev.sourceTable || firstTable?.sourceTable || "",
          sourceKeyColumn: prev.sourceKeyColumn || firstSuggestedKey
        }));
      } catch (apiError) {
        if (!cancelled) {
          setPrefillMappings({ formVersionId: "", tables: [] });
        }
      } finally {
        if (!cancelled) {
          setMappingLoading(false);
        }
      }
    };

    loadPrefillMappings();
    return () => {
      cancelled = true;
    };
  }, [prefillConfig.formVersionId]);

  const renderFieldByPath = (field, path, nestingLevel = 0) => {
    const type = normalizeFieldType(field.fieldType);
    const label = field.label || field.fieldName || "Field";
    const required = Boolean(field.required);
    const placeholder = field.placeholder || label;
    const value = getByPath(formValues, path);

    if (type === "subform") {
      const subFields = Array.isArray(field.subFields) ? field.subFields : [];

      return (
        <div className="rounded-xl border border-slate-700 bg-slate-900/40 p-4">
          <p className="mb-3 text-xs font-semibold uppercase tracking-wide text-slate-300">
            {label}
            {required ? <span className="ml-1 text-red-300">*</span> : null}
          </p>
          <div className="grid gap-3 md:grid-cols-2">
            {subFields.map((subField) => {
              const subPath = joinPath(path, subField.fieldName);
              const subType = normalizeFieldType(subField.fieldType);
              const fullWidth = ["table", "subform", "multivalue"].includes(subType);

              return (
                <div
                  key={`${subPath}-${subField.fieldType || "text"}`}
                  className={fullWidth ? "md:col-span-2" : ""}
                >
                  {renderFieldByPath(subField, subPath, nestingLevel + 1)}
                </div>
              );
            })}
          </div>
        </div>
      );
    }

    if (type === "table") {
      const columns = Array.isArray(field.columns) ? field.columns : [];
      const rows = Array.isArray(value) ? value : [];

      const addRow = () => {
        const rowDefault = buildDefaultFormValues(columns);
        updatePathValue(path, [...rows, rowDefault]);
      };

      const removeRow = (rowIndex) => {
        updatePathValue(
          path,
          rows.filter((_, index) => index !== rowIndex)
        );
      };

      return (
        <div className="rounded-xl border border-slate-700 bg-slate-900/40 p-4">
          <div className="mb-3 flex items-center justify-between gap-2">
            <p className="text-xs font-semibold uppercase tracking-wide text-slate-300">
              {label}
              {required ? <span className="ml-1 text-red-300">*</span> : null}
            </p>
            <button className="secondary-btn" type="button" onClick={addRow}>
              <CirclePlus className="h-4 w-4" /> Add Row
            </button>
          </div>

          {rows.length === 0 ? (
            <p className="text-sm text-slate-300">No rows yet.</p>
          ) : (
            <div className="space-y-3">
              {rows.map((_, rowIndex) => (
                <div
                  key={`${path}-row-${rowIndex}`}
                  className="rounded-lg border border-slate-700 bg-slate-950/60 p-3"
                >
                  <div className="mb-2 flex items-center justify-between">
                    <p className="text-xs font-semibold uppercase tracking-wide text-slate-300">
                      Row {rowIndex + 1}
                    </p>
                    <button className="secondary-btn" type="button" onClick={() => removeRow(rowIndex)}>
                      <Trash2 className="h-4 w-4" /> Remove
                    </button>
                  </div>
                  <div className="grid gap-3 md:grid-cols-2">
                    {columns.map((column) => {
                      const columnPath = joinPath(joinPath(path, String(rowIndex)), column.fieldName);
                      const columnType = normalizeFieldType(column.fieldType);
                      const fullWidth = ["table", "subform", "multivalue"].includes(columnType);
                      return (
                        <div
                          key={`${columnPath}-${column.fieldType || "text"}`}
                          className={fullWidth ? "md:col-span-2" : ""}
                        >
                          {renderFieldByPath(column, columnPath, nestingLevel + 1)}
                        </div>
                      );
                    })}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      );
    }

    if (type === "multivalue") {
      const values = Array.isArray(value) ? value : [];

      const addValue = () => {
        updatePathValue(path, [...values, ""]);
      };

      const removeValue = (indexToRemove) => {
        updatePathValue(
          path,
          values.filter((_, index) => index !== indexToRemove)
        );
      };

      const updateValue = (indexToUpdate, nextValue) => {
        const nextValues = [...values];
        nextValues[indexToUpdate] = nextValue;
        updatePathValue(path, nextValues);
      };

      return (
        <div className="rounded-xl border border-slate-700 bg-slate-900/40 p-4">
          <div className="mb-3 flex items-center justify-between gap-2">
            <p className="text-xs font-semibold uppercase tracking-wide text-slate-300">
              {label}
              {required ? <span className="ml-1 text-red-300">*</span> : null}
            </p>
            <button className="secondary-btn" type="button" onClick={addValue}>
              <CirclePlus className="h-4 w-4" /> Add Value
            </button>
          </div>

          {values.length === 0 ? (
            <p className="text-sm text-slate-300">No values yet.</p>
          ) : (
            <div className="space-y-2">
              {values.map((entry, index) => (
                <div key={`${path}-value-${index}`} className="grid gap-2 md:grid-cols-[1fr_auto]">
                  <input
                    className="form-input"
                    value={entry}
                    placeholder={placeholder}
                    onChange={(event) => updateValue(index, event.target.value)}
                  />
                  <button className="secondary-btn" type="button" onClick={() => removeValue(index)}>
                    <Trash2 className="h-4 w-4" />
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>
      );
    }

    if (type === "checkbox") {
      return (
        <label className="inline-flex items-center gap-2 rounded-lg border border-slate-700 bg-slate-900/40 px-3 py-2 text-sm text-slate-200">
          <input
            type="checkbox"
            checked={Boolean(value)}
            onChange={(event) => updatePathValue(path, event.target.checked)}
          />
          {label}
          {required ? <span className="text-red-300">*</span> : null}
        </label>
      );
    }

    if (type === "dropdown") {
      const options = Array.isArray(field.options) ? field.options : [];
      return (
        <label className="block">
          <span className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-300">
            {label}
            {required ? <span className="ml-1 text-red-300">*</span> : null}
          </span>
          <select
            className="form-input"
            value={value ?? ""}
            onChange={(event) => updatePathValue(path, event.target.value)}
          >
            <option value="">Select...</option>
            {options.map((option, index) => {
              const optionValue = getOptionValue(option);
              return (
                <option key={`${optionValue}-${index}`} value={optionValue}>
                  {getOptionLabel(option)}
                </option>
              );
            })}
          </select>
        </label>
      );
    }

    if (type === "number") {
      return (
        <label className="block">
          <span className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-300">
            {label}
            {required ? <span className="ml-1 text-red-300">*</span> : null}
          </span>
          <input
            className="form-input"
            type="number"
            value={value ?? ""}
            placeholder={placeholder}
            onChange={(event) => {
              const nextValue = event.target.value;
              updatePathValue(path, nextValue === "" ? "" : Number(nextValue));
            }}
          />
        </label>
      );
    }

    if (type === "date") {
      return (
        <label className="block">
          <span className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-300">
            {label}
            {required ? <span className="ml-1 text-red-300">*</span> : null}
          </span>
          <input
            className="form-input"
            type="date"
            value={value ?? ""}
            onChange={(event) => updatePathValue(path, event.target.value)}
          />
        </label>
      );
    }

    const isReadOnly = type === "calculated";
    return (
      <label className="block">
        <span className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-300">
          {label}
          {required ? <span className="ml-1 text-red-300">*</span> : null}
        </span>
        <input
          className="form-input"
          value={value ?? ""}
          placeholder={placeholder}
          readOnly={isReadOnly}
          onChange={(event) => updatePathValue(path, event.target.value)}
        />
      </label>
    );
  };

  return (
    <section className="space-y-5">
      <article className="glass-card p-6">
        <div className="mb-4 flex flex-wrap items-center justify-between gap-2">
          <h2 className="text-xl font-semibold text-white">Create Order Version</h2>
          <div className="flex flex-wrap items-center gap-2">
            <button className="secondary-btn" type="button" onClick={loadActiveSchema} disabled={schemaLoading}>
              {schemaLoading ? <Loader2 className="h-4 w-4 animate-spin" /> : <RefreshCcw className="h-4 w-4" />}
              Refresh Schema
            </button>
            <label className="inline-flex items-center gap-2 rounded-lg border border-slate-600 bg-slate-900/40 px-3 py-1.5 text-xs text-slate-200">
              <input
                type="checkbox"
                checked={autoSaveEnabled}
                onChange={(event) => setAutoSaveEnabled(event.target.checked)}
              />
              Auto-save drafts
            </label>
          </div>
        </div>

        <div className="mb-4 rounded-lg border border-slate-700 bg-slate-900/40 px-3 py-2 text-sm text-slate-200">
          Active Schema: <span className="font-semibold text-white">{activeSchema?.formVersionId || "Not loaded"}</span>
          {activeSchema?.formName ? <span className="text-slate-400"> ({activeSchema.formName})</span> : null}
        </div>

        {!hasCoreFields.hasOrderId || !hasCoreFields.hasDeliveryLocations ? (
          <div className="mb-4 rounded-lg border border-yellow-500/50 bg-yellow-500/20 px-3 py-2 text-sm text-yellow-100">
            Active schema should include both <code>orderId</code> and <code>deliveryLocations</code> fields for order creation.
          </div>
        ) : null}

        {schemaLoading ? (
          <div className="mb-4 inline-flex items-center gap-2 text-sm text-slate-300">
            <Loader2 className="h-4 w-4 animate-spin" /> Loading schema-driven form...
          </div>
        ) : null}

        {schemaFields.length > 0 ? (
          <div className="grid gap-4 md:grid-cols-2">
            {schemaFields.map((field) => {
              const type = normalizeFieldType(field.fieldType);
              const fullWidth = ["table", "subform", "multivalue"].includes(type);
              const key = `${field.fieldName}-${field.fieldType || "text"}`;

              return (
                <div key={key} className={fullWidth ? "md:col-span-2" : ""}>
                  {renderFieldByPath(field, field.fieldName)}
                </div>
              );
            })}

            <label className="block md:col-span-2">
              <span className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-300">
                Change Description
              </span>
              <input
                className="form-input"
                value={changeDescription}
                onChange={(event) => {
                  setHasInteracted(true);
                  setChangeDescription(event.target.value);
                }}
                placeholder="Describe this save"
              />
            </label>
          </div>
        ) : (
          <p className="text-sm text-slate-300">No schema fields available to render.</p>
        )}

        {hasInteracted && missingRequiredFields.length > 0 ? (
          <div className="mt-4 rounded-lg border border-yellow-500/50 bg-yellow-500/20 px-3 py-2 text-sm text-yellow-100">
            Missing required fields: {Array.from(new Set(missingRequiredFields)).join(", ")}
          </div>
        ) : null}

        {error ? (
          <div className="mt-4 rounded-lg border border-red-500/50 bg-red-500/20 px-3 py-2 text-sm text-red-200">
            {error}
          </div>
        ) : null}

        <div className="mt-6 flex flex-wrap items-center gap-3">
          <button
            className="secondary-btn"
            type="button"
            disabled={!canSubmit || loadingAction.length > 0 || prefillLoading || schemaLoading}
            onClick={() => handleSubmit(false)}
          >
            {loadingAction === "draft" ? <Loader2 className="h-4 w-4 animate-spin" /> : <Save className="h-4 w-4" />}
            Save as Draft
          </button>

          <button
            className="primary-btn"
            type="button"
            disabled={!canSubmit || loadingAction.length > 0 || prefillLoading || schemaLoading}
            onClick={() => handleSubmit(true)}
          >
            {loadingAction === "submit" ? <Loader2 className="h-4 w-4 animate-spin" /> : <Send className="h-4 w-4" />}
            Submit Order
          </button>

          <span className="inline-flex items-center gap-1 text-xs text-slate-300">
            <Clock3 className="h-3.5 w-3.5" />
            {autoSaveEnabled
              ? autoSaveState === "saving"
                ? "Auto-save in progress"
                : autoSaveState === "saved"
                  ? `Auto-saved ${formatDateTime(lastSavedAt)}`
                  : autoSaveState === "error"
                    ? "Auto-save failed"
                    : "Auto-save watches for changes every 12s"
              : "Auto-save disabled"}
          </span>
        </div>
      </article>

      <article className="glass-card p-5">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <div>
            <h3 className="inline-flex items-center gap-2 text-sm font-semibold uppercase tracking-wide text-slate-200">
              <Wand2 className="h-4 w-4" /> Prefill Assistant
            </h3>
            <p className="mt-1 text-xs text-slate-300">
              Load dimensional data into the form only when needed.
            </p>
          </div>
          <button
            className="secondary-btn"
            type="button"
            onClick={() => setIsPrefillPanelOpen((prev) => !prev)}
          >
            {isPrefillPanelOpen ? <ChevronDown className="h-4 w-4" /> : <ChevronRight className="h-4 w-4" />}
            {isPrefillPanelOpen ? "Hide Panel" : "Show Panel"}
          </button>
        </div>

        {isPrefillPanelOpen ? (
          <div className="mt-4 rounded-xl border border-slate-700 bg-slate-900/40 p-4">
            <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
              <input
                className="form-input"
                placeholder="Source Table"
                list="prefill-source-tables"
                value={prefillConfig.sourceTable}
                onChange={(event) =>
                  setPrefillConfig((prev) => ({ ...prev, sourceTable: event.target.value }))
                }
              />
              <input
                className="form-input"
                placeholder="Key Column"
                list="prefill-key-columns"
                value={prefillConfig.sourceKeyColumn}
                onChange={(event) =>
                  setPrefillConfig((prev) => ({ ...prev, sourceKeyColumn: event.target.value }))
                }
              />
              <input
                className="form-input"
                placeholder="Key Value"
                value={prefillConfig.sourceKeyValue}
                onChange={(event) =>
                  setPrefillConfig((prev) => ({ ...prev, sourceKeyValue: event.target.value }))
                }
              />
              <input
                className="form-input"
                placeholder="Form Version (optional)"
                value={prefillConfig.formVersionId}
                onChange={(event) =>
                  setPrefillConfig((prev) => ({ ...prev, formVersionId: event.target.value }))
                }
              />
            </div>

            <datalist id="prefill-source-tables">
              {prefillMappings.tables.map((table) => (
                <option key={table.sourceTable} value={table.sourceTable} />
              ))}
            </datalist>

            <datalist id="prefill-key-columns">
              {(selectedTableMapping?.suggestedKeyColumns || []).map((column) => (
                <option key={column} value={column} />
              ))}
            </datalist>

            <div className="mt-3">
              <button
                className="secondary-btn"
                type="button"
                onClick={handlePrefill}
                disabled={prefillLoading || loadingAction.length > 0 || mappingLoading}
              >
                {prefillLoading ? <Loader2 className="h-4 w-4 animate-spin" /> : <Wand2 className="h-4 w-4" />}
                Prefill
              </button>
              <p className="mt-2 text-xs text-slate-300">
                {mappingLoading
                  ? "Loading mapping options..."
                  : prefillMappings.tables.length > 0
                    ? `Using schema ${prefillMappings.formVersionId}. ${prefillMappings.tables.length} mapped source table(s) available.`
                    : "No active prefill mappings found for selected schema."}
              </p>
            </div>
          </div>
        ) : null}
      </article>
    </section>
  );
}
