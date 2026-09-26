import { NavLink } from "react-router-dom";

interface ProjectNavProps {
  projectId: string;
}

export function ProjectNav({ projectId }: ProjectNavProps) {
  return (
    <nav className="subnav">
      <NavLink end to={`/projects/${projectId}`}>
        Visão geral
      </NavLink>
      <NavLink to={`/projects/${projectId}/events`}>Eventos</NavLink>
      <NavLink to={`/projects/${projectId}/settings`}>Configurações</NavLink>
    </nav>
  );
}
