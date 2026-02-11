import { useEffect, useMemo, useState } from "react";
import {
  ArrowDownToLine,
  Clock3,
  Copy,
  Database,
  Loader2,
  RefreshCcw,
  Search
} from "lucide-react";
import {
  getCommittedVersions,
  getLatestVersion,
  getSpecificVersion,
  getVersionHistory
} from "../services/api";

function formatDateTime(value) {
  if (!value) {
    return "-";
  }

  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString();
}

function normalize(value) {
  if (Array.isArray(value)) {
    return value.map(normalize);
  }
  if (value && typeof value === "object") {
    return Object.keys(value)
      .sort()
      .reduce((acc, key) => {
        acc[key] = normalize(value[key]);
        return acc;
      }, {});
  }
  return value;
}

function computeDiff(current, previous) {
  const a = normalize(current || {});
  const b = normalize(previous || {});

  const keys = new Set([...Object.keys(a), ...Object.keys(b)]);
  const diff = [];

  keys.forEach((key) => {
    const nextValue = a[key];
    const prevValue = b[key];

    if (JSON.stringify(nextValue) !== JSON.stringify(prevValue)) {
      diff.push({
        key,
        previous: prevValue,
        current: nextValue
      });
    }
  });

  return diff;
}

export default function VersionHistory({ onNotify }) {
  const [orderId, setOrderId] = useState("");
  const [historyData, setHistoryData] = useState(null);
  const [latestVersion, setLatestVersion] = useState(null);
  const [committedVersions, setCommittedVersions] = useState([]);
  const [showCommittedOnly, setShowCommittedOnly] = useState(false);
  const [selectedVersionNumber, setSelectedVersionNumber] = useState(null);
  const [versionDetails, setVersionDetails] = useState({});
  const [loadingDetails, setLoadingDetails] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [autoRefresh, setAutoRefresh] = useState(false);

  const versions = historyData?.versions || [];

  const displayedVersions = useMemo(() => {
    const source = showCommittedOnly ? committedVersions : versions;
    return [...source].sort((a, b) => a.orderVersionNumber - b.orderVersionNumber);
  }, [committedVersions, showCommittedOnly, versions]);

  const selectedVersionSummary = useMemo(() => {
    if (displayedVersions.length === 0) {
      return null;
    }

    if (selectedVersionNumber) {
      const selected = displayedVersions.find(
        (version) => version.orderVersionNumber === selectedVersionNumber
      );
      if (selected) {
        return selected;
      }
    }

    return displayedVersions.find((version) => version.isLatestVersion) || displayedVersions.at(-1);
  }, [displayedVersions, selectedVersionNumber]);

  const previousVersionSummary = useMemo(() => {
    if (!selectedVersionSummary) {
      return null;
    }

    const currentIndex = displayedVersions.findIndex(
      (version) => version.orderVersionNumber === selectedVersionSummary.orderVersionNumber
    );

    if (currentIndex <= 0) {
      return null;
    }

    return displayedVersions[currentIndex - 1] || null;
  }, [displayedVersions, selectedVersionSummary]);

  const selectedVersionDetail = useMemo(() => {
    if (!selectedVersionSummary) {
      return null;
    }

    return versionDetails[selectedVersionSummary.orderVersionNumber] || selectedVersionSummary;
  }, [selectedVersionSummary, versionDetails]);

  const previousVersionDetail = useMemo(() => {
    if (!previousVersionSummary) {
      return null;
    }

    return versionDetails[previousVersionSummary.orderVersionNumber] || previousVersionSummary;
  }, [previousVersionSummary, versionDetails]);

  const diffRows = useMemo(
    () => computeDiff(selectedVersionDetail?.data || {}, previousVersionDetail?.data || {}),
    [selectedVersionDetail, previousVersionDetail]
  );

  const loadVersionDetails = async (targetOrderId, targetVersionNumber) => {
    if (!targetOrderId || !targetVersionNumber) {
      return;
    }

    if (versionDetails[targetVersionNumber]) {
      return;
    }

    setLoadingDetails(true);
    try {
      const detail = await getSpecificVersion(targetOrderId, targetVersionNumber);
      setVersionDetails((prev) => ({
        ...prev,
        [targetVersionNumber]: detail
      }));
    } catch (apiError) {
      onNotify({
        type: "error",
        title: "Version Detail Load Failed",
        message: apiError.message || "Could not load selected version details"
      });
    } finally {
      setLoadingDetails(false);
    }
  };

  const loadHistory = async (searchOrderId = orderId) => {
    const trimmedOrderId = searchOrderId.trim();

    if (!trimmedOrderId) {
      setError("Enter a valid order ID");
      return;
    }

    setLoading(true);
    setError("");

    try {
      const [historyResponse, latestResponse, committedResponse] = await Promise.all([
        getVersionHistory(trimmedOrderId),
        getLatestVersion(trimmedOrderId),
        getCommittedVersions(trimmedOrderId)
      ]);

      setHistoryData(historyResponse);
      setLatestVersion(latestResponse);
      setCommittedVersions(Array.isArray(committedResponse) ? committedResponse : []);
      setVersionDetails({});

      const defaultVersions = showCommittedOnly
        ? [...(Array.isArray(committedResponse) ? committedResponse : [])].sort(
            (a, b) => a.orderVersionNumber - b.orderVersionNumber
          )
        : [...(historyResponse.versions || [])].sort(
            (a, b) => a.orderVersionNumber - b.orderVersionNumber
          );

      const defaultVersion =
        defaultVersions.find((version) => version.isLatestVersion) || defaultVersions.at(-1) || null;

      setSelectedVersionNumber(defaultVersion?.orderVersionNumber || null);

      if (defaultVersion?.orderVersionNumber) {
        await loadVersionDetails(trimmedOrderId, defaultVersion.orderVersionNumber);
      }
    } catch (apiError) {
      const message = apiError.message || "Unable to fetch version history";
      setError(message);
      onNotify({ type: "error", title: "History Load Failed", message });
      setHistoryData(null);
      setLatestVersion(null);
      setCommittedVersions([]);
      setSelectedVersionNumber(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!autoRefresh || !historyData?.orderId) {
      return undefined;
    }

    const intervalId = window.setInterval(() => {
      loadHistory(historyData.orderId);
    }, 20000);

    return () => {
      window.clearInterval(intervalId);
    };
  }, [autoRefresh, historyData?.orderId]);

  useEffect(() => {
    if (!historyData?.orderId || !selectedVersionSummary?.orderVersionNumber) {
      return;
    }

    loadVersionDetails(historyData.orderId, selectedVersionSummary.orderVersionNumber);
  }, [historyData?.orderId, selectedVersionSummary?.orderVersionNumber]);

  useEffect(() => {
    if (displayedVersions.length === 0) {
      setSelectedVersionNumber(null);
      return;
    }

    const hasSelected = displayedVersions.some(
      (version) => version.orderVersionNumber === selectedVersionNumber
    );

    if (!hasSelected) {
      setSelectedVersionNumber(displayedVersions.at(-1)?.orderVersionNumber || null);
    }
  }, [displayedVersions, selectedVersionNumber]);

  const copyOrderId = async () => {
    if (!historyData?.orderId) {
      return;
    }

    await navigator.clipboard.writeText(historyData.orderId);
    onNotify({ title: "Copied", message: `${historyData.orderId} copied` });
  };

  const exportJson = () => {
    if (!historyData) {
      return;
    }

    const exportPayload = {
      ...historyData,
      latestVersion,
      committedVersions,
      versionDetails
    };

    const blob = new Blob([JSON.stringify(exportPayload, null, 2)], {
      type: "application/json"
    });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = `${historyData.orderId}-history.json`;
    anchor.click();
    URL.revokeObjectURL(url);
  };

  return (
    <section className="grid gap-6 xl:grid-cols-[1.4fr,1fr]">
      <article className="glass-card p-6">
        <div className="mb-4 flex flex-wrap items-end gap-3">
          <label className="block min-w-[240px] flex-1">
            <span className="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-300">
              Search Order ID
            </span>
            <div className="flex gap-2">
              <input
                className="form-input"
                value={orderId}
                onChange={(event) => setOrderId(event.target.value.toUpperCase())}
                placeholder="ORD-12345"
                onKeyDown={(event) => {
                  if (event.key === "Enter") {
                    loadHistory();
                  }
                }}
              />
              <button className="primary-btn" type="button" onClick={() => loadHistory()}>
                {loading ? <Loader2 className="h-4 w-4 animate-spin" /> : <Search className="h-4 w-4" />}
                Search
              </button>
            </div>
          </label>

          <button
            className="secondary-btn"
            type="button"
            onClick={() => loadHistory()}
            disabled={loading}
          >
            <RefreshCcw className={`h-4 w-4 ${loading ? "animate-spin" : ""}`} /> Refresh
          </button>

          <label className="inline-flex items-center gap-2 rounded-lg border border-slate-600 bg-slate-900/40 px-3 py-2 text-sm">
            <input
              type="checkbox"
              checked={autoRefresh}
              onChange={(event) => setAutoRefresh(event.target.checked)}
            />
            Auto refresh (20s)
          </label>

          <label className="inline-flex items-center gap-2 rounded-lg border border-slate-600 bg-slate-900/40 px-3 py-2 text-sm">
            <input
              type="checkbox"
              checked={showCommittedOnly}
              onChange={(event) => setShowCommittedOnly(event.target.checked)}
              disabled={!historyData}
            />
            Committed only
          </label>
        </div>

        {error ? (
          <div className="mb-4 rounded-lg border border-red-500/50 bg-red-500/20 px-3 py-2 text-sm text-red-200">
            {error}
          </div>
        ) : null}

        {historyData ? (
          <>
            <div className="mb-4 grid gap-3 sm:grid-cols-4">
              <div className="rounded-lg border border-slate-700 bg-slate-900/40 p-3 text-center">
                <p className="text-xs uppercase tracking-wide text-slate-300">Total Versions</p>
                <p className="mt-1 text-2xl font-semibold text-white">{historyData.totalVersions}</p>
              </div>
              <div className="rounded-lg border border-yellow-500/40 bg-yellow-500/10 p-3 text-center">
                <p className="text-xs uppercase tracking-wide text-yellow-300">WIP</p>
                <p className="mt-1 text-2xl font-semibold text-yellow-200">{historyData.wipVersions}</p>
              </div>
              <div className="rounded-lg border border-green-500/40 bg-green-500/10 p-3 text-center">
                <p className="text-xs uppercase tracking-wide text-green-300">Committed</p>
                <p className="mt-1 text-2xl font-semibold text-green-200">
                  {historyData.committedVersions}
                </p>
              </div>
              <div className="rounded-lg border border-blue-500/40 bg-blue-500/10 p-3 text-center">
                <p className="text-xs uppercase tracking-wide text-blue-300">Shown</p>
                <p className="mt-1 text-2xl font-semibold text-blue-200">{displayedVersions.length}</p>
              </div>
            </div>

            {latestVersion ? (
              <div className="mb-4 rounded-xl border border-blue-500/40 bg-blue-500/10 p-4 text-sm">
                <p className="text-xs font-semibold uppercase tracking-wide text-blue-200">
                  Latest Snapshot
                </p>
                <div className="mt-2 flex flex-wrap items-center gap-3 text-slate-100">
                  <span className="font-semibold">Version {latestVersion.orderVersionNumber}</span>
                  <span
                    className={`rounded-full px-2.5 py-1 text-xs font-semibold ${
                      latestVersion.orderStatus === "COMMITTED" ? "status-committed" : "status-wip"
                    }`}
                  >
                    {latestVersion.orderStatus}
                  </span>
                  <span className="inline-flex items-center gap-1 text-xs text-slate-300">
                    <Clock3 className="h-3 w-3" /> {formatDateTime(latestVersion.timestamp)}
                  </span>
                </div>
              </div>
            ) : null}

            <div className="mb-4 flex flex-wrap gap-2">
              <button className="secondary-btn" type="button" onClick={copyOrderId}>
                <Copy className="h-4 w-4" /> Copy Order ID
              </button>
              <button className="secondary-btn" type="button" onClick={exportJson}>
                <ArrowDownToLine className="h-4 w-4" /> Export JSON
              </button>
            </div>

            <div className="rounded-xl border border-slate-700 bg-slate-900/40 p-4">
              <h3 className="mb-4 text-sm font-semibold uppercase tracking-wide text-slate-200">
                Version Timeline {showCommittedOnly ? "(Committed Only)" : "(All Versions)"}
              </h3>

              {displayedVersions.length === 0 ? (
                <p className="text-sm text-slate-300">
                  No versions available for the selected filter.
                </p>
              ) : (
                <ul className="space-y-3">
                  {displayedVersions.map((version, index) => {
                    const isLatest = Boolean(version.isLatestVersion);
                    const isSelected =
                      selectedVersionSummary?.orderVersionNumber === version.orderVersionNumber;

                    return (
                      <li
                        key={version.orderVersionNumber}
                        className={`relative rounded-xl border p-3 transition ${
                          isSelected
                            ? "border-blue-400/60 bg-blue-500/10"
                            : "border-slate-700 bg-slate-950/60 hover:border-slate-500"
                        }`}
                      >
                        {index !== displayedVersions.length - 1 ? (
                          <span className="absolute left-[13px] top-10 h-[calc(100%+12px)] w-px bg-slate-700" />
                        ) : null}

                        <button
                          type="button"
                          className="w-full text-left"
                          onClick={() => setSelectedVersionNumber(version.orderVersionNumber)}
                        >
                          <div className="flex items-center justify-between gap-3">
                            <div className="flex items-center gap-3">
                              <span
                                className={`mt-0.5 inline-block h-3 w-3 rounded-full ${
                                  version.orderStatus === "COMMITTED"
                                    ? "bg-green-400"
                                    : "bg-yellow-400"
                                }`}
                              />
                              <div>
                                <p className="font-semibold text-white">
                                  Version {version.orderVersionNumber}
                                  {isLatest ? (
                                    <span className="ml-2 rounded border border-blue-400/60 bg-blue-500/20 px-2 py-0.5 text-xs text-blue-200">
                                      Latest
                                    </span>
                                  ) : null}
                                </p>
                                <p className="text-xs text-slate-300">
                                  {version.changeDescription || "No change description"}
                                </p>
                              </div>
                            </div>
                            <div className="text-right">
                              <span
                                className={`rounded-full px-2.5 py-1 text-xs font-semibold ${
                                  version.orderStatus === "COMMITTED"
                                    ? "status-committed"
                                    : "status-wip"
                                }`}
                              >
                                {version.orderStatus}
                              </span>
                              <p className="mt-2 inline-flex items-center gap-1 text-xs text-slate-300">
                                <Clock3 className="h-3 w-3" /> {formatDateTime(version.timestamp)}
                              </p>
                            </div>
                          </div>
                        </button>
                      </li>
                    );
                  })}
                </ul>
              )}
            </div>
          </>
        ) : (
          <p className="text-sm text-slate-300">
            Search with an order ID to view immutable version timeline.
          </p>
        )}
      </article>

      <aside className="glass-card p-5">
        <h3 className="mb-3 text-lg font-semibold text-white">Selected Version Details</h3>

        {selectedVersionSummary ? (
          <div className="space-y-4">
            <div className="rounded-lg border border-slate-700 bg-slate-900/50 p-3 text-sm">
              <p>
                <span className="text-slate-400">Order:</span> {historyData.orderId}
              </p>
              <p>
                <span className="text-slate-400">Version:</span>{" "}
                {selectedVersionSummary.orderVersionNumber}
              </p>
              <p>
                <span className="text-slate-400">Status:</span>{" "}
                <span
                  className={`rounded px-2 py-0.5 text-xs ${
                    selectedVersionSummary.orderStatus === "COMMITTED"
                      ? "status-committed"
                      : "status-wip"
                  }`}
                >
                  {selectedVersionSummary.orderStatus}
                </span>
              </p>
              <p>
                <span className="text-slate-400">User:</span> {selectedVersionSummary.userName}
              </p>
            </div>

            <div className="rounded-lg border border-slate-700 bg-slate-900/50 p-3">
              <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-slate-300">
                Full Version Data
              </p>
              {loadingDetails ? (
                <p className="inline-flex items-center gap-2 text-sm text-slate-300">
                  <Loader2 className="h-4 w-4 animate-spin" /> Loading selected version payload...
                </p>
              ) : selectedVersionDetail?.data ? (
                <pre className="max-h-56 overflow-auto rounded-lg border border-slate-700 bg-slate-950/70 p-3 text-xs text-slate-200">
                  {JSON.stringify(selectedVersionDetail.data, null, 2)}
                </pre>
              ) : (
                <p className="inline-flex items-center gap-2 text-sm text-slate-400">
                  <Database className="h-4 w-4" /> No payload available for this version.
                </p>
              )}
            </div>

            <div className="rounded-lg border border-slate-700 bg-slate-900/50 p-3">
              <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-slate-300">
                Diff Against Previous Version
              </p>
              {diffRows.length === 0 ? (
                <p className="text-sm text-slate-400">No field changes detected in payload snapshot.</p>
              ) : (
                <ul className="space-y-2 text-xs">
                  {diffRows.map((row) => (
                    <li key={row.key} className="rounded border border-slate-700 bg-slate-950/60 p-2">
                      <p className="mb-1 font-semibold text-blue-200">{row.key}</p>
                      <p className="text-red-200">Prev: {JSON.stringify(row.previous)}</p>
                      <p className="text-green-200">Curr: {JSON.stringify(row.current)}</p>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </div>
        ) : (
          <p className="text-sm text-slate-300">
            Select a version from timeline to inspect full details and diff view.
          </p>
        )}
      </aside>
    </section>
  );
}
