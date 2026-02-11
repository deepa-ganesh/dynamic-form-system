import { useEffect, useState } from "react";
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
  const [isCompact, setIsCompact] = useState(false);

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

  return (
    <div className="relative min-h-screen text-slate-100">
      <div className="pointer-events-none fixed inset-0 -z-10 bg-gradient-to-br from-gray-900 via-blue-900 to-gray-900" />
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
                  className={`rounded-xl border border-slate-600 bg-slate-900/40 text-slate-300 transition-all duration-300 ${
                    isCompact ? "px-3 py-1.5 text-[11px]" : "px-4 py-2 text-xs"
                  }`}
                >
                  Backend:{" "}
                  <span className="font-semibold text-green-300">http://localhost:8080/api</span>
                </div>
              </div>
            </header>

            <nav
              className={`glass-card mt-3 grid gap-3 transition-all duration-300 md:grid-cols-3 ${
                isCompact ? "p-2" : "p-3"
              }`}
            >
              {navItems.map((item) => {
                const Icon = item.icon;
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
