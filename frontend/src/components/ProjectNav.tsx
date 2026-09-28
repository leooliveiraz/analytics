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
      <NavLink to={`/projects/${projectId}/pages`}>Páginas</NavLink>
      <NavLink to={`/projects/${projectId}/elements`}>Elementos</NavLink>
      <NavLink to={`/projects/${projectId}/sections`}>Seções</NavLink>
      <NavLink to={`/projects/${projectId}/images`}>Imagens</NavLink>
      <NavLink to={`/projects/${projectId}/heatmap`}>Heatmap</NavLink>
      <NavLink to={`/projects/${projectId}/geo`}>Mapa</NavLink>
      <NavLink to={`/projects/${projectId}/events`}>Eventos</NavLink>
      <NavLink to={`/projects/${projectId}/sessions`}>Sessões</NavLink>
      <NavLink to={`/projects/${projectId}/settings`}>Configurações</NavLink>
    </nav>
  );
}
