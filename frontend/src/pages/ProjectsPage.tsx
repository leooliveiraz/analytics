import { useState, type FormEvent } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { api, ApiError } from "../api/client";
import type { Project } from "../api/types";

export function ProjectsPage() {
  const queryClient = useQueryClient();
  const [name, setName] = useState("");
  const [domain, setDomain] = useState("");
  const [error, setError] = useState<string | null>(null);

  const projectsQuery = useQuery({
    queryKey: ["projects"],
    queryFn: () => api<Project[]>("/api/v1/projects"),
  });

  const createMutation = useMutation({
    mutationFn: () =>
      api<Project>("/api/v1/projects", {
        method: "POST",
        body: JSON.stringify({ name, domain: domain || null }),
      }),
    onSuccess: async () => {
      setName("");
      setDomain("");
      setError(null);
      await queryClient.invalidateQueries({ queryKey: ["projects"] });
    },
    onError: (err) => setError(err instanceof ApiError ? err.message : "Falha ao criar projeto"),
  });

  const deleteMutation = useMutation({
    mutationFn: (projectId: string) =>
      api<void>(`/api/v1/projects/${projectId}`, { method: "DELETE" }),
    onSuccess: async () => {
      setError(null);
      await queryClient.invalidateQueries({ queryKey: ["projects"] });
    },
    onError: (err) => setError(err instanceof ApiError ? err.message : "Falha ao excluir projeto"),
  });

  function handleCreate(event: FormEvent) {
    event.preventDefault();
    createMutation.mutate();
  }

  function handleDelete(project: Project) {
    const confirmed = window.confirm(
      `Excluir o projeto "${project.name}" e TODOS os dados (eventos, sessões, chaves e membros)?\n\nEsta ação não pode ser desfeita.`,
    );
    if (confirmed) {
      deleteMutation.mutate(project.id);
    }
  }

  const projects = projectsQuery.data ?? [];

  return (
    <div className="stack">
      <div className="row spread">
        <h1 style={{ fontSize: 20, margin: 0 }}>Projetos</h1>
      </div>

      <form className="panel row" onSubmit={handleCreate} style={{ alignItems: "flex-end", gap: 12 }}>
        <div className="field" style={{ flex: "1 1 220px", marginBottom: 0 }}>
          <label htmlFor="project-name">Nome</label>
          <input id="project-name" value={name} onChange={(e) => setName(e.target.value)} required />
        </div>
        <div className="field" style={{ flex: "1 1 220px", marginBottom: 0 }}>
          <label htmlFor="project-domain">Domínio (opcional)</label>
          <input
            id="project-domain"
            value={domain}
            onChange={(e) => setDomain(e.target.value)}
            placeholder="exemplo.com"
          />
        </div>
        <button className="btn btn-primary" type="submit" disabled={createMutation.isPending}>
          {createMutation.isPending ? "Criando..." : "Novo projeto"}
        </button>
      </form>
      {error && <p className="error">{error}</p>}

      {projectsQuery.isLoading && <p className="muted">Carregando projetos...</p>}
      {!projectsQuery.isLoading && projects.length === 0 && (
        <p className="muted">Nenhum projeto ainda. Crie o primeiro acima.</p>
      )}

      <div className="grid">
        {projects.map((project) => (
          <div key={project.id} className="project-card">
            <Link to={`/projects/${project.id}`} style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontWeight: 600 }}>{project.name}</div>
              <div className="muted" style={{ fontSize: 13 }}>
                {project.domain ?? "sem domínio"} · {project.timezone}
              </div>
            </Link>
            <span className="badge">{project.role}</span>
            {project.role === "OWNER" && (
              <button
                className="btn btn-sm btn-danger"
                type="button"
                title="Excluir projeto"
                onClick={() => handleDelete(project)}
                disabled={deleteMutation.isPending}
              >
                Excluir
              </button>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}
