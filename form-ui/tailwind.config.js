/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,jsx}"],
  theme: {
    extend: {
      boxShadow: {
        glass: "0 10px 35px rgba(0, 0, 0, 0.35)"
      },
      animation: {
        "fade-in": "fadeIn 250ms ease-out",
        "slide-up": "slideUp 300ms ease-out",
        pulseGlow: "pulseGlow 1500ms ease-in-out infinite"
      },
      keyframes: {
        fadeIn: {
          "0%": { opacity: "0" },
          "100%": { opacity: "1" }
        },
        slideUp: {
          "0%": { transform: "translateY(8px)", opacity: "0" },
          "100%": { transform: "translateY(0)", opacity: "1" }
        },
        pulseGlow: {
          "0%, 100%": { boxShadow: "0 0 0 rgba(59,130,246,0)" },
          "50%": { boxShadow: "0 0 24px rgba(59,130,246,0.35)" }
        }
      }
    }
  },
  plugins: []
};
