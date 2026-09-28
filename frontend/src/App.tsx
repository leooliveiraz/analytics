import { Navigate, Route, Routes } from "react-router-dom";
import { ProtectedRoute } from "./auth/ProtectedRoute";
import { Layout } from "./components/Layout";
import { LoginPage } from "./pages/LoginPage";
import { RegisterPage } from "./pages/RegisterPage";
import { ProjectsPage } from "./pages/ProjectsPage";
import { DashboardPage } from "./pages/DashboardPage";
import { PagesPage } from "./pages/PagesPage";
import { ElementsPage } from "./pages/ElementsPage";
import { SectionsPage } from "./pages/SectionsPage";
import { ImagesPage } from "./pages/ImagesPage";
import { HeatmapPage } from "./pages/HeatmapPage";
import { GeoPage } from "./pages/GeoPage";
import { EventsPage } from "./pages/EventsPage";
import { SessionsPage } from "./pages/SessionsPage";
import { SessionDetailPage } from "./pages/SessionDetailPage";
import { SettingsPage } from "./pages/SettingsPage";

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route element={<ProtectedRoute />}>
        <Route element={<Layout />}>
          <Route path="/" element={<Navigate to="/projects" replace />} />
          <Route path="/projects" element={<ProjectsPage />} />
          <Route path="/projects/:projectId" element={<DashboardPage />} />
          <Route path="/projects/:projectId/pages" element={<PagesPage />} />
          <Route path="/projects/:projectId/elements" element={<ElementsPage />} />
          <Route path="/projects/:projectId/sections" element={<SectionsPage />} />
          <Route path="/projects/:projectId/images" element={<ImagesPage />} />
          <Route path="/projects/:projectId/heatmap" element={<HeatmapPage />} />
          <Route path="/projects/:projectId/geo" element={<GeoPage />} />
          <Route path="/projects/:projectId/events" element={<EventsPage />} />
          <Route path="/projects/:projectId/sessions" element={<SessionsPage />} />
          <Route path="/projects/:projectId/sessions/:sessionId" element={<SessionDetailPage />} />
          <Route path="/projects/:projectId/settings" element={<SettingsPage />} />
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/projects" replace />} />
    </Routes>
  );
}
