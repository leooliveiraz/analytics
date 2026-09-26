import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

export function Layout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  async function handleLogout() {
    await logout();
    navigate("/login");
  }

  return (
    <div className="app">
      <header className="topbar">
        <NavLink to="/projects" className="brand">
          ◧ Analytics
        </NavLink>
        <div className="topbar-right">
          <span className="muted">{user?.email}</span>
          <button type="button" className="btn btn-ghost" onClick={handleLogout}>
            Sair
          </button>
        </div>
      </header>
      <main className="content">
        <Outlet />
      </main>
    </div>
  );
}
