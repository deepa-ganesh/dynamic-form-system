import axios from "axios";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api";
const API_USERNAME = import.meta.env.VITE_API_USERNAME || "admin";
const API_PASSWORD = import.meta.env.VITE_API_PASSWORD || "admin";

const basicToken = btoa(`${API_USERNAME}:${API_PASSWORD}`);

const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
  headers: {
    Authorization: `Basic ${basicToken}`,
    "Content-Type": "application/json"
  }
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

export const getActiveSchema = () => callApi(api.get("/v1/schemas/active"));

export const getAllSchemas = () => callApi(api.get("/v1/schemas"));

export const getSchemaByVersionId = (formVersionId) =>
  callApi(api.get(`/v1/schemas/${encodeURIComponent(formVersionId)}`));

export const createSchema = (payload) => callApi(api.post("/v1/schemas", payload));

export const activateSchema = (formVersionId) =>
  callApi(api.put(`/v1/schemas/${encodeURIComponent(formVersionId)}/activate`));

export const deprecateSchema = (formVersionId) =>
  callApi(api.delete(`/v1/schemas/${encodeURIComponent(formVersionId)}`));

export default api;
