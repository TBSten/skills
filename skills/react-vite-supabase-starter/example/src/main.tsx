import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import "./index.css";
import { setupGlobalErrorHandlers } from "./lib/setup-error-handlers";
import App from "./App.tsx";

setupGlobalErrorHandlers();

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
