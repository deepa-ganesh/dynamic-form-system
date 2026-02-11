import axios from "axios";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api";
const DEMO_ROLE_STORAGE_KEY = "dynamicFormDemoRole";

export const DEMO_ROLES = {
  USER: "USER",
  ADMIN: "ADMIN"
};

const DEMO_CREDENTIALS = {
  [DEMO_ROLES.USER]: {
    username: import.meta.env.VITE_DEMO_USER_USERNAME || "user",
    password: import.meta.env.VITE_DEMO_USER_PASSWORD || "password"
  },
  [DEMO_ROLES.ADMIN]: {
    username: import.meta.env.VITE_DEMO_ADMIN_USERNAME || "admin",
    password: import.meta.env.VITE_DEMO_ADMIN_PASSWORD || "admin"
  }
};

function toBasicToken(username, password) {
  return btoa(`${username}:${password}`);
}

export function getDemoRole() {
  const storedRole = window.localStorage.getItem(DEMO_ROLE_STORAGE_KEY);
  if (storedRole === DEMO_ROLES.USER || storedRole === DEMO_ROLES.ADMIN) {
    return storedRole;
  }
  return DEMO_ROLES.ADMIN;
}

export function setDemoRole(role) {
  const safeRole = role === DEMO_ROLES.USER ? DEMO_ROLES.USER : DEMO_ROLES.ADMIN;
  window.localStorage.setItem(DEMO_ROLE_STORAGE_KEY, safeRole);
  window.dispatchEvent(
    new CustomEvent("dynamic-form-demo-role-change", {
      detail: safeRole
    })
  );
}

function getRoleCredentials(role) {
  return DEMO_CREDENTIALS[role] || DEMO_CREDENTIALS[DEMO_ROLES.ADMIN];
}

const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
  headers: {
    "Content-Type": "application/json"
  }
});

api.interceptors.request.use((config) => {
  const role = getDemoRole();
  const { username, password } = getRoleCredentials(role);

  const headers = {
    ...(config.headers || {}),
    Authorization: `Basic ${toBasicToken(username, password)}`
  };

  return {
    ...config,
    headers
  };
});

function normalizeError(error) {
  const message =
    error?.response?.data?.message ||
    error?.response?.data?.error ||
    error?.message ||
    "Unexpected API error";

  return new Error(message);
}

async function callApi(promise) {
  try {
    const response = await promise;
    return response.data;
  } catch (error) {
    throw normalizeError(error);
  }
}

export const createOrder = (payload) => callApi(api.post("/v1/orders", payload));

export const getAllOrders = () => callApi(api.get("/v1/orders"));

export const prefillFromDimensional = ({
  sourceTable,
  sourceKeyColumn,
  sourceKeyValue,
  formVersionId
}) =>
  callApi(
    api.get("/v1/orders/prefill", {
      params: {
        sourceTable,
        sourceKeyColumn,
        sourceKeyValue,
        ...(formVersionId ? { formVersionId } : {})
      }
    })
  );

export const getPrefillMappings = (formVersionId) =>
  callApi(
    api.get("/v1/orders/prefill/mappings", {
      params: {
        ...(formVersionId ? { formVersionId } : {})
      }
    })
  );

export const getLatestVersion = (orderId) =>
  callApi(api.get(`/v1/orders/${encodeURIComponent(orderId)}`));

export const getVersionHistory = (orderId) =>
  callApi(api.get(`/v1/orders/${encodeURIComponent(orderId)}/versions`));

export const getSpecificVersion = (orderId, versionNumber) =>
  callApi(
    api.get(
      `/v1/orders/${encodeURIComponent(orderId)}/versions/${encodeURIComponent(versionNumber)}`
    )
  );

export const getCommittedVersions = (orderId) =>
  callApi(api.get(`/v1/orders/${encodeURIComponent(orderId)}/committed-versions`));

export const promoteOrderVersion = (orderId, versionNumber, payload = {}) =>
  callApi(
    api.post(
      `/v1/orders/${encodeURIComponent(orderId)}/versions/${encodeURIComponent(versionNumber)}/promote`,
      payload
    )
  );

export const getActiveSchema = () => callApi(api.get("/v1/schemas/active"));

export const getAllSchemas = () => callApi(api.get("/v1/schemas"));

export const getSchemaByVersionId = (formVersionId) =>
  callApi(api.get(`/v1/schemas/${encodeURIComponent(formVersionId)}`));

export const createSchema = (payload) => callApi(api.post("/v1/schemas", payload));

export const activateSchema = (formVersionId) =>
  callApi(api.put(`/v1/schemas/${encodeURIComponent(formVersionId)}/activate`));

export const deprecateSchema = (formVersionId) =>
  callApi(api.delete(`/v1/schemas/${encodeURIComponent(formVersionId)}`));

export const getFieldMappings = (formVersionId) =>
  callApi(api.get(`/v1/schemas/${encodeURIComponent(formVersionId)}/mappings`));

export const createFieldMapping = (formVersionId, payload) =>
  callApi(api.post(`/v1/schemas/${encodeURIComponent(formVersionId)}/mappings`, payload));

export const updateFieldMapping = (formVersionId, mappingId, payload) =>
  callApi(
    api.put(
      `/v1/schemas/${encodeURIComponent(formVersionId)}/mappings/${encodeURIComponent(mappingId)}`,
      payload
    )
  );

export const deleteFieldMapping = (formVersionId, mappingId) =>
  callApi(
    api.delete(
      `/v1/schemas/${encodeURIComponent(formVersionId)}/mappings/${encodeURIComponent(mappingId)}`
    )
  );

export default api;
