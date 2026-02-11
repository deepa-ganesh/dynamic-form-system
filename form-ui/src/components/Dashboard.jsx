import { useEffect, useState } from "react";
import { NavLink, Outlet, useLocation, useNavigate } from "react-router-dom";
import { ClipboardList, FileDiff, History, Layers3, ListOrdered } from "lucide-react";
import { DEMO_ROLES, getDemoRole, setDemoRole } from "../services/api";

const navItems = [
  {
    to: "/orders",
    label: "Order Form",
    icon: ClipboardList,
    description: "Create WIP and committed versions"
  },
  {
    to: "/orders-list",
    label: "All Orders",
    icon: ListOrdered,
    description: "Browse latest snapshot of every order"
  },
  {
    to: "/history",
    label: "Version History",
    icon: History,
    description: "Track immutable version timeline"
  },
  {
    to: "/schemas",
    label: "Schema Manager",
    icon: Layers3,
    description: "Create and activate form schemas"
  }
];

export default function Dashboard() {
  const [isCompact, setIsCompact] = useState(false);
  const [demoRole, setDemoRoleState] = useState(DEMO_ROLES.ADMIN);
  const location = useLocation();
  const navigate = useNavigate();

  useEffect(() => {
    const COMPACT_ON_SCROLL_Y = 72;
    const EXPAND_ON_SCROLL_Y = 24;

    const handleScroll = () => {
      setIsCompact((current) => {
        if (!current && window.scrollY >= COMPACT_ON_SCROLL_Y) {
          return true;
        }
        if (current && window.scrollY <= EXPAND_ON_SCROLL_Y) {
          return false;
        }
        return current;
      });
    };

    handleScroll();
    window.addEventListener("scroll", handleScroll, { passive: true });

    return () => {
      window.removeEventListener("scroll", handleScroll);
    };
  }, []);

  useEffect(() => {
    setDemoRoleState(getDemoRole());

    const onRoleChange = (event) => {
      const nextRole = event?.detail || getDemoRole();
      setDemoRoleState(nextRole);
    };

    window.addEventListener("dynamic-form-demo-role-change", onRoleChange);
    return () => {
      window.removeEventListener("dynamic-form-demo-role-change", onRoleChange);
    };
  }, []);

  useEffect(() => {
    if (demoRole === DEMO_ROLES.USER && location.pathname.startsWith("/schemas")) {
      navigate("/orders", { replace: true });
    }
  }, [demoRole, location.pathname, navigate]);

  return (
    <div className="relative min-h-screen text-slate-100">
      <div className="pointer-events-none fixed inset-0 -z-10 bg-gradient-to-br from-slate-950 via-blue-950 to-black" />
      <div className="pointer-events-none fixed inset-0 -z-10 bg-[radial-gradient(circle_at_30%_20%,rgba(15,23,42,0.45),transparent_55%)]" />
      <div className="mx-auto w-full max-w-[110rem] px-4 pb-10 pt-8 sm:px-6 lg:px-10">
        <div className="sticky top-0 z-40 mb-6">
          <div className={`transition-all duration-300 ${isCompact ? "pt-2" : "pt-0"}`}>
            <header className={`glass-card transition-all duration-300 ${isCompact ? "p-3" : "p-5"}`}>
              <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
                <div>
                  <h1
                    className={`inline-flex items-center gap-2 font-semibold tracking-tight text-white transition-all duration-300 ${
                      isCompact ? "text-lg sm:text-xl" : "text-2xl sm:text-3xl"
                    }`}
                  >
                    <FileDiff className={`${isCompact ? "h-5 w-5" : "h-6 w-6"} text-blue-300 transition-all duration-300`} />
                    Dynamic Versioned Form System
                  </h1>
                </div>
                <div
                  className={`flex flex-wrap items-center gap-2 rounded-xl border border-slate-600 bg-slate-900/40 text-slate-300 transition-all duration-300 ${
                    isCompact ? "px-3 py-1.5 text-[11px]" : "px-4 py-2 text-xs"
                  }`}
                >
                  <span>
                    Backend:{" "}
                    <span className="font-semibold text-green-300">http://localhost:8080/api</span>
                  </span>
                  <span className="mx-1 hidden text-slate-500 sm:inline">|</span>
                  <div className="inline-flex items-center rounded-lg border border-slate-600 bg-slate-950/50 p-0.5">
                    <button
                      type="button"
                      className={`rounded-md px-2 py-1 font-semibold transition ${
                        demoRole === DEMO_ROLES.USER
                          ? "bg-blue-600 text-white"
                          : "text-slate-300 hover:bg-slate-800/80"
                      }`}
                      onClick={() => setDemoRole(DEMO_ROLES.USER)}
                    >
                      User
                    </button>
                    <button
                      type="button"
                      className={`rounded-md px-2 py-1 font-semibold transition ${
                        demoRole === DEMO_ROLES.ADMIN
                          ? "bg-blue-600 text-white"
                          : "text-slate-300 hover:bg-slate-800/80"
                      }`}
                      onClick={() => setDemoRole(DEMO_ROLES.ADMIN)}
                    >
                      Admin
                    </button>
                  </div>
                </div>
              </div>
            </header>

            <nav
              className={`glass-card mt-3 grid gap-3 transition-all duration-300 md:grid-cols-2 xl:grid-cols-4 ${
                isCompact ? "p-2" : "p-3"
              }`}
            >
              {navItems.map((item) => {
                const Icon = item.icon;
                const isSchemaTab = item.to === "/schemas";
                const isSchemaDisabled = isSchemaTab && demoRole !== DEMO_ROLES.ADMIN;

                if (isSchemaDisabled) {
                  return (
                    <div
                      key={item.to}
                      className={`rounded-xl border border-slate-700 bg-slate-900/20 text-slate-400 transition-all duration-300 ${
                        isCompact ? "px-3 py-2" : "px-4 py-3"
                      } cursor-not-allowed opacity-55`}
                      title="Switch to Admin role to access Schema Manager"
                    >
                      <div
                        className={`flex items-center gap-2 font-semibold ${
                          isCompact ? "text-xs" : "mb-1 text-sm"
                        }`}
                      >
                        <Icon className={isCompact ? "h-3.5 w-3.5" : "h-4 w-4"} /> {item.label}
                      </div>
                      {!isCompact ? (
                        <p className="text-xs text-slate-400">Admin role required</p>
                      ) : null}
                    </div>
                  );
                }

                return (
                  <NavLink
                    key={item.to}
                    to={item.to}
                    className={({ isActive }) =>
                      `rounded-xl border transition-all duration-300 ${
                        isCompact ? "px-3 py-2" : "px-4 py-3"
                      } ${
                        isActive
                          ? "border-blue-400/60 bg-blue-500/20"
                          : "border-slate-600 bg-slate-900/30 hover:border-slate-400 hover:bg-slate-800/60"
                      }`
                    }
                  >
                    <div className={`flex items-center gap-2 font-semibold text-white ${isCompact ? "text-xs" : "mb-1 text-sm"}`}>
                      <Icon className={isCompact ? "h-3.5 w-3.5" : "h-4 w-4"} /> {item.label}
                    </div>
                    {!isCompact ? (
                      <p className="text-xs text-slate-300">{item.description}</p>
                    ) : null}
                  </NavLink>
                );
              })}
            </nav>
          </div>
        </div>

        <main className="animate-fade-in">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
