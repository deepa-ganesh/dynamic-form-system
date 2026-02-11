import { NavLink, Outlet } from "react-router-dom";
import { ClipboardList, FileDiff, History, Layers3 } from "lucide-react";

const navItems = [
  {
    to: "/orders",
    label: "Order Form",
    icon: ClipboardList,
    description: "Create WIP and committed versions"
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
  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-900 via-blue-900 to-gray-900 text-slate-100">
      <div className="mx-auto max-w-7xl px-4 pb-10 pt-8 sm:px-6 lg:px-8">
        <header className="glass-card mb-6 p-5">
          <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
            <div>
              <h1 className="inline-flex items-center gap-2 text-2xl font-semibold tracking-tight text-white sm:text-3xl">
                <FileDiff className="h-6 w-6 text-blue-300" /> Dynamic Versioned Form System
              </h1>
            </div>
            <div className="rounded-xl border border-slate-600 bg-slate-900/40 px-4 py-2 text-xs text-slate-300">
              Backend: <span className="font-semibold text-green-300">http://localhost:8080/api</span>
            </div>
          </div>
        </header>

        <nav className="glass-card mb-6 grid gap-3 p-3 md:grid-cols-3">
          {navItems.map((item) => {
            const Icon = item.icon;
            return (
              <NavLink
                key={item.to}
                to={item.to}
                className={({ isActive }) =>
                  `rounded-xl border px-4 py-3 transition ${
                    isActive
                      ? "border-blue-400/60 bg-blue-500/20"
                      : "border-slate-600 bg-slate-900/30 hover:border-slate-400 hover:bg-slate-800/60"
                  }`
                }
              >
                <div className="mb-1 flex items-center gap-2 text-sm font-semibold text-white">
                  <Icon className="h-4 w-4" /> {item.label}
                </div>
                <p className="text-xs text-slate-300">{item.description}</p>
              </NavLink>
            );
          })}
        </nav>

        <main className="animate-fade-in">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
