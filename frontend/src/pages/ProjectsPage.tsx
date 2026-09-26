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

  function handleCreate(event: FormEvent) {
    event.preventDefault();
    createMutation.mutate();
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
          <Link key={project.id} className="project-card" to={`/projects/${project.id}`}>
            <div>
              <div style={{ fontWeight: 600 }}>{project.name}</div>
              <div className="muted" style={{ fontSize: 13 }}>
                {project.domain ?? "sem domínio"} · {project.timezone}
              </div>
            </div>
            <span className="badge">{project.role}</span>
          </Link>
        ))}
      </div>
    </div>
  );
}
